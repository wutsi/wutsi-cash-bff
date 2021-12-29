package com.wutsi.application.cash.endpoint.pay.screen

import com.wutsi.application.cash.endpoint.AbstractQuery
import com.wutsi.application.cash.endpoint.Page
import com.wutsi.application.cash.endpoint.Theme
import com.wutsi.application.cash.service.TenantProvider
import com.wutsi.application.cash.service.URLBuilder
import com.wutsi.flutter.sdui.AppBar
import com.wutsi.flutter.sdui.Column
import com.wutsi.flutter.sdui.Container
import com.wutsi.flutter.sdui.Flexible
import com.wutsi.flutter.sdui.MoneyText
import com.wutsi.flutter.sdui.QrView
import com.wutsi.flutter.sdui.Screen
import com.wutsi.flutter.sdui.Text
import com.wutsi.flutter.sdui.Widget
import com.wutsi.flutter.sdui.enums.Alignment
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/pay/scan")
class PayScanScreen(
    private val urlBuilder: URLBuilder,
    private val tenantProvider: TenantProvider,
) : AbstractQuery() {
    @PostMapping
    fun index(@RequestParam amount: Double): Widget {
        val tenant = tenantProvider.get()
        return Screen(
            id = Page.PAY_SCAN,
            appBar = AppBar(
                elevation = 0.0,
                backgroundColor = Theme.WHITE_COLOR,
                foregroundColor = Theme.BLACK_COLOR,
                title = getText("page.pay-span.app-bar.title"),
            ),
            child = Column(
                children = listOf(
                    Container(
                        padding = 10.0,
                        alignment = Alignment.Center,
                        child = MoneyText(
                            value = amount,
                            currency = tenant.currency,
                            numberFormat = tenant.numberFormat
                        )
                    ),
                    Container(
                        padding = 10.0,
                        alignment = Alignment.Center,
                        child = Text(getText("page.pay-span.title"), size = Theme.LARGE_TEXT_SIZE),
                    ),
                    Flexible(
                        child = QrView(
                            submitUrl = urlBuilder.build("/pay/process?amount=$amount")
                        )
                    ),
                ),
            )
        ).toWidget()
    }
}
