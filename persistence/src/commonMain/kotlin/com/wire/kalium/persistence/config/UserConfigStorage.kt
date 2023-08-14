/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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

package com.wire.kalium.persistence.config

import com.wire.kalium.persistence.kmmSettings.KaliumPreferences
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Suppress("TooManyFunctions")
interface UserConfigStorage {

    /**
     * save flag from the file sharing api, and if the status changes
     */
    fun persistFileSharingStatus(status: Boolean, isStatusChanged: Boolean?)

    /**
     * get the saved flag that been saved to know if the file sharing is enabled or not with the flag
     * to know if there was a status change
     */
    fun isFileSharingEnabled(): IsFileSharingEnabledEntity?

    /**
     * returns the Flow of file sharing status
     */
    fun isFileSharingEnabledFlow(): Flow<IsFileSharingEnabledEntity?>

    fun setFileSharingAsNotified()

    /**
     * returns a Flow containing the status and list of classified domains
     */
    fun isClassifiedDomainsEnabledFlow(): Flow<ClassifiedDomainsEntity>

    /**
     * save the flag and list of trusted domains
     */
    fun persistClassifiedDomainsStatus(status: Boolean, classifiedDomains: List<String>)

    /**
     * Saves the flag that indicates whether a 2FA challenge is
     * required for some operations such as:
     * Login, Create Account, Register Client, etc.
     * @see isSecondFactorPasswordChallengeRequired
     */
    fun persistSecondFactorPasswordChallengeStatus(isRequired: Boolean)

    /**
     * Checks if the 2FA challenge is
     * required for some operations such as:
     * Login, Create Account, Register Client, etc.
     * @see persistSecondFactorPasswordChallengeStatus
     */
    fun isSecondFactorPasswordChallengeRequired(): Boolean

    /**
     * save flag from the user settings to enable and disable MLS
     */
    fun enableMLS(enabled: Boolean)

    /**
     * get the saved flag to know if MLS enabled or not
     */
    fun isMLSEnabled(): Boolean

    /**
     * save MLSE2EISetting
     */
    fun setE2EISettings(settingEntity: E2EISettingsEntity)

    /**
     * get MLSE2EISetting
     */
    fun getE2EISettings(): E2EISettingsEntity?

    /**
     * get Flow of the saved MLSE2EISetting
     */
    fun e2EISettingsFlow(): Flow<E2EISettingsEntity?>

    /**
     * save flag from user settings to enable or disable Conference Calling
     */
    fun persistConferenceCalling(enabled: Boolean)

    /**
     * get the saved flag to know if Conference Calling is enabled or not
     */
    fun isConferenceCallingEnabled(): Boolean

    /**
     * get the saved flag to know if user's Read Receipts are enabled or not
     */
    fun isReadReceiptsEnabled(): Flow<Boolean>

    /**
     * save the flag to know if user's Read Receipts are enabled or not
     */
    fun persistReadReceipts(enabled: Boolean)

    fun persistGuestRoomLinkFeatureFlag(status: Boolean, isStatusChanged: Boolean?)
    fun isGuestRoomLinkEnabled(): IsGuestRoomLinkEnabledEntity?
    fun isGuestRoomLinkEnabledFlow(): Flow<IsGuestRoomLinkEnabledEntity?>
    fun isScreenshotCensoringEnabledFlow(): Flow<Boolean>
    fun persistScreenshotCensoring(enabled: Boolean)
    fun setE2EINotificationTime(timeStamp: Long)
    fun getE2EINotificationTime(): Long?
    fun e2EINotificationTimeFlow(): Flow<Long?>
}

@Serializable
data class IsFileSharingEnabledEntity(
    @SerialName("status") val status: Boolean,
    @SerialName("isStatusChanged") val isStatusChanged: Boolean?
)

@Serializable
data class ClassifiedDomainsEntity(
    @SerialName("status") val status: Boolean,
    @SerialName("trustedDomains") val trustedDomains: List<String>,
)

@Serializable
data class IsGuestRoomLinkEnabledEntity(
    @SerialName("status") val status: Boolean,
    @SerialName("isStatusChanged") val isStatusChanged: Boolean?
)

@Serializable
data class TeamSettingsSelfDeletionStatusEntity(
    @SerialName("selfDeletionTimer") val selfDeletionTimerEntity: SelfDeletionTimerEntity,
    @SerialName("isStatusChanged") val isStatusChanged: Boolean?
)

