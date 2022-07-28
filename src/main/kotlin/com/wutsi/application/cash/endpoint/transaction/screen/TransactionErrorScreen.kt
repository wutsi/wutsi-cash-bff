package com.wutsi.application.cash.endpoint.transaction.screen

import com.wutsi.application.cash.endpoint.Page
import com.wutsi.application.shared.Theme
import com.wutsi.application.shared.service.TenantProvider
import com.wutsi.flutter.sdui.Column
import com.wutsi.flutter.sdui.Screen
import com.wutsi.flutter.sdui.Widget
import com.wutsi.platform.account.WutsiAccountApi
import com.wutsi.platform.payment.dto.Transaction
import com.wutsi.platform.payment.entity.TransactionType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/transaction/error")
class TransactionErrorScreen(
    accountApi: WutsiAccountApi,
    tenantProvider: TenantProvider,
) : AbstractTransactionStatusScreen(accountApi, tenantProvider) {
    @PostMapping
    fun index(
        @RequestParam error: String,
        @RequestParam amount: Double,
        @RequestParam type: TransactionType,
        @RequestParam(name = "recipient-id", required = false) recipientId: Long? = null,
        @RequestParam(name = "payment-token", required = false) paymentToken: String? = null,
    ): Widget {
        val tenant = tenantProvider.get()
        val tx = Transaction(
            type = type.name,
            recipientId = recipientId,
            accountId = securityContext.currentAccountId(),
            amount = amount,
            paymentMethodToken = paymentToken,
            currency = tenant.currency
        )

        return Screen(
            id = Page.TRANSACTION_ERROR,
            backgroundColor = Theme.COLOR_GRAY_LIGHT,
            appBar = null,
            safe = true,
            child = Column(
                children = listOf(
                    toSectionWidget(
                        padding = null,
                        child = toSection1(tx, tenant)
                    ),
                    toSectionWidget(
                        child = toTransactionStatusWidget(tx, error)
                    ),
                ),
            ),
        ).toWidget()
    }
}
