package com.wire.kalium.network

import com.wire.kalium.network.api.user.login.LoginApi
import com.wire.kalium.network.api.user.login.LoginApiImp
import io.ktor.client.engine.HttpClientEngine

class LoginNetworkContainer(
    private val engine: HttpClientEngine = defaultHttpEngine(),
    private val isRequestLoggingEnabled: Boolean = false
) {

    val loginApi: LoginApi get() = LoginApiImp(anonymousHttpClient)

    internal val anonymousHttpClient by lazy {
        provideBaseHttpClient(engine, isRequestLoggingEnabled)
    }
}