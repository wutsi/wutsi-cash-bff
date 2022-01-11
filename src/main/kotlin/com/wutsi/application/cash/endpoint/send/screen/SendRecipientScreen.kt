package com.wutsi.application.cash.endpoint.send.screen

import com.wutsi.application.cash.endpoint.AbstractQuery
import com.wutsi.application.cash.endpoint.Page
import com.wutsi.application.shared.Theme
import com.wutsi.application.shared.service.StringUtil
import com.wutsi.application.shared.service.TenantProvider
import com.wutsi.application.shared.service.URLBuilder
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.AppBar
import com.wutsi.flutter.sdui.CircleAvatar
import com.wutsi.flutter.sdui.Column
import com.wutsi.flutter.sdui.Container
import com.wutsi.flutter.sdui.DefaultTabController
import com.wutsi.flutter.sdui.Divider
import com.wutsi.flutter.sdui.Flexible
import com.wutsi.flutter.sdui.Form
import com.wutsi.flutter.sdui.IconButton
import com.wutsi.flutter.sdui.Image
import com.wutsi.flutter.sdui.Input
import com.wutsi.flutter.sdui.ListItem
import com.wutsi.flutter.sdui.ListView
import com.wutsi.flutter.sdui.Screen
import com.wutsi.flutter.sdui.Tab
import com.wutsi.flutter.sdui.TabBar
import com.wutsi.flutter.sdui.TabBarView
import com.wutsi.flutter.sdui.Text
import com.wutsi.flutter.sdui.Widget
import com.wutsi.flutter.sdui.WidgetAware
import com.wutsi.flutter.sdui.enums.ActionType.Command
import com.wutsi.flutter.sdui.enums.ActionType.Route
import com.wutsi.flutter.sdui.enums.Alignment.Center
import com.wutsi.flutter.sdui.enums.InputType.Phone
import com.wutsi.flutter.sdui.enums.InputType.Submit
import com.wutsi.flutter.sdui.enums.TextAlignment
import com.wutsi.platform.account.WutsiAccountApi
import com.wutsi.platform.account.dto.AccountSummary
import com.wutsi.platform.account.dto.SearchAccountRequest
import com.wutsi.platform.contact.WutsiContactApi
import com.wutsi.platform.contact.dto.ContactSummary
import com.wutsi.platform.contact.dto.SearchContactRequest
import com.wutsi.platform.tenant.dto.Tenant
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.text.DecimalFormat

