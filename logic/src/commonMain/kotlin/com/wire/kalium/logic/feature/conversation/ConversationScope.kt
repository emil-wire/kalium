package com.wire.kalium.logic.feature.conversation

import com.wire.kalium.logic.cache.SelfConversationIdProvider
import com.wire.kalium.logic.data.asset.AssetRepository
import com.wire.kalium.logic.data.connection.ConnectionRepository
import com.wire.kalium.logic.data.conversation.ConversationGroupRepository
import com.wire.kalium.logic.data.conversation.ConversationRepository
import com.wire.kalium.logic.data.conversation.MLSConversationRepository
import com.wire.kalium.logic.data.conversation.UpdateKeyingMaterialThresholdProvider
import com.wire.kalium.logic.data.message.PersistMessageUseCase
import com.wire.kalium.logic.data.team.TeamRepository
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.UserRepository
import com.wire.kalium.logic.feature.CurrentClientIdProvider
import com.wire.kalium.logic.feature.SelfTeamIdProvider
import com.wire.kalium.logic.feature.connection.MarkConnectionRequestAsNotifiedUseCase
import com.wire.kalium.logic.feature.connection.MarkConnectionRequestAsNotifiedUseCaseImpl
import com.wire.kalium.logic.feature.connection.ObserveConnectionListUseCase
import com.wire.kalium.logic.feature.connection.ObserveConnectionListUseCaseImpl
import com.wire.kalium.logic.feature.conversation.keyingmaterials.UpdateKeyingMaterialsUseCase
import com.wire.kalium.logic.feature.conversation.keyingmaterials.UpdateKeyingMaterialsUseCaseImpl
import com.wire.kalium.logic.feature.message.MessageSender
import com.wire.kalium.logic.feature.team.DeleteTeamConversationUseCase
import com.wire.kalium.logic.feature.team.DeleteTeamConversationUseCaseImpl
import com.wire.kalium.logic.feature.team.GetSelfTeamUseCase
import com.wire.kalium.logic.feature.team.GetSelfTeamUseCaseImpl
import com.wire.kalium.logic.sync.SyncManager

@Suppress("LongParameterList")
class ConversationScope internal constructor(
    private val conversationRepository: ConversationRepository,
    private val conversationGroupRepository: ConversationGroupRepository,
    private val connectionRepository: ConnectionRepository,
    private val userRepository: UserRepository,
    private val syncManager: SyncManager,
    private val mlsConversationRepository: MLSConversationRepository,
    private val currentClientIdProvider: CurrentClientIdProvider,
    private val assetRepository: AssetRepository,
    private val messageSender: MessageSender,
    private val teamRepository: TeamRepository,
    private val selfUserId: UserId,
    private val selfConversationIdProvider: SelfConversationIdProvider,
    private val persistMessage: PersistMessageUseCase,
    private val updateKeyingMaterialThresholdProvider: UpdateKeyingMaterialThresholdProvider,
    private val selfTeamIdProvider: SelfTeamIdProvider
) {

    val getSelfTeamUseCase: GetSelfTeamUseCase
        get() = GetSelfTeamUseCaseImpl(
            userRepository = userRepository,
            teamRepository = teamRepository,
        )

    val getConversations: GetConversationsUseCase
        get() = GetConversationsUseCase(conversationRepository)

    val getConversationDetails: GetConversationUseCase
        get() = GetConversationUseCase(conversationRepository)

    val getOneToOneConversation: GetOneToOneConversationUseCase
        get() = GetOneToOneConversationUseCase(conversationRepository)

    val observeConversationListDetails: ObserveConversationListDetailsUseCase
        get() = ObserveConversationListDetailsUseCaseImpl(conversationRepository)

    val observeConversationMembers: ObserveConversationMembersUseCase
        get() = ObserveConversationMembersUseCaseImpl(conversationRepository, userRepository)

    val getMembersToMention: MembersToMentionUseCase
        get() = MembersToMentionUseCase(observeConversationMembers, userRepository)

    val observeUserListById: ObserveUserListByIdUseCase
        get() = ObserveUserListByIdUseCase(userRepository)
    val persistMigratedConversation: PersistMigratedConversationUseCase
        get() = PersistMigratedConversationUseCaseImpl(conversationRepository)

    val observeConversationDetails: ObserveConversationDetailsUseCase
        get() = ObserveConversationDetailsUseCase(conversationRepository)

    val observeIsSelfUserMemberUseCase: ObserveIsSelfUserMemberUseCase
        get() = ObserveIsSelfUserMemberUseCaseImpl(conversationRepository, selfUserId)

    val observeConversationInteractionAvailabilityUseCase: ObserveConversationInteractionAvailabilityUseCase
        get() = ObserveConversationInteractionAvailabilityUseCase(conversationRepository)

    val deleteTeamConversation: DeleteTeamConversationUseCase
        get() = DeleteTeamConversationUseCaseImpl(selfTeamIdProvider, teamRepository, conversationRepository)

    val createGroupConversation: CreateGroupConversationUseCase
        get() = CreateGroupConversationUseCase(conversationRepository, conversationGroupRepository, syncManager, currentClientIdProvider)

    val addMemberToConversationUseCase: AddMemberToConversationUseCase
        get() = AddMemberToConversationUseCaseImpl(conversationGroupRepository)

    val getOrCreateOneToOneConversationUseCase: GetOrCreateOneToOneConversationUseCase
        get() = GetOrCreateOneToOneConversationUseCase(conversationRepository, conversationGroupRepository)

    val updateConversationMutedStatus: UpdateConversationMutedStatusUseCase
        get() = UpdateConversationMutedStatusUseCaseImpl(conversationRepository)

    val observeConnectionList: ObserveConnectionListUseCase
        get() = ObserveConnectionListUseCaseImpl(connectionRepository)

    val markConnectionRequestAsNotified: MarkConnectionRequestAsNotifiedUseCase
        get() = MarkConnectionRequestAsNotifiedUseCaseImpl(connectionRepository)

    val updateConversationReadDateUseCase: UpdateConversationReadDateUseCase
        get() = UpdateConversationReadDateUseCase(
            conversationRepository,
            messageSender,
            currentClientIdProvider,
            selfUserId,
            selfConversationIdProvider,
        )

    val updateConversationAccess: UpdateConversationAccessRoleUseCase
        get() = UpdateConversationAccessRoleUseCase(conversationRepository)

    val updateConversationMemberRole: UpdateConversationMemberRoleUseCase
        get() = UpdateConversationMemberRoleUseCaseImpl(conversationRepository)

    val removeMemberFromConversation: RemoveMemberFromConversationUseCase
        get() = RemoveMemberFromConversationUseCaseImpl(conversationGroupRepository)

    val leaveConversation: LeaveConversationUseCase
        get() = LeaveConversationUseCaseImpl(conversationGroupRepository, selfUserId)

    val renameConversation: RenameConversationUseCase
        get() = RenameConversationUseCaseImpl(conversationRepository, persistMessage, selfUserId)

    val updateMLSGroupsKeyingMaterials: UpdateKeyingMaterialsUseCase
        get() = UpdateKeyingMaterialsUseCaseImpl(mlsConversationRepository, updateKeyingMaterialThresholdProvider)

    val clearConversationContent: ClearConversationContentUseCase
        get() = ClearConversationContentUseCaseImpl(
            clearConversationContent = ClearConversationContentImpl(conversationRepository, assetRepository),
            currentClientIdProvider,
            messageSender,
            selfUserId,
            selfConversationIdProvider
        )
}
