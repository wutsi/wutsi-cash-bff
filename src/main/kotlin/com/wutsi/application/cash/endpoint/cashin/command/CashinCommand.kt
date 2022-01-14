package com.wutsi.application.cash.endpoint.cashin.command

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.application.cash.endpoint.AbstractCommand
import com.wutsi.application.cash.exception.TransactionException
import com.wutsi.application.shared.service.TenantProvider
import com.wutsi.application.shared.service.URLBuilder
import com.wutsi.flutter.sdui.Action
import com.wutsi.platform.payment.core.Status
import com.wutsi.platform.payment.dto.CreateCashinRequest
import feign.FeignException
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/commands/cashin")
class CashinCommand(
    private val tenantProvider: TenantProvider,
    private val urlBuilder: URLBuilder,
    private val objectMapper: ObjectMapper,
) : AbstractCommand() {
    @PostMapping
    fun index(@RequestParam amount: Double, @RequestParam("payment-token") paymentToken: String): Action {
        logger.add("amount", amount)
        logger.add("payment_token", paymentToken)

        // Cashin
        try {
            val response = paymentApi.createCashin(
                CreateCashinRequest(
                    paymentMethodToken = paymentToken,
                    amount = amount,
                    currency = tenantProvider.get().currency
                )
            )
            logger.add("transaction_id", response.id)
            logger.add("transaction_status", response.status)

            return gotoUrl(
                url = if (response.status == Status.SUCCESSFUL.name)
                    urlBuilder.build("cashin/success?amount=$amount")
                else
                    urlBuilder.build("cashin/pending")
            )
        } catch (ex: FeignException) {
            throw TransactionException.of(objectMapper, ex)
        }
    }
}
