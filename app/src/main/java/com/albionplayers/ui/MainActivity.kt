package com.albionplayers.ui

import android.app.Activity
import android.app.VpnService
import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import com.albionplayers.R
import com.albionplayers.data.ZonesDatabase
import com.albionplayers.parser.PhotonPacketParser
import com.albionplayers.vpn.AlbionVpnService

class MainActivity : Activity() {
    
    private var vpnIntent: Intent? = null
    private lateinit var radarView: PlayerRendererView
    private lateinit var parser: PhotonPacketParser
    private val handler = android.os.Handler(mainLooper)
    
    private val updateRunnable = object : Runnable {
        override fun run() {
            val players = parser.getPlayers()
            radarView.setPlayers(players)
            handler.postDelayed(this, 500)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#1a1a2e"))
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        // Title
        container.addView(TextView(this).apply {
            text = "Albion Players Radar"
            setTextColor(Color.WHITE)
            textSize = 24f
            setPadding(0, 0, 0, 24)
        })
        
        // VPN status
        val statusView = TextView(this).apply {
            text = "VPN: Tap to Start"
            setTextColor(Color.parseColor("#7C3AED"))
            textSize = 16f
            setPadding(0, 16, 0, 16)
        }
        container.addView(statusView)
        
        // Start VPN button
        val vpnBtn = Button(this).apply {
            text = "Start VPN Capture"
            setOnClickListener { startVpn() }
        }
        container.addView(vpnBtn)
        
        // Radar view
        radarView = PlayerRendererView(this)
        radarView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 800
        ).apply { topMargin = 24 }
        container.addView(radarView)
        
        // Info
        val infoView = TextView(this).apply {
            text = "Players detected: 0"
            setTextColor(Color.parseColor("#cccccc"))
            textSize = 14f
            setPadding(0, 16, 0, 0)
        }
        container.addView(infoView)
        
        // Legend
        listOf(
            "🟢 Passive" to "#00ff88",
            "🟠 Faction" to "#ffa500",
            "🔴 Hostile" to "#ff4444"
        ).forEach { (label, color) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            val dot = View(this).apply {
                setBackgroundColor(Color.parseColor(color))
            }
            dot.layoutParams = LinearLayout.LayoutParams(16, 16).apply { rightMargin = 12 }
            row.addView(dot)
            row.addView(TextView(this).apply {
                text = label
                setTextColor(Color.WHITE)
            })
            container.addView(row)
        }
        
        scroll.addView(container)
        setContentView(scroll)
        
        parser = PhotonPacketParser()
        handler.post(updateRunnable)
    }
    
    private fun startVpn() {
        vpnIntent = VpnService.prepare(this)
        if (vpnIntent != null) {
            startActivityForResult(vpnIntent, 0)
        } else {
            launchVpnService()
        }
    }
    
    override fun onActivityResult(request: Int, result: Int, data: Intent?) {
        super.onActivityResult(request, result, data)
        if (result == RESULT_OK) launchVpnService()
    }
    
    private fun launchVpnService() {
        val intent = Intent(this, AlbionVpnService::class.java)
        startForegroundService(intent)
        Toast.makeText(this, "VPN Started", Toast.LENGTH_SHORT).show()
    }
}
