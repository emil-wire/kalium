package com.wire.kalium.logic.feature.session

import com.wire.kalium.logic.configuration.ServerConfig
import com.wire.kalium.logic.data.session.SessionRepository
import com.wire.kalium.logic.failure.SessionFailure
import com.wire.kalium.logic.feature.auth.AuthSession
import com.wire.kalium.logic.functional.Either
import io.mockative.Mock
import io.mockative.classOf
import io.mockative.given
import io.mockative.mock
import io.mockative.once
import io.mockative.verify
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CurrentSessionUseCaseTest {
    @Mock
    val sessionRepository: SessionRepository = mock(classOf<SessionRepository>())

    lateinit var currentSessionUseCase: CurrentSessionUseCase

    @BeforeTest
    fun setup() {
        currentSessionUseCase = CurrentSessionUseCase(sessionRepository)
    }

    @Test
    fun givenAUserID_whenCurrentSessionSuccess_thenTheSuccessIsPropagated() = runTest {
        val expected: AuthSession = randomAuthSession()

        given(sessionRepository).coroutine { currentSession() }.then { Either.Right(expected) }

        val actual = currentSessionUseCase()

        assertIs<CurrentSessionResult.Success>(actual)
        assertEquals(expected, actual.authSession)

        verify(sessionRepository).coroutine { currentSession() }.wasInvoked(exactly = once)
    }

    @Test
    fun givenAUserID_whenCurrentSessionFailWithNoSessionFound_thenTheErrorIsPropagated() = runTest {
        val expected: SessionFailure = SessionFailure.NoSessionFound

        given(sessionRepository).coroutine { currentSession() }.then { Either.Left(expected) }

        val actual = currentSessionUseCase()

        assertIs<CurrentSessionResult.Failure.SessionNotFound>(actual)

        verify(sessionRepository).coroutine { currentSession() }.wasInvoked(exactly = once)
    }


    private companion object {
        val randomString get() = Random.nextBytes(64).decodeToString()
        fun randomServerConfig(): ServerConfig =
            ServerConfig(randomString, randomString, randomString, randomString, randomString, randomString)

        fun randomAuthSession(): AuthSession = AuthSession(randomString, randomString, randomString, randomString, randomServerConfig())
    }
}