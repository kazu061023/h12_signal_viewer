/**
 * MAVSerialHelper
 *
 *
 * @copyright Copyright (c) 2023 Toko Tekko Co.,Ltd. All Rights Reserved.
 * @author Kazumasa Echizenya
 */

package info.smt.android.h12signal

import android.system.ErrnoException
import kotlinx.coroutines.*
import tp.xmaihh.serialport.SerialHelper
import tp.xmaihh.serialport.bean.ComBean
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import kotlin.coroutines.CoroutineContext

class MainSerialHelper private constructor(serialPort: String,
                                           baudRate: Int): CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    private val serialHelper = object : SerialHelper(serialPort, baudRate) {
        override fun onDataReceived(comBean: ComBean) {
            launch(coroutineContext) { onDataReceived?.invoke(comBean.bRec) }
        }
    }

    var onDataReceived: ((ByteArray) -> Unit)? = null

    /**
     * シリアルポートにデータを送信する
     */
    suspend fun serialPortDataSend(data: ByteArray) {
        withContext(Dispatchers.IO) {
            try {
                if (data.isNotEmpty()) if (serialHelper.isOpen) serialHelper.send(data)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * オープン処理
     */
    fun open() {
        if (!serialHelper.isOpen) {
            isOpen = true
            serialHelper.open()
        }
    }

    /**
     * クローズ処理
     */
    private fun close() {
        if (serialHelper.isOpen) serialHelper.close()
        isOpen = false
    }


    companion object {
        private var instance: MainSerialHelper? = null

        var isOpen = false
            private set

        /**
         * MainSerialHelperのインスタンスを取得
         *
         * @param serialPort
         * @param baudRate
         *
         * @return MAVSerialHelper
         */
        fun getInstance(
            serialPort: String = "/dev/ttyHS0",
            baudRate: Int = 115200,
        ) = instance ?: synchronized(this) {
            instance ?: MainSerialHelper(serialPort, baudRate).also { instance = it }
        }

        fun open() {
            if (instance != null) instance?.open()
        }

        /**
         * MainSerialHelperの停止処理
         */
        fun destroy() {
            if (instance != null) {
                instance?.close()
                instance = null
            }
        }
    }
}