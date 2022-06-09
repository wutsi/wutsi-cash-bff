package com.wutsi.application.cash.endpoint.send.screen

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import com.wutsi.platform.account.dto.Account
import com.wutsi.platform.account.dto.GetAccountResponse
import com.wutsi.platform.payment.core.ErrorCode
import com.wutsi.platform.payment.dto.GetTransactionResponse
import com.wutsi.platform.payment.dto.Transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class SendSuccessScreenTest : AbstractEndpointTest() {
    @LocalServerPort
    val port: Int = 0

    private val recipientId = 111L
    private lateinit var recipient: Account

    @BeforeEach
    override fun setUp() {
        super.setUp()

        recipient = Account(
            id = recipientId,
            displayName = "Roger Milla",
            pictureUrl = "https://www.goo.com/1.png"
        )
        doReturn(GetAccountResponse(recipient)).whenever(accountApi).getAccount(recipientId)

        val tx = Transaction(
            id = "123",
            status = "PENDING",
            amount = 1100.0,
            fees = 100.0,
            net = 1000.0,
            recipientId = recipientId,
        )
        doReturn(GetTransactionResponse(tx)).whenever(paymentApi).getTransaction(any())
    }

    @Test
    fun success() {
        val url = "http://localhost:$port/send/success?transaction-id=xxx"
        assertEndpointEquals("/screens/send/success.json", url)
    }

    @Test
    fun error() {
        val url =
            "http://localhost:$port/send/success?transaction-id=xxx&error=${ErrorCode.NOT_ENOUGH_FUNDS}"
        assertEndpointEquals("/screens/send/error.json", url)
    }
}
