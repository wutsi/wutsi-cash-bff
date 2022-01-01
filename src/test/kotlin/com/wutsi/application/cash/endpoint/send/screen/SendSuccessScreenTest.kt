package com.wutsi.application.cash.endpoint.send.screen

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import com.wutsi.platform.account.dto.Account
import com.wutsi.platform.account.dto.GetAccountResponse
import com.wutsi.platform.payment.core.ErrorCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort

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
    }

    @Test
    fun success() {
        val url = "http://localhost:$port/send/success?amount=3000&recipient-id=$recipientId"
        assertEndpointEquals("/screens/send/success.json", url)
    }

    @Test
    fun error() {
        val url =
            "http://localhost:$port/send/success?amount=3000&recipient-id=$recipientId&error=${ErrorCode.NOT_ENOUGH_FUNDS}"
        assertEndpointEquals("/screens/send/success-fail.json", url)
    }
}
