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
package com.wire.kalium.logic.feature.e2ei.usecase

import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.client.MLSClientProvider
import com.wire.kalium.logic.data.e2ei.CertificateRevocationListRepository
import com.wire.kalium.logic.data.id.CurrentClientIdProvider
import com.wire.kalium.logic.feature.conversation.MLSConversationsVerificationStatusesHandler
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.flatMap
import com.wire.kalium.logic.functional.map

/**
 * Use case to check if the CRL is expired and if so, register CRL and update conversation statuses if there is a change.
 */
interface CheckRevocationListUseCase {
    suspend operator fun invoke(url: String): Either<CoreFailure, ULong?>
}

internal class CheckRevocationListUseCaseImpl(
    private val certificateRevocationListRepository: CertificateRevocationListRepository,
    private val currentClientIdProvider: CurrentClientIdProvider,
    private val mlsClientProvider: MLSClientProvider,
    private val mLSConversationsVerificationStatusesHandler: MLSConversationsVerificationStatusesHandler
) : CheckRevocationListUseCase {
    override suspend fun invoke(url: String): Either<CoreFailure, ULong?> {
        return certificateRevocationListRepository.getClientDomainCRL(url).flatMap {
            currentClientIdProvider().flatMap { clientId ->
                mlsClientProvider.getMLSClient(clientId).map { mlsClient ->
                    mlsClient.registerCrl(url, it).run {
                        if (dirty) {
                            mLSConversationsVerificationStatusesHandler()
                        }
                        this.expiration
                    }
                }
            }
        }
    }
}
