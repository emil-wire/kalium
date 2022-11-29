package com.wire.kalium.api.v0.user.self

import com.wire.kalium.api.ApiTest
import com.wire.kalium.api.json.model.ErrorResponseJson
import com.wire.kalium.model.UserDTOJson
import com.wire.kalium.network.api.v0.authenticated.SelfApiV0
import com.wire.kalium.network.exceptions.KaliumException
import com.wire.kalium.network.utils.isSuccessful
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class SelfApiV0Test : ApiTest {
    @Test
    fun givenAValidRegisterLogoutRequest_whenCallingTheRegisterLogoutEndpoint_theRequestShouldBeConfiguredCorrectly() =
        runTest {
            val networkClient = mockAuthenticatedNetworkClient(
                VALID_SELF_RESPONSE.rawJson,
                statusCode = HttpStatusCode.Created,
                assertion = {
                    assertGet()
                    assertNoQueryParams()
                    assertPathEqual(PATH_SELF)
                }
            )
            val selfApi = SelfApiV0(networkClient)
            val response = selfApi.getSelfInfo()
            assertTrue(response.isSuccessful())
            assertEquals(response.value, VALID_SELF_RESPONSE.serializableData)
        }

    @Test
    fun givenTheServerReturnsAnError_whenCallingTheGetSelfEndpoint_thenExceptionIsPropagated() = runTest {
        val networkClient = mockAuthenticatedNetworkClient(
            ErrorResponseJson.valid.rawJson,
            statusCode = HttpStatusCode.BadRequest
        )
        val selfApi = SelfApiV0(networkClient)
        val response = selfApi.getSelfInfo()
        assertFalse(response.isSuccessful())
        assertTrue(response.kException is KaliumException.InvalidRequestError)
        assertEquals((response.kException as KaliumException.InvalidRequestError).errorResponse, ERROR_RESPONSE)
    }

    private companion object {
        const val PATH_SELF = "/self"
        val VALID_SELF_RESPONSE = UserDTOJson.valid
        val ERROR_RESPONSE = ErrorResponseJson.valid.serializableData
    }
}
