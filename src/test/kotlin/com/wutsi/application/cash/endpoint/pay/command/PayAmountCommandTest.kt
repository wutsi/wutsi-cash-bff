package com.wutsi.application.cash.endpoint.pay.command

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import com.wutsi.application.cash.endpoint.pay.dto.PayAmountRequest
import com.wutsi.flutter.sdui.Action
import com.wutsi.flutter.sdui.enums.ActionType
import com.wutsi.flutter.sdui.enums.DialogType
import com.wutsi.platform.payment.dto.CreatePaymentRequestRequest
import com.wutsi.platform.payment.dto.CreatePaymentRequestResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.MessageSource
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertNull

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class PayAmountCommandTest : AbstractEndpointTest() {
    @LocalServerPort
    val port: Int = 0

    private lateinit var url: String

    @Autowired
    private lateinit var messages: MessageSource

    @BeforeEach
    override fun setUp() {
        super.setUp()

        url = "http://localhost:$port/commands/pay/amount"
    }

    @Test
    fun success() {
        // GIVEN
        doReturn(CreatePaymentRequestResponse("xxxx")).whenever(paymentApi).createPaymentRequest(any())

        // WHEN
        val request = PayAmountRequest(
            amount = 1000.0
        )
        val response = rest.postForEntity(url, request, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val req = argumentCaptor<CreatePaymentRequestRequest>()
        verify(paymentApi).createPaymentRequest(req.capture())
        assertEquals("XAF", req.firstValue.currency)
        assertEquals(request.amount, req.firstValue.amount)
        assertEquals(300, req.firstValue.timeToLive)
        assertNull(req.firstValue.invoiceId)
        assertNull(req.firstValue.description)

        val action = response.body
        assertEquals(ActionType.Route, action.type)
        assertEquals("http://localhost:0/pay/qr-code?payment-request-id=xxxx&amount=1000.0", action.url)
    }

    @Test
    fun zero() {
        // WHEN
        val request = PayAmountRequest(
            amount = 0.0
        )
        val response = rest.postForEntity(url, request, Action::class.java)

        // THEN
        assertEquals(200, response.statusCodeValue)

        val action = response.body
        assertEquals(ActionType.Prompt, action.type)
        assertEquals(DialogType.Error.name, action.prompt?.attributes?.get("type"))
        assertEquals(
            messages.getMessage("prompt.error.amount-required", emptyArray(), Locale.ENGLISH),
            action.prompt?.attributes?.get("message")
        )
    }
}
