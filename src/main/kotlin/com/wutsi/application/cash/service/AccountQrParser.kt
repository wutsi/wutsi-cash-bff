package com.wutsi.application.cash.service

import org.springframework.stereotype.Service

@Service
class AccountQrParser {
    fun parse(key: String): AccountQrCode {
        val parts = key.split(':')
        if (parts.size != 3)
            throw IllegalStateException("Invalid QR code: $key")

        if (parts[0] != "account")
            throw IllegalStateException("Invalid QR code type: $key")

        val expiry = parts[2].toLong()
        if (System.currentTimeMillis() > expiry)
            throw IllegalStateException("QR code has expired: $key")

        return AccountQrCode(parts[2].toLong(), key)
    }
}

data class AccountQrCode(val accountId: Long, val code: String)
