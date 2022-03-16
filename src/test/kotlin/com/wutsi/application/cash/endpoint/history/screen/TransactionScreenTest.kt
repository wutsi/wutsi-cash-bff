package com.wutsi.application.cash.endpoint.history.screen

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.application.cash.endpoint.AbstractEndpointTest
import com.wutsi.ecommerce.order.WutsiOrderApi
import com.wutsi.ecommerce.order.dto.GetOrderResponse
import com.wutsi.ecommerce.order.dto.Order
import com.wutsi.ecommerce.order.entity.OrderStatus
import com.wutsi.platform.account.dto.AccountSummary
import com.wutsi.platform.account.dto.ListPaymentMethodResponse
import com.wutsi.platform.account.dto.PaymentMethodSummary
import com.wutsi.platform.account.dto.SearchAccountResponse
import com.wutsi.platform.payment.dto.GetTransactionResponse
import com.wutsi.platform.payment.dto.Transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.web.server.LocalServerPort
import java.time.OffsetDateTime
import java.time.ZoneOffset

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class TransactionScreenTest : AbstractEndpointTest() {
    @LocalServerPort
    val port: Int = 0

    private lateinit var url: String

    @MockBean
    private lateinit var orderApi: WutsiOrderApi

    @BeforeEach
    override fun setUp() {
        super.setUp()

        url = "http://localhost:$port/transaction?id=111"
    }

    @Test
    fun index() {
        val tx = createTransferTransaction(USER_ID, 100, status = "PENDING")
        doReturn(GetTransactionResponse(tx)).whenever(paymentApi).getTransaction(any())

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

        val order = createOrder()
        doReturn(GetOrderResponse(order)).whenever(orderApi).getOrder(any())

        assertEndpointEquals("/screens/history/transaction.json", url)
    }

    private fun createOrder() = Order(
        id = "111",
        status = OrderStatus.PROCESSING.name,
        created = OffsetDateTime.now()
    )

    private fun createTransferTransaction(accountId: Long, recipientId: Long, status: String = "SUCCESSFUL") =
        Transaction(
            accountId = accountId,
            recipientId = recipientId,
            type = "TRANSFER",
            status = status,
            net = 9000.0,
            amount = 10100.0,
            fees = 1000.0,
            gatewayFees = 100.0,
            description = "Sample description",
            orderId = "1111",
            created = OffsetDateTime.of(2021, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC),
            feesToSender = true,
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
