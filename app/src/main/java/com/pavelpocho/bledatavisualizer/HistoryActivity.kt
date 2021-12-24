package com.pavelpocho.bledatavisualizer

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import lecho.lib.hellocharts.model.Line
import lecho.lib.hellocharts.model.LineChartData
import lecho.lib.hellocharts.model.PointValue
import lecho.lib.hellocharts.view.LineChartView


class HistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        val extras: Bundle = intent.extras!!
        val history: FloatArray = extras.get("history") as FloatArray
        var historyType: String = extras.get("historyType") as String
        val historyStrings: MutableList<String> = mutableListOf<String>()
        for (f in history) {
            historyStrings.add(f.toString())
        }
        val historyString: String = historyStrings.joinToString()
        findViewById<TextView>(R.id.history_text).text = historyString

        var title: String = ""
        if (historyType == "CA") {
            title = "Historie teploty svodu #1"
        }
        else if (historyType == "CB") {
            title = "Historie teploty svodu #2"
        }
        else if (historyType == "CC") {
            title = "Historie teploty svodu #3"
        }
        else if (historyType == "CD") {
            title = "Historie teploty svodu #4"
        }
        else if (historyType == "CT") {
            title = "Historie tep. chlad. kap."
        }
        else if (historyType == "AA") {
            title = "Historie teploty nas. vzd."
        }
        else if (historyType == "AB") {
            title = "Historie teploty ok. vzd."
        }

        setTitle(title)

        // calling the action bar

        // calling the action bar
        val actionBar: ActionBar? = supportActionBar

        // showing the back button in action bar

        // showing the back button in action bar
        actionBar!!.setDisplayHomeAsUpEnabled(true)

        displayBluetoothGraphData(history.toMutableList(), findViewById(R.id.history_chart))
    }

    // this event will enable the back
    // function to the button on press
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun displayBluetoothGraphData(floatList: MutableList<Float>, chartView: LineChartView) {
        val vals: MutableList<PointValue> = mutableListOf<PointValue>()
        for (i in 0 until floatList.size) {
            vals.add(PointValue(i.toFloat(), floatList[i]))
        }

        val line: Line = Line(vals).setColor(Color.BLUE).setCubic(true)
        val lines: MutableList<Line> = mutableListOf<Line>()
        lines.add(line)

        val data = LineChartData()
        data.lines = lines

        chartView.lineChartData = data
    }
}