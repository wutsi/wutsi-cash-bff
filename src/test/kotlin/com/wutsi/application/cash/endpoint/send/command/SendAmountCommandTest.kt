package com.wutsi.application.cash.endpoint.send.command

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import com.wutsi.application.cash.endpoint.send.dto.SendAmountRequest
import com.wutsi.application.shared.service.TogglesProvider
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.enums.ActionType
import com.wutsi.flutter.sdui.enums.ActionType.Route
import com.wutsi.flutter.sdui.enums.DialogType
import com.wutsi.platform.account.dto.ListPaymentMethodResponse
import com.wutsi.platform.account.dto.PaymentMethodSummary
import com.wutsi.platform.payment.dto.Balance
import com.wutsi.platform.payment.dto.GetBalanceResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.MessageSource
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertNull

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class SendAmountCommandTest : AbstractEndpointTest() {
    @LocalServerPort
    val port: Int = 0

    private lateinit var url: String

    @MockBean
    private lateinit var togglesProvider: TogglesProvider

    @Autowired
    private lateinit var messages: MessageSource

    @BeforeEach
    override fun setUp() {
        super.setUp()

        url = "http://localhost:$port/commands/send/amount"

        doReturn(GetBalanceResponse(Balance(amount = 10000.0))).whenever(paymentApi).getBalance(any())
    }

    @Test
    fun success() {
        // WHEN
        val request = SendAmountRequest(
            amount = 1000.0
        )
        val response = rest.postForEntity(url, request, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val action = response.body!!
        assertEquals(Route, action.type)
        assertEquals("http://localhost:0/send/recipient?amount=1000.0", action.url)
        assertNull(action.parameters)
    }

    @Test
    fun successWithRecipientId() {
        // WHEN
        url = "http://localhost:$port/commands/send/amount?recipient-id=555"
        val request = SendAmountRequest(
            amount = 1000.0
        )
        val response = rest.postForEntity(url, request, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val action = response.body!!
        assertEquals(Route, action.type)
        assertEquals("http://localhost:0/send/confirm?amount=1000.0&recipient-id=555", action.url)
        assertNull(action.parameters)
    }

    @Test
    fun zero() {
        // WHEN
        val request = SendAmountRequest(
            amount = 0.0
        )
        val response = rest.postForEntity(url, request, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val action = response.body!!
        assertEquals(ActionType.Prompt, action.type)
        assertEquals(DialogType.Error.name, action.prompt?.attributes?.get("type"))
        assertEquals(
            messages.getMessage("prompt.error.amount-required", emptyArray(), Locale.ENGLISH),
            action.prompt?.attributes?.get("message")
        )
    }

    @Test
    fun notEnoughFunds() {
        // GIVEN
        doReturn(ListPaymentMethodResponse(listOf(PaymentMethodSummary()))).whenever(accountApi)
            .listPaymentMethods(any())

        // WHEN
        val request = SendAmountRequest(
            amount = 1000000.0
        )
        val response = rest.postForEntity(url, request, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val action = response.body!!
        assertEquals(ActionType.Prompt, action.type)
        assertEquals(DialogType.Error.name, action.prompt?.attributes?.get("type"))
        assertEquals(
            messages.getMessage("prompt.error.transaction-failed.NOT_ENOUGH_FUNDS", emptyArray(), Locale.ENGLISH),
            action.prompt?.attributes?.get("message")
        )
    }

    @Test
    fun noAccountLinked() {
        // GIVEN
        doReturn(true).whenever(togglesProvider).isAccountEnabled()
        doReturn(ListPaymentMethodResponse()).whenever(accountApi).listPaymentMethods(any())

        // WHEN
        val request = SendAmountRequest(
            amount = 1000000.0
        )
        val response = rest.postForEntity(url, request, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val action = response.body!!
        assertEquals(ActionType.Prompt, action.type)
        assertEquals(DialogType.Error.name, action.prompt?.attributes?.get("type"))
        assertEquals(
            messages.getMessage("prompt.error.transaction-failed.NO_ACCOUNT_LINKED", emptyArray(), Locale.ENGLISH),
            action.prompt?.attributes?.get("message")
        )
    }
}
