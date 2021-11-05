package com.wutsi.application.cash.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class URLBuilder(
    @Value("\${wutsi.application.server-url}") private val serverUrl: String
) {
    fun build(path: String) = "$serverUrl/$path"
}
