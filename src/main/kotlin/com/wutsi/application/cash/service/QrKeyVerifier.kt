package com.wutsi.application.cash.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.springframework.stereotype.Service
import java.security.interfaces.RSAPublicKey

@Service
class QrKeyVerifier(private val keyProvider: QrKeyProvider) {
    fun verify(key: String) {
        val algorithm = getAlgorithm(key)
        JWT.require(algorithm)
            .build()
            .verify(key)
    }

    private fun getAlgorithm(jwt: String): Algorithm {
        val token = JWT.decode(jwt)
        if (token.algorithm == "RS256") {
            val key = keyProvider.getKey(token.keyId)
            return Algorithm.RSA256(key as RSAPublicKey, null)
        }

        throw IllegalStateException("Encryption algorithm not supported: ${token.algorithm}")
    }
}
