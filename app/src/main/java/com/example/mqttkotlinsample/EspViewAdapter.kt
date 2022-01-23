package com.example.mqttkotlinsample

import android.graphics.Color.*
import android.util.Log
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
) : RecyclerView.Adapter<EspViewAdapter.ViewHolder>() {

    interface EventsListener {
        fun onEspClicked(esp: Esp)
        fun onEspToggled(esp: Esp, toggle: Boolean)
        fun onEspColorSelected(esp: Esp, selectedColor: Int)
    }

    lateinit var colorPicker: AlertDialog

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.card_view_design, parent, false)
        return ViewHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        // TODO
        return super.getItemViewType(position)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val esp = mList[position]

        holder.titleTextView.text = esp.textTitle
//        holder.underTextView.text = espViewModel.textUnder

        when(esp.status) {
            EspStatus.ALIVE -> holder.iotImageView.setColorFilter(GREEN)
            EspStatus.NO_PING_YET -> holder.iotImageView.setColorFilter(YELLOW)
            EspStatus.NOT_ALIVE -> holder.iotImageView.setColorFilter(RED)
        }

        holder.itemView.setOnClickListener {
            eventsCallback.onEspClicked(esp);
        }

        holder.toggleOnOffSwitch.isChecked = esp.isLedOnOff
        holder.toggleOnOffSwitch.setOnClickListener { view ->
            eventsCallback.onEspToggled(esp, holder.toggleOnOffSwitch.isChecked)
        }
        var previousSelectedColor = -1;
        holder.colorPickerButton.setOnClickListener {

            colorPicker = ColorPickerDialogBuilder
                .with(holder.itemView.context)
                .setTitle("Choose color")
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(6)
                .lightnessSliderOnly()
                .setOnColorSelectedListener { selectedColor ->

                    eventsCallback.onEspColorSelected(esp, selectedColor)
                    holder.colorPickerButton.setColorFilter(selectedColor);

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

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val iotImageView: ImageView = itemView.findViewById(R.id.image_iot)
        val titleTextView: TextView = itemView.findViewById(R.id.text_title)
//        val underTextView: TextView = itemView.findViewById(R.id.text_under)
        val toggleOnOffSwitch: Switch = itemView.findViewById(R.id.toggle_on_off_switch)
        val colorPickerButton: ImageButton = itemView.findViewById(R.id.button_color_item)
    }
}

