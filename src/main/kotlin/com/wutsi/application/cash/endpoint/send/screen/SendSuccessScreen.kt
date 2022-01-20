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
import com.wutsi.flutter.sdui.enums.Alignment
import com.wutsi.flutter.sdui.enums.Alignment.Center
import com.wutsi.flutter.sdui.enums.ButtonType.Elevated
import com.wutsi.flutter.sdui.enums.TextAlignment
import com.wutsi.platform.account.WutsiAccountApi
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.text.DecimalFormat

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
        @RequestParam(name = "transaction-id") transactionId: String,
        @RequestParam(name = "error", required = false) error: String? = null
    ): Widget {
        val tenant = tenantProvider.get()
        val fmt = DecimalFormat(tenant.monetaryFormat)
        val tx = paymentApi.getTransaction(transactionId).transaction
        val recipient = accountApi.getAccount(tx.recipientId!!).account
        return Screen(
            id = Page.SEND_SUCCESS,
            appBar = AppBar(
                elevation = 0.0,
                backgroundColor = Theme.COLOR_WHITE,
                foregroundColor = Theme.COLOR_BLACK,
                automaticallyImplyLeading = false,
                actions = listOf(
                    IconButton(
                        icon = Theme.ICON_CANCEL,
                        action = gotoHome()
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
                        showWebsite = false,
                    ),
                    Divider(color = Theme.COLOR_DIVIDER),
                    MoneyText(
                        value = tx.net,
                        currency = tenant.currencySymbol,
                        numberFormat = tenant.numberFormat,
                        color = Theme.COLOR_PRIMARY,
                    ),
                    Container(
                        alignment = Alignment.Center,
                        child = Text(
                            getText("page.send-success.fees", arrayOf(fmt.format(tx.fees))),
                            bold = true,
                            size = Theme.TEXT_SIZE_LARGE
                        )
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
