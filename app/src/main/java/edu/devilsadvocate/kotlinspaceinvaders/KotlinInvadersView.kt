package edu.devilsadvocate.kotlinspaceinvaders

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceView

class KotlinInvadersView(context: Context, private val size: Point) : SurfaceView(context), Runnable {

    private val soundPlayer = SoundPlayer(context)
    // Game thread where we update
    private val gameThread = Thread(this)

    // Boolean for telling if player is playing
    private var playing = false

    // Boolean for pause state
    private var paused = true

    // Canvas and Paint object for creating view
    private var canvas: Canvas = Canvas()
    private var paint: Paint = Paint()

    // Game Objects

    // Players ship and laser beam object
    private var playerShip: PlayerShip = PlayerShip(context, size.x, size.y)
    // Player laser beam is much faster but also shorter
    private var playerLaserBeam: LaserBeam = LaserBeam(size.y, 1200f, 40f)

    // Invaders ships and laser beam objects
    private val invaders = ArrayList<Invader>()
    private var numberOfInvaders = 0
    private val invaderLaserBeams = ArrayList<LaserBeam>()
    private var nextLaser = 0
    private val maxLaserBeams = 10

    // The shelter bricks
    private val bricks = ArrayList<ShelterBrick>()
    private var numberOfBricks = 0

    // Current Score
    private var score = 0

    // Current Wave
    private var wave = 1

    // Number of remaining live
    private var lives = 3

