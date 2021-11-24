package com.wutsi.application.cash.endpoint.cashout.command

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.application.cash.endpoint.AbstractCommand
import com.wutsi.application.cash.endpoint.cashout.dto.CashoutRequest
import com.wutsi.application.cash.exception.TransactionException
import com.wutsi.application.cash.service.TenantProvider
import com.wutsi.application.cash.service.URLBuilder
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.Dialog
import com.wutsi.flutter.sdui.enums.ActionType.Prompt
import com.wutsi.flutter.sdui.enums.ActionType.Route
import com.wutsi.flutter.sdui.enums.DialogType.Error
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.platform.payment.WutsiPaymentApi
import com.wutsi.platform.payment.core.Status
import com.wutsi.platform.payment.dto.CreateCashoutRequest
import com.wutsi.platform.tenant.dto.Tenant
import feign.FeignException
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.text.DecimalFormat
import javax.validation.Valid

@RestController
@RequestMapping("/commands/cashout")
class CashoutCommand(
    private val paymentApi: WutsiPaymentApi,
    private val tenantProvider: TenantProvider,
    private val logger: KVLogger,
    private val urlBuilder: URLBuilder,
    private val objectMapper: ObjectMapper,
) : AbstractCommand() {
    @PostMapping
    fun index(@RequestBody @Valid request: CashoutRequest): Action {
        logger.add("amount", request.amount)
        logger.add("payment_token", request.paymentToken)

        // Validate
        val tenant = tenantProvider.get()
        val error = validate(request, tenant)
        if (error != null)
            return error

        // Cashout
        try {
            val response = paymentApi.createCashout(
                CreateCashoutRequest(
                    paymentMethodToken = request.paymentToken,
                    amount = request.amount,
                    currency = tenantProvider.get().currency
                )
            )
            logger.add("transaction_id", response.id)
            logger.add("transaction_status", response.status)

            return Action(
                type = Route,
                url = if (response.status == Status.SUCCESSFUL.name)
                    urlBuilder.build("cashout/success?amount=${request.amount}")
                else
                    urlBuilder.build("cashout/pending")
            )
        } catch (ex: FeignException) {
            throw TransactionException.of(objectMapper, ex)
        }
    }

    private fun validate(request: CashoutRequest, tenant: Tenant): Action? {
        if (request.amount == 0.0)
            return Action(
                type = Prompt,
                prompt = Dialog(
                    type = Error,
                    message = getText("prompt.error.amount-required")
                )
            )

        if (request.amount < tenant.limits.minCashout) {
            val amountText = DecimalFormat(tenant.monetaryFormat).format(tenant.limits.minCashin)
            return Action(
                type = Prompt,
                prompt = Dialog(
                    type = Error,
                    message = getText("prompt.error.min-cashout", arrayOf(amountText))
                )
            )
        }
        return null
    }
}
