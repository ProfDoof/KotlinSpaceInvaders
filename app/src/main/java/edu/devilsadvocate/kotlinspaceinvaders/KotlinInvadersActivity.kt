package edu.devilsadvocate.kotlinspaceinvaders

import android.app.Activity
import android.graphics.Point
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class KotlinInvadersActivity : Activity() {

    // All logic of game will occur in kotlinInvadersView
    private var kotlinInvadersView: KotlinInvadersView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get Display details using display object
        val display = windowManager.defaultDisplay

        // Save resolution in a Point object
        val size = Point()
        display.getSize(size)

        // Create gameView and set it as the content view
        kotlinInvadersView = KotlinInvadersView(this, size)
        setContentView(kotlinInvadersView)
    }

    override fun onResume() {
        super.onResume()

        // Tell gameView to resume.
        kotlinInvadersView?.resume()
    }

    override fun onPause() {
        super.onPause()

        // Tell gameView to pause
        kotlinInvadersView?.pause()
    }
}
