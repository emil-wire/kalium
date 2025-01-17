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
package com.wire.kalium.monkeys.actions

import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.monkeys.conversation.Monkey
import com.wire.kalium.monkeys.conversation.MonkeyConversation
import com.wire.kalium.monkeys.model.ActionType
import com.wire.kalium.monkeys.model.Event
import com.wire.kalium.monkeys.model.EventType
import com.wire.kalium.monkeys.pool.ConversationPool
import com.wire.kalium.monkeys.pool.MonkeyPool

open class AddUserToConversationAction(val config: ActionType.AddUsersToConversation, sender: suspend (Event) -> Unit) : Action(sender) {
    override suspend fun execute(coreLogic: CoreLogic, monkeyPool: MonkeyPool) {
        val targets = pickConversations(monkeyPool)
        targets.forEach { (monkeyConversation, participants) ->
            monkeyConversation.addMonkeys(participants)
            this.sender(
                Event(
                    monkeyConversation.creator.internalId,
                    EventType.AddUsersToConversation(monkeyConversation.conversationId, participants.map { it.internalId })
                )
            )
        }
    }

    open suspend fun pickConversations(monkeyPool: MonkeyPool): List<Pair<MonkeyConversation, List<Monkey>>> {
        val targets = ConversationPool.randomDynamicConversations(this.config.countGroups.toInt())
        return targets.map { target ->
            val filterOut = target.membersIds()
            val participants = target.creator.randomPeers(this.config.userCount, monkeyPool, filterOut)
            Pair(target, participants)
        }
    }
}
