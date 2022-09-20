package com.example.mqttkotlinsample

import android.graphics.Color.GREEN
import android.graphics.Color.RED
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import kotlin.collections.ArrayList

class HomeFragment : Fragment(R.layout.fragment_home), EspViewAdapter.EventsListener {

//    : Thread.UncaughtExceptionHandler
//    override fun uncaughtException(p0: Thread, p1: Throwable) {
//        Log.d("TAG", "mqtt exception")
//    }

    private val serverURI = "tcp://192.168.0.11:1883"
    private val clientID = "android"

    private lateinit var mqttClient: MqttAndroidClient

    private lateinit var colorPicker: AlertDialog

    private lateinit var dotServerAliveImage: ImageButton
    private lateinit var globalColorButton: ImageButton
    private lateinit var globalLedOnButton: ImageButton
    private lateinit var globalLedOffButton: ImageButton

    private lateinit var mediaVolumeDownBtn: ImageButton
    private lateinit var mediaVolumeUpBtn: ImageButton
    private lateinit var mediaPreviousButton: ImageButton
    private lateinit var mediaPlayPauseButton: ImageButton
    private lateinit var mediaNextButton: ImageButton

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView

    private val esps = ArrayList<Esp>()

    private val displayedEsps = ArrayList<Esp>()
    private val customAdapter = EspViewAdapter(displayedEsps, this)

    //
    // Lifecycle
    //

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d("TAG", "ClientFragment.onCreateView")
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("TAG", "ClientFragment.onViewCreated")

        mqttClient = MqttAndroidClient(context, serverURI, clientID)

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        recyclerView = view.findViewById(R.id.recycler_view)

        dotServerAliveImage = view.findViewById(R.id.dot_server_alive)
        globalColorButton = view.findViewById(R.id.toggle_color_btn_g)
        globalLedOffButton = view.findViewById(R.id.toggle_off_btn_g)
        globalLedOnButton = view.findViewById(R.id.toggle_on_btn_g)

        mediaVolumeDownBtn = view.findViewById(R.id.toggle_volume_down)
        mediaVolumeUpBtn = view.findViewById(R.id.toggle_volume_up)
        mediaPreviousButton = view.findViewById(R.id.toggle_media_previous)
        mediaPlayPauseButton = view.findViewById(R.id.toggle_media_play_pause)
        mediaNextButton = view.findViewById(R.id.toggle_media_next)

        mediaVolumeDownBtn.setOnClickListener {
            mqttClient.publish("msi/media/vol_down", MqttMessage())
        }
        mediaVolumeUpBtn.setOnClickListener {
            mqttClient.publish("msi/media/vol_up", MqttMessage())
        }
        mediaPreviousButton.setOnClickListener {
            mqttClient.publish("msi/media/prev", MqttMessage())
        }
        mediaPlayPauseButton.setOnClickListener {
            mqttClient.publish("msi/media/play_pause", MqttMessage())
        }
        mediaNextButton.setOnClickListener {
            mqttClient.publish("msi/media/next", MqttMessage())
        }

        updateRecyclerViewData()

        var previousSelectedColor = -1;
        globalColorButton.setOnClickListener {
            colorPicker = ColorPickerDialogBuilder
                .with(context)
                .setTitle("Choose color")
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(6)
                .lightnessSliderOnly()
                .setOnColorSelectedListener { selectedColor ->
                    Log.d("TAG", Integer.toHexString(selectedColor))

                    globalColorButton.setColorFilter(selectedColor);
                    if(previousSelectedColor == selectedColor) {
                        colorPicker.hide()
                    }
                    previousSelectedColor = selectedColor
                }
                .build()

            colorPicker.show()
        }

