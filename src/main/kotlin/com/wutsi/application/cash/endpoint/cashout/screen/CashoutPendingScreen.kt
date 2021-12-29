package com.wutsi.application.cash.endpoint.cashout.screen

import com.wutsi.application.cash.endpoint.AbstractQuery
import com.wutsi.application.cash.endpoint.Page
import com.wutsi.application.cash.endpoint.Theme
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.AppBar
import com.wutsi.flutter.sdui.Button
import com.wutsi.flutter.sdui.Column
import com.wutsi.flutter.sdui.Container
import com.wutsi.flutter.sdui.Icon
import com.wutsi.flutter.sdui.Screen
import com.wutsi.flutter.sdui.Text
import com.wutsi.flutter.sdui.Widget
import com.wutsi.flutter.sdui.enums.ActionType.Route
import com.wutsi.flutter.sdui.enums.Alignment.Center
import com.wutsi.flutter.sdui.enums.ButtonType.Elevated
import com.wutsi.flutter.sdui.enums.TextAlignment
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/cashout/pending")
class CashoutPendingScreen : AbstractQuery() {
    @PostMapping
    fun index(): Widget = Screen(
        id = Page.CASHOUT_PENDING,
        safe = true,
        appBar = AppBar(
            elevation = 0.0,
            backgroundColor = Theme.COLOR_WHITE,
            automaticallyImplyLeading = false
        ),
        child = Column(
            children = listOf(
                Container(padding = 40.0),
                Container(
                    alignment = Center,
                    padding = 20.0,
                    child = Icon(
                        code = Theme.ICON_PENDING,
                        size = 80.0,
                        color = Theme.COLOR_PRIMARY
                    )
                ),
                Container(
                    alignment = Center,
                    padding = 10.0,
                    child = Text(
                        caption = getText(
                            "page.cashout-pending.message_1"
                        ),
                        alignment = TextAlignment.Center,
                        size = Theme.TEXT_SIZE_X_LARGE,
                    )
                ),
                Container(
                    alignment = Center,
                    padding = 10.0,
                    child = Text(
                        caption = getText(
                            "page.cashout-pending.message_2"
                        ),
                        alignment = TextAlignment.Center,
                        size = Theme.TEXT_SIZE_X_LARGE,
                    )
                ),
                Container(
                    padding = 10.0,
                    child = Button(
                        type = Elevated,
                        caption = getText("page.cashout-pending.button.submit"),
                        action = Action(
                            type = Route,
                            url = "route:/~"
                        )
                    )
                )
            )
        ),
    ).toWidget()
}
