package com.wutsi.application.cash.dto

data class CashinRequest(
    val paymentToken: String = "",
    val amount: Double = 0.0,
)
