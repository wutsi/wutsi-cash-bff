package com.wutsi.application.cash.util

import com.wutsi.application.shared.service.StringUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class StringUtilTest {
    @Test
    fun test() {
        assertEquals("L", StringUtil.initials("l"))
        assertEquals("L", StringUtil.initials("lm"))
        assertEquals("L", StringUtil.initials(" lm "))
        assertEquals("HT", StringUtil.initials("Herve tchepannou"))
        assertEquals("RS", StringUtil.initials("Ray T. Sponsible "))
    }
}
