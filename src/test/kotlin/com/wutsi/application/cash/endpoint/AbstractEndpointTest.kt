package com.wutsi.application.cash.endpoint

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.platform.account.WutsiAccountApi
import com.wutsi.platform.account.dto.Account
import com.wutsi.platform.account.dto.GetAccountResponse
import com.wutsi.platform.account.dto.Phone
import com.wutsi.platform.core.security.SubjectType
import com.wutsi.platform.core.security.SubjectType.USER
import com.wutsi.platform.core.security.spring.SpringAuthorizationRequestInterceptor
import com.wutsi.platform.core.security.spring.jwt.JWTBuilder
import com.wutsi.platform.core.test.TestRSAKeyProvider
import com.wutsi.platform.core.test.TestTokenProvider
import com.wutsi.platform.core.tracing.TracingContext
import com.wutsi.platform.core.tracing.spring.SpringTracingRequestInterceptor
import com.wutsi.platform.core.util.URN
import com.wutsi.platform.payment.WutsiPaymentApi
import com.wutsi.platform.payment.dto.Balance
import com.wutsi.platform.payment.dto.GetBalanceResponse
import com.wutsi.platform.tenant.WutsiTenantApi
import com.wutsi.platform.tenant.dto.GetTenantResponse
import com.wutsi.platform.tenant.dto.Limits
import com.wutsi.platform.tenant.dto.Logo
import com.wutsi.platform.tenant.dto.MobileCarrier
import com.wutsi.platform.tenant.dto.PhonePrefix
import com.wutsi.platform.tenant.dto.Tenant
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.web.client.RestTemplate
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

abstract class AbstractEndpointTest {
    companion object {
        const val DEVICE_ID = "0000-1111"
        const val TENANT_ID = "1"
        const val USER_ID = 1L
    }

    @Autowired
    private lateinit var mapper: ObjectMapper

    @MockBean
    private lateinit var tracingContext: TracingContext

    @MockBean
    private lateinit var tenantApi: WutsiTenantApi

    @MockBean
    protected lateinit var accountApi: WutsiAccountApi

    @MockBean
    protected lateinit var paymentApi: WutsiPaymentApi

    @Autowired
    private lateinit var messages: MessageSource

    protected lateinit var rest: RestTemplate

    lateinit var traceId: String

    @BeforeTest
    open fun setUp() {
        traceId = UUID.randomUUID().toString()
        doReturn(DEVICE_ID).whenever(tracingContext).deviceId()
        doReturn(traceId).whenever(tracingContext).traceId()
        doReturn(TENANT_ID).whenever(tracingContext).tenantId()

        val tenant = Tenant(
            id = TENANT_ID.toLong(),
            name = "test",
            installUrl = "https://www.wutsi.com/install",
            logos = listOf(
                Logo(type = "PICTORIAL", url = "http://www.goole.com/images/1.png")
            ),
            countries = listOf("CM"),
            languages = listOf("en", "fr"),
            currency = "XAF",
            domainName = "www.wutsi.com",
            numberFormat = "#,###,##0",
            monetaryFormat = "#,###,##0 XAF",
            mobileCarriers = listOf(
                MobileCarrier(
                    code = "mtn",
                    name = "MTN",
                    countries = listOf("CM", "CD"),
                    phonePrefixes = listOf(
                        PhonePrefix(
                            country = "CM",
                            prefixes = listOf("+23795")
                        ),
                    ),
                    logos = listOf(
                        Logo(type = "PICTORIAL", url = "http://www.goole.com/images/mtn.png")
                    )
                ),
                MobileCarrier(
                    code = "orange",
                    name = "ORANGE",
                    countries = listOf("CM"),
                    phonePrefixes = listOf(
                        PhonePrefix(
                            country = "CM",
                            prefixes = listOf("+23722")
                        ),
                    ),
                    logos = listOf(
                        Logo(type = "PICTORIAL", url = "http://www.goole.com/images/orange.png")
                    )
                )
            ),
            limits = Limits(
                minCashin = 5000.0
            )
        )
        doReturn(GetTenantResponse(tenant)).whenever(tenantApi).getTenant(any())

        val account = Account(
            id = USER_ID,
            displayName = "Ray Sponsible",
            country = "CM",
            language = "en",
            status = "ACTIVE",
            phone = Phone(
                id = 7777,
                number = "+237699999999"
            )
        )
        doReturn(GetAccountResponse(account)).whenever(accountApi).getAccount(any())

        val balance = Balance(
            amount = 100000.0,
            currency = "XAF",
            userId = USER_ID
        )
        doReturn(GetBalanceResponse(balance)).whenever(paymentApi).getBalance(any())

        rest = createResTemplate()
    }

    private fun createResTemplate(
        scope: List<String> = listOf(
            "user-read",
            "user-manage",
            "payment-method-manage",
            "payment-method-read",
            "payment-manage",
            "payment-read"
        ),
        subjectId: Long = USER_ID,
        subjectType: SubjectType = USER
    ): RestTemplate {
        val rest = RestTemplate()
        val tokenProvider = TestTokenProvider(
            JWTBuilder(
                subject = subjectId.toString(),
                name = URN.of("user", subjectId.toString()).value,
                subjectType = subjectType,
                scope = scope,
                keyProvider = TestRSAKeyProvider(),
                admin = false
            ).build()
        )

        rest.interceptors.add(SpringTracingRequestInterceptor(tracingContext))
        rest.interceptors.add(SpringAuthorizationRequestInterceptor(tokenProvider))
        rest.interceptors.add(LanguageClientHttpRequestInterceptor())
        return rest
    }

    protected fun assertEndpointEquals(expectedPath: String, url: String) {
        val request = emptyMap<String, String>()
        val response = rest.postForEntity(url, request, Map::class.java)

        assertJsonEquals(expectedPath, response.body)
    }

    private fun assertJsonEquals(expectedPath: String, value: Any?) {
        val input = AbstractEndpointTest::class.java.getResourceAsStream(expectedPath)
        val expected = mapper.readValue(input, Any::class.java)

        val writer = mapper.writerWithDefaultPrettyPrinter()
        assertEquals(writer.writeValueAsString(expected), writer.writeValueAsString(value))
    }

    protected fun getText(key: String, args: Array<Any?> = emptyArray()) =
        messages.getMessage(key, args, LocaleContextHolder.getLocale()) ?: key
}
