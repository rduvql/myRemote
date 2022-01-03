package com.example.mqttkotlinsample

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.slidingpanelayout.widget.SlidingPaneLayout

class MainActivity : AppCompatActivity(R.layout.activity_main), SlidingPaneLayout.PanelSlideListener {

    val BACK_DELAY = 2000
    var lastBackPress = System.currentTimeMillis()

    lateinit var slidingPaneLayout: SlidingPaneLayout;

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("TAG", "MainActivity.onCreate")

        slidingPaneLayout = findViewById(R.id.sliding_pane_layout)
        slidingPaneLayout.setPanelSlideListener(this)

//        setShowWhenLocked(true)
//        setTurnScreenOn(true)
    }

    override fun onResume() {
        super.onResume()
        Log.d("TAG", "MainActivity.onResume")
    }

    override fun onBackPressed() {
        if(slidingPaneLayout.isOpen) {
            slidingPaneLayout.closePane()
            return
        }

        if(lastBackPress + BACK_DELAY > System.currentTimeMillis()) {
            application.onTerminate()
        } else {
            Toast.makeText(baseContext, "Press once again to exit!", Toast.LENGTH_SHORT).show();
        }

        lastBackPress = System.currentTimeMillis()

        // default
//        super.onBackPressed()
    }

    override fun onPanelOpened(panel: View) {
//        Log.d("TAG", "opened")
    }

    override fun onPanelClosed(panel: View) {
//        Log.d("TAG", "closed")
    }

    override fun onPanelSlide(panel: View, slideOffset: Float) {
    }
}