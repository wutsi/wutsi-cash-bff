package com.wutsi.application.cash.endpoint.transaction.screen

import com.wutsi.application.cash.endpoint.Page
import com.wutsi.application.shared.Theme
import com.wutsi.application.shared.service.TenantProvider
import com.wutsi.flutter.sdui.Center
import com.wutsi.flutter.sdui.Column
import com.wutsi.flutter.sdui.Container
import com.wutsi.flutter.sdui.Screen
import com.wutsi.flutter.sdui.Text
import com.wutsi.flutter.sdui.Timeout
import com.wutsi.flutter.sdui.Widget
import com.wutsi.platform.account.WutsiAccountApi
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/transaction/processing")
class TransactionProcessingScreen(
    accountApi: WutsiAccountApi,
    tenantProvider: TenantProvider,
) : AbstractTransactionStatusScreen(accountApi, tenantProvider) {
    @PostMapping
    fun index(
        @RequestParam(name = "transaction-id") transactionId: String,
    ): Widget {
        val tx = paymentApi.getTransaction(transactionId).transaction
        val tenant = tenantProvider.get()

        return Screen(
            id = Page.TRANSACTION_PROCESSING,
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
                        child = Column(
                            children = listOf(
                                Center(
                                    Container(
                                        padding = 10.0,
                                        child = Text(getText("page.transaction.processing.message"))
                                    )
                                ),
                                Timeout(
                                    url = urlBuilder.build("widgets/transaction/status?transaction-id=$transactionId"),
                                    delay = 15
                                )
                            )
                        )
                    ),
                ),
            ),
        ).toWidget()
    }
}
