package com.wutsi.application.cash.endpoint.send.screen

import com.wutsi.application.cash.endpoint.AbstractQuery
import com.wutsi.application.cash.endpoint.Page
import com.wutsi.application.shared.Theme
import com.wutsi.application.shared.service.CategoryService
import com.wutsi.application.shared.service.TenantProvider
import com.wutsi.application.shared.service.TogglesProvider
import com.wutsi.application.shared.service.URLBuilder
import com.wutsi.application.shared.ui.ProfileCard
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.AppBar
import com.wutsi.flutter.sdui.Button
import com.wutsi.flutter.sdui.Column
import com.wutsi.flutter.sdui.Container
import com.wutsi.flutter.sdui.Divider
import com.wutsi.flutter.sdui.Icon
import com.wutsi.flutter.sdui.IconButton
import com.wutsi.flutter.sdui.Input
import com.wutsi.flutter.sdui.MoneyText
import com.wutsi.flutter.sdui.Screen
import com.wutsi.flutter.sdui.Text
import com.wutsi.flutter.sdui.Widget
import com.wutsi.flutter.sdui.enums.ActionType
import com.wutsi.flutter.sdui.enums.ActionType.Route
import com.wutsi.flutter.sdui.enums.Alignment
import com.wutsi.flutter.sdui.enums.Alignment.Center
import com.wutsi.flutter.sdui.enums.InputType.Submit
import com.wutsi.flutter.sdui.enums.TextAlignment
import com.wutsi.platform.account.WutsiAccountApi
import com.wutsi.platform.account.dto.Account
import com.wutsi.platform.account.dto.SearchAccountRequest
import com.wutsi.platform.core.error.Error
import com.wutsi.platform.core.error.exception.BadRequestException
import com.wutsi.platform.payment.dto.ComputeTransactionFeesRequest
import com.wutsi.platform.tenant.dto.Tenant
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.text.DecimalFormat

