package com.wutsi.application.cash.endpoint.pay.screen

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import com.wutsi.platform.account.dto.Account
import com.wutsi.platform.account.dto.GetAccountResponse
import com.wutsi.platform.payment.dto.GetPaymentRequestResponse
import com.wutsi.platform.payment.dto.PaymentRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class PayConfirmScreenTest : AbstractEndpointTest() {
    @LocalServerPort
    val port: Int = 0

    private lateinit var url: String

    @BeforeEach
    override fun setUp() {
        super.setUp()

        url = "http://localhost:$port/pay/confirm?payment-request-id=1111"
    }

    @Test
    fun index() {
        val paymentRequest = PaymentRequest(
            id = "1111",
            accountId = 1111,
            amount = 50000.0,
            currency = "XAF",
        )
        doReturn(GetPaymentRequestResponse(paymentRequest)).whenever(paymentApi).getPaymentRequest(any())

        val account = Account(
            id = 1111L,
            displayName = "Maison H",
            pictureUrl = "https://pic.com/1.png"
        )
        doReturn(GetAccountResponse(account)).whenever(accountApi).getAccount(paymentRequest.accountId)

        assertEndpointEquals("/screens/pay/confirm.json", url)
    }
}
