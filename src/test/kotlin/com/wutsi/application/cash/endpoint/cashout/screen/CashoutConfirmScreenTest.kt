package com.wutsi.application.cash.endpoint.cashout.screen

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import com.wutsi.platform.account.dto.GetPaymentMethodResponse
import com.wutsi.platform.account.dto.PaymentMethod
import com.wutsi.platform.account.dto.Phone
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class CashoutConfirmScreenTest : AbstractEndpointTest() {
    @LocalServerPort
    val port: Int = 0

    private lateinit var url: String

    @BeforeEach
    override fun setUp() {
        super.setUp()

        url = "http://localhost:$port/cashout/confirm?amount=5000&payment-token=4304309409"
    }

    @Test
    fun confirm() {
        val response = GetPaymentMethodResponse(
            paymentMethod = PaymentMethod(
                token = "xxxxxx",
                provider = "MTN",
                maskedNumber = "xxxx9999",
                phone = Phone(
                    number = "+237670000001"
                )
            )
        )
        doReturn(response).whenever(accountApi).getPaymentMethod(any(), any())

        assertEndpointEquals("/screens/cashout/confirm.json", url)
    }
}
