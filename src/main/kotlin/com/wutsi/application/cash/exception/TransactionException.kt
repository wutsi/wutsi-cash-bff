package com.wutsi.application.cash.exception

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.platform.core.error.ErrorResponse
import com.wutsi.platform.payment.core.ErrorCode
import com.wutsi.platform.payment.core.ErrorCode.UNEXPECTED_ERROR
import feign.FeignException

class TransactionException(val error: ErrorCode, val transactionId: String? = null, cause: FeignException) :
    Exception(cause) {
    companion object {
        fun of(mapper: ObjectMapper, cause: FeignException): TransactionException {
            try {
                val response = mapper.readValue(cause.contentUTF8(), ErrorResponse::class.java)
                return TransactionException(
                    ErrorCode.valueOf(response.error.downstreamCode!!),
                    response.error.data?.get("transaction-id")?.toString(),
                    cause
                )
            } catch (ex: Exception) {
                return TransactionException(UNEXPECTED_ERROR, null, cause)
            }
        }
    }
}
