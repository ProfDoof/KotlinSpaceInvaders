package edu.devilsadvocate.kotlinspaceinvaders

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF

class PlayerShip(context: Context, private val screenX: Int, screenY: Int) {

    // PlayerShip bitmap
    var bitmap: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.playership)

    // Initial location of ship
    val width = screenX / 20f
    private val height = screenY / 20f

    // Current location of ship
    val position = RectF( screenX / 2f, screenY - height, screenX / 2 + width, screenY.toFloat())

    // The pixels per second speed of the ship
    private val speed = 450f

    companion object {
        const val stopped = 0
        const val left = 1
        const val right = 2
    }

    // The current direction that the PlayerShip is moving
    var moving = stopped

    init {
        // Scale the bitmap to fit the screen
        bitmap = Bitmap.createScaledBitmap(bitmap, width.toInt(), height.toInt(), false)
    }

    // Determine if the ship needs to move and changes the coordinates if it does
    fun update(fps: Long) {
        // Move as long as we don't try to leave the screen
        if (moving == left && position.left > 0) {
            position.left -= speed / fps
        } else if (moving == right && position.left < screenX - width) {
            position.left += speed / fps
        }
    }
}