@Serializable
data class E2EISettingsEntity(
    @SerialName("status") val status: Boolean,
    @SerialName("discoverUrl") val discoverUrl: String,
    @SerialName("gracePeriodEndMs") val gracePeriodEndMs: Long?,
)

@Serializable
sealed class SelfDeletionTimerEntity {

    @Serializable
    @SerialName("disabled")
    object Disabled : SelfDeletionTimerEntity()

    @Serializable
    @SerialName("enabled")
    object Enabled : SelfDeletionTimerEntity()

    @Serializable
    @SerialName("enforced")
    data class Enforced(val enforcedDuration: Duration) : SelfDeletionTimerEntity()
}

@Suppress("TooManyFunctions")
class UserConfigStorageImpl(
    private val kaliumPreferences: KaliumPreferences
) : UserConfigStorage {

    private val isReadReceiptsEnabledFlow =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val isFileSharingEnabledFlow =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val isClassifiedDomainsEnabledFlow =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val isGuestRoomLinkEnabledFlow =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val e2EIFlow =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val e2EINotificationFlow =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val isScreenshotCensoringEnabledFlow =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override fun persistFileSharingStatus(
        status: Boolean,
        isStatusChanged: Boolean?
    ) {
        kaliumPreferences.putSerializable(
            FILE_SHARING,
            IsFileSharingEnabledEntity(status, isStatusChanged),
            IsFileSharingEnabledEntity.serializer()
        ).also {
            isFileSharingEnabledFlow.tryEmit(Unit)
        }
    }

    override fun isFileSharingEnabled(): IsFileSharingEnabledEntity? =
        kaliumPreferences.getSerializable(FILE_SHARING, IsFileSharingEnabledEntity.serializer())

    override fun isFileSharingEnabledFlow(): Flow<IsFileSharingEnabledEntity?> = isFileSharingEnabledFlow
        .map { isFileSharingEnabled() }
        .onStart { emit(isFileSharingEnabled()) }
        .distinctUntilChanged()

    override fun setFileSharingAsNotified() {
        val newValue =
            kaliumPreferences.getSerializable(FILE_SHARING, IsFileSharingEnabledEntity.serializer())?.copy(isStatusChanged = false)
                ?: return
        kaliumPreferences.putSerializable(
            FILE_SHARING,
            newValue,
            IsFileSharingEnabledEntity.serializer()
        ).also {
            isFileSharingEnabledFlow.tryEmit(Unit)
        }
    }

    override fun isClassifiedDomainsEnabledFlow(): Flow<ClassifiedDomainsEntity> {
        return isClassifiedDomainsEnabledFlow
            .map {
                kaliumPreferences.getSerializable(ENABLE_CLASSIFIED_DOMAINS, ClassifiedDomainsEntity.serializer())!!
            }.onStart {
                emit(
                    kaliumPreferences.getSerializable(
                        ENABLE_CLASSIFIED_DOMAINS,
                        ClassifiedDomainsEntity.serializer()
                    )!!
                )
            }.distinctUntilChanged()
    }

    override fun persistClassifiedDomainsStatus(status: Boolean, classifiedDomains: List<String>) {
        kaliumPreferences.putSerializable(
            ENABLE_CLASSIFIED_DOMAINS,
            ClassifiedDomainsEntity(status, classifiedDomains),
            ClassifiedDomainsEntity.serializer()
        ).also {
            isClassifiedDomainsEnabledFlow.tryEmit(Unit)
        }
    }

    override fun persistSecondFactorPasswordChallengeStatus(isRequired: Boolean) {
        kaliumPreferences.putBoolean(REQUIRE_SECOND_FACTOR_PASSWORD_CHALLENGE, isRequired)
    }

    override fun isSecondFactorPasswordChallengeRequired(): Boolean =
        kaliumPreferences.getBoolean(REQUIRE_SECOND_FACTOR_PASSWORD_CHALLENGE, false)

    override fun enableMLS(enabled: Boolean) {
        kaliumPreferences.putBoolean(ENABLE_MLS, enabled)
    }

    override fun isMLSEnabled(): Boolean = kaliumPreferences.getBoolean(ENABLE_MLS, false)

    override fun setE2EISettings(settingEntity: E2EISettingsEntity) {
        kaliumPreferences.putSerializable(
            E2EI_SETTINGS,
            settingEntity,
            E2EISettingsEntity.serializer()
        ).also {
            e2EIFlow.tryEmit(Unit)
        }
    }

    override fun getE2EISettings(): E2EISettingsEntity? {
        return kaliumPreferences.getSerializable(E2EI_SETTINGS, E2EISettingsEntity.serializer())
    }

    override fun e2EISettingsFlow(): Flow<E2EISettingsEntity?> = e2EIFlow
        .map { getE2EISettings() }
        .onStart { emit(getE2EISettings()) }
        .distinctUntilChanged()

    override fun setE2EINotificationTime(timeStamp: Long) {
        kaliumPreferences.putLong(
            E2EI_NOTIFICATION_TIME,
            timeStamp
        ).also {
            e2EIFlow.tryEmit(Unit)
        }
    }

    override fun getE2EINotificationTime(): Long? {
        return kaliumPreferences.getLong(E2EI_NOTIFICATION_TIME)
    }

    override fun e2EINotificationTimeFlow(): Flow<Long?> = e2EINotificationFlow
        .map { getE2EINotificationTime() }
        .onStart { emit(getE2EINotificationTime()) }
        .distinctUntilChanged()

    override fun persistConferenceCalling(enabled: Boolean) {
        kaliumPreferences.putBoolean(ENABLE_CONFERENCE_CALLING, enabled)
    }

    override fun isConferenceCallingEnabled(): Boolean =
        kaliumPreferences.getBoolean(ENABLE_CONFERENCE_CALLING, DEFAULT_CONFERENCE_CALLING_ENABLED_VALUE)

    override fun isReadReceiptsEnabled(): Flow<Boolean> = isReadReceiptsEnabledFlow
        .map { kaliumPreferences.getBoolean(ENABLE_READ_RECEIPTS, true) }
        .onStart { emit(kaliumPreferences.getBoolean(ENABLE_READ_RECEIPTS, true)) }
        .distinctUntilChanged()

    override fun persistReadReceipts(enabled: Boolean) {
        kaliumPreferences.putBoolean(ENABLE_READ_RECEIPTS, enabled).also {
            isReadReceiptsEnabledFlow.tryEmit(Unit)
        }
    }

    override fun persistGuestRoomLinkFeatureFlag(
        status: Boolean,
        isStatusChanged: Boolean?
    ) {
        kaliumPreferences.putSerializable(
            GUEST_ROOM_LINK,
            IsGuestRoomLinkEnabledEntity(status, isStatusChanged),
            IsGuestRoomLinkEnabledEntity.serializer()
        ).also {
            isGuestRoomLinkEnabledFlow.tryEmit(Unit)
        }
    }

    override fun isGuestRoomLinkEnabled(): IsGuestRoomLinkEnabledEntity? =
        kaliumPreferences.getSerializable(GUEST_ROOM_LINK, IsGuestRoomLinkEnabledEntity.serializer())

    override fun isGuestRoomLinkEnabledFlow(): Flow<IsGuestRoomLinkEnabledEntity?> =
        isGuestRoomLinkEnabledFlow
            .map { isGuestRoomLinkEnabled() }
            .onStart { emit(isGuestRoomLinkEnabled()) }
            .distinctUntilChanged()

    override fun isScreenshotCensoringEnabledFlow(): Flow<Boolean> = isScreenshotCensoringEnabledFlow
        .map { kaliumPreferences.getBoolean(ENABLE_SCREENSHOT_CENSORING, false) }
        .onStart { emit(kaliumPreferences.getBoolean(ENABLE_SCREENSHOT_CENSORING, false)) }
        .distinctUntilChanged()

    override fun persistScreenshotCensoring(enabled: Boolean) {
        kaliumPreferences.putBoolean(ENABLE_SCREENSHOT_CENSORING, enabled).also {
            isScreenshotCensoringEnabledFlow.tryEmit(Unit)
        }
    }

    private companion object {
        const val FILE_SHARING = "file_sharing"
        const val GUEST_ROOM_LINK = "guest_room_link"
        const val ENABLE_CLASSIFIED_DOMAINS = "enable_classified_domains"
        const val ENABLE_MLS = "enable_mls"
        const val E2EI_SETTINGS = "end_to_end_identity_settings"
        const val E2EI_NOTIFICATION_TIME = "end_to_end_identity_notification_time"
        const val ENABLE_CONFERENCE_CALLING = "enable_conference_calling"
        const val ENABLE_READ_RECEIPTS = "enable_read_receipts"
        const val DEFAULT_CONFERENCE_CALLING_ENABLED_VALUE = false
        const val REQUIRE_SECOND_FACTOR_PASSWORD_CHALLENGE = "require_second_factor_password_challenge"
        const val ENABLE_SCREENSHOT_CENSORING = "enable_screenshot_censoring"
    }
}
