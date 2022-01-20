package com.wutsi.application.cash.endpoint.send.screen

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import com.wutsi.platform.payment.dto.GetTransactionResponse
import com.wutsi.platform.payment.dto.Transaction
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class SendApprovalScreenTest : AbstractEndpointTest() {
    @LocalServerPort
    val port: Int = 0

    //    @Test
    fun invoke() {
        val tx = Transaction(
            id = "1232",
            status = "PENDING",
            requiresApproval = false,
            amount = 1100.0,
            fees = 100.0,
            net = 1000.0,
            accountId = USER_ID,
            recipientId = 124
        )
        doReturn(GetTransactionResponse(tx)).whenever(paymentApi).getTransaction(any())

        val url = "http://localhost:$port/send/approval?transaction-id=xxxx"
        assertEndpointEquals("/screens/send/approval.json", url)
    }
}
