package com.wire.kalium.logic.feature.conversation

import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.UserRepository
import com.wire.kalium.logic.data.user.UserTypeMapper
import com.wire.kalium.logic.sync.SyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class ObserveMemberDetailsByIdsUseCase(
    private val userRepository: UserRepository,
    private val syncManager: SyncManager,
    private val userTypeMapper: UserTypeMapper,
) {

    suspend operator fun invoke(userIdList: List<UserId>): Flow<List<MemberDetails>> {
        syncManager.startSyncIfIdle()
        val selfDetailsFlow = userRepository.getSelfUser()
        val selfUser = selfDetailsFlow.first()

        return flowOf(userIdList).map { members ->
            members.map {
                if (it == selfUser.id) {
                    selfDetailsFlow.map(MemberDetails::Self)
                } else {
                    userRepository.getKnownUser(it).filterNotNull().map { otherUser ->
                        MemberDetails.Other(
                            otherUser = otherUser,
                            userType = userTypeMapper.fromOtherUserAndSelfUser(otherUser, selfUser)
                        )
                    }
                }
            }
        }.flatMapLatest { detailsFlows ->
            combine(detailsFlows) { it.toList() }
        }
    }
}