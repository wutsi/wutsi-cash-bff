package com.wutsi.application.cash.endpoint.pay.screen

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import com.wutsi.platform.account.dto.Account
import com.wutsi.platform.account.dto.GetAccountResponse
import com.wutsi.platform.payment.core.ErrorCode
import com.wutsi.platform.payment.dto.GetPaymentRequestResponse
import com.wutsi.platform.payment.dto.PaymentRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class PaySuccessScreenTest : AbstractEndpointTest() {
    @LocalServerPort
    val port: Int = 0

    private lateinit var paymentRequest: PaymentRequest

    private lateinit var merchant: Account

    @BeforeEach
    override fun setUp() {
        super.setUp()

        paymentRequest = PaymentRequest(
            id = "1111",
            accountId = 1111,
            amount = 50000.0,
            currency = "XAF",
        )
        doReturn(GetPaymentRequestResponse(paymentRequest)).whenever(paymentApi).getPaymentRequest(any())

        merchant = Account(
            id = paymentRequest.accountId,
            displayName = "Maison H"
        )
        doReturn(GetAccountResponse(merchant)).whenever(accountApi).getAccount(paymentRequest.accountId)
    }

    @Test
    fun success() {
        val url = "http://localhost:$port/pay/success?payment-request-id=1111"
        assertEndpointEquals("/screens/pay/success.json", url)
    }

    @Test
    fun failure() {
        val url = "http://localhost:$port/pay/success?payment-request-id=1111&error=${ErrorCode.NOT_ENOUGH_FUNDS}"
        assertEndpointEquals("/screens/pay/success-fail.json", url)
    }
}
