package pl.cheapskountant.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import pl.cheapskountant.mobile.ui.navigation.CheapskountantNavHost
import pl.cheapskountant.mobile.ui.theme.CheapskountantTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CheapskountantApp).container
        setContent {
            CheapskountantTheme {
                CheapskountantNavHost(container)
            }
        }
    }
}
