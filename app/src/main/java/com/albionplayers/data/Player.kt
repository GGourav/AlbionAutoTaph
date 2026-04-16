package com.albionplayers.data

data class Player(
    val id: Long,
    val name: String,
    val guildName: String,
    val allianceName: String? = null,
    val faction: Int = 0,
    var posX: Float = 0f,
    var posY: Float = 0f,
    var equipment: List<Int> = emptyList()
) {
    val isHostile: Boolean get() = faction == 255
    val isPassive: Boolean get() = faction == 0
    val isFaction: Boolean get() = faction in 1..6
}