@RestController
@RequestMapping("/send/recipient")
class SendRecipientScreen(
    private val urlBuilder: URLBuilder,
    private val tenantProvider: TenantProvider,
    private val contactApi: WutsiContactApi,
    private val accountApi: WutsiAccountApi,
) : AbstractQuery() {
    @PostMapping
    fun index(@RequestParam amount: Double): Widget {
        val tenant = tenantProvider.get()
        val amountText = DecimalFormat(tenant.monetaryFormat).format(amount)
        val contacts = contactApi.searchContact(
            SearchContactRequest(
                limit = 100,
                offset = 0,
            )
        ).contacts

        return DefaultTabController(
            length = 2,
            child = Screen(
                id = Page.SEND_RECIPIENT,
                backgroundColor = Theme.COLOR_WHITE,
                appBar = AppBar(
                    elevation = 0.0,
                    backgroundColor = Theme.COLOR_PRIMARY,
                    foregroundColor = Theme.COLOR_WHITE,
                    title = getText("page.send-recipient.title"),
                    actions = listOf(
                        IconButton(
                            icon = Theme.ICON_CANCEL,
                            action = Action(
                                type = Route,
                                url = "route:/~"
                            )
                        )
                    ),
                    bottom = TabBar(
                        tabs = if (contacts.isEmpty())
                            listOf(
                                Tab(icon = Theme.ICON_PHONE, caption = getText("page.send-recipient.tab.phone"))
                            )
                        else
                            listOf(
                                Tab(icon = Theme.ICON_CONTACT, caption = getText("page.send-recipient.tab.contact")),
                                Tab(icon = Theme.ICON_PHONE, caption = getText("page.send-recipient.tab.phone"))
                            )
                    )
                ),
                child = TabBarView(
                    children = if (contacts.isEmpty())
                        listOf(
                            phoneTab(amount, amountText, tenant)
                        )
                    else
                        listOf(
                            contactTab(contacts, amount, tenant),
                            phoneTab(amount, amountText, tenant)
                        )
                )
            )
        ).toWidget()
    }

    private fun phoneTab(amount: Double, amountText: String, tenant: Tenant) = Container(
        alignment = Center,
        child = Column(
            children = listOf(
                Form(
                    children = listOf(
                        Container(
                            alignment = Center,
                            padding = 10.0,
                            child = Text(
                                caption = getText(
                                    "page.send-recipient.phone.title"
                                ),
                                alignment = TextAlignment.Center,
                                size = Theme.TEXT_SIZE_LARGE,
                                color = Theme.COLOR_PRIMARY,
                                bold = true
                            )
                        ),
                        Container(
                            alignment = Center,
                            padding = 10.0,
                            child = Text(
                                caption = getText(
                                    "page.send-recipient.phone.sub-title"
                                ),
                                alignment = TextAlignment.Center,
                                color = Theme.COLOR_BLACK,
                            )
                        ),
                        Container(
                            padding = 10.0,
                            child = Input(
                                name = "phoneNumber",
                                type = Phone,
                                required = true,
                                countries = tenant.countries
                            ),
                        ),
                        Container(
                            padding = 10.0,
                            child = Input(
                                name = "command",
                                type = Submit,
                                caption = getText("page.send-recipient.button.submit", arrayOf(amountText)),
                                action = Action(
                                    type = Command,
                                    url = urlBuilder.build("commands/send/recipient"),
                                    parameters = mapOf("amount" to amount.toString())
                                )
                            )
                        )
                    )
                )
            )
        )
    )

    private fun contactTab(contacts: List<ContactSummary>, amount: Double, tenant: Tenant): WidgetAware {
        val accountIds = contacts.map { it.contactId }
        if (accountIds.isEmpty())
            return Container()

        val accounts = accountApi.searchAccount(
            SearchAccountRequest(
                ids = accountIds,
                limit = accountIds.size,
                offset = 0
            )
        ).accounts.sortedBy { it.displayName }
        return Container(
            child = Column(
                children = listOf(
                    Container(
                        padding = 10.0,
                        alignment = Center,
                        child = Text(
                            caption = getText(
                                "page.send-recipient.contact.title"
                            ),
                            alignment = TextAlignment.Left,
                            size = Theme.TEXT_SIZE_LARGE,
                            color = Theme.COLOR_PRIMARY,
                            bold = true,
                        )
                    ),
                    Container(
                        padding = 10.0,
                        alignment = Center,
                        child = Text(
                            caption = getText(
                                "page.send-recipient.contact.sub-title"
                            ),
                            alignment = TextAlignment.Left,
                            color = Theme.COLOR_BLACK,
                        )
                    ),
                    Divider(color = Theme.COLOR_DIVIDER),
                    Flexible(
                        child = ListView(
                            children = accounts.map { toListItem(it, amount, tenant) },
                            separator = true,
                        )
                    )
                ),
            )
        )
    }

    fun toListItem(account: AccountSummary, amount: Double, tenant: Tenant) = ListItem(
        caption = account.displayName ?: "",
        iconRight = Theme.ICON_CHEVRON_RIGHT,
        padding = 10.0,
        leading = CircleAvatar(
            radius = 24.0,
            child = account.pictureUrl?.let { Image(width = 48.0, height = 48.0, url = it) }
                ?: Text(
                    caption = StringUtil.initials(account.displayName),
                    size = Theme.TEXT_SIZE_X_LARGE,
                    bold = true
                )
        ),
        action = Action(
            type = Route,
            url = urlBuilder.build("send/confirm"),
            parameters = mapOf(
                "amount" to amount.toString(),
                "recipient-id" to account.id.toString()
            )
        )
    )
}
