package com.wutsi.application.cash.endpoint.send.command

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.application.cash.endpoint.AbstractCommand
import com.wutsi.application.cash.exception.TransactionException
import com.wutsi.application.shared.service.URLBuilder
import com.wutsi.flutter.sdui.Action
import feign.FeignException
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/commands/send/approve")
class SendApproveCommand(
    private val urlBuilder: URLBuilder,
    private val objectMapper: ObjectMapper,
) : AbstractCommand() {
    @PostMapping
    fun index(
        @RequestParam(name = "transaction-id") transactionId: String,
    ): Action {
        try {
            paymentApi.approveTransaction(transactionId)
            return gotoUrl(
                url = urlBuilder.build("send/success?transaction-id=$transactionId")
            )
        } catch (ex: FeignException) {
            val e = TransactionException.of(objectMapper, ex)
            return gotoUrl(
                url = urlBuilder.build("send/success?error=${e.error}&transaction-id=$transactionId")
            )
        }
    }
}
