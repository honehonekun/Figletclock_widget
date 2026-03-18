package com.example.fgclockwidget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fgclockwidget.WidgetSettings.fontList
import com.example.fgclockwidget.ui.theme.FgclockwidgetTheme
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FgclockwidgetTheme {
                Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
                    CenterAlignedTopAppBar({
                        Image(
                            painterResource(R.drawable.ascii_art_text_removebg_preview),
                            contentDescription = "Figlet Clock"
                        )
                    }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black))
                }) { innerPadding ->
                    FigletClockApp(
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


@Composable
fun FigletClockApp(context: Context, modifier: Modifier = Modifier) {
    var showAlarmDialog by remember { mutableStateOf(false) }
    var showBatteryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!isExactAlarmPermissionGranted(context)) {
            showAlarmDialog = true
        }
        if (!isIgnoringBatteryOptimizations(context)) {
            showBatteryDialog = true
        }
    }

    if (showAlarmDialog) {
        AlertDialog(
            onDismissRequest = { showAlarmDialog = false },
            title = { Text("Enable Exact Alarm") },
            text = { Text("To ensure your clock is updated accurately, please allow ”Alarm and Reminder Settings\" in the Settings menu.") },
            confirmButton = {
                TextButton(onClick = {
                    showAlarmDialog = false
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                }) { Text("Open Setting") }
            },
            dismissButton = {
                TextButton(onClick = { showAlarmDialog = false }) { Text("Cancel") }
            }
        )
    }
    if (showBatteryDialog) {
        AlertDialog(
            onDismissRequest = { showBatteryDialog = false },
            title = { Text("Disable Battery Restrictions") },
            text = { Text("To ensure the clock continues to update in the background, we recommend setting battery optimization to “No restrictions.”") },
            confirmButton = {
                TextButton(onClick = {
                    showBatteryDialog = false
                    val intent =
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    context.startActivity(intent)
                }) { Text("Open Setting") }
            },
            dismissButton = {
                TextButton(onClick = { showBatteryDialog = false }) { Text("Cancel") }
            }
        )
    }


    var color: Color by remember { mutableStateOf(Color.White) }
    val controller = rememberColorPickerController()
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        verticalArrangement =Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {

        Card(border = BorderStroke(2.dp, color)) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                HsvColorPicker(
                    modifier = Modifier.size(200.dp),
                    controller = controller,
                    onColorChanged = { value ->
                        color = value.color
                    }
                )
                Spacer(Modifier.height(16.dp))
                BrightnessSlider(
                    //明暗スライダー
                    modifier = Modifier
                        .width(256.dp)
                        .height(16.dp),
                    controller = controller,
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        scope.launch {
                            setColor(context, color)
                        }
                    }, shape = RoundedCornerShape(8.dp), border = BorderStroke(2.dp,color),
                    modifier = Modifier.width(256.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(painterResource(R.drawable.setcolor), contentDescription = "Set Color",)
                    }
                }
            }

        }
        Spacer(Modifier.height(24.dp))
        Box(modifier = Modifier.padding(16.dp)) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.width(256.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier,
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                ) {
                    Spacer(Modifier.height(8.dp))
                    Icon(painterResource(R.drawable.choosefont2), contentDescription = "Choose Font",)
                }
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



