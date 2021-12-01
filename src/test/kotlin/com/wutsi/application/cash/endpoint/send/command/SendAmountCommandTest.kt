package com.wutsi.application.cash.endpoint.send.command

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import com.wutsi.application.cash.endpoint.send.dto.SendAmountRequest
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.enums.ActionType
import com.wutsi.flutter.sdui.enums.ActionType.Route
import com.wutsi.flutter.sdui.enums.DialogType
import com.wutsi.platform.payment.WutsiPaymentApi
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class SendAmountCommandTest : AbstractEndpointTest() {
    @LocalServerPort
    public val port: Int = 0

    private lateinit var url: String

    @Autowired
    private lateinit var messages: MessageSource

    @MockBean
    private lateinit var paymentApi: WutsiPaymentApi

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

        val action = response.body
        assertEquals(Route, action.type)
        assertEquals("http://localhost:0/send/recipient?amount=1000.0", action.url)
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

        val action = response.body
        assertEquals(ActionType.Prompt, action.type)
        assertEquals(DialogType.Error, action.prompt?.type)
        assertEquals(
            messages.getMessage("prompt.error.amount-required", emptyArray(), Locale.ENGLISH),
            action.prompt?.message
        )
    }

    @Test
    fun notEnoughFunds() {
        // WHEN
        val request = SendAmountRequest(
            amount = 1000000.0
        )
        val response = rest.postForEntity(url, request, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val action = response.body
        assertEquals(ActionType.Prompt, action.type)
        assertEquals(DialogType.Error, action.prompt?.type)
        assertEquals(
            messages.getMessage("prompt.error.transaction-failed.NOT_ENOUGH_FUNDS", emptyArray(), Locale.ENGLISH),
            action.prompt?.message
        )
    }
}
