package com.pioneer.nycfirewire.utils

import com.pioneer.nycfirewire.R

/**
 * Presentation-only categorization of responding units for the incident
 * detail redesign. The API sends plain unit-name strings; these prefix
 * rules color the unit chips (mirrors the iOS implementation).
 *
 * NOTE: product wants these colors admin-configurable from the portal
 * eventually; until then the mapping is hardcoded here and must stay in
 * sync with the iOS UnitCategory implementation.
 */
enum class UnitCategory(val bgColorRes: Int, val fgColorRes: Int) {
    ENGINE(R.color.fw_unit_engine_bg, R.color.fw_unit_engine_fg),
    TRUCK_RESCUE(R.color.fw_unit_truck_bg, R.color.fw_unit_truck_fg),
    MARINE(R.color.fw_unit_marine_bg, R.color.fw_unit_marine_fg),
    CHIEF(R.color.fw_unit_chief_bg, R.color.fw_unit_chief_fg),
    SOC(R.color.fw_unit_soc_bg, R.color.fw_unit_soc_fg),
    OTHER(R.color.fw_unit_other_bg, R.color.fw_unit_other_fg);

    companion object {
        private val socPrefixes = listOf("SQUAD", "HAZMAT", "RAC", "SOC", "SATELLITE")
        private val truckPrefixes = listOf("L-", "TL-", "RESCUE", "RES-", "LADDER", "TOWER")
        private val chiefPrefixes = listOf("BN", "DIV", "CAR")

        fun classify(unitName: String): UnitCategory {
            val name = unitName.trim().uppercase()

            if (name.startsWith("MARINE")) return MARINE
            if (socPrefixes.any { name.startsWith(it) }) return SOC

            if (truckPrefixes.any { name.startsWith(it) }) return TRUCK_RESCUE
            // Ladders/tower ladders without a dash, e.g. "L2", "TL7"
            if (name.length >= 2 && name[0] == 'L' && name[1].isDigit()) return TRUCK_RESCUE
            if (name.length >= 3 && name.startsWith("TL") && name[2].isDigit()) return TRUCK_RESCUE

            if (chiefPrefixes.any { name.startsWith(it) }) return CHIEF

            if (name.startsWith("E-") || name.startsWith("ENGINE")) return ENGINE
            if (name.length >= 2 && name[0] == 'E' && name[1].isDigit()) return ENGINE

            return OTHER
        }
    }
}
