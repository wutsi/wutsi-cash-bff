package com.wutsi.application.cash.endpoint.send.screen

import com.wutsi.application.cash.endpoint.AbstractQuery
import com.wutsi.application.cash.endpoint.Page
import com.wutsi.application.cash.endpoint.Theme
import com.wutsi.application.cash.service.SecurityManager
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
import com.wutsi.flutter.sdui.Text
import com.wutsi.flutter.sdui.Widget
import com.wutsi.flutter.sdui.enums.ActionType.Command
import com.wutsi.flutter.sdui.enums.ActionType.Route
import com.wutsi.flutter.sdui.enums.Alignment.Center
import com.wutsi.flutter.sdui.enums.InputType.Submit
import com.wutsi.flutter.sdui.enums.TextAlignment
import com.wutsi.platform.payment.WutsiPaymentApi
import com.wutsi.platform.payment.core.Money
import com.wutsi.platform.tenant.dto.Tenant
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
    private val paymentApi: WutsiPaymentApi,
    private val securityManager: SecurityManager,

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
            child = Container(
                alignment = Center,
                padding = 20.0,
                child = Column(
                    children = listOf(
                        Text(
                            alignment = TextAlignment.Center,
                            color = Theme.WHITE_COLOR,
                            caption = getText("page.send.your-balance", arrayOf(balanceText)),
                            size = Theme.LARGE_TEXT_SIZE,
                        ),
                        Container(
                            padding = 20.0,
                        ),
                        Form(
                            children = listOf(
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

    private fun getBalance(tenant: Tenant): Money {
        try {
            val userId = securityManager.currentUserId()
            val balance = paymentApi.getBalance(userId).balance
            return Money(
                value = balance.amount,
                currency = balance.currency
            )
        } catch (ex: Throwable) {
            return Money(currency = tenant.currency)
        }
    }
}
