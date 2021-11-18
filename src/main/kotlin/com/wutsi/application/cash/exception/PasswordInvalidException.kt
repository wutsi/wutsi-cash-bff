package com.wutsi.application.cash.exception

import feign.FeignException

class PasswordInvalidException(cause: FeignException) : Exception(cause)
