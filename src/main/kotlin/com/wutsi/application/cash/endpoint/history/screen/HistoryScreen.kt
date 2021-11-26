package com.wutsi.application.cash.endpoint.history.screen

import com.wutsi.application.cash.endpoint.AbstractQuery
import com.wutsi.application.cash.endpoint.Page
import com.wutsi.application.cash.endpoint.Theme
import com.wutsi.application.cash.service.TenantProvider
import com.wutsi.application.cash.service.UserProvider
import com.wutsi.application.cash.util.StringUtil
import com.wutsi.flutter.sdui.AppBar
import com.wutsi.flutter.sdui.CircleAvatar
import com.wutsi.flutter.sdui.Column
import com.wutsi.flutter.sdui.Container
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
import com.wutsi.platform.account.WutsiAccountApi
import com.wutsi.platform.account.dto.AccountSummary
import com.wutsi.platform.account.dto.SearchAccountRequest
import com.wutsi.platform.payment.WutsiPaymentApi
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
    private val userProvider: UserProvider,
    private val paymentApi: WutsiPaymentApi,
) : AbstractQuery() {
    @PostMapping
    fun index(): Widget {
        val tenant = tenantProvider.get()
        val txs = findTransactions(tenant)
        val accounts = findAccounts(txs)

        return Screen(
            id = Page.HISTORY,
            appBar = AppBar(
                elevation = 0.0,
                backgroundColor = Theme.WHITE_COLOR,
                foregroundColor = Theme.BLACK_COLOR,
                title = getText("page.history.app-bar.title")
            ),
            child = ListView(
                separator = true,
                children = txs.map { toListItem(it, accounts, tenant) }
            ),
        ).toWidget()
    }

    private fun findTransactions(tenant: Tenant): List<TransactionSummary> =
        paymentApi.searchTransaction(
            SearchTransactionRequest(
                userId = userProvider.id(),
                tenantId = tenant.id,
                limit = 30,
                offset = 0
            )
        ).transactions

    private fun findAccounts(txs: List<TransactionSummary>): Map<Long, AccountSummary> {
        if (txs.isEmpty())
            return emptyMap()

        val accountIds = txs.map { it.userId }.toMutableSet()
        accountIds.addAll(txs.mapNotNull { it.recipientId })

        return accountApi.searchAccount(
            SearchAccountRequest(
                ids = accountIds.toList(),
                limit = accountIds.size
            )
        ).accounts.map { it.id to it }.toMap()
    }

    private fun toListItem(tx: TransactionSummary, accounts: Map<Long, AccountSummary>, tenant: Tenant): WidgetAware =
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
                            child = caption(tx, accounts, tenant),
                            padding = 10.0,
                        ),
                    ),
                    Flexible(
                        flex = 4,
                        child = Container(
                            alignment = Alignment.TopRight,
                            child = amount(tx, tenant),
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
                        size = Theme.LARGE_TEXT_SIZE,
                        bold = true
                    )
            )
        }
    }

    private fun caption(tx: TransactionSummary, accounts: Map<Long, AccountSummary>, tenant: Tenant): WidgetAware {
        val children = mutableListOf<WidgetAware>(
            Text(
                caption = toCaption1(tx, accounts, tenant),
                bold = true,
            ),
        )

        val sub = toCaption2(tx, tenant)
        if (sub != null) {
            children.add(
                Text(caption = sub)
            )
        }

        if (tx.status != "SUCCESSFUL") {
            children.add(
                Text(
                    caption = getText("transaction.status.${tx.status}"),
                    bold = true,
                    color = color(tx),
                    size = Theme.SMALL_TEXT_SIZE
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
                Container(
                    padding = 5.0,
                    alignment = Alignment.TopRight,
                    child = Text(
                        caption = moneyFormat.format(tx.amount),
                        bold = true,
                        color = color(tx),
                    ),
                ),
                Container(
                    padding = 5.0,
                    alignment = Alignment.TopRight,
                    child = Text(
                        caption = tx.created.format(dateFormat),
                        size = Theme.SMALL_TEXT_SIZE,
                    )
                ),
            ),
        )
    }

    private fun color(tx: TransactionSummary): String =
        when (tx.status) {
            "SUCCESSFUL" -> Theme.PRIMARY_COLOR
            "FAILED" -> Theme.DANGER_COLOR
            "PENDING" -> Theme.WARNING_COLOR
            else -> Theme.BLACK_COLOR
        }

    private fun toCaption1(tx: TransactionSummary, accounts: Map<Long, AccountSummary>, tenant: Tenant): String {
        if (tx.type == "CASHIN") {
            val carrier = getMobileCarrier(tx, tenant)
            return getText("page.history.cashout.caption", arrayOf(carrier?.name ?: ""))
        } else if (tx.type == "CASHOUT") {
            val carrier = getMobileCarrier(tx, tenant)
            return getText("page.history.cashin.caption", arrayOf(carrier?.name ?: ""))
        } else {
            return if (tx.userId == userProvider.id()) {
                val account = accounts[tx.userId]
                getText("page.history.transfer.to.caption", arrayOf(account?.displayName ?: ""))
            } else {
                val account = accounts[tx.recipientId]
                getText("page.history.transfer.from.caption", arrayOf(account?.displayName ?: ""))
            }
        }
    }

    private fun toCaption2(tx: TransactionSummary, tenant: Tenant): String? {
        if (tx.type == "CASHIN" || tx.type == "CASHOUT") {
            val carrier = getMobileCarrier(tx, tenant)
            return carrier?.name
        } else {
            return tx.description
        }
    }

    private fun getAccount(tx: TransactionSummary, accounts: Map<Long, AccountSummary>): AccountSummary? =
        if (tx.userId == userProvider.id())
            accounts[tx.userId]
        else
            accounts[tx.recipientId]

    private fun getMobileCarrier(tx: TransactionSummary, tenant: Tenant): MobileCarrier? =
        tenant.mobileCarriers.find { it.code.equals(tx.paymentMethodProvider, true) }
}
