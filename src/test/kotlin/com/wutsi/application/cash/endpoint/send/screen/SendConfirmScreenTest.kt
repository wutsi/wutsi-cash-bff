package com.wutsi.application.cash.endpoint.send.screen

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import com.wutsi.platform.account.dto.Account
import com.wutsi.platform.account.dto.AccountSummary
import com.wutsi.platform.account.dto.GetAccountResponse
import com.wutsi.platform.account.dto.Phone
import com.wutsi.platform.account.dto.SearchAccountResponse
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class SendConfirmScreenTest : AbstractEndpointTest() {
    @LocalServerPort
    val port: Int = 0

    @Test
    fun phone() {
        val account = AccountSummary(id = 111, displayName = "Ray Sponsible")
        doReturn(SearchAccountResponse(listOf(account))).whenever(accountApi).searchAccount(any())

        val url = "http://localhost:$port/send/confirm?amount=3000&phone-number=+237999999999"
        assertEndpointEquals("/screens/send/confirm-phone.json", url)
    }

    @Test
    fun phoneNotFound() {
        doReturn(SearchAccountResponse()).whenever(accountApi).searchAccount(any())

        val url = "http://localhost:$port/send/confirm?amount=3000&phone-number=+237999999999"
        assertEndpointEquals("/screens/send/confirm-no-recipient.json", url)
    }

    @Test
    fun recipientId() {
        val account = Account(id = 111, displayName = "Ray Sponsible", phone = Phone(number = "+237999999999"))
        doReturn(GetAccountResponse(account)).whenever(accountApi).getAccount(any())

        val url = "http://localhost:$port/send/confirm?amount=3000&account-id=55"
        assertEndpointEquals("/screens/send/confirm-id.json", url)
    }
}
