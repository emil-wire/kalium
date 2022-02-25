package com.wire.kalium.logic.data.message

import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.IdMapper
import com.wire.kalium.logic.data.id.PersistenceQualifiedId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.network.api.message.MessageApi
import com.wire.kalium.persistence.dao.message.MessageDAO
import com.wire.kalium.persistence.dao.message.MessageEntity
import io.mockative.Mock
import io.mockative.anything
import io.mockative.configure
import io.mockative.eq
import io.mockative.given
import io.mockative.mock
import io.mockative.once
import io.mockative.verify
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageRepositoryTest {

    @Mock
    val idMapper = mock(IdMapper::class)

    @Mock
    val messageApi = mock(MessageApi::class)

    @Mock
    val messageDAO = configure(mock(MessageDAO::class)) { stubsUnitByDefault = true }

    @Mock
    val sendMessageFailureMapper = mock(SendMessageFailureMapper::class)

    @Mock
    val messageMapper = mock(MessageMapper::class)

    private lateinit var messageRepository: MessageRepository

    @BeforeTest
    fun setup() {
        messageRepository = MessageDataSource(messageApi, messageDAO, messageMapper, idMapper, sendMessageFailureMapper)
    }

    @Test
    fun givenAnConversationId_whenGettingMessagesOfConversation_thenShouldUseIdMapperToMapTheConversationId() = runTest {
        val mappedId = TEST_QUALIFIED_ID_ENTITY
        given(idMapper)
            .function(idMapper::toDaoModel)
            .whenInvokedWith(anything())
            .then { mappedId }

        given(messageDAO)
            .suspendFunction(messageDAO::getMessageByConversation)
            .whenInvokedWith(anything(), anything())
            .then { _, _ -> flowOf(listOf()) }

        given(messageMapper)
            .function(messageMapper::fromEntityToMessage)
            .whenInvokedWith(anything())
            .then { TEST_MESSAGE }

        messageRepository.getMessagesForConversation(TEST_CONVERSATION_ID, 0).collect()

        verify(messageDAO)
            .suspendFunction(messageDAO::getMessageByConversation)
            .with(eq(mappedId), anything())
            .wasInvoked(exactly = once)
    }

    @Test
    fun givenAnMessageEntityAndMapper_whenGettingMessagesOfConversation_thenTheMapperShouldBeUsed() = runTest {
        val entity = TEST_MESSAGE_ENTITY
        val mappedMessage = TEST_MESSAGE
        given(messageMapper)
            .function(messageMapper::fromEntityToMessage)
            .whenInvokedWith(anything())
            .then { mappedMessage }

        given(messageDAO)
            .suspendFunction(messageDAO::getMessageByConversation)
            .whenInvokedWith(anything(), anything())
            .then { _, _ -> flowOf(listOf(entity)) }

        given(idMapper)
            .function(idMapper::toDaoModel)
            .whenInvokedWith(anything())
            .then { TEST_QUALIFIED_ID_ENTITY }

        val messageList = messageRepository.getMessagesForConversation(TEST_CONVERSATION_ID, 0)
            .first()
        assertEquals(listOf(mappedMessage), messageList)

        verify(messageMapper)
            .function(messageMapper::fromEntityToMessage)
            .with(eq(entity))
            .wasInvoked(exactly = once)
    }

    @Test
    fun givenAnMessage_whenPersisting_thenTheDAOShouldBeUsedWithMappedValues() = runTest {
        val message = TEST_MESSAGE
        val mappedEntity = TEST_MESSAGE_ENTITY
        given(messageMapper)
            .function(messageMapper::fromMessageToEntity)
            .whenInvokedWith(anything())
            .then { mappedEntity }

        given(idMapper)
            .function(idMapper::toDaoModel)
            .whenInvokedWith(anything())
            .then { TEST_QUALIFIED_ID_ENTITY }

        messageRepository.persistMessage(message)

        verify(messageMapper)
            .function(messageMapper::fromMessageToEntity)
            .with(eq(message))
            .wasInvoked(exactly = once)

        verify(messageDAO)
            .suspendFunction(messageDAO::insertMessage)
            .with(eq(mappedEntity))
            .wasInvoked(exactly = once)
    }

    private companion object {
        val TEST_QUALIFIED_ID_ENTITY = PersistenceQualifiedId("value", "domain")
        val TEST_MESSAGE_ENTITY =
            MessageEntity(
                "uid", "content", TEST_QUALIFIED_ID_ENTITY, "date",
                TEST_QUALIFIED_ID_ENTITY, "sender", MessageEntity.Status.SENT
            )
        val TEST_CONVERSATION_ID = ConversationId("value", "domain")
        val TEST_CLIENT_ID = ClientId("clientId")
        val TEST_USER_ID = UserId("userId", "domain")
        val TEST_CONTENT = MessageContent.Text("Ciao!")
        val TEST_MESSAGE = Message(
            "uid", TEST_CONTENT, TEST_CONVERSATION_ID, "date", TEST_USER_ID, TEST_CLIENT_ID,
            Message.Status.SENT
        )
    }
}