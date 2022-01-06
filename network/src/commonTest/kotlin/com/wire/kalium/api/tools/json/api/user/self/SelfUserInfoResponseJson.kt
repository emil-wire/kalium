package com.wire.kalium.api.tools.json.api.user.self

import com.wire.kalium.api.tools.json.ValidJsonProvider
import com.wire.kalium.network.api.UserId
import com.wire.kalium.network.api.model.UserAsset
import com.wire.kalium.network.api.model.AssetType
import com.wire.kalium.network.api.user.self.ManagedBy
import com.wire.kalium.network.api.user.self.SelfUserInfoResponse

object SelfUserInfoResponseJson {
    private val jsonProvider = { serializable: SelfUserInfoResponse ->
        """
        |{
        |  "id": "${serializable.id}",
        |  "qualified_id": {
        |       "id": "${serializable.qualifiedId.value}"
        |       "domain": "${serializable.qualifiedId.domain}"
        |  },
        |  "name": "${serializable.name}",
        |  "accent_id": ${serializable.accentId},
        |  "assets": [
        |       {
        |           "key": "${serializable.assets[0].key}",
        |           "type": "${serializable.assets[0].type}"
        |       }
        |  ],
        |  "picture": []
        |  "locale": "${serializable.locale}",
        |  "team": "${serializable.team}",
        |  "managed_by": "${serializable.managedBy}"
        |}
        """.trimMargin()
    }

    val valid = ValidJsonProvider(
        SelfUserInfoResponse(
            email = null,
            phone = null,
            userSsoId = null,
            id = "id",
            qualifiedId = UserId(value = "99db9768-04e3-4b5d-9268-831b6a25c4ab", domain = "domain"),
            name = "cool_name",
            picture = listOf(),
            accentId = 0,
            deleted = null,
            assets = listOf(UserAsset(type = AssetType.Image, key = "asset_key", size = null)),
            locale = "user_local",
            service = null,
            expiresAt = null,
            handle = null,
            team = "99db9768-04e3-4b5d-9268-831b6a25c4ab",
            managedBy = ManagedBy.Wire
        ),
        jsonProvider
    )
}