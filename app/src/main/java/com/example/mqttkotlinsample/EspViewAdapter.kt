package com.example.mqttkotlinsample

import android.graphics.Color.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder

class EspViewAdapter(
    private val mList: List<Esp>,
    private val eventsCallback: EventsListener
) : RecyclerView.Adapter<EspViewAdapter.EspViewHolder>() {

    interface EventsListener {
        fun onEspClicked(esp: Esp)
        fun onLedOnClicked(esp: Esp)
        fun onLedOffClicked(esp: Esp)
        fun onLedColorSelected(esp: Esp, selectedColor: Int)
    }

    lateinit var colorPicker: AlertDialog

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EspViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_view_design, parent, false)
        return EspViewHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        // TODO
        return super.getItemViewType(position)
    }

    override fun onBindViewHolder(espView: EspViewHolder, position: Int) {

        val esp = mList[position]

        espView.titleTextView.text = esp.textTitle
//        holder.underTextView.text = espViewModel.textUnder

        when(esp.status) {
            EspStatus.ALIVE -> espView.iotImageView.setColorFilter(GREEN)
            EspStatus.NO_PING_YET -> espView.iotImageView.setColorFilter(YELLOW)
            EspStatus.NOT_ALIVE -> espView.iotImageView.setColorFilter(RED)
        }

        espView.itemView.setOnClickListener {
            eventsCallback.onEspClicked(esp);
        }

        espView.ledOnButton.setOnClickListener { view ->
            eventsCallback.onLedOnClicked(esp)
        }

        espView.ledOffButton.setOnClickListener { view ->
            eventsCallback.onLedOffClicked(esp)
        }

        var previousSelectedColor = -1;
        espView.ledColorPickerButton.setOnClickListener {

            colorPicker = ColorPickerDialogBuilder
                .with(espView.itemView.context)
                .setTitle("Choose color")
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(6)
                .lightnessSliderOnly()
                .setOnColorSelectedListener { selectedColor ->

                    eventsCallback.onLedColorSelected(esp, selectedColor)
                    espView.ledColorPickerButton.setColorFilter(selectedColor);

                    if(previousSelectedColor == selectedColor) {
                        colorPicker.hide()
                    }
                    previousSelectedColor = selectedColor
                }
                .build()

            colorPicker.show()
        }
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    class EspViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val iotImageView: ImageView = itemView.findViewById(R.id.image_iot)
        val titleTextView: TextView = itemView.findViewById(R.id.text_title)

        val ledColorPickerButton: ImageButton = itemView.findViewById(R.id.toggle_color_switch)
        val ledOnButton: ImageButton = itemView.findViewById(R.id.toggle_on_btn)
        val ledOffButton: ImageButton = itemView.findViewById(R.id.toggle_off_btn)
    }
}

