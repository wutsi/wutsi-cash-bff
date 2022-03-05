package com.wutsi.application.cash.endpoint.history.screen

import com.wutsi.application.cash.endpoint.AbstractQuery
import com.wutsi.application.cash.endpoint.Page
import com.wutsi.application.shared.Theme
import com.wutsi.application.shared.service.DateTimeUtil
import com.wutsi.application.shared.service.SharedUIMapper
import com.wutsi.application.shared.service.TenantProvider
import com.wutsi.application.shared.ui.ProfileListItem
import com.wutsi.ecommerce.order.WutsiOrderApi
import com.wutsi.ecommerce.order.dto.Order
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.AppBar
import com.wutsi.flutter.sdui.Container
import com.wutsi.flutter.sdui.Flexible
import com.wutsi.flutter.sdui.Icon
import com.wutsi.flutter.sdui.Image
import com.wutsi.flutter.sdui.ListItem
import com.wutsi.flutter.sdui.ListView
import com.wutsi.flutter.sdui.Row
import com.wutsi.flutter.sdui.Screen
import com.wutsi.flutter.sdui.Text
import com.wutsi.flutter.sdui.Widget
import com.wutsi.flutter.sdui.WidgetAware
import com.wutsi.flutter.sdui.enums.ActionType
import com.wutsi.flutter.sdui.enums.TextAlignment
import com.wutsi.platform.account.WutsiAccountApi
import com.wutsi.platform.account.dto.AccountSummary
import com.wutsi.platform.account.dto.PaymentMethodSummary
import com.wutsi.platform.account.dto.SearchAccountRequest
import com.wutsi.platform.payment.dto.Transaction
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
    private val orderApi: WutsiOrderApi,
    private val sharedUIMapper: SharedUIMapper,

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
        val color = toColor(tx)
        val order = findOrder(tx)

        return Screen(
            id = Page.TRANSACTION,
            appBar = AppBar(
                elevation = 0.0,
                backgroundColor = Theme.COLOR_WHITE,
                foregroundColor = Theme.COLOR_BLACK,
                title = getText("page.transaction.app-bar.title")
            ),
            child = Container(
                child = ListView(
                    separator = true,
                    separatorColor = Theme.COLOR_DIVIDER,
                    children = listOfNotNull(
                        listItem(
                            "page.transaction.date",
                            DateTimeUtil.convert(tx.created, account.timezoneId).format(dateFormat)
                        ),
                        listItem(
                            "page.transaction.type",
                            if (tx.type == "TRANSFER")
                                if (tx.recipientId == securityContext.currentAccountId())
                                    getText("shared-ui.transaction.type.${tx.type}.receive")
                                else
                                    getText("shared-ui.transaction.type.${tx.type}.send")
                            else
                                getText("shared-ui.transaction.type.${tx.type}")
                        ),
                        listItem(
                            "page.transaction.status",
                            getText("shared-ui.transaction.status.${tx.status}"),
                            bold = true, color = color
                        ),
                        listItem("page.transaction.amount", moneyFormat.format(amount(tx)), color = color),
                        listItem("page.transaction.fees", moneyFormat.format(fees(tx)), color = color),
                        listItem("page.transaction.from", from(tx, accounts, paymentMethods, tenant)),
                        listItem("page.transaction.to", to(tx, accounts, paymentMethods, tenant)),
                        order?.let { listItem("page.transaction.order", order(it)) },
                    ),
                )
            ),
            bottomNavigationBar = bottomNavigationBar()
        ).toWidget()
    }

    private fun toColor(tx: Transaction): String =
        when (tx.status.uppercase()) {
            "FAILED" -> Theme.COLOR_DANGER
            "PENDING" -> Theme.COLOR_WARNING
            else -> when (tx.type.uppercase()) {
                "CASHIN" -> Theme.COLOR_SUCCESS
                "CASHOUT" -> Theme.COLOR_DANGER
                else -> if (tx.recipientId == securityContext.currentAccountId())
                    Theme.COLOR_SUCCESS
                else
                    Theme.COLOR_DANGER
            }
        }

    private fun amount(tx: Transaction): Double =
        if (isRecipient(tx))
            tx.net
        else
            tx.amount

    private fun fees(tx: Transaction): Double =
        if (tx.feesToSender)
            if (isSender(tx))
                tx.fees
            else
                0.0
        else if (isRecipient(tx))
            tx.fees
        else
            0.0

    private fun isSender(tx: Transaction): Boolean =
        if (tx.type == "CASHIN")
            false
        else
            tx.accountId == securityContext.currentAccountId()

    private fun isRecipient(tx: Transaction): Boolean =
        if (tx.type == "CASHIN")
            tx.accountId == securityContext.currentAccountId()
        else
            tx.recipientId == securityContext.currentAccountId()

    private fun from(
        tx: Transaction,
        accounts: Map<Long, AccountSummary>,
        paymentMethods: Map<String, PaymentMethodSummary>,
        tenant: Tenant
    ): WidgetAware =
        if (tx.type == "CASHIN")
            paymentProvider(tx, paymentMethods, tenant)
        else
            accounts[tx.accountId]?.let { account(it) } ?: Container()

    private fun to(
        tx: Transaction,
        accounts: Map<Long, AccountSummary>,
        paymentMethods: Map<String, PaymentMethodSummary>,
        tenant: Tenant
    ): WidgetAware =
        if (tx.type == "CASHIN")
            accounts[tx.accountId]?.let { account(it) } ?: Container()
        else if (tx.type == "CASHOUT")
            paymentProvider(tx, paymentMethods, tenant)
        else
            accounts[tx.recipientId]?.let { account(it) } ?: Container()

    private fun order(order: Order) = ListItem(
        caption = order.id,
        subCaption = getText("page.transaction.status") + ": " + getText("order.status.${order.status}"),
        trailing = Icon(Theme.ICON_CHEVRON_RIGHT),
        action = gotoUrl(
            urlBuilder.build(storeUrl, "order?id=${order.id}")
        )
    )

    private fun paymentProvider(
        tx: Transaction,
        paymentMethods: Map<String, PaymentMethodSummary>,
        tenant: Tenant
    ): WidgetAware {
        val carrier = tenant.mobileCarriers.find { it.code.equals(tx.paymentMethodProvider, true) }
        return ListItem(
            caption = paymentMethods[tx.paymentMethodToken]?.maskedNumber ?: "",
            leading = carrier?.let { tenantProvider.logo(it) }?.let {
                Image(
                    width = 48.0,
                    height = 48.0,
                    url = it
                )
            }
        )
    }

    private fun account(account: AccountSummary): WidgetAware =
        ProfileListItem(
            model = sharedUIMapper.toAccountModel(account),
            action = Action(
                type = ActionType.Route,
                url = urlBuilder.build(shellUrl, "profile?id=${account.id}")
            )
        )

    private fun listItem(key: String, value: String?, color: String? = null, bold: Boolean? = null) = listItem(
        key,
        Container(
            padding = 10.0,
            child = Text(
                value ?: "",
                alignment = TextAlignment.Left,
                size = Theme.TEXT_SIZE_SMALL,
                color = color,
                bold = bold
            )
        ),
    )

    private fun listItem(key: String, value: WidgetAware) = Row(
        children = listOf(
            Flexible(
                flex = 1,
                child = Container(
                    padding = 10.0,
                    child = Text(
                        getText(key),
                        bold = true,
                        alignment = TextAlignment.Right,
                        size = Theme.TEXT_SIZE_SMALL
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

    private fun findOrder(tx: Transaction): Order? =
        try {
            tx.orderId?.let {
                orderApi.getOrder(it).order
            }
        } catch (ex: Exception) {
            null
        }
}
