package com.wutsi.application.cash.endpoint.cashin.command

import com.wutsi.application.cash.endpoint.AbstractCommand
import com.wutsi.application.shared.service.TenantProvider
import com.wutsi.flutter.sdui.Action
import com.wutsi.platform.payment.core.Status
import com.wutsi.platform.payment.dto.CreateCashinRequest
import com.wutsi.platform.payment.dto.CreateCashinResponse
import com.wutsi.platform.payment.dto.Transaction
import feign.FeignException
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/commands/cashin")
class CashinCommand(
    private val tenantProvider: TenantProvider,
) : AbstractCommand() {
    companion object {
        const val DELAY_SECONDS = 11L
        const val MAX_RETRIES = 5
        private val LOGGER = LoggerFactory.getLogger(CashinCommand::class.java)
    }

    @PostMapping
    fun index(
        @RequestParam amount: Double,
        @RequestParam("payment-token") paymentToken: String,
        @RequestParam(name = "idempotency-key") idempotencyKey: String
    ): Action {
        logger.add("amount", amount)
        logger.add("payment_token", paymentToken)

        // Cashin
        try {
            val response = cashin(amount, paymentToken, idempotencyKey)
            var status = response.status
            logger.add("transaction_id", response.id)

            if (response.status == Status.PENDING.name) {
                val tx = waitForCompletion(response.id)
                status = tx.status
            }

            logger.add("transaction_status", status)
            return gotoUrl(
                url = if (response.status == Status.SUCCESSFUL.name)
                    urlBuilder.build("cashin/success?amount=$amount")
                else
                    urlBuilder.build("cashin/pending")
            )
        } catch (ex: FeignException) {
            logger.setException(ex)
            val error = getErrorText(ex)
            return gotoUrl(
                url = urlBuilder.build("cashin/success?amount=$amount&error=" + encodeURLParam(error))
            )
        }
    }

    private fun cashin(amount: Double, paymentToken: String, idempotencyKey: String): CreateCashinResponse =
        paymentApi.createCashin(
            CreateCashinRequest(
                paymentMethodToken = paymentToken,
                amount = amount,
                currency = tenantProvider.get().currency,
                idempotencyKey = idempotencyKey
            )
        )

    private fun waitForCompletion(transactionId: String): Transaction {
        var retries = 0
        var tx = Transaction()
        try {
            while (retries++ < MAX_RETRIES) {
                LOGGER.info("$retries - Transaction #$transactionId is PENDING. Wait for $DELAY_SECONDS sec...")
                Thread.sleep(DELAY_SECONDS * 1000) // Wait for 15 secs...
                val response = paymentApi.getTransaction(transactionId)
                tx = response.transaction
                if (tx.status != Status.PENDING.name)
                    break
            }
            return tx
        } finally {
            logger.add("retries", retries - 1)
        }
    }
}
