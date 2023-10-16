package info.smt.android.h12signal

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val intent = Intent()
        intent.setClassName("info.smt.android.h12signal", "info.smt.android.h12signal.SignalService")
        startForegroundService(intent)
    }
}