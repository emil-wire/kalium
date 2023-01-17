package com.wire.kalium.cli

import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.featureFlags.KaliumConfigs

actual fun homeDirectory(): String {
    return System.getProperty("user.home")
}

actual fun coreLogic(
    rootPath: String,
    kaliumConfigs: KaliumConfigs
): CoreLogic = CoreLogic(rootPath, kaliumConfigs)
