package com.wutsi.application.cash.endpoint.transaction.screen

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import com.wutsi.platform.account.dto.AccountSummary
import com.wutsi.platform.account.dto.ListPaymentMethodResponse
import com.wutsi.platform.account.dto.PaymentMethodSummary
import com.wutsi.platform.account.dto.SearchAccountResponse
import com.wutsi.platform.payment.PaymentMethodProvider
import com.wutsi.platform.payment.dto.GetTransactionResponse
import com.wutsi.platform.payment.dto.Transaction
import com.wutsi.platform.payment.entity.TransactionType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class TransactionSuccessScreenTest : AbstractEndpointTest() {
    @LocalServerPort
    val port: Int = 0

    val recipientId = 888L
    val paymentToken = "20932093"
    private val accounts = listOf(
        AccountSummary(id = recipientId, displayName = "John Smith"),
        AccountSummary(id = USER_ID, displayName = "Ray Spnsible")
    )

    val paymentMethods = listOf(
        PaymentMethodSummary(
            token = paymentToken,
            provider = PaymentMethodProvider.ORANGE.name
        )
    )

    @BeforeEach
    override fun setUp() {
        super.setUp()

        doReturn(SearchAccountResponse(accounts)).whenever(accountApi).searchAccount(any())
        doReturn(ListPaymentMethodResponse(paymentMethods)).whenever(accountApi).listPaymentMethods(any())
    }

    @Test
    fun success() {
        val tx = createTransaction(TransactionType.TRANSFER)
        doReturn(GetTransactionResponse(tx)).whenever(paymentApi).getTransaction(any())

        val url = "http://localhost:$port/transaction/success?transaction-id=555"
        assertEndpointEquals("/screens/transaction/success.json", url)
    }

    private fun createTransaction(type: TransactionType) = Transaction(
        accountId = USER_ID,
        recipientId = recipientId,
        type = type.name,
        amount = 1000.0,
        currency = "XAF",
        paymentMethodToken = paymentToken
    )
}
