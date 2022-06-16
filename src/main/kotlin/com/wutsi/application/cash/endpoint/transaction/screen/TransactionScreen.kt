package com.wutsi.application.cash.endpoint.transaction.screen

import com.wutsi.application.cash.endpoint.AbstractQuery
import com.wutsi.application.cash.endpoint.Page
import com.wutsi.application.shared.Theme
import com.wutsi.application.shared.service.DateTimeUtil
import com.wutsi.application.shared.service.TenantProvider
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.AppBar
import com.wutsi.flutter.sdui.Column
import com.wutsi.flutter.sdui.Container
import com.wutsi.flutter.sdui.Divider
import com.wutsi.flutter.sdui.Flexible
import com.wutsi.flutter.sdui.Icon
import com.wutsi.flutter.sdui.Image
import com.wutsi.flutter.sdui.ListItem
import com.wutsi.flutter.sdui.Row
import com.wutsi.flutter.sdui.Screen
import com.wutsi.flutter.sdui.SingleChildScrollView
import com.wutsi.flutter.sdui.Text
import com.wutsi.flutter.sdui.Widget
import com.wutsi.flutter.sdui.WidgetAware
import com.wutsi.flutter.sdui.enums.ActionType
import com.wutsi.flutter.sdui.enums.CrossAxisAlignment
import com.wutsi.flutter.sdui.enums.MainAxisAlignment
import com.wutsi.flutter.sdui.enums.TextAlignment
import com.wutsi.platform.account.WutsiAccountApi
import com.wutsi.platform.account.dto.AccountSummary
import com.wutsi.platform.account.dto.PaymentMethodSummary
import com.wutsi.platform.account.dto.SearchAccountRequest
import com.wutsi.platform.payment.core.Status
import com.wutsi.platform.payment.dto.Transaction
import com.wutsi.platform.payment.entity.TransactionType
import com.wutsi.platform.tenant.dto.Tenant
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.text.DecimalFormat
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/transaction")
class TransactionScreen(
    private val tenantProvider: TenantProvider,
    private val accountApi: WutsiAccountApi,

    @Value("\${wutsi.application.store-url}") private val storeUrl: String
) : AbstractQuery() {
    @PostMapping
    fun index(@RequestParam id: String): Widget {
        val account = securityContext.currentAccount()

        val tenant = tenantProvider.get()
        val moneyFormat = DecimalFormat(tenant.monetaryFormat)
        val dateFormat = DateTimeFormatter.ofPattern(tenant.dateTimeFormat, LocaleContextHolder.getLocale())

        val tx = paymentApi.getTransaction(id).transaction
        val accounts = findAccounts(tx)
        val paymentMethods = findPaymentMethods()

        return Screen(
            id = Page.TRANSACTION,
            appBar = AppBar(
                elevation = 0.0,
                backgroundColor = Theme.COLOR_WHITE,
                foregroundColor = Theme.COLOR_BLACK,
                title = getText("page.transaction.app-bar.title", arrayOf(tx.id.uppercase().takeLast(4)))
            ),

            child = SingleChildScrollView(
                child = Column(
                    children = listOfNotNull(
                        toSectionWidget(
                            padding = null,
                            child = Column(
                                mainAxisAlignment = MainAxisAlignment.start,
                                crossAxisAlignment = CrossAxisAlignment.start,
                                children = listOfNotNull(
                                    Container(padding = 5.0),
                                    toRowWidget(
                                        "page.transaction.id",
                                        Text(tx.id, size = Theme.TEXT_SIZE_SMALL)
                                    ),
                                    Divider(color = Theme.COLOR_DIVIDER),

                                    toRowWidget(
                                        "page.transaction.date",
                                        DateTimeUtil.convert(tx.created, account.timezoneId).format(dateFormat)
                                    ),
                                    Divider(color = Theme.COLOR_DIVIDER),

                                    toRowWidget(
                                        "page.transaction.type",
                                        getText("transaction.type.${tx.type}")
                                    ),
                                    Divider(color = Theme.COLOR_DIVIDER),

                                    toRowWidget(
                                        "page.transaction.status",
                                        getText("transaction.status.${tx.status}"),
                                        bold = true,
                                        color = when (tx.status.uppercase()) {
                                            Status.FAILED.name -> Theme.COLOR_DANGER
                                            Status.PENDING.name -> Theme.COLOR_WARNING
                                            Status.SUCCESSFUL.name -> Theme.COLOR_SUCCESS
                                            else -> null
                                        }
                                    ),
                                    Divider(color = Theme.COLOR_DIVIDER),

                                    if (tx.status == Status.FAILED.name)
                                        Column(
                                            children = listOf(
                                                toRowWidget(
                                                    "page.transaction.error",
                                                    Text(
                                                        caption = getTransactionErrorMessage(tx.errorCode),
                                                        size = Theme.TEXT_SIZE_SMALL,
                                                        maxLines = 5,
                                                    )
                                                ),
                                                Divider(color = Theme.COLOR_DIVIDER),
                                            )
                                        )
                                    else
                                        null,

                                    toRowWidget("page.transaction.amount", moneyFormat.format(amount(tx))),
                                    Divider(color = Theme.COLOR_DIVIDER),

                                    toRowWidget("page.transaction.fees", moneyFormat.format(fees(tx))),
                                    Divider(color = Theme.COLOR_DIVIDER),

                                    toRowWidget("page.transaction.net", moneyFormat.format(net(tx))),

                                    if (tenant.testUserIds.contains(account.id))
                                        Column(
                                            children = listOf(
                                                Divider(color = Theme.COLOR_DIVIDER),
                                                toRowWidget(
                                                    "page.transaction.fees-gateway",
                                                    moneyFormat.format(tx.gatewayFees)
                                                )
                                            )
                                        )
                                    else
                                        null,
                                )
                            )
                        ),
                        toSectionWidget(
                            padding = null,
                            child = Column(
                                mainAxisAlignment = MainAxisAlignment.start,
                                crossAxisAlignment = CrossAxisAlignment.start,
                                children = listOfNotNull(
                                    toRowWidget(
                                        "page.transaction.from",
                                        toSenderWidget(tx, accounts, paymentMethods, tenant)
                                    ),
                                    Divider(color = Theme.COLOR_DIVIDER),
                                    toRowWidget(
                                        "page.transaction.to",
                                        toRecipientWidget(tx, accounts, paymentMethods, tenant)
                                    ),
                                    tx.orderId?.let {
                                        Divider(color = Theme.COLOR_DIVIDER)
                                    },
                                    tx.orderId?.let {
                                        toRowWidget("page.transaction.order", toOrderWidget(it))
                                    }
                                )
                            )
                        )
                    ),
                )
            ),
            bottomNavigationBar = bottomNavigationBar(),
            backgroundColor = Theme.COLOR_GRAY_LIGHT,
        ).toWidget()
    }

    private fun net(tx: Transaction): Double =
        if ((isSender(tx) && tx.applyFeesToSender) || (isRecipient(tx) && !tx.applyFeesToSender))
            tx.net * toAmountSign(tx)
        else
            tx.amount * toAmountSign(tx)

    private fun amount(tx: Transaction): Double =
        if (isSender(tx) || !tx.applyFeesToSender)
            tx.amount * toAmountSign(tx)
        else
            tx.net * toAmountSign(tx)

    private fun toAmountSign(tx: Transaction): Double =
        if (tx.type == TransactionType.CASHOUT.name ||
            (tx.type == TransactionType.CHARGE.name && isSender(tx)) ||
            (tx.type == TransactionType.TRANSFER.name && isSender(tx))
        )
            -1.0
        else
            1.0

    private fun fees(tx: Transaction): Double =
        if ((isSender(tx) && tx.applyFeesToSender) || (isRecipient(tx) && !tx.applyFeesToSender))
            tx.fees * toFeesSign(tx)
        else
            0.0

    private fun toFeesSign(tx: Transaction): Double =
        if ((isSender(tx) && tx.applyFeesToSender) || (isRecipient(tx) && !tx.applyFeesToSender))
            -toAmountSign(tx)
        else
            toAmountSign(tx)

    private fun isSender(tx: Transaction): Boolean =
        tx.accountId == securityContext.currentAccountId()

    private fun isRecipient(tx: Transaction): Boolean =
        tx.recipientId == securityContext.currentAccountId()

    private fun toSenderWidget(
        tx: Transaction,
        accounts: Map<Long, AccountSummary>,
        paymentMethods: Map<String, PaymentMethodSummary>,
        tenant: Tenant
    ): WidgetAware =
        if (tx.type == TransactionType.CASHIN.name)
            toPaymentProvideWidget(tx, paymentMethods, tenant)
        else
            accounts[tx.accountId]?.let { toAccountWidget(it) } ?: Container()

    private fun toRecipientWidget(
        tx: Transaction,
        accounts: Map<Long, AccountSummary>,
        paymentMethods: Map<String, PaymentMethodSummary>,
        tenant: Tenant
    ): WidgetAware =
        if (tx.type == TransactionType.CASHIN.name)
            accounts[tx.accountId]?.let { toAccountWidget(it) } ?: Container()
        else if (tx.type == TransactionType.CASHOUT.name)
            toPaymentProvideWidget(tx, paymentMethods, tenant)
        else
            accounts[tx.recipientId]?.let { toAccountWidget(it) } ?: Container()

    private fun toOrderWidget(orderId: String): WidgetAware =
        ListItem(
            caption = "#" + orderId.uppercase().takeLast(4),
            trailing = Icon(Theme.ICON_CHEVRON_RIGHT),
            action = gotoUrl(
                urlBuilder.build(storeUrl, "order?id=$orderId")
            )
        )

    private fun toPaymentProvideWidget(
        tx: Transaction,
        paymentMethods: Map<String, PaymentMethodSummary>,
        tenant: Tenant
    ): WidgetAware {
        val carrier = tenant.mobileCarriers.find { it.code.equals(tx.paymentMethodProvider, true) }
        return ListItem(
            caption = paymentMethods[tx.paymentMethodToken]?.phone?.number
                ?: paymentMethods[tx.paymentMethodToken]?.maskedNumber
                ?: "",
            leading = carrier?.let {
                tenantProvider.logo(it)
            }?.let {
                Image(
                    width = 48.0,
                    height = 48.0,
                    url = it
                )
            }
        )
    }

    private fun toAccountWidget(account: AccountSummary): WidgetAware =
        ListItem(
            caption = account.displayName ?: "",
            trailing = Icon(Theme.ICON_CHEVRON_RIGHT),
            action = Action(
                type = ActionType.Route,
                url = urlBuilder.build(shellUrl, "profile?id=${account.id}")
            ),
        )

    private fun toRowWidget(key: String, value: String?, color: String? = null, bold: Boolean? = null): WidgetAware =
        toRowWidget(
            key,
            Container(
                padding = 10.0,
                child = Text(
                    value ?: "",
                    alignment = TextAlignment.Left,
                    color = color,
                    bold = bold
                )
            ),
        )

    private fun toRowWidget(key: String, value: WidgetAware): WidgetAware =
        Row(
            children = listOf(
                Flexible(
                    flex = 1,
                    child = Container(
                        padding = 10.0,
                        child = Text(
                            getText(key),
                            bold = true,
                            alignment = TextAlignment.Right,
                        )
                    ),
                ),
                Flexible(
                    flex = 3,
                    child = value,
                )
            )
        )

    private fun findPaymentMethods(): Map<String, PaymentMethodSummary> =
        accountApi.listPaymentMethods(securityContext.currentAccountId())
            .paymentMethods
            .map { it.token to it }.toMap()

    private fun findAccounts(tx: Transaction): Map<Long, AccountSummary> =
        accountApi.searchAccount(
            SearchAccountRequest(
                ids = listOf(tx.accountId, tx.recipientId).filterNotNull(),
            )
        ).accounts.map { it.id to it }.toMap()
}
