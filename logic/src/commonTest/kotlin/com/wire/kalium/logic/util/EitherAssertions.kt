package com.wire.kalium.logic.util

import com.wire.kalium.logic.functional.Either
import kotlin.test.fail

inline infix fun <L, R> Either<L, R>.shouldSucceed(crossinline successAssertion: (R) -> Unit) =
    this.fold({ fail("Expected a Right value but got Left") }) { successAssertion(it) }!!

inline infix fun <L, R> Either<L, R>.shouldFail(crossinline failAssertion: (L) -> Unit) =
    this.fold({ failAssertion(it) }) { fail("Expected a Left value but got Right") }!!

fun <L> Either<L, Unit>.shouldFail() = shouldFail { }