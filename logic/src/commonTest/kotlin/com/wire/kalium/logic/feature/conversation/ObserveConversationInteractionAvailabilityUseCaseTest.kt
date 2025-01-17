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

package com.wire.kalium.logic.feature.conversation

import app.cash.turbine.test
import com.wire.kalium.logic.StorageFailure
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.SupportedProtocol
import com.wire.kalium.logic.framework.TestConversation
import com.wire.kalium.logic.framework.TestConversationDetails
import com.wire.kalium.logic.framework.TestUser
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.util.arrangement.repository.ConversationRepositoryArrangement
import com.wire.kalium.logic.util.arrangement.repository.ConversationRepositoryArrangementImpl
import com.wire.kalium.logic.util.arrangement.repository.UserRepositoryArrangement
import com.wire.kalium.logic.util.arrangement.repository.UserRepositoryArrangementImpl
import io.mockative.eq
import io.mockative.once
import io.mockative.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ObserveConversationInteractionAvailabilityUseCaseTest {

    @Test
    fun givenUserIsAGroupMember_whenInvokingInteractionForConversation_thenInteractionShouldBeEnabled() = runTest {
        val conversationId = TestConversation.ID

        val (arrangement, observeConversationInteractionAvailability) = arrange {
            withSelfUserBeingMemberOfConversation(isMember = true)
        }

        observeConversationInteractionAvailability(conversationId).test {
            val interactionResult = awaitItem()
            assertEquals(IsInteractionAvailableResult.Success(InteractionAvailability.ENABLED), interactionResult)

            verify(arrangement.conversationRepository)
                .suspendFunction(arrangement.conversationRepository::observeConversationDetailsById)
                .with(eq(conversationId))
                .wasInvoked(exactly = once)

            awaitComplete()
        }

    }

    @Test
    fun givenUserIsNoLongerAGroupMember_whenInvokingInteractionForConversation_thenInteractionShouldBeEnabled() = runTest {
        val conversationId = TestConversation.ID

        val (arrangement, observeConversationInteractionAvailability) = arrange {
            withSelfUserBeingMemberOfConversation(isMember = false)
        }

        observeConversationInteractionAvailability(conversationId).test {
            val interactionResult = awaitItem()
            assertEquals(IsInteractionAvailableResult.Success(InteractionAvailability.NOT_MEMBER), interactionResult)

            verify(arrangement.conversationRepository)
                .suspendFunction(arrangement.conversationRepository::observeConversationDetailsById)
                .with(eq(conversationId))
                .wasInvoked(exactly = once)

            awaitComplete()
        }

    }

    @Test
    fun givenGroupDetailsReturnsError_whenInvokingInteractionForConversation_thenInteractionShouldReturnFailure() = runTest {
        val conversationId = TestConversation.ID

        val (arrangement, observeConversationInteractionAvailability) = arrange {
            withGroupConversationError()
        }

        observeConversationInteractionAvailability(conversationId).test {
            val interactionResult = awaitItem()
            assertIs<IsInteractionAvailableResult.Failure>(interactionResult)

            verify(arrangement.conversationRepository)
                .suspendFunction(arrangement.conversationRepository::observeConversationDetailsById)
                .with(eq(conversationId))
                .wasInvoked(exactly = once)

            awaitComplete()
        }

    }

    @Test
    fun givenOtherUserIsBlocked_whenInvokingInteractionForConversation_thenInteractionShouldBeDisabled() = runTest {
        val conversationId = TestConversation.ID

        val (arrangement, observeConversationInteractionAvailability) = arrange {
            withBlockedUserConversation()
        }

        observeConversationInteractionAvailability(conversationId).test {
            val interactionResult = awaitItem()
            assertEquals(IsInteractionAvailableResult.Success(InteractionAvailability.BLOCKED_USER), interactionResult)

            verify(arrangement.conversationRepository)
                .suspendFunction(arrangement.conversationRepository::observeConversationDetailsById)
                .with(eq(conversationId))
                .wasInvoked(exactly = once)

            awaitComplete()
        }

    }

    @Test
    fun givenOtherUserIsDeleted_whenInvokingInteractionForConversation_thenInteractionShouldBeDisabled() = runTest {
        val conversationId = TestConversation.ID

        val (arrangement, observeConversationInteractionAvailability) = arrange {
            withDeletedUserConversation()
        }

        observeConversationInteractionAvailability(conversationId).test {
            val interactionResult = awaitItem()
            assertEquals(IsInteractionAvailableResult.Success(InteractionAvailability.DELETED_USER), interactionResult)

            verify(arrangement.conversationRepository)
                .suspendFunction(arrangement.conversationRepository::observeConversationDetailsById)
                .with(eq(conversationId))
                .wasInvoked(exactly = once)

            awaitComplete()
        }
    }

    @Test
    fun givenProteusConversationAndUserSupportsOnlyMLS_whenObserving_thenShouldReturnUnsupportedProtocol() = runTest {
        testProtocolSupport(
            conversationProtocolInfo = Conversation.ProtocolInfo.Proteus,
            userSupportedProtocols = setOf(SupportedProtocol.MLS),
            expectedResult = InteractionAvailability.UNSUPPORTED_PROTOCOL
        )
    }

    @Test
    fun givenMLSConversationAndUserSupportsOnlyMLS_whenObserving_thenShouldReturnUnsupportedProtocol() = runTest {
        testProtocolSupport(
            conversationProtocolInfo = TestConversation.MLS_PROTOCOL_INFO,
            userSupportedProtocols = setOf(SupportedProtocol.PROTEUS),
            expectedResult = InteractionAvailability.UNSUPPORTED_PROTOCOL
        )
    }

    @Test
    fun givenMixedConversationAndUserSupportsOnlyMLS_whenObserving_thenShouldReturnUnsupportedProtocol() = runTest {
        testProtocolSupport(
            conversationProtocolInfo = TestConversation.MIXED_PROTOCOL_INFO,
            userSupportedProtocols = setOf(SupportedProtocol.PROTEUS),
            expectedResult = InteractionAvailability.ENABLED
        )
    }

    @Test
    fun givenMixedConversationAndUserSupportsProteus_whenObserving_thenShouldReturnEnabled() = runTest {
        testProtocolSupport(
            conversationProtocolInfo = TestConversation.MIXED_PROTOCOL_INFO,
            userSupportedProtocols = setOf(SupportedProtocol.PROTEUS),
            expectedResult = InteractionAvailability.ENABLED
        )
    }

    @Test
    fun givenMLSConversationAndUserSupportsMLS_whenObserving_thenShouldReturnEnabled() = runTest {
        testProtocolSupport(
            conversationProtocolInfo = TestConversation.MLS_PROTOCOL_INFO,
            userSupportedProtocols = setOf(SupportedProtocol.MLS),
            expectedResult = InteractionAvailability.ENABLED
        )
    }

    @Test
    fun givenProteusConversationAndUserSupportsProteus_whenObserving_thenShouldReturnEnabled() = runTest {
        testProtocolSupport(
            conversationProtocolInfo = TestConversation.PROTEUS_PROTOCOL_INFO,
            userSupportedProtocols = setOf(SupportedProtocol.PROTEUS),
            expectedResult = InteractionAvailability.ENABLED
        )
    }

    private suspend fun testProtocolSupport(
        conversationProtocolInfo: Conversation.ProtocolInfo,
        userSupportedProtocols: Set<SupportedProtocol>,
        expectedResult: InteractionAvailability
    ) {
        val convId = TestConversationDetails.CONVERSATION_GROUP.conversation.id
        val (_, observeConversationInteractionAvailabilityUseCase) = arrange {
            val proteusGroupDetails = TestConversationDetails.CONVERSATION_GROUP.copy(
                conversation = TestConversationDetails.CONVERSATION_GROUP.conversation.copy(
                    protocol = conversationProtocolInfo
                )
            )
            withObserveConversationDetailsByIdReturning(Either.Right(proteusGroupDetails))
            withObservingSelfUserReturning(flowOf(TestUser.SELF.copy(supportedProtocols = userSupportedProtocols)))
        }

        observeConversationInteractionAvailabilityUseCase(convId).test {
            val result = awaitItem()
            assertIs<IsInteractionAvailableResult.Success>(result)
            assertEquals(expectedResult, result.interactionAvailability)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private class Arrangement(
        private val configure: Arrangement.() -> Unit
    ) : UserRepositoryArrangement by UserRepositoryArrangementImpl(),
        ConversationRepositoryArrangement by ConversationRepositoryArrangementImpl() {

        init {
            withObservingSelfUserReturning(
                flowOf(
                    TestUser.SELF.copy(supportedProtocols = setOf(SupportedProtocol.MLS, SupportedProtocol.PROTEUS))
                )
            )
        }

        fun withSelfUserBeingMemberOfConversation(isMember: Boolean) = apply {
            withObserveConversationDetailsByIdReturning(
                Either.Right(TestConversationDetails.CONVERSATION_GROUP.copy(isSelfUserMember = isMember))
            )
        }

        fun withGroupConversationError() {
            withObserveConversationDetailsByIdReturning(Either.Left(StorageFailure.DataNotFound))
        }

        fun withBlockedUserConversation() = apply {
            withObserveConversationDetailsByIdReturning(
                Either.Right(
                    TestConversationDetails.CONVERSATION_ONE_ONE.copy(
                        otherUser = TestUser.OTHER.copy(
                            connectionStatus = ConnectionState.BLOCKED
                        )
                    )
                )
            )
        }

        fun withDeletedUserConversation() = apply {
            withObserveConversationDetailsByIdReturning(
                Either.Right(
                    TestConversationDetails.CONVERSATION_ONE_ONE.copy(
                        otherUser = TestUser.OTHER.copy(
                            deleted = true
                        )
                    )
                )
            )
        }

        fun arrange(): Pair<Arrangement, ObserveConversationInteractionAvailabilityUseCase> = run {
            configure()
            this@Arrangement to ObserveConversationInteractionAvailabilityUseCase(
                conversationRepository = conversationRepository,
                userRepository = userRepository
            )
        }
    }

    private companion object {
        fun arrange(configure: Arrangement.() -> Unit) = Arrangement(configure).arrange()
    }
}
