package cn.xzbim.stoneai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cn.xzbim.stoneai.navigation.AppNavHost
import cn.xzbim.stoneai.ui.theme.StoneAITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StoneAITheme {
                AppNavHost()
            }
        }
    }
}
