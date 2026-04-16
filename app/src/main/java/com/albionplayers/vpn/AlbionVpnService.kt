package com.albionplayers.vpn

import android.app.*
import android.content.Intent
import android.net.VpnService
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.albionplayers.MainApplication
import com.albionplayers.parser.PhotonPacketParser
import com.albionplayers.ui.MainActivity
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.nio.ByteBuffer

class AlbionVpnService : VpnService() {
    
    private val binder = LocalBinder()
    private var thread: Thread? = null
    private var running = false
    private val parser = PhotonPacketParser()
    private var localTunnel: ParcelFileDescriptor? = null
    
    // Albion server IP (placeholder — detect dynamically)
    private val SERVER_IP = "5.45.187.219"
    private val ALBION_UDP_PORT = 5056
    
    companion object {
        const val TAG = "AlbionVpnService"
        const val NOTIFICATION_ID = 1
    }
    
    inner class VpnRunnable : Runnable {
        override fun run() {
            try {
                val socket = DatagramSocket(0)
                socket.soTimeout = 100
                
                val packet = ByteArray(2048)
                val buf = ByteBuffer.wrap(packet)
                
                while (running) {
                    try {
                        val p = DatagramPacket(packet, packet.size)
                        socket.receive(p)
                        
                        val len = p.length
                        buf.position(0)
                        buf.limit(len)
                        
                        val isAlbion = p.address.hostAddress == SERVER_IP && p.port == ALBION_UDP_PORT
                        
                        // Write to tunnel if outbound to Albion
                        if (isAlbion) {
                            localTunnel?.let { tun ->
                                val out = FileOutputStream(tun.fileDescriptor)
                                out.write(buf.array(), 0, len)
                            }
                        }
                        
                        // Parse Photon
                        if (isAlbion) {
                            val data = packet.copyOf(len)
                            parser.parseServerPacket(data)
                        }
                    } catch (e: Exception) {
                        // timeout or parse error — continue
                    }
                }
                socket.close()
            } catch (e: Exception) {
                Log.e(TAG, "VPN loop error: ${e.message}")
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        
        if (!running) {
            running = true
            thread = Thread(VpnRunnable()).apply { start() }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onDestroy() {
        running = false
        thread?.interrupt()
        thread = null
        localTunnel?.close()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
    
    private fun buildNotification(): Notification {
        return Notification.Builder(this, MainApplication.CHANNEL_ID)
            .setContentTitle("Albion Radar Active")
            .setContentText("Capturing player data")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(Notification.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}
