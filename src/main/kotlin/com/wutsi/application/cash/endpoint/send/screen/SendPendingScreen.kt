package com.wutsi.application.cash.endpoint.send.screen

import com.wutsi.application.cash.endpoint.AbstractQuery
import com.wutsi.application.cash.endpoint.Page
import com.wutsi.application.shared.Theme
import com.wutsi.application.shared.service.QrService
import com.wutsi.application.shared.service.SharedUIMapper
import com.wutsi.application.shared.service.TenantProvider
import com.wutsi.application.shared.ui.ProfileCard
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.AppBar
import com.wutsi.flutter.sdui.Button
import com.wutsi.flutter.sdui.Center
import com.wutsi.flutter.sdui.Column
import com.wutsi.flutter.sdui.Container
import com.wutsi.flutter.sdui.Divider
import com.wutsi.flutter.sdui.Flexible
import com.wutsi.flutter.sdui.Icon
import com.wutsi.flutter.sdui.IconButton
import com.wutsi.flutter.sdui.Image
import com.wutsi.flutter.sdui.MoneyText
import com.wutsi.flutter.sdui.Screen
import com.wutsi.flutter.sdui.Text
import com.wutsi.flutter.sdui.Widget
import com.wutsi.flutter.sdui.WidgetAware
import com.wutsi.flutter.sdui.enums.ActionType
import com.wutsi.flutter.sdui.enums.Alignment
import com.wutsi.flutter.sdui.enums.ButtonType
import com.wutsi.flutter.sdui.enums.ButtonType.Elevated
import com.wutsi.flutter.sdui.enums.MainAxisAlignment
import com.wutsi.flutter.sdui.enums.TextAlignment
import com.wutsi.platform.account.WutsiAccountApi
import com.wutsi.platform.account.dto.Account
import com.wutsi.platform.payment.dto.Transaction
import com.wutsi.platform.qr.WutsiQrApi
import com.wutsi.platform.qr.dto.EncodeQRCodeRequest
import com.wutsi.platform.qr.entity.EntityType
import com.wutsi.platform.tenant.dto.Tenant
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.text.DecimalFormat

@RestController
@RequestMapping("/send/pending")
class SendPendingScreen(
    private val qrApi: WutsiQrApi,
    private val qrService: QrService,
    private val tenantProvider: TenantProvider,
    private val accountApi: WutsiAccountApi,
    private val sharedUIMapper: SharedUIMapper
) : AbstractQuery() {
    @PostMapping
    fun index(
        @RequestParam(name = "transaction-id") transactionId: String
    ): Widget {
        val tx = paymentApi.getTransaction(transactionId).transaction
        val recipient = accountApi.getAccount(tx.recipientId!!).account
        val tenant = tenantProvider.get()

        return Screen(
            id = Page.SEND_PENDING,
            appBar = AppBar(
                elevation = 0.0,
                backgroundColor = Theme.COLOR_WHITE,
                foregroundColor = Theme.COLOR_BLACK,
                automaticallyImplyLeading = false,
                actions = listOf(
                    IconButton(
                        icon = Theme.ICON_CANCEL,
                        action = Action(
                            type = ActionType.Route,
                            url = "route:/~"
                        )
                    )
                ),
            ),
            child = Column(
                children = if (tx.requiresApproval) approve(tx, tenant) else pending(tx, recipient, tenant)
            ),
        ).toWidget()
    }

    private fun pending(tx: Transaction, recipient: Account, tenant: Tenant): List<WidgetAware> {
        val fmt = DecimalFormat(tenant.monetaryFormat)
        return listOf(
            ProfileCard(
                model = sharedUIMapper.toAccountModel(recipient),
                showPhoneNumber = false,
                showWebsite = false,
            ),
            Divider(color = Theme.COLOR_DIVIDER),
            MoneyText(
                value = tx.net,
                numberFormat = tenant.numberFormat,
                currency = tenant.currencySymbol,
            ),
            Container(
                alignment = Alignment.Center,
                child = Text(
                    getText("page.send-pending.fees", arrayOf(fmt.format(tx.fees))),
                    bold = true,
                    size = Theme.TEXT_SIZE_LARGE
                )
            ),
            Container(
                alignment = Alignment.Center,
                padding = 10.0,
                child = Icon(code = Theme.ICON_PENDING, color = Theme.COLOR_WARNING, size = 48.0)
            ),
            Text(
                caption = getText("page.send-pending.message"),
                alignment = TextAlignment.Center,
                size = Theme.TEXT_SIZE_LARGE,
            ),
            Container(
                padding = 10.0,
                child = Button(
                    type = Elevated,
                    caption = getText("page.send-pending.button.submit"),
                    action = Action(
                        type = ActionType.Route,
                        url = "route:/~"
                    )
                )
            )
        )
    }

    private fun approve(tx: Transaction, tenant: Tenant): List<WidgetAware> {
        val fmt = DecimalFormat(tenant.monetaryFormat)
        val token = qrApi.encode(
            EncodeQRCodeRequest(
                type = EntityType.TRANSACTION_APPROVAL.name,
                id = tx.id,
            )
        ).token

        return listOf(
            Container(
                padding = 10.0,
                child = MoneyText(
                    value = tx.net,
                    numberFormat = tenant.numberFormat,
                    currency = tenant.currencySymbol
                ),
            ),
            Text(
                getText("page.send-pending.fees", arrayOf(fmt.format(tx.fees))),
                bold = true,
                size = Theme.TEXT_SIZE_LARGE
            ),
            Container(
                padding = 10.0,
                child = Container(
                    child = Text(
                        caption = getText("page.send-pending.requires-approval"),
                        alignment = TextAlignment.Center,
                        size = Theme.TEXT_SIZE_X_LARGE,
                    )
                ),
            ),
            Center(
                Image(
                    width = 230.0,
                    height = 230.0,
                    url = qrService.imageUrl(token)
                ),
            ),
            Flexible(
                child = Column(
                    mainAxisAlignment = MainAxisAlignment.end,
                    children = listOf(
                        Container(
                            alignment = Alignment.BottomCenter,
                            child = Button(
                                type = ButtonType.Text,
                                caption = getText("page.send-pending.button.close"),
                                action = gotoHome()
                            )
                        )
                    )
                )
            )
        )
    }
}
