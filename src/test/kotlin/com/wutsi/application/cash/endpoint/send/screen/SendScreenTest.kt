package com.wutsi.application.cash.endpoint.send.screen

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import com.wutsi.platform.payment.dto.Balance
import com.wutsi.platform.payment.dto.GetBalanceResponse
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class SendScreenTest : AbstractEndpointTest() {
    @LocalServerPort
    val port: Int = 0

    @Test
    fun sendTo() {
        // GIVEN
        val balance = Balance(amount = 50000.0, currency = "XAF")
        doReturn(GetBalanceResponse(balance)).whenever(paymentApi).getBalance(any())

        // THEN
        val url = "http://localhost:$port/send"
        assertEndpointEquals("/screens/send/send.json", url)
    }

    @Test
    fun sendToRecipient() {
        // GIVEN
        val balance = Balance(amount = 50000.0, currency = "XAF")
        doReturn(GetBalanceResponse(balance)).whenever(paymentApi).getBalance(any())

        // THEN
        val url = "http://localhost:$port/send?recipient-id=123"
        assertEndpointEquals("/screens/send/send-to-recipient.json", url)
    }
}
