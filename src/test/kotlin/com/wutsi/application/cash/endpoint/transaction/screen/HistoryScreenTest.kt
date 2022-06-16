package com.wutsi.application.cash.endpoint.transaction.screen

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import com.wutsi.platform.account.dto.AccountSummary
import com.wutsi.platform.account.dto.ListPaymentMethodResponse
import com.wutsi.platform.account.dto.PaymentMethodSummary
import com.wutsi.platform.account.dto.SearchAccountResponse
import com.wutsi.platform.payment.dto.SearchTransactionResponse
import com.wutsi.platform.payment.dto.TransactionSummary
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import java.time.OffsetDateTime
import java.time.ZoneOffset

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class HistoryScreenTest : AbstractEndpointTest() {
    @LocalServerPort
    val port: Int = 0

    private lateinit var url: String

    @BeforeEach
    override fun setUp() {
        super.setUp()

        url = "http://localhost:$port/history"
    }

    @Test
    fun index() {
        val txs = listOf(
            createCashInOutTransactionSummary(true, "A", "FAILED"),
            createCashInOutTransactionSummary(true, "A"),
            createTransferTransactionSummary(USER_ID, 100),
            createTransferTransactionSummary(101, USER_ID, "PENDING"),
            createTransferTransactionSummary(101, USER_ID),
            createCashInOutTransactionSummary(false, "B")
        )
        doReturn(SearchTransactionResponse(txs)).whenever(paymentApi).searchTransaction(any())

        val paymentMethods = listOf(
            createPaymentMethodSummary("A", "11111"),
            createPaymentMethodSummary("B", "22222"),
            createPaymentMethodSummary("C", "33333"),
        )
        doReturn(ListPaymentMethodResponse(paymentMethods)).whenever(accountApi).listPaymentMethods(any())

        val accounts = listOf(
            createAccount(USER_ID),
            createAccount(100),
            createAccount(101)
        )
        doReturn(SearchAccountResponse(accounts)).whenever(accountApi).searchAccount(any())

        assertEndpointEquals("/screens/transaction/history.json", url)
    }

    @Test
    fun noTransactions() {
        doReturn(SearchTransactionResponse()).whenever(paymentApi).searchTransaction(any())

        assertEndpointEquals("/screens/transaction/history-empty.json", url)
    }

    private fun createTransferTransactionSummary(accountId: Long, recipientId: Long, status: String = "SUCCESSFUL") =
        TransactionSummary(
            accountId = accountId,
            recipientId = recipientId,
            type = "TRANSFER",
            status = status,
            net = 9000.0,
            amount = 10000.0,
            fees = 1000.0,
            description = "Sample description",
            created = OffsetDateTime.of(2021, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC)
        )

    private fun createCashInOutTransactionSummary(
        cashin: Boolean,
        paymentMethodToken: String,
        status: String = "SUCCESSFUL"
    ) =
        TransactionSummary(
            accountId = USER_ID,
            type = if (cashin) "CASHIN" else "CASHOUT",
            status = status,
            net = 10000.0,
            amount = 10000.0,
            paymentMethodToken = paymentMethodToken,
            description = "Sample description",
            created = OffsetDateTime.of(2021, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC)
        )

    private fun createPaymentMethodSummary(token: String, maskedNumber: String) = PaymentMethodSummary(
        token = token,
        maskedNumber = maskedNumber
    )

    private fun createAccount(id: Long) = AccountSummary(
        id = id,
        displayName = "Name $id"
    )
}
