package com.wutsi.application.cash.endpoint.screen.send

import com.wutsi.application.cash.endpoint.AbstractQuery
import com.wutsi.application.cash.endpoint.Page
import com.wutsi.application.cash.endpoint.Theme
import com.wutsi.application.cash.service.URLBuilder
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.AppBar
import com.wutsi.flutter.sdui.Column
import com.wutsi.flutter.sdui.Container
import com.wutsi.flutter.sdui.PinWithKeyboard
import com.wutsi.flutter.sdui.Screen
import com.wutsi.flutter.sdui.Text
import com.wutsi.flutter.sdui.Widget
import com.wutsi.flutter.sdui.enums.ActionType
import com.wutsi.flutter.sdui.enums.Alignment.Center
import com.wutsi.flutter.sdui.enums.Alignment.TopCenter
import com.wutsi.flutter.sdui.enums.TextAlignment
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/send/pin")
class SendPINScreen(
    private val urlBuilder: URLBuilder
) : AbstractQuery() {
    @PostMapping
    fun index(@RequestParam amount: Double, @RequestParam(name = "phone-number") phoneNumber: String): Widget {
        return Screen(
            id = Page.SEND_PIN,
            backgroundColor = Theme.PRIMARY_COLOR,
            appBar = AppBar(
                elevation = 0.0,
                backgroundColor = Theme.PRIMARY_COLOR,
                foregroundColor = Theme.WHITE_COLOR,
            ),
            child = Container(
                padding = 20.0,
                alignment = Center,
                child = Column(
                    children = listOf(
                        Container(
                            alignment = Center,
                            padding = 10.0,
                            child = Text(
                                caption = getText("page.send-pin.title"),
                                alignment = TextAlignment.Center,
                                size = Theme.X_LARGE_TEXT_SIZE,
                                bold = true,
                                color = Theme.WHITE_COLOR
                            )
                        ),
                        Container(
                            alignment = TopCenter,
                            padding = 10.0,
                            child = Text(
                                caption = getText("page.send-pin.sub-title"),
                                alignment = TextAlignment.Center,
                                size = Theme.LARGE_TEXT_SIZE,
                                color = Theme.WHITE_COLOR
                            )
                        ),
                        Container(
                            child = PinWithKeyboard(
                                name = "pin",
                                hideText = true,
                                pinSize = 15.0,
                                keyboardButtonSize = 90.0,
                                color = Theme.WHITE_COLOR,
                                action = Action(
                                    type = ActionType.Command,
                                    url = urlBuilder.build("commands/send"),
                                    parameters = mapOf(
                                        "amount" to amount.toString(),
                                        "phone-number" to phoneNumber
                                    )
                                ),
                            ),
                        )
                    )
                )
            ),
        ).toWidget()
    }
}
