/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.kalium.logic.data.auth.verification

import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.test_util.TestNetworkException
import com.wire.kalium.logic.util.shouldFail
import com.wire.kalium.logic.util.shouldSucceed
import com.wire.kalium.network.api.base.unauthenticated.VerificationCodeApi
import com.wire.kalium.network.exceptions.KaliumException
import com.wire.kalium.network.utils.NetworkResponse
import io.ktor.http.HttpStatusCode
import io.mockative.Mock
import io.mockative.any
import io.mockative.classOf
import io.mockative.eq
import io.mockative.given
import io.mockative.mock
import io.mockative.once
import io.mockative.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SecondFactorVerificationRepositoryTest {

    @Test
    fun givenApiSucceeds_whenInvokingSendVerificationCode_thenShouldPropagateSuccess() = runTest {
        val (_, secondFactorVerificationRepository) = Arrangement()
            .withCodeRequestSucceeding()
            .arrange()

        val result = secondFactorVerificationRepository.requestVerificationCode(
            EMAIL,
            VerifiableAction.LOGIN_OR_CLIENT_REGISTRATION
        )

        result.shouldSucceed()
    }

    @Test
    fun givenApiFails_whenInvokingSendVerificationCode_thenShouldPropagateFailure() = runTest {
        val failure = TestNetworkException.badRequest
        val (_, secondFactorVerificationRepository) = Arrangement()
            .withCodeRequestFailingWith(failure)
            .arrange()

        val result = secondFactorVerificationRepository.requestVerificationCode(
            EMAIL,
            VerifiableAction.LOGIN_OR_CLIENT_REGISTRATION
        )

        result.shouldFail { networkFailure ->
            assertIs<NetworkFailure.ServerMiscommunication>(networkFailure)
            assertEquals(networkFailure.kaliumException, failure)
        }
    }

    @Test
    fun givenAnEmail_whenInvokingSendVerificationCode_thenShouldPassTheCorrectEmailToTheApiSuccess() = runTest {
        val (arrangement, secondFactorVerificationRepository) = Arrangement()
            .withCodeRequestSucceeding()
            .arrange()

        secondFactorVerificationRepository.requestVerificationCode(
            EMAIL,
            VerifiableAction.LOGIN_OR_CLIENT_REGISTRATION
        )

        verify(arrangement.verificationCodeApi)
            .suspendFunction(arrangement.verificationCodeApi::sendVerificationCode)
            .with(eq(EMAIL), any())
            .wasInvoked(exactly = once)
    }

    private class Arrangement {

        @Mock
        val verificationCodeApi = mock(classOf<VerificationCodeApi>())

        fun withCodeRequestSucceeding() = withCodeRequestReturning(
            NetworkResponse.Success(
                value = Unit,
                headers = mapOf(),
                httpCode = HttpStatusCode.OK.value
            )
        )

        fun withCodeRequestFailingWith(kaliumException: KaliumException) = withCodeRequestReturning(
            NetworkResponse.Error(kaliumException)
        )

        fun withCodeRequestReturning(networkResponse: NetworkResponse<Unit>) = apply {
            given(verificationCodeApi)
                .suspendFunction(verificationCodeApi::sendVerificationCode)
                .whenInvokedWith(any(), any())
                .thenReturn(networkResponse)
        }

        fun arrange(): Pair<Arrangement, SecondFactorVerificationRepository> =
            this to SecondFactorVerificationRepositoryImpl(verificationCodeApi)

    }

    private companion object {
        const val EMAIL = "email"
    }
}