package com.albionplayers.data

object ZonesDatabase {
    
    data class ZoneInfo(
        val id: String,
        val name: String,
        val pvpType: String,  // safe | yellow | red | black
        val tier: Int = 0
    )
    
    private val zones = mapOf(
        "TNL-MARTLOCK" to ZoneInfo("TNL-MARTLOCK", "Martlock", "safe", 1),
        "TNL-LYM" to ZoneInfo("TNL-LYM", "Lymhurst", "safe", 1),
        "TNL-FORT" to ZoneInfo("TNL-FORT", "Fort Sterling", "safe", 1),
        "TNL-THETF" to ZoneInfo("TNL-THETF", "Thetford", "safe", 1),
        "TNL-BRIDGE" to ZoneInfo("TNL-BRIDGE", "Bridgewatch", "yellow", 3),
        "TNL-CAERLEON" to ZoneInfo("TNL-CAERLEON", "Caerleon", "red", 5),
        "TNL-BLACK" to ZoneInfo("TNL-BLACK", "The Deep", "black", 8),
    )
    
    fun getZone(id: String): ZoneInfo? = zones[id]
    fun getPvpType(id: String): String = zones[id]?.pvpType ?: "safe"
    fun isBlackZone(id: String): Boolean = getPvpType(id) == "black"
    fun isDangerous(id: String): Boolean = getPvpType(id) in listOf("red", "black")
}
