package com.wutsi.application.cash.endpoint.pay.screen

import com.wutsi.application.cash.endpoint.AbstractQuery
import com.wutsi.application.cash.endpoint.Page
import com.wutsi.application.shared.Theme
import com.wutsi.application.shared.service.StringUtil
import com.wutsi.application.shared.service.TenantProvider
import com.wutsi.application.shared.service.URLBuilder
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.AppBar
import com.wutsi.flutter.sdui.Button
import com.wutsi.flutter.sdui.CircleAvatar
import com.wutsi.flutter.sdui.Column
import com.wutsi.flutter.sdui.Container
import com.wutsi.flutter.sdui.IconButton
import com.wutsi.flutter.sdui.Image
import com.wutsi.flutter.sdui.MoneyText
import com.wutsi.flutter.sdui.Screen
import com.wutsi.flutter.sdui.Text
import com.wutsi.flutter.sdui.Widget
import com.wutsi.flutter.sdui.enums.ActionType
import com.wutsi.flutter.sdui.enums.Alignment
import com.wutsi.platform.account.WutsiAccountApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.text.DecimalFormat

@RestController
@RequestMapping("/pay/confirm")
class PayConfirmScreen(
    private val urlBuilder: URLBuilder,
    private val tenantProvider: TenantProvider,
    private val accountApi: WutsiAccountApi,

    @Value("\${wutsi.application.login-url}") private val loginUrl: String,
) : AbstractQuery() {
    @PostMapping
    fun index(@RequestParam(name = "payment-request-id") paymentRequestId: String): Widget {
        val tenant = tenantProvider.get()
        val paymentRequest = paymentApi.getPaymentRequest(paymentRequestId).paymentRequest
        val merchant = accountApi.getAccount(paymentRequest.accountId).account
        val amountText = DecimalFormat(tenant.monetaryFormat).format(paymentRequest.amount)
        return Screen(
            id = Page.PAY_CONFIRM,
            appBar = AppBar(
                elevation = 0.0,
                backgroundColor = Theme.COLOR_WHITE,
                foregroundColor = Theme.COLOR_BLACK,
                title = getText("page.pay-confirm.app-bar.title"),
                actions = listOf(
                    IconButton(
                        icon = Theme.ICON_CANCEL,
                        action = Action(
                            type = ActionType.Route,
                            url = "route:/~"
                        )
                    )
                )
            ),
            child = Column(
                children = listOf(
                    Container(padding = 20.0),
                    Container(
                        padding = 10.0,
                        alignment = Alignment.Center,
                        child = CircleAvatar(
                            radius = 48.0,
                            child = if (!merchant.pictureUrl.isNullOrBlank())
                                Image(url = merchant.pictureUrl!!)
                            else
                                Text(
                                    caption = StringUtil.initials(merchant.displayName),
                                    size = 30.0,
                                    bold = true
                                )
                        )
                    ),
                    Container(
                        padding = 10.0,
                        alignment = Alignment.Center,
                        child = Text(
                            merchant.displayName ?: "",
                            size = Theme.TEXT_SIZE_X_LARGE,
                            bold = true,
                            color = Theme.COLOR_PRIMARY,
                        )
                    ),
                    Container(
                        padding = 10.0,
                        alignment = Alignment.Center,
                        child = MoneyText(
                            value = paymentRequest.amount,
                            currency = tenant.currencySymbol,
                            numberFormat = tenant.numberFormat,
                        )
                    ),
                    Container(
                        padding = 10.0,
                        alignment = Alignment.Center,
                        child = Button(
                            caption = getText("page.pay-confirm.button.pay", arrayOf(amountText)),
                            action = Action(
                                type = ActionType.Route,
                                url = urlBuilder.build(loginUrl, getLoginUrlPath(paymentRequestId)),
                            )
                        )
                    )
                ),
            )
        ).toWidget()
    }

    private fun getLoginUrlPath(paymentRequestId: String): String {
        val me = accountApi.getAccount(securityContext.currentUserId()).account
        return "?phone=" + encodeURLParam(me.phone!!.number) +
            "&icon=" + Theme.ICON_LOCK +
            "&screen-id=" + Page.PAY_PIN +
            "&title=" + encodeURLParam(getText("page.pay-pin.title")) +
            "&sub-title=" + encodeURLParam(getText("page.pay-pin.sub-title")) +
            "&auth=false" +
            "&return-to-route=false" +
            "&return-url=" + encodeURLParam(
            urlBuilder.build(
                "commands/pay?payment-request-id=$paymentRequestId"
            )
        )
    }
}
