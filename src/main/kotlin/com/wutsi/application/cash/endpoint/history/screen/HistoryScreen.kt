package com.wutsi.application.cash.endpoint.history.screen

import com.wutsi.application.cash.endpoint.AbstractQuery
import com.wutsi.application.cash.endpoint.Page
import com.wutsi.application.cash.endpoint.Theme
import com.wutsi.application.cash.service.TenantProvider
import com.wutsi.application.cash.util.StringUtil
import com.wutsi.flutter.sdui.AppBar
import com.wutsi.flutter.sdui.CircleAvatar
import com.wutsi.flutter.sdui.Column
import com.wutsi.flutter.sdui.Container
import com.wutsi.flutter.sdui.Divider
import com.wutsi.flutter.sdui.Flexible
import com.wutsi.flutter.sdui.Image
import com.wutsi.flutter.sdui.ListView
import com.wutsi.flutter.sdui.Row
import com.wutsi.flutter.sdui.Screen
import com.wutsi.flutter.sdui.Text
import com.wutsi.flutter.sdui.Widget
import com.wutsi.flutter.sdui.WidgetAware
import com.wutsi.flutter.sdui.enums.Alignment
import com.wutsi.flutter.sdui.enums.CrossAxisAlignment
import com.wutsi.flutter.sdui.enums.MainAxisAlignment
import com.wutsi.flutter.sdui.enums.TextAlignment
import com.wutsi.platform.account.WutsiAccountApi
import com.wutsi.platform.account.dto.AccountSummary
import com.wutsi.platform.account.dto.PaymentMethodSummary
import com.wutsi.platform.account.dto.SearchAccountRequest
import com.wutsi.platform.payment.dto.SearchTransactionRequest
import com.wutsi.platform.payment.dto.TransactionSummary
import com.wutsi.platform.tenant.dto.MobileCarrier
import com.wutsi.platform.tenant.dto.Tenant
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.text.DecimalFormat
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/history")
class HistoryScreen(
    private val tenantProvider: TenantProvider,
    private val accountApi: WutsiAccountApi,
) : AbstractQuery() {
    @PostMapping
    fun index(): Widget {
        val limit = 30
        val tenant = tenantProvider.get()
        val balance = getBalance(tenant)
        val balanceText = DecimalFormat(tenant.monetaryFormat).format(balance.value)

        return Screen(
            id = Page.HISTORY,
            appBar = AppBar(
                elevation = 0.0,
                backgroundColor = Theme.COLOR_WHITE,
                foregroundColor = Theme.COLOR_BLACK,
                title = getText("page.history.app-bar.title", arrayOf(balanceText))
            ),
            child = Column(
                children = listOf(
                    Container(
                        padding = 10.0,
                        child = Text(getText("page.history.title", arrayOf(limit))),
                    ),
                    Divider(color = Theme.COLOR_DIVIDER, height = 1.0),
                    Flexible(
                        child = transactionsWidget(limit, tenant)
                    )
                )
            ),
        ).toWidget()
    }

    private fun transactionsWidget(limit: Int, tenant: Tenant): WidgetAware {
        val txs = findTransactions(limit)
        if (txs.isEmpty())
            return Container(
                padding = 20.0,
                alignment = Alignment.TopCenter,
                child = Text(
                    getText("page.history.no-transaction"),
                    bold = true,
                    size = Theme.TEXT_SIZE_LARGE,
                    alignment = TextAlignment.Center
                )
            )

        val accounts = findAccounts(txs)
        val paymentMethods = findPaymentMethods()
        return Flexible(
            child = ListView(
                separator = true,
                children = txs.map { toListItem(it, accounts, paymentMethods, tenant) }
            )
        )
    }

    private fun findTransactions(limit: Int): List<TransactionSummary> =
        paymentApi.searchTransaction(
            SearchTransactionRequest(
                accountId = securityManager.currentUserId(),
                limit = limit,
                offset = 0
            )
        ).transactions

    private fun findPaymentMethods(): Map<String, PaymentMethodSummary> =
        accountApi.listPaymentMethods(securityManager.currentUserId())
            .paymentMethods
            .map { it.token to it }.toMap()

    private fun findAccounts(txs: List<TransactionSummary>): Map<Long, AccountSummary> {
        val accountIds = txs.map { it.accountId }.toMutableSet()
        accountIds.addAll(txs.mapNotNull { it.recipientId })

        return accountApi.searchAccount(
            SearchAccountRequest(
                ids = accountIds.toList(),
                limit = accountIds.size
            )
        ).accounts.map { it.id to it }.toMap()
    }

    private fun toListItem(
        tx: TransactionSummary,
        accounts: Map<Long, AccountSummary>,
        paymentMethods: Map<String, PaymentMethodSummary>,
        tenant: Tenant
    ): WidgetAware =
        Container(
            padding = 10.0,
            child = Row(
                crossAxisAlignment = CrossAxisAlignment.start,
                mainAxisAlignment = MainAxisAlignment.start,
                children = listOf(
                    Flexible(
                        flex = 2,
                        child = Container(
                            alignment = Alignment.TopLeft,
                            child = icon(tx, accounts, tenant),
                        ),
                    ),
                    Flexible(
                        flex = 6,
                        child = Container(
                            alignment = Alignment.TopLeft,
                            child = caption(tx, accounts, paymentMethods),
                            padding = 5.0,
                        ),
                    ),
                    Flexible(
                        flex = 4,
                        child = Container(
                            alignment = Alignment.TopRight,
                            child = amount(tx, tenant),
                            padding = 5.0,
                        ),
                    ),
                )
            )
        )

    private fun icon(tx: TransactionSummary, accounts: Map<Long, AccountSummary>, tenant: Tenant): WidgetAware {
        if (tx.type == "CASHIN" || tx.type == "CASHOUT") {
            val carrier = getMobileCarrier(tx, tenant)
            return Image(width = 48.0, height = 48.0, url = carrier?.let { tenantProvider.logo(it) } ?: "")
        } else {
            val account = getAccount(tx, accounts)
            return CircleAvatar(
                radius = 24.0,
                child = account?.pictureUrl?.let { Image(width = 48.0, height = 48.0, url = it) }
                    ?: Text(
                        caption = StringUtil.initials(account?.displayName),
                        size = Theme.TEXT_SIZE_X_LARGE,
                        bold = true
                    )
            )
        }
    }

    private fun caption(
        tx: TransactionSummary,
        accounts: Map<Long, AccountSummary>,
        paymentMethods: Map<String, PaymentMethodSummary>
    ): WidgetAware {
        val children = mutableListOf<WidgetAware>(
            Text(
                caption = toCaption1(tx)
            ),
        )

        val caption2 = toCaption2(tx, accounts, paymentMethods)
        if (caption2 != null) {
            children.add(
                Text(caption = caption2, color = Theme.COLOR_GRAY)
            )
        }

        if (tx.status != "SUCCESSFUL") {
            children.add(
                Text(
                    caption = getText("transaction.status.${tx.status}"),
                    bold = true,
                    color = toColor(tx),
                    size = Theme.TEXT_SIZE_SMALL
                )
            )
        }
        return Column(
            mainAxisAlignment = MainAxisAlignment.spaceBetween,
            crossAxisAlignment = CrossAxisAlignment.start,
            children = children,
        )
    }

    private fun amount(tx: TransactionSummary, tenant: Tenant): WidgetAware {
        val moneyFormat = DecimalFormat(tenant.monetaryFormat)
        val locale = LocaleContextHolder.getLocale()
        val dateFormat = DateTimeFormatter.ofPattern("dd MMM yyyy", locale)
        return Column(
            mainAxisAlignment = MainAxisAlignment.spaceBetween,
            crossAxisAlignment = CrossAxisAlignment.end,
            children = listOf(
                Text(
                    caption = moneyFormat.format(toDisplayAmount(tx)),
                    bold = true,
                    color = toColor(tx),
                    alignment = TextAlignment.Right
                ),
                Text(
                    caption = tx.created.format(dateFormat),
                    size = Theme.TEXT_SIZE_SMALL,
                    alignment = TextAlignment.Right
                )
            ),
        )
    }

    private fun toDisplayAmount(tx: TransactionSummary): Double =
        when (tx.type.uppercase()) {
            "CASHOUT" -> -tx.amount
            "CASHIN" -> tx.amount
            else -> if (tx.recipientId == securityManager.currentUserId())
                tx.amount
            else
                -tx.amount
        }

    private fun toColor(tx: TransactionSummary): String =
        when (tx.status.uppercase()) {
            "FAILED" -> Theme.COLOR_DANGER
            "PENDING" -> Theme.COLOR_WARNING
            else -> when (tx.type.uppercase()) {
                "CASHIN" -> Theme.COLOR_SUCCESS
                "CASHOUT" -> Theme.COLOR_DANGER
                else -> if (tx.recipientId == securityManager.currentUserId())
                    Theme.COLOR_SUCCESS
                else
                    Theme.COLOR_DANGER
            }
        }

    private fun toCaption1(
        tx: TransactionSummary
    ): String {
        if (tx.type == "CASHIN") {
            return getText("page.history.cashin.caption")
        } else if (tx.type == "CASHOUT") {
            return getText("page.history.cashout.caption")
        } else {
            return if (tx.accountId == securityManager.currentUserId())
                getText("page.history.transfer.to.caption")
            else
                getText("page.history.transfer.from.caption")
        }
    }

    private fun toCaption2(
        tx: TransactionSummary,
        accounts: Map<Long, AccountSummary>,
        paymentMethods: Map<String, PaymentMethodSummary>
    ): String? {
        if (tx.type == "CASHIN" || tx.type == "CASHOUT") {
            val paymentMethod = paymentMethods[tx.paymentMethodToken]
            return getPhoneNumber(paymentMethod)
        } else {
            val account = getAccount(tx, accounts)
            return account?.displayName
        }
    }

    private fun getPhoneNumber(paymentMethod: PaymentMethodSummary?): String =
        formattedPhoneNumber(paymentMethod?.phone?.number, paymentMethod?.phone?.country)
            ?: paymentMethod?.maskedNumber
            ?: ""

    private fun getAccount(tx: TransactionSummary, accounts: Map<Long, AccountSummary>): AccountSummary? =
        if (tx.accountId == securityManager.currentUserId())
            accounts[tx.recipientId]
        else
            accounts[tx.accountId]

    private fun getMobileCarrier(tx: TransactionSummary, tenant: Tenant): MobileCarrier? =
        tenant.mobileCarriers.find { it.code.equals(tx.paymentMethodProvider, true) }
}
