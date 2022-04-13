package com.wire.kalium.api.tools.json.api.user.login

import com.wire.kalium.api.ApiTest
import com.wire.kalium.network.api.user.login.SSOLoginApi
import com.wire.kalium.network.api.user.login.SSOLoginApiImpl
import com.wire.kalium.network.utils.NetworkResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class SSOLoginApiTest: ApiTest {

    @Test
    fun givenBEResponseSuccess_whenCallingInitiateSSOEndpointWithNoRedirect_thenRequestConfiguredCorrectly() = runTest{
        val uuid = "uuid"
        val param = SSOLoginApi.InitiateParam.WithoutRedirect(uuid)
        val httpClient = mockAuthenticatedHttpClient(
            "",
            statusCode = HttpStatusCode.OK,
            assertion = {
                assertHead()
                assertNoQueryParams()
                assertPathEqual("$PATH_SSO_INITIATE/$uuid")
            }
        )
        val ssoApi: SSOLoginApi = SSOLoginApiImpl(httpClient)
        val actual = ssoApi.initiate(param, TEST_HOST)

        assertIs<NetworkResponse.Success<String>>(actual)
        assertEquals("${TEST_HOST}sso/initiate-login/$uuid", actual.value)
    }

    @Test
    fun givenBEResponseSuccess_whenCallingInitiateSSOEndpointWithRedirect_thenRequestConfiguredCorrectly() = runTest {
        val uuid = "uuid"
        val param = SSOLoginApi.InitiateParam.WithRedirect(uuid = uuid, success = "wire://success", error = "wire://error")
        val httpClient = mockAuthenticatedHttpClient(
            "",
            statusCode = HttpStatusCode.OK,
            assertion = {
                assertHead()
                assertPathEqual("$PATH_SSO_INITIATE/$uuid%3Fsuccess_redirect=${param.success}&error_redirect=${param.error}")
            }
        )
        val ssoApi: SSOLoginApi = SSOLoginApiImpl(httpClient)
        val actual = ssoApi.initiate(param, TEST_HOST)

        assertIs<NetworkResponse.Success<String>>(actual)
        assertEquals("${TEST_HOST}sso/initiate-login/$uuid%3Fsuccess_redirect=${param.success}&error_redirect=${param.error}", actual.value)
    }

    @Test
    fun givenBEResponseSuccess_whenCallingFinalizeSSOEndpointWithRedirect_thenRequestConfiguredCorrectly() = runTest {
        val cookie = "cookie"
        val httpClient = mockAuthenticatedHttpClient(
            "",
            statusCode = HttpStatusCode.OK,
            assertion = {
                assertPost()
                assertHeaderEqual(HttpHeaders.Cookie, "zuid=$cookie")
                assertPathEqual(PATH_SSO_FINALIZE)
            }
        )
        val ssoApi: SSOLoginApi = SSOLoginApiImpl(httpClient)
        val actual = ssoApi.finalize(cookie, TEST_HOST)

        assertIs<NetworkResponse.Success<String>>(actual)
    }


    private companion object {
        const val PATH_SSO_INITIATE = "/sso/initiate-login"
        const val PATH_SSO_FINALIZE = "/sso/finalize-login"

        val TEST_HOST = Url("""https://test-https.wire.com""")
    }

}