package com.wutsi.application.cash.endpoint.send.screen

import com.wutsi.application.cash.endpoint.AbstractQuery
import com.wutsi.application.cash.endpoint.Page
import com.wutsi.application.shared.Theme
import com.wutsi.application.shared.service.CategoryService
import com.wutsi.application.shared.service.TenantProvider
import com.wutsi.application.shared.service.TogglesProvider
import com.wutsi.application.shared.service.URLBuilder
import com.wutsi.application.shared.ui.ProfileCard
import com.wutsi.application.shared.ui.ProfileCardType
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.AppBar
import com.wutsi.flutter.sdui.Button
import com.wutsi.flutter.sdui.Column
import com.wutsi.flutter.sdui.Container
import com.wutsi.flutter.sdui.Divider
import com.wutsi.flutter.sdui.IconButton
import com.wutsi.flutter.sdui.MoneyText
import com.wutsi.flutter.sdui.Screen
import com.wutsi.flutter.sdui.Text
import com.wutsi.flutter.sdui.Widget
import com.wutsi.flutter.sdui.enums.ActionType
import com.wutsi.flutter.sdui.enums.Alignment
import com.wutsi.flutter.sdui.enums.ButtonType
import com.wutsi.platform.account.WutsiAccountApi
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.text.DecimalFormat

@RestController
@RequestMapping("/send/approval")
class SendApprovalScreen(
    private val tenantProvider: TenantProvider,
    private val accountApi: WutsiAccountApi,
    private val categoryService: CategoryService,
    private val togglesProvider: TogglesProvider,
    private val urlBuilder: URLBuilder,
) : AbstractQuery() {
    @PostMapping
    fun index(
        @RequestParam(name = "transaction-id") transactionId: String
    ): Widget {
        val tenant = tenantProvider.get()
        val fmt = DecimalFormat(tenant.monetaryFormat)
        val tx = paymentApi.getTransaction(transactionId).transaction
        val sender = accountApi.getAccount(tx.accountId).account
        return Screen(
            id = Page.SEND_APPROVAL,
            appBar = AppBar(
                elevation = 0.0,
                backgroundColor = Theme.COLOR_WHITE,
                automaticallyImplyLeading = false,
                actions = listOf(
                    IconButton(
                        icon = Theme.ICON_CANCEL,
                        action = gotoHome()
                    )
                ),
                title = getText("page.send-approval.app-bar.title"),
            ),
            child = Column(
                children = listOf(
                    Container(
                        padding = 10.0,
                        alignment = Alignment.Center,
                        child = Text(
                            caption = getText("page.send-approval.message"),
                            size = Theme.TEXT_SIZE_LARGE,
                        )
                    ),
                    MoneyText(
                        value = tx.net,
                        currency = tenant.currencySymbol,
                        numberFormat = tenant.numberFormat,
                        color = Theme.COLOR_PRIMARY
                    ),
                    Container(
                        alignment = Alignment.Center,
                        child = Text(
                            getText("page.send-approval.fees", arrayOf(fmt.format(tx.fees))),
                            bold = true,
                            size = Theme.TEXT_SIZE_LARGE
                        )
                    ),
                    Divider(color = Theme.COLOR_DIVIDER),
                    Container(
                        padding = 10.0,
                        alignment = Alignment.Center,
                        child = Text(
                            caption = getText("page.send-approval.from"),
                            size = Theme.TEXT_SIZE_LARGE,
                        )
                    ),
                    ProfileCard(
                        account = sender,
                        phoneNumber = null,
                        categoryService = categoryService,
                        togglesProvider = togglesProvider,
                        showWebsite = false,
                        type = ProfileCardType.Summary
                    ),
                    Divider(color = Theme.COLOR_DIVIDER),
                    Container(
                        padding = 10.0,
                        child = Button(
                            caption = getText("page.send-approval.button.submit", arrayOf(fmt.format(tx.amount))),
                            action = Action(
                                type = ActionType.Command,
                                url = urlBuilder.build("commands/send/approve?transaction-id=$transactionId")
                            )
                        )
                    ),
                    Button(
                        type = ButtonType.Text,
                        caption = getText("page.send-approval.button.cancel"),
                        action = gotoHome()
                    )
                )
            ),
        ).toWidget()
    }
}
