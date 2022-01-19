package com.wutsi.application.cash.endpoint.send.screen

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import com.wutsi.platform.payment.dto.GetTransactionResponse
import com.wutsi.platform.payment.dto.Transaction
import com.wutsi.platform.qr.WutsiQrApi
import com.wutsi.platform.qr.dto.EncodeQRCodeResponse
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.web.server.LocalServerPort

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class SendPendingScreenTest : AbstractEndpointTest() {
    @LocalServerPort
    val port: Int = 0

    @MockBean
    private lateinit var qrApi: WutsiQrApi

    @Test
    fun approve() {
        val tx = Transaction(
            id = "123",
            status = "PENDING",
            requiresApproval = true,
            amount = 1100.0,
            fees = 100.0,
            net = 1000.0,
            recipientId = 124
        )
        doReturn(GetTransactionResponse(tx)).whenever(paymentApi).getTransaction(any())

        doReturn(EncodeQRCodeResponse("yyyy")).whenever(qrApi).encode(any())

        val url = "http://localhost:$port/send/pending?transaction-id=xxx"
        assertEndpointEquals("/screens/send/pending-approve.json", url)
    }

    @Test
    fun noApproval() {
        val tx = Transaction(
            id = "1232",
            status = "PENDING",
            requiresApproval = false,
            amount = 1100.0,
            fees = 100.0,
            net = 1000.0,
            recipientId = 124
        )
        doReturn(GetTransactionResponse(tx)).whenever(paymentApi).getTransaction(any())

        val url = "http://localhost:$port/send/pending?transaction-id=xxx"
        assertEndpointEquals("/screens/send/pending-no-approval.json", url)
    }

}
