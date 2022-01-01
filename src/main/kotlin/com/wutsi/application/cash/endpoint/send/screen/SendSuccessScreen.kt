package com.wutsi.application.cash.endpoint.send.screen

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
import com.wutsi.flutter.sdui.enums.ActionType.Route
import com.wutsi.flutter.sdui.enums.Alignment
import com.wutsi.flutter.sdui.enums.Alignment.Center
import com.wutsi.flutter.sdui.enums.ButtonType.Elevated
import com.wutsi.flutter.sdui.enums.TextAlignment
import com.wutsi.platform.account.WutsiAccountApi
import com.wutsi.platform.payment.core.ErrorCode
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/send/success")
class SendSuccessScreen(
    private val tenantProvider: TenantProvider,
    private val accountApi: WutsiAccountApi,
) : AbstractQuery() {
    @PostMapping
    fun index(
        @RequestParam amount: Double,
        @RequestParam("recipient-id") recipientId: Long,
        @RequestParam(required = false) error: ErrorCode? = null,
    ): Widget {
        val tenant = tenantProvider.get()
        val recipient = accountApi.getAccount(recipientId).account
        return Screen(
            id = Page.SEND_SUCCESS,
            safe = true,
            appBar = AppBar(
                elevation = 0.0,
                backgroundColor = Theme.COLOR_WHITE,
                automaticallyImplyLeading = false,
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
            child = Column(
                children = listOf(
                    Container(padding = 20.0),
                    Container(
                        padding = 10.0,
                        alignment = Center,
                        child = CircleAvatar(
                            radius = 48.0,
                            child = if (recipient.pictureUrl.isNullOrEmpty())
                                Text(StringUtil.initials(recipient.displayName))
                            else
                                Image(url = recipient.pictureUrl!!)
                        )
                    ),
                    Container(
                        padding = 10.0,
                        alignment = Center,
                        child = Text(
                            caption = recipient.displayName ?: "",
                            alignment = TextAlignment.Center,
                            size = Theme.TEXT_SIZE_X_LARGE,
                            color = Theme.COLOR_PRIMARY,
                            bold = true,
                        )
                    ),
                    Container(
                        padding = 10.0,
                        alignment = Alignment.Center,
                        child = MoneyText(
                            value = amount,
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
                        padding = 10.0,
                        child = Button(
                            type = Elevated,
                            caption = getText("page.send-success.button.submit"),
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
}
