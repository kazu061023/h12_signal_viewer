/**
 * SignalService
 *
 *
 * @copyright Copyright (c) 2023 Toko Tekko Co.,Ltd. All Rights Reserved.
 * @author Kazumasa Echizenya
 */

package info.smt.android.h12signal

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.experimental.xor

class SignalService : Service(), CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    private var serialHelper: MainSerialHelper? = null

    private lateinit var channel: NotificationChannel
    private lateinit var notificationManager: NotificationManager

    private var notification: Notification? = null

    private var signal: Int = 0

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = getString(R.string.notification_channel_description)
        }

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        updateNotification()
        startForeground(10001, notification)

        startSignalHelper()

        return START_STICKY
    }


    private fun updateNotification() {
        signal / 5

        val resources = when (signal) {
            in 80..100 -> R.drawable.rc_signal_lv5
            in 60..79 -> R.drawable.rc_signal_lv4
            in 40..59-> R.drawable.rc_signal_lv3
            in 20..39 -> R.drawable.rc_signal_lv2
            else -> R.drawable.rc_signal_lv1
        }

        notification = NotificationCompat.Builder(this, CHANNEL_ID).apply {
            setSound(null)
            setContentTitle("H12 Signal")
            setContentText("Signal $signal%")
            setSmallIcon(
                IconCompat.createWithResource(this@SignalService, resources)
            )
        }.build()

        notificationManager.notify(10001, notification)
    }


    private fun startSignalHelper() {
        serialHelper = MainSerialHelper.getInstance(
            "/dev/ttyHS1",
            921600
        )
        serialHelper?.open()

        serialHelper?.onDataReceived = {
            try {
                if (it.size > PACKAGE_HEADER_BUF.size) {
                    when(it[PACKAGE_HEADER_BUF.size]){
                        0xB0.toByte()->{
                            val functionIndex = PACKAGE_HEADER_BUF.size
                            val length = it[functionIndex + 1]
                            if(it.size > (functionIndex + 1 + length + 1)){
                                val hundred = it[functionIndex + 1 + 1].toChar()
                                val ten = it[functionIndex + 1 + 2].toChar()
                                val individual  = it[functionIndex + 1 + 3].toChar()

                                val s = hundred.toString() + ten.toString() + individual.toString()

                                try {
                                    signal = s.toInt()
                                    updateNotification()
                                } catch (e: NumberFormatException) { }

                            }
                        }
                    }
                }
            } catch (e: ArrayIndexOutOfBoundsException) {

            }

        }

        launch(coroutineContext) {
            while (MainSerialHelper.isOpen) {
                delay(1000)

                val cmd = ByteArray(PACKAGE_HEADER_BUF.size + 8)
                var count = PACKAGE_HEADER_BUF.size
                System.arraycopy(PACKAGE_HEADER_BUF,0, cmd, 0, count)

                cmd[count++] = 0xB0.toByte()
                cmd[count++] = 2.toByte()
                cmd[count++] = 'R'.toByte()
                cmd[count++] = 1.toByte()
                cmd[count] = getBCC(cmd)

                serialHelper?.serialPortDataSend(cmd)
            }
        }
    }

    private fun getBCC(data: ByteArray): Byte {
        var temp = data[0]
        for (i in 1 until data.size - 1) {
            temp = temp xor data[i]
        }
        return temp
    }

    companion object {
        private const val CHANNEL_ID = "channel_h12_notification_signal"

        val PACKAGE_HEADER_BUF = "fengyingdianzi:".toByteArray()
    }
}