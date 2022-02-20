package com.wutsi.application.cash.endpoint.pay.screen

import com.wutsi.application.cash.endpoint.AbstractQuery
import com.wutsi.application.cash.endpoint.Page
import com.wutsi.application.shared.Theme
import com.wutsi.application.shared.service.SharedUIMapper
import com.wutsi.application.shared.ui.ProfileCard
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.AppBar
import com.wutsi.flutter.sdui.Button
import com.wutsi.flutter.sdui.Column
import com.wutsi.flutter.sdui.Container
import com.wutsi.flutter.sdui.Divider
import com.wutsi.flutter.sdui.Icon
import com.wutsi.flutter.sdui.IconButton
import com.wutsi.flutter.sdui.Screen
import com.wutsi.flutter.sdui.Text
import com.wutsi.flutter.sdui.Widget
import com.wutsi.flutter.sdui.enums.ActionType
import com.wutsi.flutter.sdui.enums.Alignment.Center
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
    private val accountApi: WutsiAccountApi,
    private val sharedUIMapper: SharedUIMapper
) : AbstractQuery() {
    @PostMapping
    fun index(
        @RequestParam(name = "payment-request-id") paymentRequestId: String,
        @RequestParam(name = "error", required = false) error: ErrorCode? = null,
    ): Widget {
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
                    com.wutsi.flutter.sdui.Center(
                        child = ProfileCard(
                            model = sharedUIMapper.toAccountModel(merchant),
                            showWebsite = false,
                            showPhoneNumber = false
                        )
                    ),
                    Divider(color = Theme.COLOR_DIVIDER),
                    Container(
                        alignment = Center,
                        child = Icon(
                            code = error?.let { Theme.ICON_ERROR } ?: Theme.ICON_CHECK_CIRCLE,
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
                        padding = 10.0,
                        child = Button(
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
