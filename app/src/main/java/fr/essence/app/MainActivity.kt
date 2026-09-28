package fr.essence.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import fr.essence.app.ui.StationsScreen
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Les serveurs de tuiles OpenStreetMap exigent un User-Agent identifiant l'appli.
        Configuration.getInstance().userAgentValue = packageName
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                StationsScreen()
            }
        }
    }
}