    // To remember the high score
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "Kotlin Invaders",
        Context.MODE_PRIVATE)

    private var highScore = prefs.getInt("highScore", 0)

    // How long should our menacing sounds be?
    private var menaceInterval: Long = 1000.toLong()

    // Which menacing sound will be played next
    private var uhIfTrueOhIfFalse: Boolean = false

    // When did we last play a menacing sound
    private var lastMenaceTime = System.currentTimeMillis()

    private fun prepareLevel() {
        // Initialize all game objects
        Invader.numberOfInvaders = 0
        numberOfInvaders = 0

        // Create Invaders
        for (column in 0..10) {
            for (row in 0..5) {
                invaders.add(Invader(context, row, column, size.x, size.y))
                numberOfInvaders++
            }
        }

        for (i in 0 until maxLaserBeams) {
            invaderLaserBeams.add(LaserBeam(size.y))
        }

        // Generate Shelters
        numberOfBricks = 0
        for (shelterNumber in 0..4) {
            for (column in 0..18) {
                for (row in 0..8) {
                    bricks.add(
                        ShelterBrick(
                            row,
                            column,
                            shelterNumber,
                            size.x,
                            size.y
                        )
                    )

                    numberOfBricks++
                }
            }
        }
    }

    override fun run() {
        // Need to track game FPS
        var fps: Long = 0

        while (playing) {
            // Capture current time
            val startFrameTime = System.currentTimeMillis()

            // Update our frame
            if (!paused) {
                update(fps)
            }

            // Draw our new frame
            draw()

            // Calculate current FPS
            val timeThisFrame = System.currentTimeMillis() - startFrameTime
            if (timeThisFrame >= 1) {
                fps = 1000 / timeThisFrame
            }

            if (!paused && ((startFrameTime - lastMenaceTime) > menaceInterval))
                menacePlayer()
        }
    }

    private fun update(fps: Long) {
        // Update state of all of our game objects

        // Move the player's ship
        playerShip.update(fps)

        // Did an invader bump into the side of the screen
        var bumped = false

        // Has the player lost?
        var lost = false

        // Update all visible invaders
        for (invader in invaders) {
            if (invader.isVisible) {
                invader.update(fps)

                if (invader.takeAim(
                        playerShip.position.left,
                        playerShip.width,
                        wave
                    )) {
                    if (invaderLaserBeams[nextLaser].shoot(
                            invader.position.left + invader.width / 2,
                            invader.position.top, LaserBeam.down)) {
                        // Shot was fired
                        // Prepare for next shot
                        nextLaser++

                        // Loop back around if we hit max number of laserbeams
                        if (nextLaser == maxLaserBeams) {
                            nextLaser = 0
                        }
                    }
                }

                if (invader.position.left > size.x - invader.width ||
                    invader.position.left < 0) {
                    bumped = true
                }
            }
        }

        if (playerLaserBeam.isActive) {
            playerLaserBeam.update(fps)
        }

        for (laserBeam in invaderLaserBeams) {
            if (laserBeam.isActive) {
                laserBeam.update(fps)
            }
        }

        // If an invader bumped into the edge of the screen
        // Drop all the invaders down and reverse their direction
        if (bumped) {
            for (invader in invaders) {
                invader.dropDownAndReverse(wave)

                // If the invaders land then game over (player lost)
                if (invader.position.bottom >= size.y && invader.isVisible) {
                    lost = true
                }
            }
        }

        // If players's laser hits top of screen reset
        if (playerLaserBeam.position.bottom < 0) {
            playerLaserBeam.isActive = false
        }

        // Player laser beam checks
        if (playerLaserBeam.isActive) {
            // Has the player laser beam hit an invader?
            for (invader in invaders) {
                if (invader.isVisible) {
                    if (playerLaserBeam.position.intersect(invader.position)) {
                        invader.isVisible = false

                        soundPlayer.playSound(SoundPlayer.invaderExplodeID)

                        playerLaserBeam.isActive = false

                        Invader.numberOfInvaders--
                        score += 10
                        if (score > highScore) {
                            highScore = score
                        }

                        // Has the player cleared the wave?
                        if (Invader.numberOfInvaders == 0) {
                            paused = true
                            lives++
                            invaders.clear()
                            bricks.clear()
                            invaderLaserBeams.clear()
                            prepareLevel()
                            wave++
                        }

                        break
                    }
                }
            }

            for (brick in bricks) {
                if (brick.isVisible) {
                    if (playerLaserBeam.position.intersect(brick.position)) {
                        // A collision has occurred
                        playerLaserBeam.isActive = false
                        brick.isVisible = false
                        soundPlayer.playSound(SoundPlayer.damageShelterID)
                    }
                }
            }
        }

        // Has an invaders laser beam hit the bottom of the screen?
        for (laserBeam in invaderLaserBeams) {
            if (laserBeam.isActive) {
                if (laserBeam.position.top > size.y) {
                    laserBeam.isActive = false
                    break
                }

                for (brick in bricks) {
                    if (brick.isVisible) {
                        if (laserBeam.position.intersect(brick.position)) {
                            // A collision has occurred
                            laserBeam.isActive = false
                            brick.isVisible = false
                            soundPlayer.playSound(SoundPlayer.damageShelterID)
                        }
                    }
                }

                if (playerShip.position.intersect(laserBeam.position)) {
                    laserBeam.isActive = false
                    lives--
                    soundPlayer.playSound(SoundPlayer.playerExplodeID)

                    // Is it game over?
                    if (lives == 0) {
                        lost = true
                        break
                    }
                }
            }
        }

        for (invader in invaders) {
            for (brick in bricks) {
                if (invader.isVisible && brick.isVisible &&
                    invader.position.intersect(brick.position)) {
                    brick.isVisible = false
                }

            }
        }

        if (lost) {
            paused = true
            lives = 3
            score = 0
            wave = 1
            invaders.clear()
            bricks.clear()
            invaderLaserBeams.clear()
            prepareLevel()
        }
    }

    private fun draw() {
        // Verify the validity of our drawing surface
        if (holder.surface.isValid) {
            // Lock the canvas so that you can draw
            canvas = holder.lockCanvas()

            // Draw our background color
            canvas.drawColor(Color.argb(255, 0, 0, 0))

            // Choose the brush color for drawing
            paint.color = Color.argb(255, 0, 255, 0)

            // Draw all game objects

            // Draw player's spaceship
            canvas.drawBitmap(playerShip.bitmap, playerShip.position.left, playerShip.position.top, paint)

            // Draw Invaders
            for (invader in invaders) {
                if (invader.isVisible) {
                    if (uhIfTrueOhIfFalse) {
                        canvas.drawBitmap(Invader.bitmap1!!, invader.position.left, invader.position.top, paint)
                    } else {
                        canvas.drawBitmap(Invader.bitmap2!!, invader.position.left, invader.position.top, paint)
                    }
                }
            }

            // Draw our shelter bricks
            for (brick in bricks) {
                if (brick.isVisible) {
                    canvas.drawRect(brick.position, paint)
                }
            }

            if (playerLaserBeam.isActive) {
                canvas.drawRect(playerLaserBeam.position, paint)
            }

            for (laserBeam in invaderLaserBeams) {
                if (laserBeam.isActive) {
                    canvas.drawRect(laserBeam.position, paint)
                }
            }

            // Change Brush color
            paint.color = Color.argb(255,255,255,255)

            // Draw Score and Remaining Lives
            paint.textSize = 50f
            canvas.drawText("Score: $score    Lives: $lives    Wave: $wave HI: $highScore", 20f, 75f, paint)


            // Draw new update canvas to the screen
            holder.unlockCanvasAndPost(canvas)
        }
    }


    // If our activity is started then start the UI thread
    fun resume() {
        playing = true
        prepareLevel()
        gameThread.start()
    }

    // If our activity is paused or stopped then we need to shut down our thread
    fun pause() {
        playing = false

        val prefs = context.getSharedPreferences(
            "Kotlin Invaders",
            Context.MODE_PRIVATE)

        val oldHighScore = prefs.getInt("highScore", 0)

        if(highScore > oldHighScore) {
            val editor = prefs.edit()

            editor.putInt(
                "highScore", highScore)

            editor.apply()
        }

        try {
            gameThread.join()
        } catch (e: InterruptedException) {
            Log.e("Error:", "joining thread")
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {
                paused = false

                if (event.y > size.y - size.y / 8) {
                    if (event.x > size.x / 2) {
                        playerShip.moving = PlayerShip.right
                    } else {
                        playerShip.moving = PlayerShip.left
                    }
                }

                if (event.y < size.y - size.y / 8) {
                    if (playerLaserBeam.shoot(
                            playerShip.position.left + playerShip.width / 2f,
                            playerShip.position.top,
                            LaserBeam.up
                        )) {
                        soundPlayer.playSound(SoundPlayer.shootID)
                    }
                }
            }
        }

        return true
    }

    private fun menacePlayer() {
        if (uhIfTrueOhIfFalse) {
            soundPlayer.playSound(SoundPlayer.uhID)
        } else {
            soundPlayer.playSound(SoundPlayer.ohID)
        }

        lastMenaceTime = System.currentTimeMillis()
        uhIfTrueOhIfFalse = !uhIfTrueOhIfFalse
    }

}