        swipeRefreshLayout.setOnRefreshListener {
            esps.forEach { esp -> esp.setIsNotAlive() }
            updateRecyclerViewData()
            mqttClient.publish("ping/esp", MqttMessage())
            swipeRefreshLayout.isRefreshing = false
        }

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = customAdapter
    }

    override fun onStart() {
        super.onStart()
        Log.d("TAG", "ClientFragment.onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d("TAG", "ClientFragment.onResume")

        mqttClient.connect(null, object : IMqttActionListener {

            override fun onSuccess(asyncActionToken: IMqttToken?) {

//                Toast.makeText(context, "connected", Toast.LENGTH_SHORT).show()
                dotServerAliveImage.setColorFilter(GREEN)

                // each subscribe is synchronous and wait for others to finish
                mqttClient.subscribe("pong/esp/led/+/+", 0) { topic, message ->

                    val (_, _, _, roomTopic, id) = topic.split("/")

                    var espLed = esps.find { it.id == id.toInt() }
                    if (espLed == null) {
                        espLed = Esp(id.toInt(), "esp - $id", "")
                        esps.add(espLed)
                    }
                    espLed.setIsAlive()
                    espLed.defineAsLed()

                    updateRecyclerViewData()
                }

                // each subscribe is synchronous and wait for others to finish
                mqttClient.subscribe("pong/esp/dht/+/+", 0) { topic, message ->

                    val (_, _, _, roomTopic, id) = topic.split("/")

                    var espDHT = esps.find { it.id == id.toInt() }
                    if (espDHT == null) {
                        espDHT = Esp(id.toInt(), "esp - $id", "")
                        esps?.add(espDHT)
                    }
                    espDHT.setIsAlive()
                    espDHT.defineAsDHT()

                    updateRecyclerViewData()
                }

                mqttClient.subscribe("esp/dht/+/+", 0) { topic, message ->

                    val (_, _, id, tempHumidity) = topic.split("/")

                    Log.d("TAG", "${id}: ${tempHumidity} = ${String(message.payload)}")
                }

                mqttClient.publish("ping/esp", MqttMessage())
            }


            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
//                Toast.makeText(context, exception?.message, Toast.LENGTH_SHORT).show()
                dotServerAliveImage.setColorFilter(RED)
            }
        })
    }

    override fun onPause() {
        Log.d("TAG", "ClientFragment.onPause")
        super.onPause()
    }

    override fun onStop() {
        Log.d("TAG", "ClientFragment.onStop")
        mqttClient.disconnect()
        esps.forEach { esp -> esp.setIsNotAlive() }
        super.onStop()
    }

    //
    // EVENTS
    //

    override fun onEspClicked(esp: Esp) {
        Log.d("TAG", "onEspClicked")

//        if(esp.isDHT()) {
//            Log.d("TAG", "[onEspClicked] sending temp request to ${esp.id}")
//            mqttClient.publish("esp/dht/${esp.id}/temp", MqttMessage())
//        }
    }

    override fun onLedOnClicked(esp: Esp) {
        Log.d("TAG", "onLedOnClicked")
        if(esp.isLedOn) {
            when(esp.ledBrightness) {
                16 -> esp.ledBrightness = 32
                32 -> esp.ledBrightness = 64
                64 -> esp.ledBrightness = 128
                128 -> esp.ledBrightness = 255
                255 -> esp.ledBrightness = 16
            }
            mqttClient.publish("esp/led/${esp.id}/brightness", MqttMessage("${esp.ledBrightness}".toByteArray()))
        } else {
            esp.isLedOn = true
            mqttClient.publish("esp/led/${esp.id}/on", MqttMessage())
        }
    }

    override fun onLedOffClicked(esp: Esp) {
        Log.d("TAG", "onLedOffClicked")
        esp.isLedOn = false
        mqttClient.publish("esp/led/${esp.id}/off", MqttMessage())
    }

    override fun onLedColorSelected(esp: Esp, selectedColor: Int) {
        Log.d("TAG", "onEspColorSelected")

        val hexColor = Integer.toHexString(selectedColor).substring(2)
        Log.d("TAG", hexColor)

        mqttClient.publish("esp/led/${esp.id}/color", MqttMessage(hexColor.toByteArray()))
    }

    //
    // UTILS
    //

    private fun updateRecyclerViewData() {
        displayedEsps.clear()
        displayedEsps.addAll(esps);
        displayedEsps.sortBy { espViewModel -> espViewModel.id }
        Handler(Looper.getMainLooper()).post {
            customAdapter.notifyDataSetChanged()
        }
    }
}
