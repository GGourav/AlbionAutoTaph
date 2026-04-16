package com.albionplayers.vpn

import android.os.Binder

class LocalBinder : Binder() {
    fun getService(): AlbionVpnService? = null
}