@RestController
@RequestMapping("/send/confirm")
class SendConfirmScreen(
    private val urlBuilder: URLBuilder,
    private val tenantProvider: TenantProvider,
    private val accountApi: WutsiAccountApi,
    private val categoryService: CategoryService,
    private val togglesProvider: TogglesProvider,

    @Value("\${wutsi.application.login-url}") private val loginUrl: String,
) : AbstractQuery() {
    @PostMapping
    fun index(
        @RequestParam amount: Double,
        @RequestParam("phone-number", required = false) phoneNumber: String? = null,
        @RequestParam("recipient-id", required = false) recipientId: Long? = null
    ): Widget {
        if (phoneNumber == null && recipientId == null) {
            throw BadRequestException(
                error = Error(
                    code = "no-recipient",
                    message = "phone-number and recipient-id are missing"
                )
            )
        }

        val tenant = tenantProvider.get()
        if (phoneNumber != null) {
            val xphoneNumber = sanitizePhoneNumber(phoneNumber)
            val recipient = findRecipient(xphoneNumber)
            return if (recipient == null)
                recipientNotFound(xphoneNumber, tenant)
            else
                confirm(amount, xphoneNumber, recipient, tenant)
        } else {
            val recipient = findRecipient(recipientId!!)
            return confirm(amount, null, recipient, tenant)
        }
    }

    private fun confirm(
        amount: Double,
        phoneNumber: String?,
        recipient: Account,
        tenant: Tenant
    ): Widget {
        // Get fees
        val response = paymentApi.computeTransactionFees(
            request = ComputeTransactionFeesRequest(
                amount = amount,
                transactionType = "TRANSFER",
                recipientId = recipient.id,
                senderId = securityContext.currentAccountId()
            )
        )

        // Adjust amount
        val fmt = DecimalFormat(tenant.monetaryFormat)
        val fees: Double = if (response.applyToSender) response.fees else 0.0

        return Screen(
            id = Page.SEND_CONFIRM,
            backgroundColor = Theme.COLOR_WHITE,
            appBar = AppBar(
                elevation = 0.0,
                backgroundColor = Theme.COLOR_WHITE,
                foregroundColor = Theme.COLOR_BLACK,
                title = getText("page.send-confirm.title"),
                actions = listOf(
                    IconButton(
                        icon = Theme.ICON_CANCEL,
                        action = Action(
                            type = ActionType.Route,
                            url = "route:/~"
                        )
                    )
                )
            ),
            child = Column(
                children = listOf(
                    ProfileCard(
                        account = recipient,
                        phoneNumber = phoneNumber,
                        categoryService = categoryService,
                        togglesProvider = togglesProvider,
                        showWebsite = false
                    ),
                    Divider(color = Theme.COLOR_DIVIDER),
                    MoneyText(
                        value = amount,
                        currency = tenant.currencySymbol,
                        numberFormat = tenant.numberFormat,
                        color = Theme.COLOR_PRIMARY,
                    ),
                    Container(
                        alignment = Alignment.Center,
                        child = Text(
                            getText("page.send-confirm.fees", arrayOf(fmt.format(fees))),
                            bold = true,
                            size = Theme.TEXT_SIZE_LARGE
                        )
                    ),
                    Container(
                        padding = 10.0,
                        child = Input(
                            name = "command",
                            type = Submit,
                            caption = getText("page.send-confirm.button.submit", arrayOf(fmt.format(amount))),
                            action = Action(
                                type = ActionType.Route,
                                url = urlBuilder.build(loginUrl, getSubmitUrl(amount, recipient)),
                            )
                        )
                    )
                )
            ),
        ).toWidget()
    }

    private fun recipientNotFound(phoneNumber: String, tenant: Tenant) = Screen(
        id = Page.SEND_RECIPIENT_NOT_FOUND,
        appBar = AppBar(
            elevation = 0.0,
            backgroundColor = Theme.COLOR_WHITE,
            foregroundColor = Theme.COLOR_BLACK,
            actions = listOf(
                IconButton(
                    icon = Theme.ICON_CANCEL,
                    action = Action(
                        type = Route,
                        url = "route:/~"
                    )
                )
            )
        ),
        child = Column(
            children = listOf(
                Container(padding = 40.0),
                Container(
                    alignment = Center,
                    padding = 20.0,
                    child = Icon(
                        code = Theme.ICON_ERROR,
                        size = 80.0,
                        color = Theme.COLOR_DANGER
                    )
                ),
                Container(
                    alignment = Center,
                    padding = 10.0,
                    child = Text(
                        caption = getText("page.send-confirm.recipient-not-found"),
                        alignment = TextAlignment.Center,
                        size = Theme.TEXT_SIZE_X_LARGE,
                    )
                ),
                Text(
                    caption = formattedPhoneNumber(phoneNumber) ?: phoneNumber
                ),
                Container(
                    alignment = Center,
                    padding = 10.0,
                    child = Text(
                        caption = getText("page.send-confirm.invite", arrayOf(tenant.name)),
                        alignment = TextAlignment.Center,
                        size = Theme.TEXT_SIZE_X_LARGE,
                    )
                ),
                Container(
                    padding = 10.0,
                    child = Button(
                        caption = getText(
                            "page.send-confirm.button.invite",
                            arrayOf(formattedPhoneNumber(phoneNumber))
                        ),
                        action = Action(
                            type = ActionType.Share,
                            message = getText(
                                "page.send-confirm.invite.message",
                                arrayOf(tenant.name, tenant.installUrl)
                            )
                        )
                    )
                ),
            )
        ),
    ).toWidget()

    private fun findRecipient(phoneNumber: String): Account? {
        val accounts = accountApi.searchAccount(
            SearchAccountRequest(
                phoneNumber = phoneNumber
            )
        ).accounts
        return if (accounts.isEmpty())
            null
        else
            accountApi.getAccount(accounts[0].id).account
    }

    private fun findRecipient(id: Long): Account =
        accountApi.getAccount(id).account

    private fun getSubmitUrl(amount: Double, recipient: Account): String {
        val me = accountApi.getAccount(securityContext.currentAccountId()).account
        return "?phone=" + encodeURLParam(me.phone!!.number) +
            "&icon=" + Theme.ICON_LOCK +
            "&screen-id=" + Page.SEND_PIN +
            "&title=" + encodeURLParam(getText("page.send-pin.title")) +
            "&sub-title=" + encodeURLParam(getText("page.send-pin.sub-title")) +
            "&auth=false" +
            "&return-to-route=false" +
            "&return-url=" + encodeURLParam(
            urlBuilder.build(
                "commands/send?amount=$amount&recipient-id=${recipient.id}&recipient-name=" + encodeURLParam(recipient.displayName)
            )
        )
    }
}
