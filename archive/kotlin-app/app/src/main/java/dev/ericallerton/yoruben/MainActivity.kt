package dev.ericallerton.yoruben

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.ericallerton.yoruben.core.CosmeticSlot
import dev.ericallerton.yoruben.core.Cosmetics
import dev.ericallerton.yoruben.ui.YorubenRoot
import dev.ericallerton.yoruben.ui.theme.YorubenTheme

class MainActivity : ComponentActivity() {
    private var startRoute by mutableStateOf("lessons")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) {
            startRoute = routeFor(intent?.data)
        }
        setContent {
            val ledger by (application as YorubenApp).game.observe().collectAsState(Cosmetics.starter())
            YorubenTheme(themeId = ledger.equipped(CosmeticSlot.THEME)) {
                YorubenRoot(startRoute = startRoute)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        startRoute = routeFor(intent.data)
    }

    companion object {
        fun routeFor(uri: Uri?): String {
            if (uri == null || uri.host != "open") return "lessons"
            val parts = uri.pathSegments
            return when (parts.firstOrNull()) {
                "lesson" -> parts.getOrNull(1)?.let { "lesson/$it" } ?: "lessons"
                "quiz" -> "quiz/${parts.getOrNull(1) ?: "mix"}"
                "practice", "progress", "prep", "lessons" -> parts.first()
                "map", "village", "cave", "shop", "wardrobe", "songs" -> parts.first()
                "drill" -> "drill/${parts.getOrNull(1) ?: "group1"}"
                "boss" -> "drill/boss"
                "run" -> if (parts.size > 1) "run/${parts[1]}" else "run"
                else -> "lessons"
            }
        }
    }
}
