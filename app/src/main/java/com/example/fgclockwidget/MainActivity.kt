package com.example.fgclockwidget

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.appwidget.updateAll
import com.example.fgclockwidget.WidgetSettings.fontList
import com.example.fgclockwidget.ui.theme.FgclockwidgetTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FgclockwidgetTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        context = applicationContext
                    )
                }
            }
        }
    }
}

//拡張プロパティ　datastore をインスタンス化　by デリゲートによってシングルトン化
val Context.dataStore by preferencesDataStore(name = "widget_settings")

object WidgetSettings {
    //キー設定
    val FONT = stringPreferencesKey("font")
    val LAST_UPDATE_TIME = stringPreferencesKey("last_update_time") // 追加
    val fontList = listOf(
        "alligator.flf",
        "alligator2.flf",
        "banner3d.flf",
        "graffiti.flf",
        "Rebel.flf",
        "BlurVisionASCII.flf",
        "ANSIShadow.flf",
        "Nipples.flf"
    )
}

//font設定
suspend fun setFont(context: Context, font: String) {

    context.dataStore.edit { prefs ->
        prefs[WidgetSettings.FONT] = font
        //一応時刻も更新
        val now =
            java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
        prefs[WidgetSettings.LAST_UPDATE_TIME] = now
    }
    FgClockWidget().updateAll(context)
}

@Composable
fun Greeting(context: Context, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Button(onClick = { expanded = true }) {
                Text("フォントを選択")
            }

            // DropdownMenu はトリガーとなる Button と同じ Box内に入れる
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                fontList.forEach { font ->
                    DropdownMenuItem(
                        text = { Text(font) },
                        onClick = {
                            expanded = false
                            scope.launch {
                                setFont(context, font)
                            }
                        }
                    )
                }
            }
        }
    }

}



