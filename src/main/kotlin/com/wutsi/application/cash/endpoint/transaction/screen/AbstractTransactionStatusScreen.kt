package com.wutsi.application.cash.endpoint.transaction.screen

import com.wutsi.application.cash.endpoint.AbstractQuery
import com.wutsi.application.shared.Theme
import com.wutsi.application.shared.service.TenantProvider
import com.wutsi.application.shared.ui.Avatar
import com.wutsi.flutter.sdui.Column
import com.wutsi.flutter.sdui.Container
import com.wutsi.flutter.sdui.Icon
import com.wutsi.flutter.sdui.MoneyText
import com.wutsi.flutter.sdui.Row
import com.wutsi.flutter.sdui.WidgetAware
import com.wutsi.flutter.sdui.enums.Alignment
import com.wutsi.flutter.sdui.enums.CrossAxisAlignment
import com.wutsi.flutter.sdui.enums.MainAxisAlignment
import com.wutsi.flutter.sdui.enums.MainAxisSize
import com.wutsi.platform.account.WutsiAccountApi
import com.wutsi.platform.account.dto.SearchAccountRequest
import com.wutsi.platform.payment.dto.Transaction
import com.wutsi.platform.payment.entity.TransactionType
import com.wutsi.platform.tenant.dto.Tenant

abstract class AbstractTransactionStatusScreen(
    protected val accountApi: WutsiAccountApi,
    protected val tenantProvider: TenantProvider,
) : AbstractQuery() {
    companion object {
        const val ICON_SIZE = 48.0
    }

    protected fun toSection1(tx: Transaction, tenant: Tenant): WidgetAware =
        Column(
            children = listOf(
                Container(
                    padding = 10.0,
                    child = MoneyText(
                        value = tx.amount,
                        currency = tenant.currencySymbol,
                        numberFormat = tenant.numberFormat,
                        color = Theme.COLOR_PRIMARY
                    )
                ),
                Container(
                    padding = 10.0,
                    alignment = Alignment.Center,
                    child = when (tx.type) {
                        TransactionType.TRANSFER.name -> toTransferWidget(tx)
                        TransactionType.CASHIN.name -> toCashInOut(tx, tenant)
                        TransactionType.CASHOUT.name -> toCashInOut(tx, tenant)
                        else -> null
                    }
                )
            )
        )

    private fun toTransferWidget(tx: Transaction): WidgetAware {
        val accounts = accountApi.searchAccount(
            request = SearchAccountRequest(
                ids = listOfNotNull(tx.recipientId, tx.accountId),
                limit = 2
            )
        ).accounts.associateBy { it.id }
        val sender = accounts[tx.accountId]
        val recipient = accounts[tx.recipientId]

        return toRow(
            sender?.let {
                Avatar(
                    model = sharedUIMapper.toAccountModel(it),
                    radius = ICON_SIZE / 2
                )
            },
            recipient?.let {
                Avatar(
                    model = sharedUIMapper.toAccountModel(it),
                    radius = ICON_SIZE / 2
                )
            },
        )
    }

    private fun toCashInOut(tx: Transaction, tenant: Tenant): WidgetAware? {
        val account = accountApi.getAccount(tx.accountId).account

        val paymentMethod = accountApi.listPaymentMethods(tx.accountId).paymentMethods
            .find { it.token == tx.paymentMethodToken }
            ?: return null

        val carrier = tenantProvider.mobileCarriers(tenant)
            .find { it.code.equals(paymentMethod.provider, true) }
            ?: return null
        val carrierIconUrl = tenantProvider.logo(carrier)

        return if (tx.type == TransactionType.CASHIN.name)
            toRow(
                carrierIconUrl?.let {
                    Icon(
                        code = carrierIconUrl,
                        size = ICON_SIZE
                    )
                },
                account.let {
                    Avatar(
                        model = sharedUIMapper.toAccountModel(it),
                        radius = ICON_SIZE / 2
                    )
                },
            )
        else
            toRow(
                account.let {
                    Avatar(
                        model = sharedUIMapper.toAccountModel(it),
                        radius = ICON_SIZE / 2
                    )
                },
                carrierIconUrl?.let {
                    Icon(
                        code = carrierIconUrl,
                        size = ICON_SIZE
                    )
                }
            )
    }

    private fun toRow(widget1: WidgetAware?, widget2: WidgetAware?) = Row(
        mainAxisAlignment = MainAxisAlignment.center,
        crossAxisAlignment = CrossAxisAlignment.center,
        mainAxisSize = MainAxisSize.min,
        children = listOfNotNull(
            widget1?.let {
                Container(
                    padding = 5.0,
                    child = it
                )
            },
            Icon(code = Theme.ICON_ARROW_FORWARD),
            widget2?.let {
                Container(
                    padding = 5.0,
                    child = it
                )
            },
        )
    )
}
