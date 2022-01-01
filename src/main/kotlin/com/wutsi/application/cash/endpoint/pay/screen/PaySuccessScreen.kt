package com.wutsi.application.cash.endpoint.pay.screen

import com.wutsi.application.cash.endpoint.AbstractQuery
import com.wutsi.application.cash.endpoint.Page
import com.wutsi.application.cash.endpoint.Theme
import com.wutsi.application.cash.service.TenantProvider
import com.wutsi.application.cash.util.StringUtil
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.AppBar
import com.wutsi.flutter.sdui.Button
import com.wutsi.flutter.sdui.CircleAvatar
import com.wutsi.flutter.sdui.Column
import com.wutsi.flutter.sdui.Container
import com.wutsi.flutter.sdui.Icon
import com.wutsi.flutter.sdui.IconButton
import com.wutsi.flutter.sdui.Image
import com.wutsi.flutter.sdui.MoneyText
import com.wutsi.flutter.sdui.Screen
import com.wutsi.flutter.sdui.Text
import com.wutsi.flutter.sdui.Widget
import com.wutsi.flutter.sdui.enums.ActionType
import com.wutsi.flutter.sdui.enums.Alignment
import com.wutsi.flutter.sdui.enums.Alignment.Center
import com.wutsi.flutter.sdui.enums.ButtonType
import com.wutsi.flutter.sdui.enums.TextAlignment
import com.wutsi.platform.account.WutsiAccountApi
import com.wutsi.platform.payment.core.ErrorCode
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/pay/success")
class PaySuccessScreen(
    private val tenantProvider: TenantProvider,
    private val accountApi: WutsiAccountApi
) : AbstractQuery() {
    @PostMapping
    fun index(
        @RequestParam(name = "payment-request-id") paymentRequestId: String,
        @RequestParam(name = "error", required = false) error: ErrorCode? = null,
    ): Widget {
        val tenant = tenantProvider.get()
        val paymentRequest = paymentApi.getPaymentRequest(paymentRequestId).paymentRequest
        val merchant = accountApi.getAccount(paymentRequest.accountId).account

        return Screen(
            id = error?.let { Page.PAY_ERROR } ?: Page.PAY_SUCCESS,
            appBar = AppBar(
                elevation = 0.0,
                backgroundColor = Theme.COLOR_WHITE,
                foregroundColor = Theme.COLOR_BLACK,
                title = getText("page.pay-confirm.app-bar.title"),
                automaticallyImplyLeading = false,
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
                        alignment = Alignment.Center,
                        padding = 10.0,
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
                            currency = tenant.currency,
                            numberFormat = tenant.numberFormat,
                        )
                    ),
                    Container(
                        alignment = Center,
                        child = Icon(
                            code = error?.let { Theme.ICON_ERROR } ?: Theme.ICON_CHECK,
                            size = 80.0,
                            color = error?.let { Theme.COLOR_DANGER } ?: Theme.COLOR_SUCCESS
                        )
                    ),
                    Container(
                        alignment = Center,
                        padding = 10.0,
                        child = Text(
                            error?.let { getTransactionErrorMessage(it) } ?: "",
                            color = Theme.COLOR_DANGER,
                            alignment = TextAlignment.Center,
                            bold = true
                        ),
                    ),
                    Container(
                        padding = 20.0,
                        child = Button(
                            type = ButtonType.Elevated,
                            caption = getText("page.pay-success.button.submit"),
                            action = Action(
                                type = ActionType.Route,
                                url = "route:/~"
                            )
                        )
                    )
                ),
            )
        ).toWidget()
    }
}
