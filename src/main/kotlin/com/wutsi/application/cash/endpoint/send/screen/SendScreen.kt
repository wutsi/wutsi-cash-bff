package com.wutsi.application.cash.endpoint.send.screen

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
import java.text.DecimalFormat

@RestController
@RequestMapping("/send")
class SendScreen(
    private val urlBuilder: URLBuilder,
    private val tenantProvider: TenantProvider,

    @Value("\${wutsi.application.shell-url}") private val shellUrl: String
) : AbstractQuery() {
    @PostMapping
    fun index(): Widget {
        val tenant = tenantProvider.get()
        val balance = getBalance(tenant)
        val balanceText = DecimalFormat(tenant.monetaryFormat).format(balance.value)
        return Screen(
            id = Page.SEND,
            backgroundColor = Theme.PRIMARY_COLOR,
            appBar = AppBar(
                elevation = 0.0,
                backgroundColor = Theme.PRIMARY_COLOR,
                foregroundColor = Theme.WHITE_COLOR,
                title = getText("page.send.app-bar.title", arrayOf(balanceText)),
                leading = IconButton(
                    icon = Theme.ICON_HISTORY,
                    action = Action(
                        type = Route,
                        url = urlBuilder.build("history")
                    )
                ),
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
                                    currency = tenant.currency,
                                    moneyColor = Theme.WHITE_COLOR,
                                    keyboardColor = Theme.WHITE_COLOR,
                                    numberFormat = tenant.numberFormat,
                                    value = 0
                                ),
                            ),
                            Container(
                                padding = 10.0,
                                child = Input(
                                    name = "command",
                                    type = Submit,
                                    caption = getText("page.send.button.submit"),
                                    action = Action(
                                        type = Command,
                                        url = urlBuilder.build("commands/send/amount")
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
