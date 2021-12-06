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
package com.wire.kalium.models.backend

import com.wire.kalium.models.outbound.otr.PreKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NewClient(
    @SerialName("lastkey") val lastKey: PreKey,
    @SerialName("prekeys") val preKeys: List<PreKey>,
    @SerialName("password") val password: String,
    @SerialName("class") val clazz: String,
    @SerialName("type") val type: String,
    @SerialName("label") val label: String,
    //val sigkeys: Sig
)

//data class Sig (
//    val enckey: String,
//    val mackey: String
//)