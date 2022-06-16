package com.wutsi.application.cash.endpoint.cashout.command

import com.wutsi.application.cash.endpoint.AbstractCommand
import com.wutsi.application.shared.service.TenantProvider
import com.wutsi.flutter.sdui.Action
import com.wutsi.platform.payment.core.Status
import com.wutsi.platform.payment.dto.CreateCashoutRequest
import com.wutsi.platform.payment.dto.CreateCashoutResponse
import feign.FeignException
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/commands/cashout")
class CashoutCommand(
    private val tenantProvider: TenantProvider,
) : AbstractCommand() {
    @PostMapping
    fun index(
        @RequestParam amount: Double,
        @RequestParam("payment-token") paymentToken: String,
        @RequestParam(name = "idempotency-key") idempotencyKey: String
    ): Action {
        logger.add("amount", amount)
        logger.add("payment_token", paymentToken)

        // Cashout
        try {
            val response = cashout(amount, paymentToken, idempotencyKey)
            logger.add("transaction_id", response.id)
            logger.add("transaction_status", response.status)

            return if (response.status == Status.SUCCESSFUL.name)
                gotoUrl(urlBuilder.build("transaction/success?transaction-id=${response.id}"))
            else
                gotoUrl(urlBuilder.build("transaction/processing?transaction-id=${response.id}"))
        } catch (ex: FeignException) {
            logger.setException(ex)
            val error = getErrorText(ex)
            return gotoUrl(
                url = urlBuilder.build("transaction/error?type=CASHOUT&error=" + encodeURLParam(error))
            )
        }
    }

    private fun cashout(amount: Double, paymentToken: String, idempotencyKey: String): CreateCashoutResponse =
        paymentApi.createCashout(
            CreateCashoutRequest(
                paymentMethodToken = paymentToken,
                amount = amount,
                currency = tenantProvider.get().currency,
                idempotencyKey = idempotencyKey
            )
        )
}
