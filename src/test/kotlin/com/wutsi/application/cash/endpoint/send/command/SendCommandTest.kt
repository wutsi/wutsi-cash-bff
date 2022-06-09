package com.wutsi.application.cash.endpoint.send.command

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import com.wutsi.application.cash.endpoint.send.dto.SendRequest
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.enums.ActionType
import com.wutsi.flutter.sdui.enums.ActionType.Route
import com.wutsi.platform.payment.core.ErrorCode
import com.wutsi.platform.payment.dto.CreateTransferRequest
import com.wutsi.platform.payment.dto.CreateTransferResponse
import com.wutsi.platform.payment.error.ErrorURN
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.MessageSource
import kotlin.test.assertEquals
import kotlin.test.assertNull

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class SendCommandTest : AbstractEndpointTest() {
    @LocalServerPort
    val port: Int = 0

    private lateinit var url: String

    @Autowired
    private lateinit var messageSource: MessageSource

    @BeforeEach
    override fun setUp() {
        super.setUp()

        url =
            "http://localhost:$port/commands/send?amount=3000.0&recipient-id=111&idempotency-key=123"
    }

    @Test
    fun success() {
        // Given
        doReturn(CreateTransferResponse("xxx", "SUCCESSFUL")).whenever(paymentApi).createTransfer(any())

        // WHEN
        val request = SendRequest(
            pin = "123456"
        )
        val response = rest.postForEntity(url, request, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val req = argumentCaptor<CreateTransferRequest>()
        verify(paymentApi).createTransfer(req.capture())
        assertEquals(3000.0, req.firstValue.amount)
        assertEquals("XAF", req.firstValue.currency)
        assertEquals(111L, req.firstValue.recipientId)
        assertEquals("123", req.firstValue.idempotencyKey)
        assertNull(req.firstValue.description)

        val action = response.body!!
        assertEquals(Route, action.type)
        assertEquals("http://localhost:0/send/success?transaction-id=xxx", action.url)
    }

    @Test
    fun pending() {
        // Given
        doReturn(CreateTransferResponse("xxx", "PENDING")).whenever(paymentApi).createTransfer(any())

        // WHEN
        val request = SendRequest(
            pin = "123456"
        )
        val response = rest.postForEntity(url, request, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val req = argumentCaptor<CreateTransferRequest>()
        verify(paymentApi).createTransfer(req.capture())
        assertEquals(3000.0, req.firstValue.amount)
        assertEquals("XAF", req.firstValue.currency)
        assertEquals(111L, req.firstValue.recipientId)
        assertEquals("123", req.firstValue.idempotencyKey)
        assertNull(req.firstValue.description)

        val action = response.body!!
        assertEquals(Route, action.type)
        assertEquals("http://localhost:0/send/pending?transaction-id=xxx", action.url)
    }

    @Test
    fun transactionError() {
        // GIVEN
        val ex = createFeignException(ErrorURN.TRANSACTION_FAILED.urn, ErrorCode.UNEXPECTED_ERROR, "xxx")
        doThrow(ex).whenever(paymentApi).createTransfer(any())

        // WHEN
        val request = SendRequest(
            pin = "123456"
        )
        val response = rest.postForEntity(url, request, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val req = argumentCaptor<CreateTransferRequest>()
        verify(paymentApi).createTransfer(req.capture())
        assertEquals(3000.0, req.firstValue.amount)
        assertEquals("XAF", req.firstValue.currency)
        assertEquals(111L, req.firstValue.recipientId)
        assertEquals("123", req.firstValue.idempotencyKey)
        assertNull(req.firstValue.description)

        val action = response.body!!
        assertEquals(Route, action.type)
        assertEquals(
            "http://localhost:0/send/success?amount=3000.0&recipient-id=111&error=Sorry%21+An+unexpected+error+has+occurred.+Please%2C+try+again.",
            action.url
        )
    }

    @Test
    fun error() {
        // Given
        val ex = createFeignException("failed", ErrorCode.UNEXPECTED_ERROR)
        doThrow(ex).whenever(paymentApi).createTransfer(any())

        // WHEN
        val request = SendRequest(
            pin = "123456"
        )
        val response = rest.postForEntity(url, request, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val req = argumentCaptor<CreateTransferRequest>()
        verify(paymentApi).createTransfer(req.capture())
        assertEquals(3000.0, req.firstValue.amount)
        assertEquals("XAF", req.firstValue.currency)
        assertEquals(111L, req.firstValue.recipientId)
        assertEquals("123", req.firstValue.idempotencyKey)
        assertNull(req.firstValue.description)

        val action = response.body!!
        assertEquals(ActionType.Route, action.type)
        assertEquals(
            "http://localhost:0/send/success?amount=3000.0&recipient-id=111&error=Oops%21+An+unexpected+error+has+occurred.+Please%2C+try+again.",
            action.url
        )
    }
}
