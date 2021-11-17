package com.wutsi.application.cash.endpoint.command

import com.wutsi.application.cash.dto.CashinRequest
import com.wutsi.application.cash.endpoint.AbstractCommand
import com.wutsi.application.cash.exception.TransactionException
import com.wutsi.application.cash.service.TenantProvider
import com.wutsi.application.cash.service.URLBuilder
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.enums.ActionType.Route
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.platform.payment.WutsiPaymentApi
import com.wutsi.platform.payment.core.Status
import com.wutsi.platform.payment.dto.CreateCashinRequest
import feign.FeignException
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
@RequestMapping("/commands/cashin")
class CashinCommand(
    private val paymentApi: WutsiPaymentApi,
    private val tenantProvider: TenantProvider,
    private val logger: KVLogger,
    private val urlBuilder: URLBuilder,
) : AbstractCommand() {
    @PostMapping
    fun index(@RequestBody @Valid request: CashinRequest): Action {
        logger.add("amount", request.amount)
        logger.add("payment_token", request.paymentToken)

        try {
            val response = paymentApi.createCashin(
                CreateCashinRequest(
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
                    urlBuilder.build("cashin/success?amount=${request.amount}")
                else
                    urlBuilder.build("cashin/pending")
            )
        } catch (ex: FeignException) {
            throw TransactionException(ex)
        }
    }
}
