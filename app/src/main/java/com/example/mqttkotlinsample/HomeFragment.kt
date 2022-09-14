package com.example.mqttkotlinsample

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
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import kotlin.collections.ArrayList

class HomeFragment : Fragment(R.layout.fragment_home), EspViewAdapter.EventsListener {

    enum class Tab(val room: String) {
        _ALL("all"),
        MAINROOM("mainroom"),
        LAUNDRYROOM("laundryroom"),
        BEDROOM("bedroom"),
        BATHROOM("bathroom")
    }

    private val serverURI = "tcp://192.168.0.11:1883"
    private val clientID = "android"

    private lateinit var mqttClient: MqttAndroidClient

    private var currentTab = Tab.MAINROOM

    private lateinit var colorPicker: AlertDialog

    private lateinit var tabLayout: TabLayout
    private lateinit var roomColorButton: ImageButton

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView

    private val rooms = HashMap<String, Room>();

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

        rooms[Tab.MAINROOM.room] = Room();
        rooms[Tab.LAUNDRYROOM.room] = Room();
        rooms[Tab.BEDROOM.room] = Room();
        rooms[Tab.BATHROOM.room] = Room();

        mqttClient = MqttAndroidClient(context, serverURI, clientID)

        tabLayout = view.findViewById(R.id.tab_layout)
        roomColorButton = view.findViewById(R.id.room_color_button)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        recyclerView = view.findViewById(R.id.recycler_view)

        tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab?) {
                Log.d("TAG", "${tab?.position}");

                when (tab?.position) {
                    0 -> currentTab = Tab._ALL
                    1 -> currentTab = Tab.MAINROOM
                    2 -> currentTab = Tab.LAUNDRYROOM
                    3 -> currentTab = Tab.BEDROOM
                    4 -> currentTab = Tab.BATHROOM
                }

                updateRecyclerViewData(currentTab)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })

        var previousSelectedColor = -1;
        roomColorButton.setOnClickListener {
            colorPicker = ColorPickerDialogBuilder
                .with(context)
                .setTitle("Choose color")
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(6)
                .lightnessSliderOnly()
                .setOnColorSelectedListener { selectedColor ->
                    Log.d("TAG", Integer.toHexString(selectedColor))

                    roomColorButton.setColorFilter(selectedColor);
                    if(previousSelectedColor == selectedColor) {
                        colorPicker.hide()
                    }
                    previousSelectedColor = selectedColor
                }
                .build()

            colorPicker.show()
        }

        swipeRefreshLayout.setOnRefreshListener {
            rooms.forEach { (_, room) -> room.esps.forEach { esp -> esp.setIsNotAlive() } }
            updateRecyclerViewData(currentTab)
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

                Toast.makeText(context, "connected", Toast.LENGTH_SHORT).show()

                // each subscribe is synchronous and wait for others to finish
                mqttClient.subscribe("pong/esp/led/+/+", 0) { topic, message ->

                    val (_, _, _, roomTopic, id) = topic.split("/")

                    var espLed = rooms[roomTopic]?.esps?.find { it.id == id.toInt() }
                    if (espLed == null) {
                        espLed = Esp(id.toInt(), "esp - $id", "")
                        rooms[roomTopic]?.esps?.add(espLed)
                    }
                    espLed.setIsAlive()
                    espLed.defineAsLed()

                    updateRecyclerViewData(currentTab)
                }

                // each subscribe is synchronous and wait for others to finish
                mqttClient.subscribe("pong/esp/dht/+/+", 0) { topic, message ->

                    val (_, _, _, roomTopic, id) = topic.split("/")

                    var espDHT = rooms[roomTopic]?.esps?.find { it.id == id.toInt() }
                    if (espDHT == null) {
                        espDHT = Esp(id.toInt(), "esp - $id", "")
                        rooms[roomTopic]?.esps?.add(espDHT)
                    }
                    espDHT.setIsAlive()
                    espDHT.defineAsDHT()

                    updateRecyclerViewData(currentTab)
                }

                mqttClient.subscribe("esp/dht/+/+", 0) { topic, message ->

                    val (_, _, id, tempHumidity) = topic.split("/")

                    Log.d("TAG", "${id}: ${tempHumidity} = ${String(message.payload)}")
                }

                mqttClient.publish("ping/esp", MqttMessage())
            }


            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Toast.makeText(context, exception?.message, Toast.LENGTH_SHORT).show()
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
        rooms.forEach { (_, room) -> room.esps.forEach { esp -> esp.setIsNotAlive() } }
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

    private fun updateRecyclerViewData(tab: Tab) {
        displayedEsps.clear()
        when(tab) {
            Tab._ALL -> displayedEsps.addAll(rooms.flatMap { (_,v) -> v.esps });
            else -> displayedEsps.addAll(rooms[tab.room]!!.esps)
        }
        displayedEsps.sortBy { espViewModel -> espViewModel.id }
        Handler(Looper.getMainLooper()).post {
            customAdapter.notifyDataSetChanged()
        }
    }
}
