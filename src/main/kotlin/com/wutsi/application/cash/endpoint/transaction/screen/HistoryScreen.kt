package com.wutsi.application.cash.endpoint.transaction.screen

import com.wutsi.application.cash.endpoint.AbstractQuery
import com.wutsi.application.cash.endpoint.Page
import com.wutsi.application.shared.Theme
import com.wutsi.application.shared.service.TenantProvider
import com.wutsi.application.shared.ui.TransactionListItem
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.AppBar
import com.wutsi.flutter.sdui.Column
import com.wutsi.flutter.sdui.Container
import com.wutsi.flutter.sdui.Divider
import com.wutsi.flutter.sdui.Flexible
import com.wutsi.flutter.sdui.ListView
import com.wutsi.flutter.sdui.MoneyText
import com.wutsi.flutter.sdui.Screen
import com.wutsi.flutter.sdui.Text
import com.wutsi.flutter.sdui.Widget
import com.wutsi.flutter.sdui.WidgetAware
import com.wutsi.flutter.sdui.enums.ActionType
import com.wutsi.flutter.sdui.enums.Alignment
import com.wutsi.flutter.sdui.enums.MainAxisAlignment
import com.wutsi.flutter.sdui.enums.TextAlignment
import com.wutsi.platform.account.WutsiAccountApi
import com.wutsi.platform.account.dto.AccountSummary
import com.wutsi.platform.account.dto.PaymentMethodSummary
import com.wutsi.platform.account.dto.SearchAccountRequest
import com.wutsi.platform.payment.core.Status
import com.wutsi.platform.payment.dto.SearchTransactionRequest
import com.wutsi.platform.payment.dto.TransactionSummary
import com.wutsi.platform.tenant.dto.Tenant
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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

        return Screen(
            id = Page.HISTORY,
            appBar = AppBar(
                elevation = 0.0,
                backgroundColor = Theme.COLOR_WHITE,
                foregroundColor = Theme.COLOR_BLACK,
                title = getText("page.history.app-bar.title")
            ),
            child = Column(
                mainAxisAlignment = MainAxisAlignment.center,
                children = listOf(
                    Text(getText("page.history.balance")),
                    MoneyText(
                        value = balance.value,
                        currency = balance.currency,
                        numberFormat = tenant.numberFormat
                    ),
                    Text(getText("page.history.title", arrayOf(limit))),
                    Divider(color = Theme.COLOR_DIVIDER),
                    Flexible(
                        child = transactionsWidget(limit, tenant)
                    )
                ),
            ),
            bottomNavigationBar = bottomNavigationBar()
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
        val currentUser = securityContext.currentAccount()
        return Flexible(
            child = ListView(
                separator = true,
                children = txs.map {
                    TransactionListItem(
                        action = Action(
                            type = ActionType.Route,
                            url = urlBuilder.build("transaction?id=${it.id}")
                        ),
                        model = sharedUIMapper.toTransactionModel(
                            it,
                            currentUser = currentUser,
                            accounts = accounts,
                            paymentMethod = it.paymentMethodToken?.let { paymentMethods[it] },
                            tenant = tenant,
                            tenantProvider = tenantProvider,
                        )
                    )
                }
            )
        )
    }

    private fun findTransactions(limit: Int): List<TransactionSummary> =
        paymentApi.searchTransaction(
            SearchTransactionRequest(
                accountId = securityContext.currentAccountId(),
                status = listOf(
                    Status.PENDING.name,
                    Status.SUCCESSFUL.name
                ),
                limit = limit,
                offset = 0
            )
        ).transactions

    private fun findPaymentMethods(): Map<String, PaymentMethodSummary> =
        accountApi.listPaymentMethods(securityContext.currentAccountId())
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
}
