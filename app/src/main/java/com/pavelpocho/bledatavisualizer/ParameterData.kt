package com.pavelpocho.bledatavisualizer

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

class ParameterData @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        LayoutInflater.from(context)
            .inflate(R.layout.parameter_data, this, true)

        orientation = VERTICAL
    }

    fun setText(text: String) {
        findViewById<TextView>(R.id.parameter_data_name).text = text
    }

    fun setIcon(id: Int) {
        findViewById<ImageView>(R.id.parameter_data_icon).setImageDrawable(ContextCompat.getDrawable(context, id))
    }

    fun setValue(text: String) {
        findViewById<TextView>(R.id.parameter_data_value).text = text
    }

    fun setUnit(text: String) {
        findViewById<TextView>(R.id.parameter_data_unit).text = text
    }

}