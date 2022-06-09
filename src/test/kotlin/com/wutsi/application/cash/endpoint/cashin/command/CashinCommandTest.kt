package com.wutsi.application.cash.endpoint.cashin.command

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.enums.ActionType
import com.wutsi.platform.payment.core.ErrorCode
import com.wutsi.platform.payment.core.Status
import com.wutsi.platform.payment.dto.CreateCashinRequest
import com.wutsi.platform.payment.dto.CreateCashinResponse
import com.wutsi.platform.payment.dto.GetTransactionResponse
import com.wutsi.platform.payment.dto.Transaction
import com.wutsi.platform.payment.error.ErrorURN
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class CashinCommandTest : AbstractEndpointTest() {
    @LocalServerPort
    val port: Int = 0

    private lateinit var url: String

    @BeforeEach
    override fun setUp() {
        super.setUp()

        url = "http://localhost:$port/commands/cashin?amount=10000&payment-token=xxxx&idempotency-key=123"
    }

    @Test
    fun success() {
        // GIVEN
        val resp = CreateCashinResponse(id = "111", status = Status.SUCCESSFUL.name)
        doReturn(resp).whenever(paymentApi).createCashin(any())

        // WHEN
        val response = rest.postForEntity(url, null, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val request = argumentCaptor<CreateCashinRequest>()
        verify(paymentApi).createCashin(request.capture())
        assertEquals("XAF", request.firstValue.currency)
        assertEquals(10000.0, request.firstValue.amount)
        assertEquals("xxxx", request.firstValue.paymentMethodToken)
        assertEquals("123", request.firstValue.idempotencyKey)

        val action = response.body!!
        assertEquals(ActionType.Route, action.type)
        assertEquals("http://localhost:0/cashin/success?amount=10000.0", action.url)
    }

    @Test
    fun pending() {
        // GIVEN
        val resp = CreateCashinResponse(id = "111", status = Status.PENDING.name)
        doReturn(resp).whenever(paymentApi).createCashin(any())

        val tx = Transaction(status = Status.PENDING.name)
        doReturn(GetTransactionResponse(tx)).whenever(paymentApi).getTransaction(any())

        // WHEN
        val response = rest.postForEntity(url, null, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val request = argumentCaptor<CreateCashinRequest>()
        verify(paymentApi).createCashin(request.capture())
        assertEquals("XAF", request.firstValue.currency)
        assertEquals(10000.0, request.firstValue.amount)
        assertEquals("xxxx", request.firstValue.paymentMethodToken)
        assertEquals("123", request.firstValue.idempotencyKey)

        val action = response.body!!
        assertEquals(ActionType.Route, action.type)
        assertEquals("http://localhost:0/cashin/pending", action.url)
    }

    @Test
    fun failure() {
        // GIVEN
        val ex = createFeignException("transaction-failed", ErrorCode.NONE)
        doThrow(ex).whenever(paymentApi).createCashin(any())

        // WHEN
        val response = rest.postForEntity(url, null, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val request = argumentCaptor<CreateCashinRequest>()
        verify(paymentApi).createCashin(request.capture())
        assertEquals("XAF", request.firstValue.currency)
        assertEquals(10000.0, request.firstValue.amount)
        assertEquals("xxxx", request.firstValue.paymentMethodToken)
        assertEquals("123", request.firstValue.idempotencyKey)

        val action = response.body!!
        assertEquals(ActionType.Route, action.type)
        assertEquals(
            "http://localhost:0/checkout/success?amount=10000.0&error=Oops%21+An+unexpected+error+has+occurred.+Please%2C+try+again.",
            action.url
        )
    }

    @Test
    fun failureNotEnoughFunds() {
        // GIVEN
        val ex = createFeignException(ErrorURN.TRANSACTION_FAILED.urn, ErrorCode.NOT_ENOUGH_FUNDS)
        doThrow(ex).whenever(paymentApi).createCashin(any())

        // WHEN
        val response = rest.postForEntity(url, null, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val action = response.body!!
        assertEquals(ActionType.Route, action.type)
        assertEquals(
            "http://localhost:0/checkout/success?amount=10000.0&error=Sorry%21+You+do+not+have+enough+in+your+Wallet.",
            action.url
        )
    }
}
