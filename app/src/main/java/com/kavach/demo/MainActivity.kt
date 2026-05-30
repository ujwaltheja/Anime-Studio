package com.kavach.demo

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.kavach.engine.KavachEngine
import com.kavach.ui.GameSurfaceView

class MainActivity : AppCompatActivity() {

    private lateinit var surfaceView: GameSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        surfaceView = GameSurfaceView(this)
        setContentView(surfaceView)

        val engine = KavachEngine.create()
        val game   = DemoGame(this)
        surfaceView.attach(engine, game)
    }

    override fun onResume() {
        super.onResume()
        surfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        surfaceView.onPause()
    }
}
