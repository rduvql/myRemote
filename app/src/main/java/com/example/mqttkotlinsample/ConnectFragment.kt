package com.example.mqttkotlinsample

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController

class ConnectFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_connect, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        val address = "tcp://192.168.1.11:1883";
//        val cliId = "android";
//        val mqttCredentialsBundle = bundleOf(
//            MQTT_SERVER_URI_KEY to address,
//            MQTT_CLIENT_ID_KEY  to cliId
//        );

        navigateToClientFragment(null)

//        MQTTClient.getConnection(context, object: IMqttActionListener {
//
//            override fun onSuccess(asyncActionToken: IMqttToken?) {
//                navigateToClientFragment(null)
//            }
//
//            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
//                Toast.makeText(context, exception?.message, Toast.LENGTH_SHORT).show()
//            }
//        })
    }

    /** @see HomeFragment */
    fun navigateToClientFragment(bundle: Bundle?) {
        view?.findNavController()?.navigate(R.id.action_ConnectFragment_to_ClientFragment, bundle)
    }
}