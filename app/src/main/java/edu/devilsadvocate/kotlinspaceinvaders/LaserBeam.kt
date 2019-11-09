package edu.devilsadvocate.kotlinspaceinvaders

import android.graphics.RectF

class LaserBeam(screenY: Int, private val speed: Float = 350f, heightModifier: Float = 20f) {
    val position = RectF()

    // Which way is it shooting?
    companion object {
        const val up = 0
        const val down = 1
        const val nowhere = -1
    }

    private var heading = nowhere

    private val width = 2
    private var height = screenY / heightModifier

    var isActive = false

    fun shoot(startX: Float, startY: Float, direction: Int) : Boolean {
        if (!isActive) {
            position.left = startX
            position.top = startY
            position.right = position.left + width
            position.bottom = position.top + height
            heading = direction
            isActive = true
            return true
        }

        // Our laser beam is already shooting out
        return false
    }

    fun update(fps: Long) {
        // Move up or down
        if (heading == up) {
            position.top -= speed / fps
        } else if (heading == down) {
            position.top += speed / fps
        }

        position.bottom = position.top + height
    }
}