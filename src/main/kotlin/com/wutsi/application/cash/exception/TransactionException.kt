package com.wutsi.application.cash.exception

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.platform.core.error.ErrorResponse
import com.wutsi.platform.payment.core.ErrorCode
import com.wutsi.platform.payment.core.ErrorCode.UNEXPECTED_ERROR
import feign.FeignException

class TransactionException(val error: ErrorCode, cause: FeignException) : Exception(cause) {
    companion object {
        fun of(mapper: ObjectMapper, cause: FeignException): TransactionException {
            try {
                val response = mapper.readValue(cause.contentUTF8(), ErrorResponse::class.java)
                return TransactionException(ErrorCode.valueOf(response.error.downstreamCode!!), cause)
            } catch (ex: Exception) {
                return TransactionException(UNEXPECTED_ERROR, cause)
            }
        }
    }
}
