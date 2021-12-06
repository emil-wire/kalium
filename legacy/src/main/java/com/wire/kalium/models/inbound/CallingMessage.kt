//
// Wire
// Copyright (C) 2016 Wire Swiss GmbH
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see http://www.gnu.org/licenses/.
//
package com.wire.kalium.models.inbound

import java.util.UUID

class CallingMessage(
    val content: String,
    eventId: UUID,
    messageId: UUID,
    convId: UUID,
    clientId: String,
    userId: UUID,
    time: String
) : MessageBase(eventId = eventId, messageId = messageId, conversationId = convId, clientId = clientId, userId = userId, time = time) {

    constructor(content: String, msgBase: MessageBase) :
            this(
                content = content,
                eventId = msgBase.eventId,
                messageId = msgBase.messageId,
                convId = msgBase.conversationId,
                clientId = msgBase.clientId,
                userId = msgBase.userId,
                time = msgBase.time
            )

}