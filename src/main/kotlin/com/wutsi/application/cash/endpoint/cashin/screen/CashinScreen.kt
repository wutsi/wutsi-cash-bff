package com.wutsi.application.cash.endpoint.cashin.screen

import com.wutsi.application.cash.endpoint.AbstractQuery
import com.wutsi.application.cash.endpoint.Page
import com.wutsi.application.cash.endpoint.Theme
import com.wutsi.application.cash.service.TenantProvider
import com.wutsi.application.cash.service.URLBuilder
import com.wutsi.application.cash.service.UserProvider
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.AppBar
import com.wutsi.flutter.sdui.Column
import com.wutsi.flutter.sdui.Container
import com.wutsi.flutter.sdui.DropdownButton
import com.wutsi.flutter.sdui.DropdownMenuItem
import com.wutsi.flutter.sdui.Form
import com.wutsi.flutter.sdui.Input
import com.wutsi.flutter.sdui.MoneyWithKeyboard
import com.wutsi.flutter.sdui.Screen
import com.wutsi.flutter.sdui.Widget
import com.wutsi.flutter.sdui.enums.ActionType.Command
import com.wutsi.flutter.sdui.enums.Alignment.Center
import com.wutsi.flutter.sdui.enums.InputType.Submit
import com.wutsi.platform.account.WutsiAccountApi
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/cashin")
class CashinScreen(
    private val urlBuilder: URLBuilder,
    private val tenantProvider: TenantProvider,
    private val accountApi: WutsiAccountApi,
    private val userProvider: UserProvider
) : AbstractQuery() {
    @PostMapping
    fun index(): Widget {
        val tenant = tenantProvider.get()
        val paymentMethods = accountApi.listPaymentMethods(
            userProvider.id()
        ).paymentMethods

        return Screen(
            id = Page.CASHIN,
            appBar = AppBar(
                elevation = 0.0,
                backgroundColor = Theme.WHITE_COLOR,
                foregroundColor = Theme.BLACK_COLOR,
                title = getText("page.cashin.app-bar.title")
            ),
            child = Container(
                alignment = Center,
                padding = 10.0,
                child = Column(
                    children = listOf(
                        Form(
                            children = listOf(
                                Container(
                                    padding = 10.0,
                                    child = DropdownButton(
                                        value = paymentMethods[0].token,
                                        name = "paymentToken",
                                        required = true,
                                        children = paymentMethods.map {
                                            DropdownMenuItem(
                                                caption = it.maskedNumber,
                                                value = it.token,
                                                icon = getMobileCarrier(it, tenant)?.let { tenantProvider.logo(it) }
                                            )
                                        }
                                    ),
                                ),
                                Container(
                                    padding = 10.0,
                                    child = MoneyWithKeyboard(
                                        name = "amount",
                                        maxLength = 7,
                                        currency = tenant.currency,
                                        deleteText = getText("keyboard.delete"),
                                        moneyColor = Theme.PRIMARY_COLOR,
                                        numberFormat = tenant.numberFormat,
                                        keyboardButtonSize = 70.0
                                    ),
                                ),
                                Container(
                                    child = Input(
                                        name = "command",
                                        type = Submit,
                                        caption = getText("page.cashin.button.submit"),
                                        action = Action(
                                            type = Command,
                                            url = urlBuilder.build("commands/cashin")
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            ),
        ).toWidget()
    }
}