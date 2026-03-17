package com.example.fgclockwidget


import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import com.github.lalyos.jfiglet.FigletFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first


class FgClockWidget : GlanceAppWidget() {

    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(context: Context, id: GlanceId) {

        //datastore定義　1度読み取ってflow終了
        val store = context.dataStore
        val initialPrefs = store.data.first()

        provideContent {
            //dataをstateとして監視　初期値はfirstで読み取ったものを使用
            val prefs by store.data.collectAsState(initial = initialPrefs)
            //state内のFONTキーをもつデータをfontとして使用
            val font = prefs[WidgetSettings.FONT] ?: "alligator2.flf"
            //時刻を取得
            val timeString = prefs[WidgetSettings.LAST_UPDATE_TIME] ?: "--:--"

            val textColor = prefs[WidgetSettings.TEXT_COLOR] ?: Color.White.toArgb()
            GlanceTheme {
                val date = java.time.LocalDate.now().toString()
                //時刻を描画
                val timef = FigletCache.render(context, font, timeString)
                Box(
                    GlanceModifier
                        .fillMaxSize().cornerRadius(0.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = GlanceModifier.cornerRadius(0.dp).padding(16.dp)
                    ) {
                        GlanceText(timef, R.font.cascadiamono, 10.sp, color = Color(textColor))
                        GlanceText(date, R.font.cascadiamono, 14.sp, color = Color(textColor))
                    }
                }
            }
        }
    }
}

//レシーバーが受信するアクションを定義する関数
class FgClockWidgetReceiver : GlanceAppWidgetReceiver() {
    //動かすウィジェットを定義
    override val glanceAppWidget = FgClockWidget()

    //シングルトン
    companion object {
        //被らないように長い合言葉を設定
        private const val ACTION_UPDATE_TICK = "com.example.fgclockwidget.ACTION_UPDATE_TICK"

        //scheduleNextUpdate関数
        fun scheduleNextUpdate(context: Context) {

            //アラームを動作させるためのサービスのコンテキストを取得
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            //現在のcontextを付与して、宛先クラスを指定したインテントを作成
            val intent = Intent(context, FgClockWidgetReceiver::class.java).apply {
                //アクション名を指定
                action = ACTION_UPDATE_TICK
            }

            //上記のインテントを実行する保留インテント作成
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                //上書き許可、不変化(必須)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            //現在時刻（通算ミリ秒）を取得
            val now = System.currentTimeMillis()
            //現在時刻を元にカレンダーのインスタンスを用意
            val calendar = Calendar.getInstance().apply {
                //現在時刻
                timeInMillis = now
                //秒をリセット
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                //そこに1分追加
                add(Calendar.MINUTE, 1)
            }

            //トリガー時刻を決定　現在時刻+1分
            val triggerAt = calendar.timeInMillis

            //トライキャッチ　アラームをセット
            try {
                //アラームマネージャーに正確なアラームを要求
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            } catch (e: SecurityException) {
                //権限的に不可の場合は正確さを犠牲にする
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            }
        }
    }

    //受信したとき
    override fun onReceive(context: Context, intent: Intent) {
        //基本処理を先に実行
        super.onReceive(context, intent)

        // ログを出してタイミングを確認できるようにする
        android.util.Log.d(
            "FgClock",
            "onReceive: ${intent.action} ID: ${System.identityHashCode(intent)}"
        )
        //intentのアクションがどれかなら
        if (intent.action == ACTION_UPDATE_TICK ||
            intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.appwidget.action.APPWIDGET_UPDATE"
        ) {
            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                //datastoreを編集
                context.dataStore.edit { prefs ->
                    val now = java.time.LocalTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                    //datastore内の時刻を現在時刻に編集
                    prefs[WidgetSettings.LAST_UPDATE_TIME] = now
                }
                //一応更新
                glanceAppWidget.updateAll(context)
            }
            //次回アラームをセット
            scheduleNextUpdate(context)


            // 非同期で更新をかける
            /*kotlinx.coroutines.MainScope().launch {
                //アップデート
                FgClockWidget().updateAll(context)
                // 更新が終わってから次のアラームをセット
                scheduleNextUpdate(context)
            }*/
        }
    }

    //設置されたときに
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleNextUpdate(context)
        // 配置された瞬間に最初の更新をキックする
        /*kotlinx.coroutines.MainScope().launch {
            //更新
            FgClockWidget().updateAll(context)
            // 更新が終わってから次のアラームをセット
            scheduleNextUpdate(context)
        }*/
        kotlinx.coroutines.runBlocking(Dispatchers.IO) {
            //datastoreを編集
            context.dataStore.edit { prefs ->
                val now = java.time.LocalTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                //datastore内の時刻を現在時刻に編集
                prefs[WidgetSettings.LAST_UPDATE_TIME] = now
            }
        }
    }
}

//figletを取得
/*fun getFigletTime(context: Context, font: String): String {
    //時刻を取得
    val time = java.time.LocalTime.now()
    val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
    val timeString = time.format(formatter)


    //時刻をfigletの文字列にして返す
    return FigletCache.render(context, font, timeString)
}*/


//figletのキャッシュ
/*object FigletCache {
    @Volatile
    private var fontData: ByteArray? = null
    fun render(context: Context, font: String, text: String): String {
        //フォントデータを格納
        if (fontData == null) {
            fontData = context.assets.open(font).readBytes()
        }
        //fontData -> inputStream
        val stream = fontData!!.inputStream()
        //streamと文字列を指定してfigletを返す
        return FigletFont.convertOneLine(stream, text)
            .replace("\r", "")
    }
}*/

object FigletCache {
    //hashmap (高速なmapみたいなもん)
    private val fontDataMap = java.util.concurrent.ConcurrentHashMap<String, ByteArray>()

    fun render(context: Context, font: String, text: String): String {
        //あるなら取り出すないなら追加
        val data = fontDataMap.getOrPut(font) {
            context.assets.open(font).readBytes()
        }
        return FigletFont.convertOneLine(data.inputStream(), text)
            .replace("\r", "")
    }
}


