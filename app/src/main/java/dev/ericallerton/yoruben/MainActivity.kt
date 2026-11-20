package dev.ericallerton.yoruben

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ericallerton.yoruben.anki.AnkiClient
import dev.ericallerton.yoruben.ui.AppModel
import dev.ericallerton.yoruben.ui.StudyRoot
import dev.ericallerton.yoruben.ui.YorubenTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val model: AppModel = viewModel()
            val permission = androidx.activity.compose.rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { granted -> model.onPermission(granted) }
            LaunchedEffect(model.askPermission) {
                if (model.askPermission) {
                    model.askPermission = false
                    permission.launch(AnkiClient.PERMISSION)
                }
            }
            YorubenTheme { StudyRoot(model) }
        }
    }
}
