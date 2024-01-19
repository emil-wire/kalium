/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.kalium.logic.util.arrangement.repository

import com.wire.kalium.logic.StorageFailure
import com.wire.kalium.logic.data.session.SessionRepository
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.functional.Either
import io.mockative.Mock
import io.mockative.given
import io.mockative.matchers.Matcher
import io.mockative.mock

internal interface SessionRepositoryArrangement {
    @Mock
    val sessionRepository: SessionRepository

    fun withIsFederated(result: Either<StorageFailure, Boolean>, userId: Matcher<UserId>)

}

internal class SessionRepositoryArrangementImpl : SessionRepositoryArrangement {
    @Mock
    override val sessionRepository: SessionRepository = mock(SessionRepository::class)
    override fun withIsFederated(result: Either<StorageFailure, Boolean>, userId: Matcher<UserId>) {
        given(sessionRepository)
            .function(sessionRepository::isFederated)
            .whenInvokedWith(userId)
            .then { result }
    }
}


