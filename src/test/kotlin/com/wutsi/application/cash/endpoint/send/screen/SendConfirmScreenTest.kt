package com.wutsi.application.cash.endpoint.send.screen

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import com.wutsi.platform.account.dto.AccountSummary
import com.wutsi.platform.account.dto.SearchAccountResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class SendConfirmScreenTest : AbstractEndpointTest() {
    @LocalServerPort
    public val port: Int = 0

    private lateinit var url: String

    @BeforeEach
    override fun setUp() {
        super.setUp()

        url = "http://localhost:$port/send/confirm?amount=3000&phone-number=+237999999999"
    }

    @Test
    fun success() {
        val account = AccountSummary(id = 111, displayName = "Ray Sponsible")
        doReturn(SearchAccountResponse(listOf(account))).whenever(accountApi).searchAccount(any())
        assertEndpointEquals("/screens/send/confirm.json", url)
    }

    @Test
    fun recipientNotFound() {
        doReturn(SearchAccountResponse()).whenever(accountApi).searchAccount(any())
        assertEndpointEquals("/screens/send/confirm-no-recipient.json", url)
    }
}
