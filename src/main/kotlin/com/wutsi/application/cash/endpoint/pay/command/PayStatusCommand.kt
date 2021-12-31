package com.wutsi.application.cash.endpoint.pay.command

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.application.cash.endpoint.AbstractCommand
import com.wutsi.application.cash.exception.TransactionException
import com.wutsi.application.cash.service.URLBuilder
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.Dialog
import com.wutsi.flutter.sdui.enums.ActionType
import com.wutsi.flutter.sdui.enums.DialogType
import com.wutsi.platform.payment.core.Status
import com.wutsi.platform.payment.dto.SearchTransactionRequest
import com.wutsi.platform.payment.dto.TransactionSummary
import feign.FeignException
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/commands/pay/status")
class PayStatusCommand(
    private val urlBuilder: URLBuilder,
    private val mapper: ObjectMapper
) : AbstractCommand() {
    companion object {
        const val DELAY = 15000L
        const val MAX_RETRIES = 7 // 7 retries of 15s => Max 2 min
    }

    @PostMapping
    fun index(
        @RequestParam(name = "payment-request-id") paymentRequestId: String,
        @RequestParam amount: Double
    ): Action {
        logger.add("payment_request_id", paymentRequestId)

        try {
            var retries = 0
            var tx: TransactionSummary? = null
            while (retries < MAX_RETRIES) {
                tx = getStatus(paymentRequestId)
                if (tx == null) {
                    Thread.sleep(DELAY)
                    retries++
                } else {
                    break
                }
            }

            logger.add("retries", retries)
            return if (tx != null && tx.status == Status.SUCCESSFUL.name)
                Action(
                    type = ActionType.Route,
                    url = urlBuilder.build("pay/success?amount=$amount")
                )
            else
                Action(
                    type = ActionType.Prompt,
                    prompt = Dialog(
                        type = DialogType.Error,
                        title = getText("prompt.error.title"),
                        message = getErrorMessage(tx?.errorCode)
                    ).toWidget()
                )
        } catch (ex: FeignException) {
            throw TransactionException.of(mapper, ex)
        }
    }

    private fun getStatus(paymentRequestId: String): TransactionSummary? =
        paymentApi.searchTransaction(
            request = SearchTransactionRequest(
                paymentRequestId = paymentRequestId
            )
        ).transactions.firstOrNull()
}
