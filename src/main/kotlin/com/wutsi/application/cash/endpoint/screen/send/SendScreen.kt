package com.wutsi.application.cash.endpoint.screen.send

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
import com.wutsi.flutter.sdui.enums.Alignment.Center
import com.wutsi.flutter.sdui.enums.InputType.Submit
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/send")
class SendScreen(
    private val urlBuilder: URLBuilder,
    private val tenantProvider: TenantProvider,
) : AbstractQuery() {
    @PostMapping
    fun index(): Widget {
        val tenant = tenantProvider.get()
        return Screen(
            id = Page.SEND,
            backgroundColor = Theme.PRIMARY_COLOR,
            appBar = AppBar(
                elevation = 0.0,
                backgroundColor = Theme.PRIMARY_COLOR,
                foregroundColor = Theme.WHITE_COLOR,
                title = getText("page.send.title"),
                actions = listOf(
                    IconButton(
                        icon = Theme.ICON_CANCEL,
                        action = Action(
                            type = Route,
                            url = "route:/~"
                        )
                    )
                )
            ),
            child = Container(
                alignment = Center,
                padding = 20.0,
                child = Column(
                    children = listOf(
                        Form(
                            children = listOf(
                                Container(
                                    padding = 30.0
                                ),
                                Container(
                                    padding = 10.0,
                                    child = MoneyWithKeyboard(
                                        name = "amount",
                                        maxLength = 7,
                                        currency = tenant.currency,
                                        deleteText = getText("keyboard.delete"),
                                        moneyColor = Theme.WHITE_COLOR,
                                        keyboardColor = Theme.WHITE_COLOR,
                                        numberFormat = tenant.numberFormat,
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
                )
            ),
        ).toWidget()
    }
}
