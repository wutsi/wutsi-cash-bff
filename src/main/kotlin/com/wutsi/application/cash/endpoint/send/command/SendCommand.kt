package com.wutsi.application.cash.endpoint.send.command

import com.wutsi.application.cash.endpoint.AbstractCommand
import com.wutsi.application.shared.service.TenantProvider
import com.wutsi.flutter.sdui.Action
import com.wutsi.platform.payment.core.Status
import com.wutsi.platform.payment.dto.CreateTransferRequest
import feign.FeignException
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/commands/send")
class SendCommand(
    private val tenantProvider: TenantProvider,
) : AbstractCommand() {
    @PostMapping
    fun index(
        @RequestParam amount: Double,
        @RequestParam(name = "recipient-id") recipientId: Long,
        @RequestParam(name = "idempotency-key") idempotencyKey: String
    ): Action {
        val tenant = tenantProvider.get()
        try {
            // Perform the transfer
            val response = paymentApi.createTransfer(
                CreateTransferRequest(
                    recipientId = recipientId,
                    amount = amount,
                    currency = tenant.currency,
                    idempotencyKey = idempotencyKey
                )
            )
            logger.add("transaction_id", response.id)
            logger.add("transaction_status", response.status)

            return if (Status.SUCCESSFUL.name == response.status)
                gotoUrl(
                    url = urlBuilder.build(
                        "send/success?transaction-id=${response.id}"
                    )
                )
            else
                gotoUrl(
                    url = urlBuilder.build(
                        "send/pending?transaction-id=${response.id}"
                    )
                )
        } catch (ex: FeignException) {
            logger.setException(ex)
            val error = getErrorText(ex)
            return gotoUrl(
                url = urlBuilder.build(
                    "send/success?amount=$amount&recipient-id=$recipientId&error=" + encodeURLParam(error)
                )
            )
        }
    }
}
