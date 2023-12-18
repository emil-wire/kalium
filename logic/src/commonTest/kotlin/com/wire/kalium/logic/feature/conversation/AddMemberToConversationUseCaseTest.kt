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

package com.wire.kalium.logic.feature.conversation

import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.StorageFailure
import com.wire.kalium.logic.data.conversation.ConversationGroupRepository
import com.wire.kalium.logic.framework.TestConversation
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.util.arrangement.repository.UserRepositoryArrangement
import com.wire.kalium.logic.util.arrangement.repository.UserRepositoryArrangementImpl
import com.wire.kalium.network.api.base.model.ErrorResponse
import com.wire.kalium.network.exceptions.KaliumException
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
import kotlin.test.assertIs

class AddMemberToConversationUseCaseTest {

    @Test
    fun givenMemberAndConversation_WhenAddMemberIsSuccessful_ThenReturnSuccess() = runTest {
        val (arrangement, addMemberUseCase) = Arrangement()
            .withAddMembers(Either.Right(Unit))
            .arrange()

        val result = addMemberUseCase(TestConversation.ID, listOf(TestConversation.USER_1))

        assertIs<AddMemberToConversationUseCase.Result.Success>(result)

        verify(arrangement.conversationGroupRepository)
            .suspendFunction(arrangement.conversationGroupRepository::addMembers)
            .with(eq(listOf(TestConversation.USER_1)), eq(TestConversation.ID))
            .wasInvoked(exactly = once)
    }

    @Test
    fun givenMemberAndConversation_WhenAddMemberFailed_ThenReturnFailure() = runTest {
        val (arrangement, addMemberUseCase) = Arrangement()
            .withAddMembers(Either.Left(StorageFailure.DataNotFound))
            .arrange()

        val result = addMemberUseCase(TestConversation.ID, listOf(TestConversation.USER_1))
        assertIs<AddMemberToConversationUseCase.Result.Failure>(result)

        verify(arrangement.conversationGroupRepository)
            .suspendFunction(arrangement.conversationGroupRepository::addMembers)
            .with(eq(listOf(TestConversation.USER_1)), eq(TestConversation.ID))
            .wasInvoked(exactly = once)
    }

    @Test
    fun givenServerResponseWith403_whenAddingToGroupConversionFail_thenUpdateUsersInfo() = runTest {

        val error = NetworkFailure.ServerMiscommunication(
            KaliumException.InvalidRequestError(
                errorResponse = ErrorResponse(
                    code = 403,
                    message = "Forbidden",
                    label = "Forbidden"
                )
            )
        )
        val (arrangement, addMemberUseCase) = Arrangement()
            .arrange {
                withAddMembers(Either.Left(error))
                withFetchUsersByIdReturning(Either.Right(Unit))
            }

        val result = addMemberUseCase(TestConversation.ID, listOf(TestConversation.USER_1))
        assertIs<AddMemberToConversationUseCase.Result.Failure>(result)

        verify(arrangement.conversationGroupRepository)
            .suspendFunction(arrangement.conversationGroupRepository::addMembers)
            .with(eq(listOf(TestConversation.USER_1)), eq(TestConversation.ID))
            .wasInvoked(exactly = once)

        verify(arrangement.userRepository)
            .suspendFunction(arrangement.userRepository::fetchUsersByIds)
            .with(eq(setOf(TestConversation.USER_1)))
            .wasInvoked(exactly = once)
    }

    private class Arrangement : UserRepositoryArrangement by UserRepositoryArrangementImpl() {
        @Mock
        val conversationGroupRepository = mock(classOf<ConversationGroupRepository>())

        private val addMemberUseCase = AddMemberToConversationUseCaseImpl(
            conversationGroupRepository,
            userRepository
        )

        fun withAddMembers(either: Either<CoreFailure, Unit>) = apply {
            given(conversationGroupRepository)
                .suspendFunction(conversationGroupRepository::addMembers)
                .whenInvokedWith(any(), any())
                .thenReturn(either)
        }

        fun arrange(block: Arrangement.() -> Unit = { }) = apply(block).let { this to addMemberUseCase }
    }
}
