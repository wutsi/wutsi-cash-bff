package com.wutsi.application.cash.endpoint.cashout.dto

data class CashoutRequest(
    val paymentToken: String = "",
    val amount: Double = 0.0,
)
