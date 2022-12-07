package com.wire.kalium.logic.feature.call

import com.sun.jna.Pointer
import com.wire.kalium.calling.Calling
import com.wire.kalium.calling.callbacks.LogHandler
import com.wire.kalium.logic.callingLogger
import com.wire.kalium.logic.data.call.CallRepository
import com.wire.kalium.logic.data.call.VideoStateChecker
import com.wire.kalium.logic.data.call.mapper.CallMapper
import com.wire.kalium.logic.data.conversation.ConversationRepository
import com.wire.kalium.logic.data.id.FederatedIdMapper
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.UserRepository
import com.wire.kalium.logic.feature.CurrentClientIdProvider
import com.wire.kalium.logic.feature.message.MessageSender
import io.ktor.util.collections.ConcurrentMap

actual class GlobalCallManager {

    private val callManagerHolder: ConcurrentMap<QualifiedID, CallManager> by lazy {
        ConcurrentMap()
    }

    private val calling by lazy {
        Calling.INSTANCE.apply {
            wcall_setup()
            wcall_run()
            wcall_set_log_handler(
                logHandler = LogHandlerImpl,
                arg = null
            )
            callingLogger.i("GlobalCallManager -> wcall_init")
        }
    }

    /**
     * Get a [CallManager] for a session, shouldn't be instantiated more than one CallManager for a single session.
     */
    @Suppress("LongParameterList")
    internal actual fun getCallManagerForClient(
        userId: QualifiedID,
        callRepository: CallRepository,
        userRepository: UserRepository,
        currentClientIdProvider: CurrentClientIdProvider,
        conversationRepository: ConversationRepository,
        messageSender: MessageSender,
        callMapper: CallMapper,
        federatedIdMapper: FederatedIdMapper,
        qualifiedIdMapper: QualifiedIdMapper,
        videoStateChecker: VideoStateChecker
    ): CallManager {
        return callManagerHolder[userId] ?: CallManagerImpl(
            calling = calling,
            callRepository = callRepository,
            userRepository = userRepository,
            currentClientIdProvider = currentClientIdProvider,
            callMapper = callMapper,
            messageSender = messageSender,
            conversationRepository = conversationRepository,
            federatedIdMapper = federatedIdMapper,
            qualifiedIdMapper = qualifiedIdMapper,
            videoStateChecker = videoStateChecker
        ).also {
            callManagerHolder[userId] = it
        }
    }

    actual fun removeInMemoryCallingManagerForUser(userId: UserId) {
        callManagerHolder.remove(userId)
    }

    actual fun getFlowManager(): FlowManagerService = FlowManagerServiceImpl()
    actual fun getMediaManager(): MediaManagerService = MediaManagerServiceImpl()
}

object LogHandlerImpl : LogHandler {
    override fun onLog(level: Int, message: String, arg: Pointer?) {
        when (level) {
            0 -> callingLogger.d(message)
            1 -> callingLogger.i(message)
            2 -> callingLogger.w(message)
            3 -> callingLogger.e(message)
        }
    }
}
