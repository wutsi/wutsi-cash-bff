package com.wutsi.application.cash.endpoint.cashin.screen

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import com.wutsi.platform.account.dto.GetPaymentMethodResponse
import com.wutsi.platform.account.dto.PaymentMethod
import com.wutsi.platform.account.dto.Phone
import com.wutsi.platform.payment.dto.ComputeTransactionFeesResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class CashinConfirmScreenTest : AbstractEndpointTest() {
    @LocalServerPort
    val port: Int = 0

    private lateinit var url: String

    @BeforeEach
    override fun setUp() {
        super.setUp()

        url = "http://localhost:$port/cashin/confirm?amount=5000&payment-token=4304309409"
    }

    @Test
    fun confirmWithFeesToSender() {
        val resp = ComputeTransactionFeesResponse(
            fees = 90.0,
            gatewayFees = 10.0,
            applyToSender = true
        )
        doReturn(resp).whenever(paymentApi).computeTransactionFees(any())

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

        assertEndpointEquals("/screens/cashin/confirm-fees-to-sender.json", url)
    }

    @Test
    fun confirmWithFeesToRecipient() {
        val resp = ComputeTransactionFeesResponse(
            fees = 90.0,
            gatewayFees = 10.0,
            applyToSender = false
        )
        doReturn(resp).whenever(paymentApi).computeTransactionFees(any())

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

        assertEndpointEquals("/screens/cashin/confirm-fees-to-recipient.json", url)
    }

    @Test
    fun confirmWithNoFees() {
        val resp = ComputeTransactionFeesResponse(
            fees = 0.0,
            gatewayFees = 0.0,
            applyToSender = false
        )
        doReturn(resp).whenever(paymentApi).computeTransactionFees(any())

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

        assertEndpointEquals("/screens/cashin/confirm-no-fees.json", url)
    }
}
