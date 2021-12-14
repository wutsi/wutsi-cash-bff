package com.wutsi.application.cash.endpoint.send.command

import com.wutsi.application.cash.endpoint.AbstractCommand
import com.wutsi.application.cash.endpoint.send.dto.SendAmountRequest
import com.wutsi.application.cash.service.SecurityManager
import com.wutsi.application.cash.service.URLBuilder
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.Button
import com.wutsi.flutter.sdui.Dialog
import com.wutsi.flutter.sdui.enums.ActionType.Prompt
import com.wutsi.flutter.sdui.enums.ActionType.Route
import com.wutsi.flutter.sdui.enums.ButtonType
import com.wutsi.flutter.sdui.enums.DialogType
import com.wutsi.platform.account.WutsiAccountApi
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.platform.payment.WutsiPaymentApi
import com.wutsi.platform.payment.core.Money
import feign.FeignException
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
@RequestMapping("/commands/send/amount")
class SendAmountCommand(
    private val logger: KVLogger,
    private val urlBuilder: URLBuilder,
    private val paymentApi: WutsiPaymentApi,
    private val accountApi: WutsiAccountApi,
    private val securityManager: SecurityManager,

    @Value("\${wutsi.application.shell-url}") private val shellUrl: String
) : AbstractCommand() {
    @PostMapping
    fun index(@RequestBody @Valid request: SendAmountRequest): Action {
        logger.add("amount", request.amount)

        // Validate
        val action = validate(request)
        if (action != null)
            return action

        // Goto next page
        return Action(
            type = Route,
            url = urlBuilder.build("send/recipient?amount=${request.amount}")
        )
    }

    private fun validate(request: SendAmountRequest): Action? {
        if (request.amount == 0.0)
            return Action(
                type = Prompt,
                prompt = Dialog(
                    type = DialogType.Error,
                    message = getText("prompt.error.amount-required"),
                    title = getText("prompt.error.title")
                ).toWidget()
            )

        val balance = getBalance()
        if (request.amount > balance.value) {
            val paymentMethods = accountApi.listPaymentMethods(securityManager.currentUserId()).paymentMethods
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
            } else {
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
        }

        return null
    }

    private fun getBalance(): Money {
        try {
            val balance = paymentApi.getBalance(securityManager.currentUserId()).balance
            return Money(balance.amount, balance.currency)
        } catch (ex: FeignException.NotFound) {
            return Money(0.0, "")
        }
    }
}
