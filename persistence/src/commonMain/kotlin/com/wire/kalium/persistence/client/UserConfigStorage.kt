package com.wire.kalium.persistence.client

import com.wire.kalium.persistence.kmm_settings.KaliumPreferences

interface UserConfigStorage {

    /**
     * save flag from the user settings to enable and disable the logging
     */
    fun enableLogging(enabled: Boolean)

    /**
     * get the saved flag to know if the logging enabled or not
     */
    fun isLoggingEnables(): Boolean
}


class UserConfigStorageImpl(private val kaliumPreferences: KaliumPreferences) : UserConfigStorage {

    override fun enableLogging(enabled: Boolean) {
        kaliumPreferences.putBoolean(ENABLE_LOGGING, enabled)
    }

    override fun isLoggingEnables(): Boolean =
        kaliumPreferences.getBoolean(ENABLE_LOGGING)

    private companion object {
        const val ENABLE_LOGGING = "enable_logging"
    }

}