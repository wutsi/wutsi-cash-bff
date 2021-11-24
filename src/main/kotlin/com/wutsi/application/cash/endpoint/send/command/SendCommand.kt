package com.wutsi.application.cash.endpoint.send.command

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.application.cash.endpoint.AbstractCommand
import com.wutsi.application.cash.exception.TransactionException
import com.wutsi.application.cash.service.TenantProvider
import com.wutsi.application.cash.service.URLBuilder
import com.wutsi.application.cash.service.UserProvider
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.Dialog
import com.wutsi.flutter.sdui.enums.ActionType.Prompt
import com.wutsi.flutter.sdui.enums.ActionType.Route
import com.wutsi.flutter.sdui.enums.DialogType.Error
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.platform.payment.WutsiPaymentApi
import com.wutsi.platform.payment.dto.CreateTransferRequest
import feign.FeignException
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/commands/send")
class SendCommand(
    private val logger: KVLogger,
    private val paymentApi: WutsiPaymentApi,
    private val userProvider: UserProvider,
    private val tenantProvider: TenantProvider,
    private val urlBuilder: URLBuilder,
    private val objectMapper: ObjectMapper,
) : AbstractCommand() {
    @PostMapping
    fun index(
        @RequestParam amount: Double,
        @RequestParam(name = "recipient-id") recipientId: Long,
        @RequestParam(name = "recipient-name") recipientName: String,
    ): Action {
        // Validate
        val error = validate(amount, recipientId)
        if (error != null)
            return error

        // Verify the password
        val tenant = tenantProvider.get()
        try {
            // Perform the transfer
            val response = paymentApi.createTransfer(
                CreateTransferRequest(
                    recipientId = recipientId,
                    amount = amount,
                    currency = tenant.currency
                )
            )
            logger.add("transaction_id", response.id)
            logger.add("transaction_status", response.status)

            return Action(
                type = Route,
                url = urlBuilder.build(
                    "send/success?amount=$amount&recipient-name=" + encodeURLParam(recipientName)
                )
            )
        } catch (ex: FeignException) {
            throw TransactionException.of(objectMapper, ex)
        }
    }

    private fun validate(amount: Double, recipientId: Long): Action? {
        if (amount == 0.0)
            return Action(
                type = Prompt,
                prompt = Dialog(
                    type = Error,
                    message = getText("prompt.error.amount-required")
                )
            )

        if (recipientId == userProvider.id())
            return Action(
                type = Prompt,
                prompt = Dialog(
                    type = Error,
                    message = getText("prompt.error.self-transfer")
                )
            )

        return null
    }
}
