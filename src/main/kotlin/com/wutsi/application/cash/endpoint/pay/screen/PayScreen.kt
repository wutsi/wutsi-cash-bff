package com.wutsi.application.cash.endpoint.pay.screen

import com.wutsi.application.cash.endpoint.AbstractQuery
import com.wutsi.application.cash.endpoint.Page
import com.wutsi.application.cash.endpoint.Theme
import com.wutsi.application.cash.service.TenantProvider
import com.wutsi.application.cash.service.URLBuilder
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.AppBar
import com.wutsi.flutter.sdui.Column
import com.wutsi.flutter.sdui.Container
import com.wutsi.flutter.sdui.Form
import com.wutsi.flutter.sdui.IconButton
import com.wutsi.flutter.sdui.Input
import com.wutsi.flutter.sdui.MoneyWithKeyboard
import com.wutsi.flutter.sdui.Screen
import com.wutsi.flutter.sdui.Widget
import com.wutsi.flutter.sdui.enums.ActionType.Command
import com.wutsi.flutter.sdui.enums.ActionType.Route
import com.wutsi.flutter.sdui.enums.InputType.Submit
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/pay")
class PayScreen(
    private val urlBuilder: URLBuilder,
    private val tenantProvider: TenantProvider,

    @Value("\${wutsi.application.shell-url}") private val shellUrl: String
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
                actions = listOf(
                    IconButton(
                        icon = Theme.ICON_SETTINGS,
                        action = Action(
                            type = Route,
                            url = urlBuilder.build(shellUrl, "settings")
                        )
                    )
                )
            ),
            child = Column(
                children = listOf(
                    Form(
                        children = listOf(
                            Container(
                                padding = 10.0,
                                child = MoneyWithKeyboard(
                                    name = "amount",
                                    maxLength = 7,
                                    currency = tenant.currencySymbol,
                                    moneyColor = Theme.COLOR_WHITE,
                                    keyboardColor = Theme.COLOR_WHITE,
                                    numberFormat = tenant.numberFormat,
                                    value = 0
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
        ).toWidget()
    }
}
