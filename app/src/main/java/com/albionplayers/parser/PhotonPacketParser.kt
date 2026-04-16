package com.albionplayers.parser

import android.util.Log
import com.albionplayers.data.Player
import java.nio.ByteBuffer
import kotlin.math.roundToInt

class PhotonPacketParser {
    
    private val players = mutableMapOf<Long, Player>()
    
    companion object {
        const val TAG = "PhotonParser"
        
        // Event codes (params[252])
        const val EVT_LEAVE = 1
        const val EVT_MOVE = 3
        const val EVT_NEW_CHARACTER = 29
        const val EVT_EQUIPMENT = 90
        const val EVT_MOUNTED = 209
        
        // Photon command types
        const val CMD_SEND_RELIABLE = 6
        const val MSG_REQUEST = 2
        const val MSG_RESPONSE = 3
        const val MSG_EVENT = 4
        
        // Protocol18 type codes
        const val TYPE_NULL = 8
        const val TYPE_COMPRESSED_INT = 9
        const val TYPE_INT1 = 11
        const val TYPE_INT2 = 13
        const val TYPE_INT_ZERO = 30
        const val TYPE_STRING = 7
        const val TYPE_BYTEARRAY = 67
        const val TYPE_HASHTABLE = 21
        const val TYPE_EVENTDATA = 26
    }
    
    fun parseServerPacket(data: ByteArray) {
        try {
            if (data.size < 12) return
            
            val buf = ByteBuffer.wrap(data)
            
            // Photon header: peerId(2) + flags(1) + cmdCount(1) + timestamp(4) + challenge(4)
            buf.position(2)
            val flags = buf.get().toInt()
            val cmdCount = buf.get().toInt()
            buf.position(buf.position() + 8) // skip timestamp + challenge
            
            if (flags == 1) return // encrypted
            
            for (i in 0 until cmdCount) {
                if (buf.remaining() < 12) break
                
                val cmdType = buf.get().toInt()
                buf.position(buf.position() + 3) // channelId + flags + reserved
                val cmdLen = buf.int
                buf.position(buf.position() + 4) // sequenceNumber
                
                val payloadLen = cmdLen - 12
                if (payloadLen <= 0 || buf.remaining() < payloadLen) break
                
                if (cmdType == CMD_SEND_RELIABLE) {
                    buf.get() // signalByte
                    val msgType = buf.get().toInt()
                    val payload = ByteArray(payloadLen - 2)
                    buf.get(payload)
                    
                    when (msgType) {
                        MSG_EVENT -> parseEvent(ByteBuffer.wrap(payload))
                        MSG_RESPONSE -> parseResponse(ByteBuffer.wrap(payload))
                    }
                } else {
                    buf.position(buf.position() + payloadLen)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parse error: ${e.message}")
        }
    }
    
    private fun parseEvent(buf: ByteBuffer) {
        if (buf.remaining() < 2) return
        
        val eventCode = buf.get().toInt() and 0xFF
        val paramCount = readCompressedInt(buf)
        
        val params = mutableMapOf<Int, Any?>()
        for (i in 0 until paramCount) {
            val key = buf.get().toInt() and 0xFF
            val value = readValue(buf)
            params[key] = value
        }
        
        // Override with real event code from params[252]
        val realCode = (params[252] as? Number)?.toInt() ?: eventCode
        
        when (realCode) {
            EVT_NEW_CHARACTER -> handleNewCharacter(params)
            EVT_LEAVE -> handleLeave(params)
            EVT_MOVE -> handleMove(params)
            EVT_EQUIPMENT -> handleEquipment(params)
        }
    }
    
    private fun parseResponse(buf: ByteBuffer) {
        if (buf.remaining() < 3) return
        val opCode = buf.get().toInt() and 0xFF
        buf.short // returnCode
        // Market order / debug slot may follow
        if (buf.hasRemaining()) readValue(buf)
        val paramCount = readCompressedInt(buf)
        for (i in 0 until paramCount) {
            val key = buf.get().toInt() and 0xFF
            readValue(buf)
        }
    }
    
    private fun handleNewCharacter(params: Map<Int, Any?>) {
        val id = (params[0] as? Number)?.toLong() ?: return
        val name = params[1] as? String ?: return
        val guild = params[8] as? String
        val faction = (params[53] as? Number)?.toInt() ?: 0
        val alliance = params[51] as? String
        
        val player = Player(
            id = id,
            name = name,
            guildName = guild ?: "",
            allianceName = alliance,
            faction = faction
        )
        
        synchronized(players) {
            players[id] = player
        }
        
        Log.i(TAG, "Player detected: $name [$guild]")
    }
    
    private fun handleLeave(params: Map<Int, Any?>) {
        val id = (params[0] as? Number)?.toLong() ?: return
        synchronized(players) { players.remove(id) }
    }
    
    private fun handleMove(params: Map<Int, Any?>) {
        val id = (params[0] as? Number)?.toLong() ?: return
        
        val raw = params[1] as? ByteArray ?: return
        if (raw.size < 17) return
        
        val buf = ByteBuffer.wrap(raw)
        buf.position(9)
        val posX = buf.float
        val posY = buf.float
        
        synchronized(players) {
            players[id]?.let { it.posX = posX; it.posY = posY }
        }
    }
    
    private fun handleEquipment(params: Map<Int, Any?>) {
        val id = (params[0] as? Number)?.toLong() ?: return
        val equips = params[40] as? List<*> ?: return
        
        synchronized(players) {
            players[id]?.equipment = equips.filterIsInstance<Number>().map { it.toInt() }
        }
    }
    
    private fun readValue(buf: ByteBuffer): Any? {
        if (!buf.hasRemaining()) return null
        val type = buf.get().toInt() and 0xFF
        
        return when (type) {
            TYPE_NULL -> null
            TYPE_INT_ZERO -> 0
            TYPE_INT1 -> buf.get().toInt() and 0xFF
            TYPE_INT2 -> buf.short.toInt() and 0xFFFF
            TYPE_COMPRESSED_INT -> readCompressedInt(buf)
            TYPE_STRING -> readString(buf)
            TYPE_BYTEARRAY -> readByteArray(buf)
            TYPE_HASHTABLE -> readHashtable(buf)
            else -> null
        }
    }
    
    private fun readCompressedInt(buf: ByteBuffer): Int {
        var result = 0
        var shift = 0
        while (buf.hasRemaining()) {
            val b = buf.get().toInt()
            result = result or ((b and 0x7F) shl shift)
            if ((b and 0x80) == 0) return result
            shift += 7
        }
        return result
    }
    
    private fun readString(buf: ByteBuffer): String {
        val len = readCompressedInt(buf)
        if (len <= 0 || len > buf.remaining()) return ""
        val bytes = ByteArray(len)
        buf.get(bytes)
        return String(bytes, Charsets.UTF_8)
    }
    
    private fun readByteArray(buf: ByteBuffer): ByteArray {
        val len = readCompressedInt(buf)
        if (len <= 0 || len > buf.remaining()) return ByteArray(0)
        val bytes = ByteArray(len)
        buf.get(bytes)
        return bytes
    }
    
    private fun readHashtable(buf: ByteBuffer): Map<*, *> {
        val count = readCompressedInt(buf)
        val map = mutableMapOf<Any?, Any?>()
        for (i in 0 until count) {
            val key = readValue(buf)
            val value = readValue(buf)
            map[key] = value
        }
        return map
    }
    
    fun getPlayers(): List<Player> = synchronized(players) { players.values.toList() }
}
