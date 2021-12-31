package com.wutsi.application.cash.exception

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.platform.core.error.ErrorResponse
import com.wutsi.platform.qr.error.ErrorURN
import feign.FeignException

class QrCodeException(val errorKey: String, cause: FeignException? = null) : Exception(cause) {
    companion object {
        const val EXPIRED = "prompt.error.qr-code.EXPIRED"
        const val INVALID = "prompt.error.qr-code.INVALID"

        fun of(mapper: ObjectMapper, cause: FeignException): QrCodeException {
            try {
                val response = mapper.readValue(cause.contentUTF8(), ErrorResponse::class.java)
                return if (response.error.code == ErrorURN.EXPIRED.urn)
                    QrCodeException(EXPIRED, cause)
                else
                    QrCodeException(INVALID, cause)
            } catch (ex: Exception) {
                return QrCodeException(INVALID, cause)
            }
        }
    }
}
