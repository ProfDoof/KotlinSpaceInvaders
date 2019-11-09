package edu.devilsadvocate.kotlinspaceinvaders

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import java.util.*

class Invader(context: Context, row: Int, column: Int, screenX: Int, screenY: Int) {
    // How wide, high, and spaced out the invader will be
    var width = screenX / 35f
    private var height = screenY / 35f
    private val padding = screenX / 45

    var position = RectF(
        column * (width + padding),
        100 + row * (width + padding / 4),
        column * (width + padding) + width,
        100 + row * (width + padding / 4) + height
    )

    // Pixels per second that the invader should move
    private var speed = 40f

    companion object {
        const val left = 1
        const val right = 2

        // The bitmaps representing the alien ships two states
        var bitmap1: Bitmap? = null
        var bitmap2: Bitmap? = null

        // Number of active invaders
        var numberOfInvaders = 0
    }

    // Which direction is the ship moving
    private var shipMoving = right

    var isVisible = true

    init {
        // initialize bitmaps
        bitmap1 = BitmapFactory.decodeResource(context.resources, R.drawable.invader1)
        bitmap2 = BitmapFactory.decodeResource(context.resources, R.drawable.invader2)

        // Scale the bitmaps to the screen
        bitmap1 = Bitmap.createScaledBitmap(
            bitmap1!!,
            width.toInt(),
            height.toInt(),
            false
        )

        bitmap2 = Bitmap.createScaledBitmap(
            bitmap2!!,
            width.toInt(),
            height.toInt(),
            false
        )

        numberOfInvaders++
    }

    fun update(fps: Long) {
        if (shipMoving == left) {
            position.left -= speed / fps
        } else if (shipMoving == right) {
            position.left += speed / fps
        }

        position.right = position.left + width
    }

    fun dropDownAndReverse (waveNumber: Int) {
        shipMoving = if (shipMoving == left) right else left

        position.top += height
        position.bottom += height

        speed *= (1.1f + (waveNumber.toFloat() / 20))
    }

    fun takeAim(playerShipX: Float, playerShipLength: Float, waves: Int) : Boolean {
        val generator = Random()
        var randomNumber: Int

        if (playerShipX + playerShipLength > position.left &&
            playerShipX + playerShipLength < position.left + width ||
            playerShipX > position.left && playerShipX < position.left + width) {

            // The chance of shooting increases
            // at an inversely proportional rate to the number of invaders
            // The chance of shooting increases
            // at a proportional rate to the wave number that they are on
            randomNumber = generator.nextInt(100 * numberOfInvaders) / waves
            if (randomNumber == 0) {
                return true
            }
        }

        // Firing randomly (not near the player)
        randomNumber = generator.nextInt(150 * numberOfInvaders)
        return randomNumber == 0
    }

}