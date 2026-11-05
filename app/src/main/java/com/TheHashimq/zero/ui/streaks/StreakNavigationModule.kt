package com.TheHashimq.zero.ui.streaks

import zero.foundation.contract.ModuleContext
import zero.foundation.contract.ModuleException
import zero.foundation.contract.ModuleId
import zero.foundation.contract.ZeroModule
import zero.foundation.navigation.NavCategory
import zero.foundation.navigation.NavEntry
import zero.foundation.runtime.ZeroServices

class StreakNavigationModule : ZeroModule {
    override val id: ModuleId = ModuleId("streaks")
    override val displayName: String = "Streaks"

    override fun onStart(context: ModuleContext) {
        val services = context as? ZeroServices
            ?: throw ModuleException("StreakNavigationModule requires ZeroServices")
        services.navigation.register(NavEntry(id.value, displayName, NavCategory.PERSONAL))
    }

    override fun onStop() = Unit
}
