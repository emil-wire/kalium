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

import com.waz.model.Messages.Asset.Original
import java.util.UUID

class VideoPreviewMessage(
    val width: Int,
    val height: Int,
    val duration: Long,
    eventId: UUID,
    messageId: UUID,
    conversationId: UUID,
    clientId: String,
    userId: UUID,
    time: String,
    mimeType: String,
    size: Long,
    name: String
) : OriginMessage(
    mimeType = mimeType,
    name = name,
    size = size,
    eventId = eventId,
    msgId = messageId,
    conversationId = conversationId,
    clientId = clientId,
    userId = userId,
    time = time
) {

    constructor(msg: MessageBase, original: Original) : this(
        width = original.video.width,
        height = original.video.height,
        duration = original.video.durationInMillis,
        eventId = msg.eventId,
        messageId = msg.messageId,
        conversationId = msg.conversationId,
        clientId = msg.clientId,
        userId = msg.userId,
        time = msg.time,
        // TODO: check if this need to change
        mimeType = original.mimeType,
        size = original.size,
        name = original.name
    )
}