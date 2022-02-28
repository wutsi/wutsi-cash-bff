package com.wutsi.application.cash.endpoint.send.command

import com.wutsi.application.cash.endpoint.AbstractCommand
import com.wutsi.application.cash.endpoint.send.dto.SendAmountRequest
import com.wutsi.application.shared.service.TogglesProvider
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.Button
import com.wutsi.flutter.sdui.Dialog
import com.wutsi.flutter.sdui.enums.ActionType.Prompt
import com.wutsi.flutter.sdui.enums.ActionType.Route
import com.wutsi.flutter.sdui.enums.ButtonType
import com.wutsi.flutter.sdui.enums.DialogType
import com.wutsi.platform.account.WutsiAccountApi
import com.wutsi.platform.payment.core.Money
import feign.FeignException
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
@RequestMapping("/commands/send/amount")
class SendAmountCommand(
    private val accountApi: WutsiAccountApi,
    private val togglesProvider: TogglesProvider,
) : AbstractCommand() {
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
        if (request.amount > balance.value) {
            if (togglesProvider.isAccountEnabled()) {
                val paymentMethods = accountApi.listPaymentMethods(securityContext.currentAccountId()).paymentMethods
                if (paymentMethods.isEmpty()) {
                    return Action(
                        type = Prompt,
                        prompt = Dialog(
                            type = DialogType.Error,
                            message = getText("prompt.error.transaction-failed.NO_ACCOUNT_LINKED"),
                            title = getText("prompt.error.title"),
                            actions = listOf(
                                Button(
                                    caption = getText("page.send.button.link-account"),
                                    action = Action(
                                        type = Route,
                                        url = urlBuilder.build(shellUrl, "settings/accounts/link/mobile")
                                    ),
                                    stretched = false
                                ),
                                Button(
                                    caption = getText("page.send.button.ok"),
                                    type = ButtonType.Text,
                                    stretched = false
                                )
                            )
                        ).toWidget()
                    )
                }
            }

            return Action(
                type = Prompt,
                prompt = Dialog(
                    type = DialogType.Error,
                    message = getText("prompt.error.transaction-failed.NOT_ENOUGH_FUNDS"),
                    title = getText("prompt.error.title"),
                    actions = listOf(
                        Button(
                            caption = getText("page.send.button.cashin"),
                            action = Action(
                                type = Route,
                                url = urlBuilder.build("cashin")
                            ),
                            stretched = false
                        ),
                        Button(
                            caption = getText("page.send.button.ok"),
                            type = ButtonType.Text,
                            stretched = false
                        )
                    )
                ).toWidget()
            )
        }

        return null
    }

    private fun getBalance(): Money {
        try {
            val balance = paymentApi.getBalance(securityContext.currentAccountId()).balance
            return Money(balance.amount, balance.currency)
        } catch (ex: FeignException.NotFound) {
            return Money(0.0, "")
        }
    }
}
