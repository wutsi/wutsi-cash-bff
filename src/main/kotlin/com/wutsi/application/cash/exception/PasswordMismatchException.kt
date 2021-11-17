package com.wutsi.application.cash.exception

import feign.FeignException

class PasswordMismatchException(cause: FeignException) : Exception(cause)
