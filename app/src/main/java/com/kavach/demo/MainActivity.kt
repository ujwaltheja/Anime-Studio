package com.kavach.demo

import android.os.Bundle
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView
import com.kavach.demo.citygame.CityBuilderGame
import com.kavach.demo.citygame.CityHUD
import com.kavach.demo.citygame.CityHUDState
import com.kavach.engine.KavachEngine
import com.kavach.ui.GameSurfaceView

class MainActivity : ComponentActivity() {

    private lateinit var surfaceView: GameSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Root layout: GL surface below, Compose HUD on top
        val root = FrameLayout(this)

        surfaceView = GameSurfaceView(this)
        root.addView(surfaceView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)

        val hudState = CityHUDState()

        val hudView = ComposeView(this).apply {
            setContent {
                CityHUD(
                    state              = hudState,
                    onBuildingSelected = { type -> hudState.selectedBuilding = type }
                )
            }
        }
        root.addView(hudView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)

        setContentView(root)

        val engine = KavachEngine.create()
        val game   = CityBuilderGame(this, hudState)
        surfaceView.attach(engine, game)
    }

    override fun onResume() { super.onResume(); surfaceView.onResume() }
    override fun onPause()  { super.onPause();  surfaceView.onPause()  }
}
