package com.wire.kalium.logic.feature.message

import com.wire.kalium.cryptography.ProteusClient
import com.wire.kalium.logic.data.client.ClientRepository
import com.wire.kalium.logic.data.conversation.ConversationRepository
import com.wire.kalium.logic.data.message.MessageRepository
import com.wire.kalium.logic.data.message.ProtoContentMapper
import com.wire.kalium.logic.data.message.provideProtoContentMapper
import com.wire.kalium.logic.data.prekey.PreKeyRepository
import com.wire.kalium.logic.data.user.UserRepository
import com.wire.kalium.logic.sync.SyncManager

class MessageScope(
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository,
    private val clientRepository: ClientRepository,
    private val proteusClient: ProteusClient,
    private val preKeyRepository: PreKeyRepository,
    private val userRepository: UserRepository,
    private val syncManager: SyncManager
) {

    private val messageSendFailureHandler: MessageSendFailureHandler
        get() = MessageSendFailureHandler(userRepository, clientRepository)

    private val sessionEstablisher: SessionEstablisher
        get() = SessionEstablisherImpl(proteusClient, preKeyRepository)

    private val protoContentMapper: ProtoContentMapper
        get() = provideProtoContentMapper()

    private val messageEnvelopeCreator: MessageEnvelopeCreator
        get() = MessageEnvelopeCreatorImpl(proteusClient, protoContentMapper)

    private val messageSender: MessageSender
        get() = MessageSenderImpl(
            messageRepository, conversationRepository, syncManager, messageSendFailureHandler, sessionEstablisher, messageEnvelopeCreator
        )

    val sendTextMessage: SendTextMessageUseCase
        get() = SendTextMessageUseCase(
            messageRepository,
            userRepository,
            clientRepository,
            syncManager,
            messageSender
        )
    val getRecentMessages: GetRecentMessagesUseCase get() = GetRecentMessagesUseCase(messageRepository)
}
