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
package com.wire.kalium.logic.util.arrangement.mls

import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.feature.message.StaleEpochVerifier
import com.wire.kalium.logic.functional.Either
import io.mockative.Mock
import io.mockative.any
import io.mockative.given
import io.mockative.mock

interface StaleEpochVerifierArrangement {

    val staleEpochVerifier: StaleEpochVerifier

    fun withVerifyEpoch(result: Either<CoreFailure, Unit>)

}

class StaleEpochVerifierArrangementImpl : StaleEpochVerifierArrangement {

    @Mock
    override val staleEpochVerifier = mock(StaleEpochVerifier::class)

    override fun withVerifyEpoch(result: Either<CoreFailure, Unit>) {
        given(staleEpochVerifier)
            .suspendFunction(staleEpochVerifier::verifyEpoch)
            .whenInvokedWith(any())
            .thenReturn(result)
    }
}
