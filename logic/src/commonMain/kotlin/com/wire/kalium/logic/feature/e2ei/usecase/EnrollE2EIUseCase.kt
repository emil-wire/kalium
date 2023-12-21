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
package com.wire.kalium.logic.feature.e2ei.usecase

import com.wire.kalium.cryptography.NewAcmeAuthz
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.E2EIFailure
import com.wire.kalium.logic.data.e2ei.E2EIRepository
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.getOrFail
import com.wire.kalium.logic.functional.getOrNull
import com.wire.kalium.logic.functional.onFailure
import com.wire.kalium.logic.kaliumLogger

/**
 * Issue an E2EI certificate and re-initiate the MLSClient
 */
interface EnrollE2EIUseCase {
    suspend fun initialEnrollment(): Either<CoreFailure, E2EIEnrollmentResult>
    suspend fun finalizeEnrollment(
        idToken: String,
        oAuthState: String,
        initializationResult: E2EIEnrollmentResult.Initialized
    ): Either<E2EIFailure, E2EIEnrollmentResult>
}

@Suppress("ReturnCount")
class EnrollE2EIUseCaseImpl internal constructor(
    private val e2EIRepository: E2EIRepository,
) : EnrollE2EIUseCase {
    /**
     * Operation to initial E2EI certificate enrollment
     *
     * @return [Either] [CoreFailure] or [E2EIEnrollmentResult]
     */
    override suspend fun initialEnrollment(): Either<CoreFailure, E2EIEnrollmentResult> {
        kaliumLogger.i("start E2EI Enrollment Initialization")

        val acmeDirectories = e2EIRepository.loadACMEDirectories().getOrFail {
            return E2EIEnrollmentResult.Failed(E2EIEnrollmentResult.E2EIStep.AcmeDirectories, it).toEitherLeft()
        }

        var prevNonce = e2EIRepository.getACMENonce(acmeDirectories.newNonce).getOrFail {
            return E2EIEnrollmentResult.Failed(E2EIEnrollmentResult.E2EIStep.AcmeNonce, it).toEitherLeft()
        }

        prevNonce = e2EIRepository.createNewAccount(prevNonce, acmeDirectories.newAccount).getOrFail {
            return E2EIEnrollmentResult.Failed(E2EIEnrollmentResult.E2EIStep.AcmeNewAccount, it).toEitherLeft()
        }

        val newOrderResponse = e2EIRepository.createNewOrder(prevNonce, acmeDirectories.newOrder).getOrFail {
            return E2EIEnrollmentResult.Failed(E2EIEnrollmentResult.E2EIStep.AcmeNewOrder, it).toEitherLeft()
        }

        prevNonce = newOrderResponse.second

        val authzResponse = e2EIRepository.createAuthz(prevNonce, newOrderResponse.first.authorizations[0]).getOrFail {
            return E2EIEnrollmentResult.Failed(E2EIEnrollmentResult.E2EIStep.AcmeNewAuthz, it).toEitherLeft()
        }
        kaliumLogger.i("getoAuth")

        val oAuthState = e2EIRepository.getOAuthRefreshToken().getOrNull()
        kaliumLogger.i("oAuthStAte: $oAuthState")

        val initializationResult = E2EIEnrollmentResult.Initialized(
            target = authzResponse.first.wireOidcChallenge!!.target,
            oAuthState = oAuthState,
            authz = authzResponse.first,
            lastNonce = authzResponse.second,
            orderLocation = newOrderResponse.third
        )

        kaliumLogger.i("E2EI Enrollment Initialization Result: $initializationResult")

        return Either.Right(initializationResult)
    }

    /**
     * Operation to finalize E2EI certificate enrollment
     *
     * @param idToken id token generated by the IdP
     * @param initializationResult e2ei initialization result
     *
     * @return [Either] [CoreFailure] or [E2EIEnrollmentResult]
     */
    override suspend fun finalizeEnrollment(
        idToken: String,
        oAuthState: String,
        initializationResult: E2EIEnrollmentResult.Initialized
    ): Either<E2EIFailure, E2EIEnrollmentResult> {

        var prevNonce = initializationResult.lastNonce
        val authz = initializationResult.authz
        val orderLocation = initializationResult.orderLocation

        val wireNonce = e2EIRepository.getWireNonce().getOrFail {
            return E2EIEnrollmentResult.Failed(E2EIEnrollmentResult.E2EIStep.WireNonce, it).toEitherLeft()
        }

        val dpopToken = e2EIRepository.getDPoPToken(wireNonce).getOrFail {
            return E2EIEnrollmentResult.Failed(E2EIEnrollmentResult.E2EIStep.DPoPToken, it).toEitherLeft()
        }

        val wireAccessToken = e2EIRepository.getWireAccessToken(dpopToken).getOrFail {
            return E2EIEnrollmentResult.Failed(E2EIEnrollmentResult.E2EIStep.WireAccessToken, it).toEitherLeft()
        }

        val dpopChallengeResponse = e2EIRepository.validateDPoPChallenge(
            wireAccessToken.token, prevNonce, authz.wireDpopChallenge!!
        ).getOrFail {
            return E2EIEnrollmentResult.Failed(E2EIEnrollmentResult.E2EIStep.DPoPChallenge, it).toEitherLeft()
        }

        prevNonce = dpopChallengeResponse.nonce

        val oidcChallengeResponse = e2EIRepository.validateOIDCChallenge(
            idToken, oAuthState, prevNonce, authz.wireOidcChallenge!!
        ).getOrFail {
            return E2EIEnrollmentResult.Failed(E2EIEnrollmentResult.E2EIStep.OIDCChallenge, it).toEitherLeft()
        }

        prevNonce = oidcChallengeResponse.nonce

        val orderResponse = e2EIRepository.checkOrderRequest(orderLocation, prevNonce).getOrFail {
            return E2EIEnrollmentResult.Failed(E2EIEnrollmentResult.E2EIStep.CheckOrderRequest, it).toEitherLeft()
        }

        prevNonce = orderResponse.first.nonce

        val finalizeResponse = e2EIRepository.finalize(orderResponse.second, prevNonce).getOrFail {
            return E2EIEnrollmentResult.Failed(E2EIEnrollmentResult.E2EIStep.FinalizeRequest, it).toEitherLeft()
        }

        prevNonce = finalizeResponse.first.nonce

        val certificateRequest = e2EIRepository.certificateRequest(finalizeResponse.second, prevNonce).getOrFail {
            return E2EIEnrollmentResult.Failed(E2EIEnrollmentResult.E2EIStep.Certificate, it).toEitherLeft()
        }

        e2EIRepository.rotateKeysAndMigrateConversations(certificateRequest.response.decodeToString()).onFailure {
            return E2EIEnrollmentResult.Failed(E2EIEnrollmentResult.E2EIStep.ConversationMigration, it).toEitherLeft()
        }

        e2EIRepository.nukeE2EIClient()

        return Either.Right(E2EIEnrollmentResult.Finalized(certificateRequest.response.decodeToString()))
    }

}

sealed interface E2EIEnrollmentResult {
    enum class E2EIStep {
        AcmeNonce,
        AcmeDirectories,
        AcmeNewAccount,
        AcmeNewOrder,
        AcmeNewAuthz,
        OAuth,
        WireNonce,
        DPoPToken,
        WireAccessToken,
        DPoPChallenge,
        OIDCChallenge,
        CheckOrderRequest,
        FinalizeRequest,
        ConversationMigration,
        Certificate
    }

    class Initialized(
        val target: String,
        val oAuthState: String?,
        val authz: NewAcmeAuthz,
        val lastNonce: String,
        val orderLocation: String
    ) : E2EIEnrollmentResult

    class Finalized(val certificate: String) : E2EIEnrollmentResult

    data class Failed(val step: E2EIStep, val failure: CoreFailure) : E2EIEnrollmentResult {
        override fun toString(): String {
            return "E2EI enrollment failed at $step: with $failure"
        }

        fun toEitherLeft() = Either.Left(E2EIFailure.Generic(Exception(this.toString())))
    }
}
