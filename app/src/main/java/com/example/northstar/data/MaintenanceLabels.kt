package com.example.northstar.data

import com.example.northstar.R
import com.example.northstar.settings.L10n

/** Translate only original seeded entries; preserve user-created and renamed services. */
val MaintenanceItem.localizedName: String
    get() {
        val seed = when (sid) {
            "seed-chain" -> "Chain clean & lube" to R.string.service_chain
            "seed-oil" -> "Engine oil" to R.string.service_oil
            "seed-airfilter" -> "Air filter" to R.string.service_air
            "seed-brakes" -> "Brake pads (front)" to R.string.service_brakes
            "seed-coolant" -> "Coolant" to R.string.service_coolant
            else -> null
        }
        return if (seed != null && name == seed.first) L10n.text(seed.second) else name
    }
