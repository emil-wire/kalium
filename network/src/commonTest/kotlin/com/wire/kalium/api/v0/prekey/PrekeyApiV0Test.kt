package com.wire.kalium.api.v0.prekey

import com.wire.kalium.api.ApiTest
import com.wire.kalium.api.json.model.DomainToUserIdToClientToPreKeyMapJson
import com.wire.kalium.api.json.model.DomainToUserIdToClientsMapJson
import com.wire.kalium.api.json.model.ErrorResponseJson
import com.wire.kalium.network.api.base.authenticated.prekey.PreKeyApi
import com.wire.kalium.network.api.v0.authenticated.PreKeyApiV0
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
class PrekeyApiV0Test : ApiTest {

    @Test
    fun givenAValidDomainToUserIdToClientsMap_whenCallingGetUsersPrekeyEndpoint_theRequestShouldBeConfiguredCorrectly() = runTest {
        val networkClient = mockAuthenticatedNetworkClient(
            VALID_GET_USERS_PREKEY_RESPONSE.rawJson,
            statusCode = HttpStatusCode.OK,
            assertion = {
                assertPost()
                assertJson()
                assertPathEqual(PATH_PREKEYS)
            }
        )
        val preKeyApi: PreKeyApi = PreKeyApiV0(networkClient)

        val response = preKeyApi.getUsersPreKey(VALID_GET_USERS_PREKEY_REQUEST.serializableData)
        assertTrue(response.isSuccessful())
        assertEquals(response.value, VALID_GET_USERS_PREKEY_RESPONSE.serializableData)
    }

    @Test
    fun givenTheServerReturnsAnError_whenCallingGetUsersPrekeyEndpoint_theCorrectExceptionIsThrown() = runTest {
        val networkClient = mockAuthenticatedNetworkClient(
            ErrorResponseJson.valid.rawJson,
            statusCode = HttpStatusCode.Forbidden,
        )
        val preKeyApi: PreKeyApi = PreKeyApiV0(networkClient)
        val errorResponse = preKeyApi.getUsersPreKey(VALID_GET_USERS_PREKEY_REQUEST.serializableData)
        assertFalse(errorResponse.isSuccessful())
        assertTrue(errorResponse.kException is KaliumException.InvalidRequestError)
        assertEquals((errorResponse.kException as KaliumException.InvalidRequestError).errorResponse, ERROR_RESPONSE)
    }

    private companion object {
        val VALID_GET_USERS_PREKEY_REQUEST = DomainToUserIdToClientsMapJson.valid
        val VALID_GET_USERS_PREKEY_RESPONSE = DomainToUserIdToClientToPreKeyMapJson.valid
        val ERROR_RESPONSE = ErrorResponseJson.valid.serializableData
        const val PATH_PREKEYS = "/users/list-prekeys"
    }
}
