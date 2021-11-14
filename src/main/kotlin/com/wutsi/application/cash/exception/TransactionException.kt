package com.wutsi.application.cash.exception

import feign.FeignException

class TransactionException(cause: FeignException) : Exception(cause)
