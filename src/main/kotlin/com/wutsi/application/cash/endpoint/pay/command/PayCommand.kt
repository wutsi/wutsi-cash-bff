package com.wutsi.application.cash.endpoint.pay.command

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.application.cash.endpoint.AbstractCommand
import com.wutsi.application.cash.exception.TransactionException
import com.wutsi.flutter.sdui.Action
import com.wutsi.platform.payment.dto.CreateTransferRequest
import feign.FeignException
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/commands/pay")
class PayCommand(
    private val mapper: ObjectMapper
) : AbstractCommand() {
    @PostMapping
    fun index(
        @RequestParam(name = "payment-request-id") paymentRequestId: String,
    ): Action {
        val payment = paymentApi.getPaymentRequest(paymentRequestId).paymentRequest
        try {
            val response = paymentApi.createTransfer(
                CreateTransferRequest(
                    paymentRequestId = payment.id,
                    recipientId = payment.accountId,
                    amount = payment.amount,
                    currency = payment.currency,
                    orderId = payment.orderId,
                )
            )
            logger.add("transaction_id", response.id)
            logger.add("transaction_status", response.status)
            return gotoUrl(
                url = urlBuilder.build("pay/success?payment-request-id=$paymentRequestId")
            )
        } catch (ex: FeignException) {
            val error = TransactionException.of(mapper, ex).error
            return gotoUrl(
                url = urlBuilder.build("pay/success?payment-request-id=$paymentRequestId&error=$error")
            )
        }
    }
}
