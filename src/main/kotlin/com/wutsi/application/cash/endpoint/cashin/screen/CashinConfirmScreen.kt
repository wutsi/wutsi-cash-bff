package com.wutsi.application.cash.endpoint.cashin.screen

import com.wutsi.application.cash.endpoint.AbstractQuery
import com.wutsi.application.cash.endpoint.Page
import com.wutsi.application.shared.Theme
import com.wutsi.application.shared.service.TenantProvider
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.AppBar
import com.wutsi.flutter.sdui.Column
import com.wutsi.flutter.sdui.Container
import com.wutsi.flutter.sdui.Divider
import com.wutsi.flutter.sdui.Icon
import com.wutsi.flutter.sdui.Input
import com.wutsi.flutter.sdui.MoneyText
import com.wutsi.flutter.sdui.Row
import com.wutsi.flutter.sdui.Screen
import com.wutsi.flutter.sdui.Text
import com.wutsi.flutter.sdui.Widget
import com.wutsi.flutter.sdui.enums.ActionType
import com.wutsi.flutter.sdui.enums.Alignment.Center
import com.wutsi.flutter.sdui.enums.CrossAxisAlignment
import com.wutsi.flutter.sdui.enums.InputType.Submit
import com.wutsi.flutter.sdui.enums.MainAxisAlignment
import com.wutsi.platform.account.WutsiAccountApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.text.DecimalFormat

@RestController
@RequestMapping("/cashin/confirm")
class CashinConfirmScreen(
    private val tenantProvider: TenantProvider,
    private val accountApi: WutsiAccountApi,

    @Value("\${wutsi.application.login-url}") private val loginUrl: String,
) : AbstractQuery() {
    @PostMapping
    fun index(
        @RequestParam amount: Double,
        @RequestParam("payment-token") paymentToken: String
    ): Widget {
        val accountId = securityContext.currentAccountId()
        val tenant = tenantProvider.get()
        val balance = getBalance(tenant)
        val fmt = DecimalFormat(tenant.monetaryFormat)
        val paymentMethod = accountApi.getPaymentMethod(accountId, paymentToken).paymentMethod
        val carrier = tenantProvider.mobileCarriers(tenant).find { it.code.equals(paymentMethod.provider, true) }

        return Screen(
            id = Page.CASHIN_CONFIRM,
            appBar = AppBar(
                elevation = 0.0,
                backgroundColor = Theme.COLOR_WHITE,
                foregroundColor = Theme.COLOR_BLACK,
                title = getText("page.cashin-confirm.app-bar.title", arrayOf(fmt.format(balance.value)))
            ),
            child = Container(
                alignment = Center,
                child = Column(
                    children = listOf(
                        Container(padding = 10.0),
                        Container(
                            padding = 10.0,
                            child = Column(
                                children = listOf(
                                    Row(
                                        mainAxisAlignment = MainAxisAlignment.start,
                                        crossAxisAlignment = CrossAxisAlignment.start,
                                        children = listOf(
                                            Icon(
                                                code = carrier?.let { tenantProvider.logo(it) } ?: "",
                                                size = 24.0
                                            ),
                                            Container(padding = 5.0),
                                            Text(paymentMethod.phone!!.number, size = Theme.TEXT_SIZE_LARGE)
                                        ),
                                    ),
                                    Divider(color = Theme.COLOR_DIVIDER),
                                )
                            ),
                        ),
                        Container(
                            padding = 10.0,
                            child = MoneyText(
                                value = amount,
                                currency = tenant.currency,
                                numberFormat = tenant.numberFormat,
                                color = Theme.COLOR_PRIMARY
                            ),
                        ),
                        Container(padding = 20.0),
                        Container(
                            padding = 10.0,
                            child = Input(
                                name = "command",
                                type = Submit,
                                caption = getText(
                                    "page.cashin-confirm.button.submit",
                                    arrayOf(fmt.format(amount))
                                ),
                                action = Action(
                                    type = ActionType.Route,
                                    url = urlBuilder.build(loginUrl, getSubmitUrl(amount, paymentToken))
                                )
                            )
                        )
                    )
                )
            ),
        ).toWidget()
    }

    private fun getSubmitUrl(amount: Double, paymentToken: String): String {
        val me = accountApi.getAccount(securityContext.currentAccountId()).account
        return "?phone=" + encodeURLParam(me.phone!!.number) +
            "&icon=" + Theme.ICON_LOCK +
            "&screen-id=" + Page.CASHIN_PIN +
            "&title=" + encodeURLParam(getText("page.cashin-pin.title")) +
            "&sub-title=" + encodeURLParam(getText("page.cashin-pin.sub-title")) +
            "&auth=false" +
            "&return-to-route=false" +
            "&return-url=" + encodeURLParam(
            urlBuilder.build(
                "commands/cashin?amount=$amount&payment-token=$paymentToken"
            )
        )
    }
}
