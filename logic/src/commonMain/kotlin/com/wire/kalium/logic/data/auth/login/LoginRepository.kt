package com.wire.kalium.logic.data.auth.login

import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.data.id.IdMapper
import com.wire.kalium.logic.data.session.SessionMapper
import com.wire.kalium.logic.data.user.SsoId
import com.wire.kalium.logic.di.MapperProvider
import com.wire.kalium.logic.feature.auth.AuthTokens
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.map
import com.wire.kalium.logic.wrapApiRequest
import com.wire.kalium.network.api.base.unauthenticated.LoginApi

internal interface LoginRepository {
    suspend fun loginWithEmail(
        email: String,
        password: String,
        label: String?,
        shouldPersistClient: Boolean
    ): Either<NetworkFailure, Pair<AuthTokens, SsoId?>>

    suspend fun loginWithHandle(
        handle: String,
        password: String,
        label: String?,
        shouldPersistClient: Boolean
    ): Either<NetworkFailure, Pair<AuthTokens, SsoId?>>
}

internal class LoginRepositoryImpl internal constructor(
    private val loginApi: LoginApi,
    private val sessionMapper: SessionMapper = MapperProvider.sessionMapper(),
    private val idMapper: IdMapper = MapperProvider.idMapper()
) : LoginRepository {

    override suspend fun loginWithEmail(
        email: String,
        password: String,
        label: String?,
        shouldPersistClient: Boolean
    ): Either<NetworkFailure, Pair<AuthTokens, SsoId?>> =
        login(LoginApi.LoginParam.LoginWithEmail(email, password, label), shouldPersistClient)

    override suspend fun loginWithHandle(
        handle: String,
        password: String,
        label: String?,
        shouldPersistClient: Boolean
    ): Either<NetworkFailure, Pair<AuthTokens, SsoId?>> =
        login(LoginApi.LoginParam.LoginWithHandel(handle, password, label), shouldPersistClient)

    private suspend fun login(
        loginParam: LoginApi.LoginParam,
        persistClient: Boolean
    ): Either<NetworkFailure, Pair<AuthTokens, SsoId?>> = wrapApiRequest {
        loginApi.login(param = loginParam, persist = persistClient)
    }.map {
        Pair(sessionMapper.fromSessionDTO(it.first), idMapper.toSsoId(it.second.ssoID))
    }
}
