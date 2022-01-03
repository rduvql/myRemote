package com.example.mqttkotlinsample

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.delay
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken

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

    /** @see ClientFragment */
    fun navigateToClientFragment(bundle: Bundle?) {
        view?.findNavController()?.navigate(R.id.action_ConnectFragment_to_ClientFragment, bundle)
    }
}