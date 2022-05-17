package com.wire.kalium.logic.sync

import com.wire.kalium.logic.data.event.Event
import com.wire.kalium.logic.data.event.EventRepository
import com.wire.kalium.logic.functional.map
import com.wire.kalium.logic.functional.onFailure
import com.wire.kalium.logic.kaliumLogger

class SyncPendingEventsUseCase(
    private val syncManager: SyncManager,
    private val eventRepository: EventRepository,
    private val conversationEventReceiver: EventReceiver<Event.Conversation>
) {

    /**
     * Syncing only Pending Events, to find out what did we miss
     */
    suspend operator fun invoke() {
        syncManager.waitForSlowSyncToComplete()

        eventRepository.pendingEvents()
            .collect { either ->
                either.map { event ->
                    kaliumLogger.i(message = "Event received: $event")
                    when (event) {
                        is Event.Conversation -> conversationEventReceiver.onEvent(event)
                        else -> kaliumLogger.i(message = "Unhandled event id=${event.id}")
                    }
                    eventRepository.updateLastProcessedEventId(event.id)
                }.onFailure {
                    kaliumLogger.e(message = "Failure when receiving events: $it")
                }
            }
    }
}