package com.wutsi.application.cash.endpoint.cashin.screen

import com.wutsi.application.cash.endpoint.AbstractQuery
import com.wutsi.application.cash.endpoint.Page
import com.wutsi.application.shared.Theme
import com.wutsi.application.shared.service.TenantProvider
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.AppBar
import com.wutsi.flutter.sdui.Button
import com.wutsi.flutter.sdui.Column
import com.wutsi.flutter.sdui.Container
import com.wutsi.flutter.sdui.Icon
import com.wutsi.flutter.sdui.Screen
import com.wutsi.flutter.sdui.Text
import com.wutsi.flutter.sdui.Widget
import com.wutsi.flutter.sdui.enums.ActionType.Route
import com.wutsi.flutter.sdui.enums.Alignment.Center
import com.wutsi.flutter.sdui.enums.ButtonType.Elevated
import com.wutsi.flutter.sdui.enums.TextAlignment
import com.wutsi.platform.tenant.dto.Tenant
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.text.DecimalFormat

@RestController
@RequestMapping("/cashin/success")
class CashinSuccessScreen(private val tenantProvider: TenantProvider) : AbstractQuery() {
    companion object {
        const val ICON_SIZE = 80.0
    }

    @PostMapping
    fun index(
        @RequestParam amount: Double,
        @RequestParam(name = "error", required = false) error: String? = null
    ): Widget {
        val tenant = tenantProvider.get()
        return Screen(
            id = error?.let { Page.CASHIN_ERROR } ?: Page.CASHIN_SUCCESS,
            safe = true,
            appBar = AppBar(
                elevation = 0.0,
                backgroundColor = Theme.COLOR_WHITE,
                automaticallyImplyLeading = false
            ),
            child = Column(
                children = listOf(
                    Container(padding = 40.0),
                    Container(
                        alignment = Center,
                        padding = 20.0,
                        child = getIcon(error)
                    ),
                    Container(
                        alignment = Center,
                        padding = 10.0,
                        child = getMessage(amount, error, tenant)
                    ),
                    Container(
                        padding = 10.0,
                        child = Button(
                            type = Elevated,
                            caption = getText("page.cashin-success.button.submit"),
                            action = Action(
                                type = Route,
                                url = "route:/~"
                            )
                        )
                    )
                )
            ),
        ).toWidget()
    }

    private fun getIcon(error: String?): Icon =
        if (error != null)
            Icon(
                code = Theme.ICON_ERROR,
                size = ICON_SIZE,
                color = Theme.COLOR_DANGER
            )
        else
            Icon(
                code = Theme.ICON_CHECK_CIRCLE,
                size = ICON_SIZE,
                color = Theme.COLOR_SUCCESS
            )

    private fun getMessage(amount: Double, error: String?, tenant: Tenant): Text =
        if (error != null)
            Text(
                caption = error,
                color = Theme.COLOR_DANGER,
                bold = true,
                alignment = TextAlignment.Center,
            )
        else
            Text(
                caption = getText(
                    "page.cashin-success.message",
                    arrayOf(DecimalFormat(tenant.monetaryFormat).format(amount))
                ),
                color = Theme.COLOR_SUCCESS,
                bold = true,
                alignment = TextAlignment.Center,
            )
}
