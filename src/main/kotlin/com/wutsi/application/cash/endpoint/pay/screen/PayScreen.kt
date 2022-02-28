package com.wutsi.application.cash.endpoint.pay.screen

import com.wutsi.application.cash.endpoint.AbstractQuery
import com.wutsi.application.cash.endpoint.Page
import com.wutsi.application.shared.Theme
import com.wutsi.application.shared.service.TenantProvider
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.AppBar
import com.wutsi.flutter.sdui.Column
import com.wutsi.flutter.sdui.Container
import com.wutsi.flutter.sdui.Form
import com.wutsi.flutter.sdui.Input
import com.wutsi.flutter.sdui.MoneyWithKeyboard
import com.wutsi.flutter.sdui.Screen
import com.wutsi.flutter.sdui.Widget
import com.wutsi.flutter.sdui.enums.ActionType.Command
import com.wutsi.flutter.sdui.enums.InputType.Submit
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/pay")
class PayScreen(
    private val tenantProvider: TenantProvider,
) : AbstractQuery() {
    @PostMapping
    fun index(): Widget {
        val tenant = tenantProvider.get()
        return Screen(
            id = Page.PAY,
            backgroundColor = Theme.COLOR_PRIMARY,
            appBar = AppBar(
                elevation = 0.0,
                backgroundColor = Theme.COLOR_PRIMARY,
                foregroundColor = Theme.COLOR_WHITE,
                title = getText("page.pay.app-bar.title"),
            ),
            child = Column(
                children = listOf(
                    Form(
                        children = listOf(
                            Container(
                                child = MoneyWithKeyboard(
                                    name = "amount",
                                    maxLength = 7,
                                    currency = tenant.currencySymbol,
                                    moneyColor = Theme.COLOR_WHITE,
                                    keyboardColor = Theme.COLOR_WHITE,
                                    numberFormat = tenant.numberFormat,
                                    value = 0,
                                    keyboardButtonSize = 70.0
                                ),
                            ),
                            Container(
                                padding = 10.0,
                                child = Input(
                                    name = "command",
                                    type = Submit,
                                    caption = getText("page.pay.button.submit"),
                                    action = Action(
                                        type = Command,
                                        url = urlBuilder.build("commands/pay/amount")
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            bottomNavigationBar = bottomNavigationBar()
        ).toWidget()
    }
}
