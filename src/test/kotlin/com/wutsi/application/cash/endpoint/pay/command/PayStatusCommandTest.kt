package com.wutsi.application.cash.endpoint.pay.command

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.enums.ActionType
import com.wutsi.platform.payment.core.ErrorCode
import com.wutsi.platform.payment.core.Status
import com.wutsi.platform.payment.dto.SearchTransactionRequest
import com.wutsi.platform.payment.dto.SearchTransactionResponse
import com.wutsi.platform.payment.dto.TransactionSummary
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class PayStatusCommandTest : AbstractEndpointTest() {
    @LocalServerPort
    val port: Int = 0

    private lateinit var url: String

    @BeforeEach
    override fun setUp() {
        super.setUp()

        url = "http://localhost:$port/commands/pay/status?payment-request-id=1234&amount=1000"
    }

    @Test
    fun success() {
        // GIVEN
        val tx = createTransactionSummary(Status.SUCCESSFUL)
        doReturn(SearchTransactionResponse(listOf(tx))).whenever(paymentApi).searchTransaction(any())

        // WHEN
        val response = rest.postForEntity(url, null, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val req = argumentCaptor<SearchTransactionRequest>()
        verify(paymentApi).searchTransaction(req.capture())
        assertEquals("1234", req.firstValue.paymentRequestId)

        val action = response.body
        assertEquals(ActionType.Route, action.type)
        assertEquals("http://localhost:0/pay/success?amount=1000.0", action.url)
    }

    @Test
    fun successOnRetry() {
        // GIVEN
        val tx = createTransactionSummary(Status.SUCCESSFUL)
        doReturn(SearchTransactionResponse())
            .doReturn(SearchTransactionResponse(listOf(tx)))
            .whenever(paymentApi).searchTransaction(any())

        // WHEN
        val response = rest.postForEntity(url, null, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        verify(paymentApi, times(2)).searchTransaction(any())

        val action = response.body
        assertEquals(ActionType.Route, action.type)
        assertEquals("http://localhost:0/pay/success?amount=1000.0", action.url)
    }

    @Test
    fun error() {
        // GIVEN
        val tx = createTransactionSummary(Status.FAILED, ErrorCode.NOT_ENOUGH_FUNDS.name)
        doReturn(SearchTransactionResponse(listOf(tx))).whenever(paymentApi).searchTransaction(any())

        // WHEN
        val response = rest.postForEntity(url, null, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val req = argumentCaptor<SearchTransactionRequest>()
        verify(paymentApi).searchTransaction(req.capture())
        assertEquals("1234", req.firstValue.paymentRequestId)

        val action = response.body
        assertEquals(ActionType.Prompt, action.type)
        assertEquals(
            getText("prompt.error.transaction-failed.NOT_ENOUGH_FUNDS"),
            action.prompt?.attributes?.get("message")
        )
    }

    @Test
    fun errorOnRetry() {
        // GIVEN
        val tx = createTransactionSummary(Status.FAILED, ErrorCode.EXPIRED.name)
        doReturn(SearchTransactionResponse())
            .doReturn(SearchTransactionResponse(listOf(tx)))
            .whenever(paymentApi).searchTransaction(any())

        // WHEN
        val response = rest.postForEntity(url, null, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val req = argumentCaptor<SearchTransactionRequest>()
        verify(paymentApi, times(2)).searchTransaction(any())

        val action = response.body
        assertEquals(ActionType.Prompt, action.type)
        assertEquals(
            getText("prompt.error.transaction-failed.EXPIRED"),
            action.prompt?.attributes?.get("message")
        )
    }

    private fun createTransactionSummary(status: Status, errorCode: String? = null) = TransactionSummary(
        status = status.name,
        errorCode = errorCode
    )
}
