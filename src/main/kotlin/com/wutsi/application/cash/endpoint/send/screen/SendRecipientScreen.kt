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
import com.wutsi.flutter.sdui.Screen
import com.wutsi.flutter.sdui.Text
import com.wutsi.flutter.sdui.Widget
import com.wutsi.flutter.sdui.enums.ActionType.Command
import com.wutsi.flutter.sdui.enums.ActionType.Route
import com.wutsi.flutter.sdui.enums.Alignment.Center
import com.wutsi.flutter.sdui.enums.InputType.Phone
import com.wutsi.flutter.sdui.enums.InputType.Submit
import com.wutsi.flutter.sdui.enums.TextAlignment
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.text.DecimalFormat

@RestController
@RequestMapping("/send/recipient")
class SendRecipientScreen(
    private val urlBuilder: URLBuilder,
    private val tenantProvider: TenantProvider,
) : AbstractQuery() {
    @PostMapping
    fun index(@RequestParam amount: Double): Widget {
        val tenant = tenantProvider.get()
        val amountText = DecimalFormat(tenant.monetaryFormat).format(amount)

        return Screen(
            id = Page.SEND_RECIPIENT,
            backgroundColor = Theme.WHITE_COLOR,
            appBar = AppBar(
                elevation = 0.0,
                backgroundColor = Theme.PRIMARY_COLOR,
                foregroundColor = Theme.WHITE_COLOR,
                title = getText("page.send-recipient.title"),
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
                                    alignment = Center,
                                    padding = 10.0,
                                    child = Text(
                                        caption = getText(
                                            "page.send-recipient.phone.title"
                                        ),
                                        alignment = TextAlignment.Center,
                                        size = Theme.X_LARGE_TEXT_SIZE,
                                        color = Theme.PRIMARY_COLOR,
                                        bold = true
                                    )
                                ),
                                Container(
                                    alignment = Center,
                                    padding = 10.0,
                                    child = Text(
                                        caption = getText(
                                            "page.send-recipient.phone.sub-title"
                                        ),
                                        alignment = TextAlignment.Center,
                                        size = Theme.LARGE_TEXT_SIZE,
                                        color = Theme.BLACK_COLOR,
                                    )
                                ),
                                Container(
                                    padding = 20.0
                                ),
                                Container(
                                    padding = 10.0,
                                    child = Input(
                                        name = "phoneNumber",
                                        type = Phone,
                                        required = true,
                                        countries = tenant.countries
                                    ),
                                ),
                                Container(
                                    padding = 10.0,
                                    child = Input(
                                        name = "command",
                                        type = Submit,
                                        caption = getText("page.send-recipient.button.submit", arrayOf(amountText)),
                                        action = Action(
                                            type = Command,
                                            url = urlBuilder.build("commands/send/recipient"),
                                            parameters = mapOf("amount" to amount.toString())
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
