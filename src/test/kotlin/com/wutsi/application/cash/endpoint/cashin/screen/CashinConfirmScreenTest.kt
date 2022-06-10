package com.wutsi.application.cash.endpoint.cashin.screen

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import com.wutsi.application.cash.service.IdempotencyKeyGenerator
import com.wutsi.platform.account.dto.GetPaymentMethodResponse
import com.wutsi.platform.account.dto.PaymentMethod
import com.wutsi.platform.account.dto.Phone
import com.wutsi.platform.payment.dto.ComputeFeesResponse
import com.wutsi.platform.payment.dto.TransactionFee
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.server.LocalServerPort

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class CashinConfirmScreenTest : AbstractEndpointTest() {
    @LocalServerPort
    val port: Int = 0

    @MockBean
    private lateinit var idempotencyKeyGenerator: IdempotencyKeyGenerator

    private lateinit var url: String
    private val amount = 5000.0
    private val fees = 50.0

    @BeforeEach
    override fun setUp() {
        super.setUp()

        url = "http://localhost:$port/cashin/confirm?amount=$amount&payment-token=4304309409"

        doReturn("123").whenever(idempotencyKeyGenerator).generate()
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

        val transactionFee = TransactionFee(
            amount = amount,
            fees = fees,
            senderAmount = amount + fees,
            recipientAmount = amount,
            applyFeesToSender = true
        )
        doReturn(ComputeFeesResponse(transactionFee)).whenever(paymentApi).computeFees(any())

        assertEndpointEquals("/screens/cashin/confirm.json", url)
    }
}
