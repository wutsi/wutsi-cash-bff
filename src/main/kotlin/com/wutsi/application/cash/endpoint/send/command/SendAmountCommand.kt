package com.wutsi.application.cash.endpoint.send.command

import com.wutsi.application.cash.endpoint.AbstractCommand
import com.wutsi.application.cash.endpoint.send.dto.SendAmountRequest
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.Dialog
import com.wutsi.flutter.sdui.enums.ActionType.Prompt
import com.wutsi.flutter.sdui.enums.DialogType
import com.wutsi.platform.payment.core.Money
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
@RequestMapping("/commands/send/amount")
class SendAmountCommand : AbstractCommand() {
    @PostMapping
    fun index(
        @RequestParam(name = "recipient-id", required = false) recipientId: Long? = null,
        @RequestBody @Valid request: SendAmountRequest
    ): Action {
        logger.add("amount", request.amount)

        // Validate
        val action = validate(request)
        if (action != null)
            return action

        // Goto next page
        return if (recipientId == null)
            gotoUrl(
                url = urlBuilder.build("send/recipient?amount=${request.amount}")
            )
        else
            gotoUrl(
                url = urlBuilder.build("send/confirm?amount=${request.amount}&recipient-id=$recipientId")
            )
    }

    private fun validate(request: SendAmountRequest): Action? {
        if (request.amount == 0.0)
            return showError(
                message = getText("prompt.error.amount-required"),
            )

        val balance = getBalance()
        if (request.amount > balance.value)
            return Action(
                type = Prompt,
                prompt = Dialog(
                    type = DialogType.Error,
                    message = getText("prompt.error.transaction-failed.NOT_ENOUGH_FUNDS"),
                    title = getText("prompt.error.title"),
                ).toWidget()
            )

        return null
    }

    private fun getBalance(): Money {
        val balance = paymentApi.getBalance(securityContext.currentAccountId()).balance
        return Money(balance.amount, balance.currency)
    }
}
