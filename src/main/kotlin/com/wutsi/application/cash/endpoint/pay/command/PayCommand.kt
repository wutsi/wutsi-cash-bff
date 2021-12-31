package com.wutsi.application.cash.endpoint.pay.command

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.application.cash.endpoint.AbstractCommand
import com.wutsi.application.cash.exception.TransactionException
import com.wutsi.application.cash.service.URLBuilder
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.enums.ActionType
import com.wutsi.platform.payment.dto.CreatePaymentRequest
import feign.FeignException
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/commands/pay")
class PayCommand(
    private val urlBuilder: URLBuilder,
    private val mapper: ObjectMapper
) : AbstractCommand() {
    @PostMapping
    fun index(
        @RequestParam(name = "payment-request-id") paymentRequestId: String,
        @RequestParam amount: Double
    ): Action {
        logger.add("payment_request_id", paymentRequestId)
        logger.add("amount", amount)

        try {
            val response = paymentApi.createPayment(
                CreatePaymentRequest(
                    requestId = paymentRequestId
                )
            )

            logger.add("transaction_id", response.id)
            logger.add("transaction_status", response.status)
            return Action(
                type = ActionType.Route,
                url = urlBuilder.build("pay/success?amount=$amount")
            )
        } catch (ex: FeignException) {
            throw TransactionException.of(mapper, ex)
        }
    }
}
