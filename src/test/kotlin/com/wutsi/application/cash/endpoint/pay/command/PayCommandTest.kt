package com.wutsi.application.cash.endpoint.pay.command

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
import com.wutsi.platform.payment.dto.CreatePaymentRequest
import com.wutsi.platform.payment.dto.CreatePaymentResponse
import com.wutsi.platform.payment.dto.GetPaymentRequestResponse
import com.wutsi.platform.payment.dto.PaymentRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class PayCommandTest : AbstractEndpointTest() {
    @LocalServerPort
    val port: Int = 0

    private lateinit var url: String

    private lateinit var paymentRequest: PaymentRequest

    @BeforeEach
    override fun setUp() {
        super.setUp()

        url = "http://localhost:$port/commands/pay?payment-request-id=1111&amount=1000"

        paymentRequest = PaymentRequest(
            id = "1111",
            accountId = 1111,
            amount = 50000.0,
            currency = "XAF",
        )
        doReturn(GetPaymentRequestResponse(paymentRequest)).whenever(paymentApi).getPaymentRequest(any())
    }

    @Test
    fun success() {
        // GIVEN
        doReturn(CreatePaymentResponse(id = "xxx", status = Status.SUCCESSFUL.name)).whenever(paymentApi)
            .createPayment(any())

        // WHEN
        val response = rest.postForEntity(url, null, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val req = argumentCaptor<CreatePaymentRequest>()
        verify(paymentApi).createPayment(req.capture())
        assertEquals(paymentRequest.id, req.firstValue.requestId)

        val action = response.body
        assertEquals(ActionType.Route, action.type)
        assertEquals("http://localhost:0/pay/success?amount=1000.0", action.url)
    }

    @Test
    fun error() {
        // GIVEN
        val ex = createFeignException("transaction-failed", ErrorCode.EXPIRED)
        doThrow(ex).whenever(paymentApi).createPayment(any())

        // WHEN
        val response = rest.postForEntity(url, null, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val req = argumentCaptor<CreatePaymentRequest>()
        verify(paymentApi).createPayment(req.capture())
        assertEquals(paymentRequest.id, req.firstValue.requestId)

        val action = response.body
        assertEquals(ActionType.Prompt, action.type)
        assertEquals(
            getText("prompt.error.transaction-failed.EXPIRED"),
            action.prompt?.attributes?.get("message")
        )
    }
}
