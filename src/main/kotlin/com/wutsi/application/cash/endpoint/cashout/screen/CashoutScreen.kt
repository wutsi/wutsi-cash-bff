package com.wutsi.application.cash.endpoint.cashout.screen

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
import com.wutsi.flutter.sdui.DropdownButton
import com.wutsi.flutter.sdui.DropdownMenuItem
import com.wutsi.flutter.sdui.Form
import com.wutsi.flutter.sdui.Input
import com.wutsi.flutter.sdui.MoneyWithKeyboard
import com.wutsi.flutter.sdui.Screen
import com.wutsi.flutter.sdui.Text
import com.wutsi.flutter.sdui.Widget
import com.wutsi.flutter.sdui.enums.ActionType.Command
import com.wutsi.flutter.sdui.enums.Alignment.Center
import com.wutsi.flutter.sdui.enums.InputType.Submit
import com.wutsi.flutter.sdui.enums.TextAlignment
import com.wutsi.platform.account.WutsiAccountApi
import com.wutsi.platform.payment.WutsiPaymentApi
import com.wutsi.platform.payment.core.Money
import com.wutsi.platform.tenant.dto.Tenant
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.text.DecimalFormat

@RestController
@RequestMapping("/cashout")
class CashoutScreen(
    private val urlBuilder: URLBuilder,
    private val tenantProvider: TenantProvider,
    private val accountApi: WutsiAccountApi,
    private val securityManager: SecurityManager,
    private val paymentApi: WutsiPaymentApi,
) : AbstractQuery() {
    @PostMapping
    fun index(): Widget {
        val tenant = tenantProvider.get()
        val paymentMethods = accountApi.listPaymentMethods(
            securityManager.currentUserId()
        ).paymentMethods
        val balance = getBalance(tenant)
        val balanceText = DecimalFormat(tenant.monetaryFormat).format(balance.value)

        return Screen(
            id = Page.CASHOUT,
            appBar = AppBar(
                elevation = 0.0,
                backgroundColor = Theme.WHITE_COLOR,
                foregroundColor = Theme.BLACK_COLOR,
                title = getText("page.cashout.app-bar.title")
            ),
            child = Container(
                alignment = Center,
                child = Column(
                    children = listOf(
                        Text(
                            alignment = TextAlignment.Center,
                            color = Theme.WHITE_COLOR,
                            caption = getText("page.cashout.your-balance", arrayOf(balanceText)),
                            size = Theme.LARGE_TEXT_SIZE,
                        ),
                        Form(
                            children = listOf(
                                Container(
                                    child = MoneyWithKeyboard(
                                        name = "amount",
                                        maxLength = 7,
                                        currency = tenant.currency,
                                        deleteText = getText("keyboard.delete"),
                                        moneyColor = Theme.PRIMARY_COLOR,
                                        numberFormat = tenant.numberFormat,
                                        keyboardButtonSize = 60.0
                                    ),
                                ),
                                Container(
                                    padding = 10.0,
                                    child = DropdownButton(
                                        value = paymentMethods[0].token,
                                        name = "paymentToken",
                                        required = true,
                                        children = paymentMethods.map {
                                            DropdownMenuItem(
                                                caption = it.maskedNumber,
                                                value = it.token,
                                                icon = getMobileCarrier(it, tenant)?.let {
                                                    tenantProvider.logo(
                                                        it
                                                    )
                                                }
                                            )
                                        }
                                    ),
                                ),
                                Container(
                                    padding = 10.0,
                                    child = Input(
                                        name = "command",
                                        type = Submit,
                                        caption = getText("page.cashout.button.submit"),
                                        action = Action(
                                            type = Command,
                                            url = urlBuilder.build("commands/cashout")
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
