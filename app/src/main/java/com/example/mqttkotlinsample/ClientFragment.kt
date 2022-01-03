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
import android.widget.Switch
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import kotlinx.coroutines.delay
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.util.HashMap

enum class Tab(val room: String) {
    MAINROOM("mainroom"),
    LAUNDRYROOM("laundryroom"),
    BEDROOM("bedroom"),
    BATHROOM("bathroom")
}

class Room(
    var esps: ArrayList<Esp> = ArrayList()
) {
    fun turnAllOn() {

    }

    fun turnAllOff() {

    }

    fun isOn(): Boolean {
        return esps.any { it.isLedOn }
    }
}

data class Esp(
    val id: Int,
    var isPingALive: Boolean,
    var isLedOn: Boolean,
    val textTitle: String,
    val textUnder: String,
    private var types: ArrayList<EspType> = ArrayList()
) {
    fun isLed(): Boolean {
        return types.contains(EspType.LED)
    }

    fun isTempSensor(): Boolean {
        return types.contains(EspType.TEMP_SENSOR)
    }
}

enum class EspType {
    LED,
    TEMP_SENSOR,
}

class ClientFragment : Fragment(R.layout.fragment_client), EspEventsListener {

    private val serverURI = "tcp://192.168.1.11:1883"
    private val clientID = "android"

    private lateinit var mqttClient: MqttAndroidClient

    private var currentTab = Tab.MAINROOM

    private lateinit var tabLayout: TabLayout
    private lateinit var roomOnOffSwitch: Switch
    private lateinit var roomColorButton: ImageButton

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView

    private val rooms = HashMap<String, ArrayList<Esp>>();
//    private val roomss = HashMap<String, Room>();


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

        tabLayout = view.findViewById(R.id.tab_layout)
        roomOnOffSwitch = view.findViewById(R.id.room_toggle_on_off_switch)
        roomColorButton = view.findViewById(R.id.room_color_button)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        recyclerView = view.findViewById(R.id.recycler_view)

        tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab?) {
                Log.d("TAG", "${tab?.position}");

                when (tab?.position) {
                    0 -> currentTab = Tab.MAINROOM
                    1 -> currentTab = Tab.LAUNDRYROOM
                    2 -> currentTab = Tab.BEDROOM
                    3 -> currentTab = Tab.BATHROOM
                }

                updateRecyclerViewData(currentTab)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })

        roomOnOffSwitch.setOnClickListener {
            // not setOnCheckedChangeListener to avoid call when updating value programmatically
            val onOff = roomOnOffSwitch.isChecked
            when (onOff) {
                true -> mqttClient.publish("esp/led/${currentTab.room}/action/on", MqttMessage())
                false -> mqttClient.publish("esp/led/${currentTab.room}/action/off", MqttMessage())
            }

            rooms[currentTab.room]?.forEach { it.isLedOn = onOff }
//            roomss.getValue(currentTab.room).esps.forEach { it.isLedOn = onOff }
            updateRecyclerViewData(currentTab)
        }

        roomColorButton.setOnClickListener {

        }

        swipeRefreshLayout.setOnRefreshListener {
            rooms.forEach { mapVal -> mapVal.value.forEach { esp -> esp.isPingALive = false } }
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

                    val (_, _, _, room, id) = topic.split("/")

                    if (!rooms.containsKey(room)) {
                        rooms[room] = ArrayList()
                    }
                    val roomLeds = rooms[room]
                    val ledd = roomLeds?.find { it.id == id.toInt() }
                    if (ledd == null) {
                        roomLeds?.add(Esp(id.toInt(), true, false, "esp - $id", "under"))
                    } else {
                        ledd.isPingALive = true
                    }

                    updateRecyclerViewData(currentTab)
                }

                // each subscribe is synchronous and wait for others to finish
                mqttClient.subscribe("pong/esp/temp_sensor/+/+", 0) { topic, message ->

                    val (_, _, _, room, id) = topic.split("/")

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
        rooms.forEach { mapVal -> mapVal.value.forEach { esp -> esp.isPingALive = false } }
        super.onStop()
    }

    //
    // EVENTS
    //

    override fun onEspClicked(esp: Esp) {
        Log.d("TAG", "onEspClicked")
    }

    override fun onEspToggled(esp: Esp, toggle: Boolean) {
        Log.d("TAG", "onEspToggled")
        esp.isLedOn = toggle
        when (toggle) {
            true -> {
                mqttClient.publish("esp/led/${esp.id}/action/on", MqttMessage())
                roomOnOffSwitch.isChecked = true
            }
            false -> {
                mqttClient.publish("esp/led/${esp.id}/action/off", MqttMessage())
                if (rooms[currentTab.room]?.all { !it.isLedOn } == true) {
                    roomOnOffSwitch.isChecked = false
                }
            }
        }
    }

    override fun onEspColorSelected(esp: Esp, selectedColor: Int) {
        Log.d("TAG", "onEspColorSelected")
        mqttClient.publish("esp/led/${esp.id}/color", MqttMessage(Integer.toHexString(selectedColor).toByteArray()))
    }

    //
    // UTILS
    //

    private fun updateRecyclerViewData(tab: Tab) {
        displayedEsps.clear()
        rooms[tab.room]?.let { roomEsps -> displayedEsps.addAll(roomEsps) }
        displayedEsps.sortBy { espViewModel -> espViewModel.id }
        Handler(Looper.getMainLooper()).post {
            customAdapter.notifyDataSetChanged()
        }
    }
}
