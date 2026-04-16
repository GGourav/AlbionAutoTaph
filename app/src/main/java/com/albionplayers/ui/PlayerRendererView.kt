package com.albionplayers.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.albionplayers.data.Player

class PlayerRendererView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private var players: List<Player> = emptyList()
    private var localX = 0f
    private var localY = 0f
    private val scale = 50f  // pixels per game unit
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dotRadius = 8f
    private val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
        color = Color.WHITE
    }
    
    private val colors = mapOf(
        0 to Color.parseColor("#00ff88"),   // passive
        255 to Color.parseColor("#ff4444"), // hostile
        -1 to Color.parseColor("#ffa500")   // faction
    )
    
    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#0d0d1a"))
        
        // Draw center dot (local player)
        val cx = width / 2f
        val cy = height / 2f
        paint.color = Color.WHITE
        canvas.drawCircle(cx, cy, 6f, paint)
        
        // Draw ring
        paint.color = Color.parseColor("#333355")
        paint.style = Paint.Style.STROKE
        canvas.drawCircle(cx, cy, 150f, paint)
        
        // Draw players
        for (player in players) {
            val dx = (player.posX - localX) * scale
            val dy = (player.posY - localY) * scale
            val px = cx + dx
            val py = cy - dy
            
            if (px < -50 || px > width + 50 || py < -50 || py > height + 50) continue
            
            val color = when {
                player.isHostile -> Color.parseColor("#ff4444")
                player.isFaction -> Color.parseColor("#ffa500")
                else -> Color.parseColor("#00ff88")
            }
            
            paint.color = color
            paint.style = Paint.Style.FILL
            canvas.drawCircle(px, py, dotRadius, paint)
            
            namePaint.color = color
            canvas.drawText(player.name, px + 12, py + 8, namePaint)
            
            if (player.guildName.isNotEmpty()) {
                namePaint.textSize = 20f
                namePaint.color = Color.parseColor("#888888")
                canvas.drawText("[${player.guildName}]", px + 12, py + 28, namePaint)
            }
        }
        
        paint.style = Paint.Style.FILL
    }
    
    fun setPlayers(list: List<Player>) {
        players = list
        invalidate()
    }
    
    fun setLocalPosition(x: Float, y: Float) {
        localX = x
        localY = y
    }
}
