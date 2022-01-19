package com.wutsi.application.cash.endpoint.send.screen

import com.wutsi.application.cash.endpoint.AbstractQuery
import com.wutsi.application.cash.endpoint.Page
import com.wutsi.application.shared.Theme
import com.wutsi.application.shared.service.CategoryService
import com.wutsi.application.shared.service.TenantProvider
import com.wutsi.application.shared.service.TogglesProvider
import com.wutsi.application.shared.ui.ProfileCard
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.AppBar
import com.wutsi.flutter.sdui.Button
import com.wutsi.flutter.sdui.Column
import com.wutsi.flutter.sdui.Container
import com.wutsi.flutter.sdui.Divider
import com.wutsi.flutter.sdui.Icon
import com.wutsi.flutter.sdui.IconButton
import com.wutsi.flutter.sdui.MoneyText
import com.wutsi.flutter.sdui.Screen
import com.wutsi.flutter.sdui.Text
import com.wutsi.flutter.sdui.Widget
import com.wutsi.flutter.sdui.enums.ActionType.Route
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
    private val categoryService: CategoryService,
    private val togglesProvider: TogglesProvider,
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
                    ProfileCard(
                        account = recipient,
                        phoneNumber = null,
                        categoryService = categoryService,
                        togglesProvider = togglesProvider,
                        showWebsite = false
                    ),
                    Divider(color = Theme.COLOR_DIVIDER),
                    MoneyText(
                        value = amount,
                        currency = tenant.currencySymbol,
                        numberFormat = tenant.numberFormat,
                    ),
                    Container(
                        alignment = Center,
                        child = Icon(
                            code = error?.let { Theme.ICON_ERROR } ?: Theme.ICON_CHECK,
                            size = 48.0,
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
