package com.pavelpocho.bt_data_visualizer

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.graphics.Color
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.pavelpocho.bledatavisualizer.HistoryActivity
import com.pavelpocho.bledatavisualizer.ParameterData
import com.pavelpocho.bledatavisualizer.R
import lecho.lib.hellocharts.model.Line
import lecho.lib.hellocharts.model.LineChartData
import lecho.lib.hellocharts.model.PointValue
import java.util.*
import kotlin.math.pow
import kotlin.math.round


class MainActivity : AppCompatActivity() {

    private var myDevice: BluetoothDevice? = null
    private var myBluetoothGatt: BluetoothGatt? = null
    private var myBluetoothConnected: Boolean = false
    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var timer: Timer? = null
    private val threshold: Int = 43
    private var lastChargeValue: Boolean? = null
    private var charReadQueue: MutableList<BluetoothGattCharacteristic> = mutableListOf<BluetoothGattCharacteristic>()
    private val CCC_DESCRIPTOR_UUID: String = "00002902-0000-1000-8000-00805f9b34fb"

    private var timeBetweenBT: Long = 0

    private var receivedBoardDatas: Int = 0
    private var receivedVeteranDatas: Int = 0

    private var setMenuToConnectedState = false

    private var tmBuffer: MutableList<Float> = mutableListOf<Float>()
    private var tbBuffer: MutableList<Float> = mutableListOf<Float>()
    private var rBuffer: MutableList<Float> = mutableListOf<Float>()
    private var cBuffer: MutableList<Float> = mutableListOf<Float>()
    private var aBuffer: MutableList<Float> = mutableListOf<Float>()
    private var bBuffer: MutableList<Float> = mutableListOf<Float>()

    private var tm: MutableList<PointValue> = mutableListOf<PointValue>()
    private var tb: MutableList<PointValue> = mutableListOf<PointValue>()
    private var r: MutableList<PointValue> = mutableListOf<PointValue>()
    private var c: MutableList<PointValue> = mutableListOf<PointValue>()
    private var a: MutableList<PointValue> = mutableListOf<PointValue>()
    private var b: MutableList<PointValue> = mutableListOf<PointValue>()

    private var ctaBuffer: MutableList<Float> = mutableListOf<Float>()
    private var ctbBuffer: MutableList<Float> = mutableListOf<Float>()
    private var ctcBuffer: MutableList<Float> = mutableListOf<Float>()
    private var ctdBuffer: MutableList<Float> = mutableListOf<Float>()
    private var ataBuffer: MutableList<Float> = mutableListOf<Float>()
    private var atbBuffer: MutableList<Float> = mutableListOf<Float>()
    // TODO: Change one air to surrounding air temperature
    private var cotBuffer: MutableList<Float> = mutableListOf<Float>()
    private var bvBuffer: MutableList<Float> = mutableListOf<Float>()

    private var cta: MutableList<PointValue> = mutableListOf<PointValue>()
    private var ctb: MutableList<PointValue> = mutableListOf<PointValue>()
    private var ctc: MutableList<PointValue> = mutableListOf<PointValue>()
    private var ctd: MutableList<PointValue> = mutableListOf<PointValue>()
    private var ata: MutableList<PointValue> = mutableListOf<PointValue>()
    private var atb: MutableList<PointValue> = mutableListOf<PointValue>()
    private var cot: MutableList<PointValue> = mutableListOf<PointValue>()
    private var bv: MutableList<PointValue> = mutableListOf<PointValue>()

    private var savedInput: String = ""

    private lateinit var powerParameterView: ParameterData
    private lateinit var batteryPercentageParameterView: ParameterData
    private lateinit var speedParameterView: ParameterData
    private lateinit var accelerationParameterView: ParameterData
    private lateinit var batteryTemperatureParameterView: ParameterData

    private lateinit var cylinderTemperature1ParameterView: ParameterData
    private lateinit var cylinderTemperature2ParameterView: ParameterData
    private lateinit var cylinderTemperature3ParameterView: ParameterData
    private lateinit var cylinderTemperature4ParameterView: ParameterData

    private lateinit var airIntakeTemperature1ParameterView: ParameterData
    private lateinit var airIntakeTemperature2ParameterView: ParameterData

    private lateinit var coolantTemperatureParameterView: ParameterData
    private lateinit var batteryVoltageParameterView: ParameterData

    private lateinit var mainMenu: Menu

    private var cylinderTemperature1CalibrationOffset: Float = -1000.0f
    private var cylinderTemperature2CalibrationOffset: Float = -1000.0f
    private var cylinderTemperature3CalibrationOffset: Float = -1000.0f
    private var cylinderTemperature4CalibrationOffset: Float = -1000.0f

    private var airIntakeTemperature1CalibrationOffset: Float = -1000.0f
    private var airIntakeTemperature2CalibrationOffset: Float = -1000.0f

    private var coolantTemperatureCalibrationOffset: Float = -1000.0f

    private var realTimeTimer: Timer = Timer()
    private var acquiredHistory: MutableList<Float> = mutableListOf<Float>()
    private var acquiredHistoryType: String = ""

    private val mGattCallback = object : BluetoothGattCallback() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val intentAction: String
            Log.i("STATE CHANGE", newState.toString())
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("CONNECT AT", BluetoothProfile.STATE_CONNECTED.toString())
//                intentAction = ACTION_GATT_CONNECTED
//                mConnectionState = STATE_CONNECTED
//                broadcastUpdate(intentAction)
                Log.i("GATT", "Connected to GATT server.")
                myBluetoothConnected = true
                runOnUiThread(Runnable {
//                    findViewById<Button>(R.id.button).text = resources.getText(R.string.disconnect)
                    findViewById<Button>(R.id.button).isEnabled = false
                    findViewById<Button>(R.id.button).visibility = View.GONE
                    findViewById<Button>(R.id.cancel_button).visibility = View.GONE
                    findViewById<Button>(R.id.reconnect_button).isEnabled = false
                    findViewById<Button>(R.id.reconnect_button).visibility = View.GONE
                    findViewById<Button>(R.id.disconnect_button).isEnabled = true
                    findViewById<TextView>(R.id.textView).visibility = View.GONE
                    if (this@MainActivity::mainMenu.isInitialized && !setMenuToConnectedState) {
                        mainMenu.findItem(R.id.connect_new_device).isEnabled = false
                        mainMenu.findItem(R.id.disconnect).isEnabled = true
                        setMenuToConnectedState = true
                    }
                    receivedVeteranDatas = 0
                    receivedBoardDatas = 0
                })
                // Attempts to discover services after successful connection.
                Log.i("BT", "Attempting to start service discovery:" + gatt.discoverServices())

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//                intentAction = ACTION_GATT_DISCONNECTED
//                mConnectionState = STATE_DISCONNECTED
                Log.i("GATT", "Disconnected from GATT server.")
                Log.i("DISCONNECT AT", BluetoothProfile.STATE_DISCONNECTED.toString())
                myBluetoothConnected = false
                runOnUiThread(Runnable {
//                    findViewById<Button>(R.id.button).text = resources.getText(R.string.connect)
                    findViewById<Button>(R.id.button).isEnabled = true
                    findViewById<Button>(R.id.reconnect_button).isEnabled = true
                    findViewById<Button>(R.id.disconnect_button).isEnabled = false
                    findViewById<TextView>(R.id.textView).text = resources.getText(R.string.hello)
                    if (this@MainActivity::mainMenu.isInitialized && setMenuToConnectedState) {
                        mainMenu.findItem(R.id.connect_new_device).isEnabled = true
                        mainMenu.findItem(R.id.disconnect).isEnabled = false
                        setMenuToConnectedState = false
                    }
                })
//                broadcastUpdate(intentAction)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
//            timer = fixedRateTimer("timer", false, 0L, 2000) {
////                communicateWithDevice()
//                receiveBtData()
//            }
            receiveBtData()
            Log.i("Services", "Services were discovered")
        }

//        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
//            var statusStr: String? = null
//            if (status == BluetoothGatt.GATT_SUCCESS) {
//                statusStr = "Succ"
//            } else if (status == BluetoothGatt.GATT_READ_NOT_PERMITTED) {
//                statusStr = "NotPerm"
//            }
//            Log.i("STATUS OF READ", statusStr.toString())
//            val value: Int = characteristic!!.getIntValue(android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8, 0)
//            Log.i("Characteristic value", value.toString())
//            runOnUiThread( Runnable {
//                findViewById<TextView>(R.id.data_text).text = "$value"
//            })
//            charReadQueue.removeAt(0)
//            if (charReadQueue.count() > 0) {
//                Log.i("BT Read", "Reading characteristic")
//                gatt!!.readCharacteristic(charReadQueue.first())
//            }
//        }

        override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
        ) {
            with(characteristic) {
                val strVal = String(value)
                if (strVal.endsWith("E")) {
                    val vals: List<String> = (savedInput + strVal).split(";")
                    savedInput = "";
                    runOnUiThread( Runnable {
                        var tv = findViewById<TextView>(R.id.textView)
                        tv.text = (System.currentTimeMillis() - timeBetweenBT).toString()
                        timeBetweenBT = System.currentTimeMillis()
                        vals.forEach {
                            if (it != "E" && it.length != 0) {
                                processBluetoothGraphData(it)
                            }
                            if (it == "E") {
                                showHistoryIfExists()
                            }
                        }
                    })
                }
                else {
                    savedInput += strVal;
                }
            }
        }
    }

    fun showHistoryIfExists() {
        Log.i("Is acq. hist. empty?", "Ha?")
        if (acquiredHistory.size > 0) {
            Log.i("Is acq. hist. empty?", "No!")
            realTimeTimer.cancel()
            val i = Intent(this@MainActivity, HistoryActivity::class.java)
            i.putExtra("history", acquiredHistory.toFloatArray())
            i.putExtra("historyType", acquiredHistoryType)
            startActivity(i)
            acquiredHistory.clear()
        }
        else {
            Log.i("Is acq. hist. empty?", "Yes...")
        }
    }

    fun processBluetoothGraphData(strVal: String) {
        /*
        Measurable things:
            - Motor Temperature - TM
            - Battery Temperature - TB
            - Motor RPM - R
            - Input Motor Current - C
            - Acceleration - A
            - Battery Voltage - B
         */

        if (strVal[0] == 'H' && strVal.length > 3) {
            acquiredHistoryType = strVal.substring(1, 3)
            Log.i("Hist point", strVal)
            try {
                acquiredHistory.add(strVal.slice(IntRange(3, strVal.length - 1)).toFloat())
            } catch (n: java.lang.NumberFormatException) {

            }
        }
        if (strVal[0] == 'T') {
            if (receivedBoardDatas < 10) {
                receivedBoardDatas ++
                receivedVeteranDatas = 0
            }
            else {
                powerParameterView.visibility = View.VISIBLE
                batteryPercentageParameterView.visibility = View.VISIBLE
                speedParameterView.visibility = View.VISIBLE
                accelerationParameterView.visibility = View.VISIBLE
                batteryTemperatureParameterView.visibility = View.VISIBLE
            }
            if (strVal[1] == 'M' && strVal.length > 2) {
                try {
                    displayBluetoothGraphData(
                            strVal.slice(IntRange(2, strVal.length - 1)).toFloat(),
                            tm,
                            tmBuffer//,
//                            findViewById<LineChartView>(R.id.tm_chart),
//                            findViewById<TextView>(R.id.tm_text),
//                            "Motor Temperature"
                    )
                } catch (n: NumberFormatException) {

                }
            }
            else if (strVal[1] == 'B' && strVal.length > 2) {
                try {
                    displayBluetoothGraphData(
                            strVal.slice(IntRange(2, strVal.length - 1)).toFloat(),
                            tb,
                            tbBuffer//,
//                            findViewById<LineChartView>(R.id.tb_chart),
//                            findViewById<TextView>(R.id.tb_text),
//                            "Battery Temperature"
                    )
                } catch (n: NumberFormatException) {

                }
            }
        }
        else if (strVal[0] == 'R' && strVal.length > 1) {
            if (receivedBoardDatas < 10) {
                receivedBoardDatas ++
                receivedVeteranDatas = 0
            }
            else {
                powerParameterView.visibility = View.VISIBLE
                batteryPercentageParameterView.visibility = View.VISIBLE
                speedParameterView.visibility = View.VISIBLE
                accelerationParameterView.visibility = View.VISIBLE
                batteryTemperatureParameterView.visibility = View.VISIBLE
            }
            try{
                displayBluetoothGraphData(
                        strVal.slice(IntRange(1, strVal.length - 1)).toFloat(),
                        r,
                        rBuffer//,
//                        findViewById<LineChartView>(R.id.r_chart),
//                        findViewById<TextView>(R.id.r_text),
//                        "RPM"
                )
            } catch (n: NumberFormatException) {

            }
        }
        else if (strVal[0] == 'C' && strVal.length > 1) {
            if (receivedBoardDatas < 10) {
                receivedBoardDatas ++
                receivedVeteranDatas = 0
            }
            else {
                powerParameterView.visibility = View.VISIBLE
                batteryPercentageParameterView.visibility = View.VISIBLE
                speedParameterView.visibility = View.VISIBLE
                accelerationParameterView.visibility = View.VISIBLE
                batteryTemperatureParameterView.visibility = View.VISIBLE
            }
            try {
                displayBluetoothGraphData(
                        strVal.slice(IntRange(1, strVal.length - 1)).toFloat(),
                        c,
                        cBuffer//,
//                        findViewById<LineChartView>(R.id.c_chart),
//                        findViewById<TextView>(R.id.c_text),
//                        "Current"
                )
            } catch (n: NumberFormatException) {

            }
        }
        else if (strVal[0] == 'A' && strVal.length > 1) {
            if (receivedBoardDatas < 10) {
                receivedBoardDatas ++
                receivedVeteranDatas = 0
            }
            else {
                powerParameterView.visibility = View.VISIBLE
                batteryPercentageParameterView.visibility = View.VISIBLE
                speedParameterView.visibility = View.VISIBLE
                accelerationParameterView.visibility = View.VISIBLE
                batteryTemperatureParameterView.visibility = View.VISIBLE
            }
            try {
                Log.i("Accell", strVal.slice(IntRange(1, strVal.length - 1)))
                var vls = strVal.slice(IntRange(1, strVal.length - 1)).split(",")
                if (vls.count() == 3) {
                    displayBluetoothGraphData(
                            Math.sqrt(vls[0].toDouble().pow(2) + vls[1].toDouble().pow(2)).toFloat(),
                            a,
                            aBuffer//,
//                            findViewById<LineChartView>(R.id.a_chart),
//                            findViewById<TextView>(R.id.a_text),
//                            "Acceleration"
                    )
                }
            } catch (n: NumberFormatException) {

            }
        }
        else if (strVal[0] == 'B' && strVal.length > 1) {
            if (receivedBoardDatas < 10) {
                receivedBoardDatas ++
                receivedVeteranDatas = 0
            }
            else {
                powerParameterView.visibility = View.VISIBLE
                batteryPercentageParameterView.visibility = View.VISIBLE
                speedParameterView.visibility = View.VISIBLE
                accelerationParameterView.visibility = View.VISIBLE
                batteryTemperatureParameterView.visibility = View.VISIBLE
            }
            try {
                displayBluetoothGraphData(
                        strVal.slice(IntRange(1, strVal.length - 1)).toFloat(),
                        b,
                        bBuffer//,
//                        findViewById<LineChartView>(R.id.b_chart),
//                        findViewById<TextView>(R.id.b_text),
//                        "Voltage"
                )
            } catch (n: NumberFormatException) {

            }
        }
        else if (strVal[0] == 'V' && strVal.length > 1) {
            if (receivedVeteranDatas < 3) {
                receivedVeteranDatas ++
                receivedBoardDatas = 0
            }
            else {
                cylinderTemperature1ParameterView.visibility = View.VISIBLE
                cylinderTemperature2ParameterView.visibility = View.VISIBLE
                cylinderTemperature3ParameterView.visibility = View.VISIBLE
                cylinderTemperature4ParameterView.visibility = View.VISIBLE

                airIntakeTemperature1ParameterView.visibility = View.VISIBLE
                airIntakeTemperature2ParameterView.visibility = View.VISIBLE

                coolantTemperatureParameterView.visibility = View.VISIBLE
                batteryVoltageParameterView.visibility = View.VISIBLE
            }

            if (strVal.length > 4 && strVal.contains("CTA")) {
                try {
                    if (cylinderTemperature1CalibrationOffset == -1000.0f) {
                        cylinderTemperature1CalibrationOffset = strVal.slice(IntRange(4, strVal.length - 1)).toFloat() - 23.2f
                    }
                    displayBluetoothGraphData(
                            strVal.slice(IntRange(4, strVal.length - 1)).toFloat() - cylinderTemperature1CalibrationOffset,
                            cta,
                            ctaBuffer//,
//                            findViewById<LineChartView>(R.id.b_chart),
//                            findViewById<TextView>(R.id.b_text),
//                            "Voltage"
                    )
                } catch (n: NumberFormatException) {

                }
            }
            else if (strVal.length > 4 && strVal.contains("CTB")) {
                try {
                    if (cylinderTemperature2CalibrationOffset == -1000.0f) {
                        cylinderTemperature2CalibrationOffset = strVal.slice(IntRange(4, strVal.length - 1)).toFloat() - 23.2f
                    }
                    displayBluetoothGraphData(
                            strVal.slice(IntRange(4, strVal.length - 1)).toFloat() - cylinderTemperature2CalibrationOffset,
                            ctb,
                            ctbBuffer//,
//                            findViewById<LineChartView>(R.id.b_chart),
//                            findViewById<TextView>(R.id.b_text),
//                            "Voltage"
                    )
                } catch (n: NumberFormatException) {

                }
            }
            else if (strVal.length > 4 && strVal.contains("CTC")) {
                try {
                    if (cylinderTemperature3CalibrationOffset == -1000.0f) {
                        cylinderTemperature3CalibrationOffset = strVal.slice(IntRange(4, strVal.length - 1)).toFloat() - 23.2f
                    }
                    displayBluetoothGraphData(
                            strVal.slice(IntRange(4, strVal.length - 1)).toFloat() - cylinderTemperature3CalibrationOffset,
                            ctc,
                            ctcBuffer//,
//                            findViewById<LineChartView>(R.id.b_chart),
//                            findViewById<TextView>(R.id.b_text),
//                            "Voltage"
                    )
                } catch (n: NumberFormatException) {

                }
            }
            else if (strVal.length > 4 && strVal.contains("CTD")) {
                try {
                    if (cylinderTemperature4CalibrationOffset == -1000.0f) {
                        cylinderTemperature4CalibrationOffset = strVal.slice(IntRange(4, strVal.length - 1)).toFloat() - 23.2f
                    }
                    displayBluetoothGraphData(
                            strVal.slice(IntRange(4, strVal.length - 1)).toFloat() - cylinderTemperature4CalibrationOffset,
                            ctd,
                            ctdBuffer//,
//                            findViewById<LineChartView>(R.id.b_chart),
//                            findViewById<TextView>(R.id.b_text),
//                            "Voltage"
                    )
                } catch (n: NumberFormatException) {

                }
            }
            else if (strVal.length > 4 && strVal.contains("ATA")) {
                try {
//                    if (airIntakeTemperature1CalibrationOffset == -1000.0f) {
//                        airIntakeTemperature1CalibrationOffset = strVal.slice(IntRange(4, strVal.length - 1)).toFloat() - 23.2f
//                    }
                    displayBluetoothGraphData(
                            strVal.slice(IntRange(4, strVal.length - 1)).toFloat(),// - airIntakeTemperature1CalibrationOffset,
                            ata,
                            ataBuffer//,
//                            findViewById<LineChartView>(R.id.b_chart),
//                            findViewById<TextView>(R.id.b_text),
//                            "Voltage"
                    )
                } catch (n: NumberFormatException) {

                }
            }
            else if (strVal.length > 4 && strVal.contains("ATB")) {
                try {
//                    if (airIntakeTemperature2CalibrationOffset == -1000.0f) {
//                        airIntakeTemperature2CalibrationOffset = strVal.slice(IntRange(4, strVal.length - 1)).toFloat() - 23.2f
//                    }
                    displayBluetoothGraphData(
                            strVal.slice(IntRange(4, strVal.length - 1)).toFloat(),// - airIntakeTemperature2CalibrationOffset,
                            atb,
                            atbBuffer//,
//                            findViewById<LineChartView>(R.id.b_chart),
//                            findViewById<TextView>(R.id.b_text),
//                            "Voltage"
                    )
                } catch (n: NumberFormatException) {

                }
            }
            else if (strVal.length > 3 && strVal.contains("BV")) {
                try {
                    displayBluetoothGraphData(
                            strVal.slice(IntRange(3, strVal.length - 1)).toFloat(),
                            bv,
                            bvBuffer//,
//                            findViewById<LineChartView>(R.id.b_chart),
//                            findViewById<TextView>(R.id.b_text),
//                            "Voltage"
                    )
                } catch (n: NumberFormatException) {

                }
            }
            else if (strVal.length > 4 && strVal.contains("COT")) {
                try {
                    if (coolantTemperatureCalibrationOffset == -1000.0f) {
                        coolantTemperatureCalibrationOffset = strVal.slice(IntRange(4, strVal.length - 1)).toFloat() - 23.2f
                    }
                    displayBluetoothGraphData(
                            strVal.slice(IntRange(4, strVal.length - 1)).toFloat() - coolantTemperatureCalibrationOffset,
                            cot,
                            cotBuffer//,
//                            findViewById<LineChartView>(R.id.b_chart),
//                            findViewById<TextView>(R.id.b_text),
//                            "Voltage"
                    )
                } catch (n: NumberFormatException) {

                }
            }
        }

        updateHeadlines()

        if (this@MainActivity::mainMenu.isInitialized && !setMenuToConnectedState) {
            mainMenu.findItem(R.id.connect_new_device).isEnabled = false
            mainMenu.findItem(R.id.disconnect).isEnabled = true
            setMenuToConnectedState = true
        }
    }

    fun updateHeadlines() {

        if (b.lastIndex != -1 && c.lastIndex != -1) {
            powerParameterView.setValue(round(b[b.lastIndex].y * c[c.lastIndex].y).toString())
        }
        if (b.lastIndex != -1) {
            batteryPercentageParameterView.setValue(round((b[b.lastIndex].y - 21.0f) / 8.2f * 100.0f).toString())
        }

        if (r.lastIndex != -1) {
            speedParameterView.setValue(round(r[r.lastIndex].y / 60.0f * 0.07f * Math.PI * 3.6f).toString())
        }
        if (a.lastIndex != -1) {
            accelerationParameterView.setValue((round((a[a.lastIndex].y / 145.0f)) / 100).toString())
        }
        if (tb.lastIndex != -1) {
            batteryTemperatureParameterView.setValue((round(tb[tb.lastIndex].y)).toString())
        }

        if (cta.lastIndex != -1) {
            cylinderTemperature1ParameterView.setValue((round(cta[cta.lastIndex].y)).toString())
            cylinderTemperature1ParameterView.findViewById<Button>(R.id.parameter_history_button).setOnClickListener {
                sendHistoryRequest("HCA")
            }
        }
        if (ctb.lastIndex != -1) {
            cylinderTemperature2ParameterView.setValue((round(ctb[ctb.lastIndex].y)).toString())
            cylinderTemperature2ParameterView.findViewById<Button>(R.id.parameter_history_button).setOnClickListener {
                sendHistoryRequest("HCB")
            }
        }
        if (ctc.lastIndex != -1) {
            cylinderTemperature3ParameterView.setValue((round(ctc[ctc.lastIndex].y)).toString())
            cylinderTemperature3ParameterView.findViewById<Button>(R.id.parameter_history_button).setOnClickListener {
                sendHistoryRequest("HCC")
            }
        }
        if (ctd.lastIndex != -1) {
            cylinderTemperature4ParameterView.setValue((round(ctd[ctd.lastIndex].y)).toString())
            cylinderTemperature4ParameterView.findViewById<Button>(R.id.parameter_history_button).setOnClickListener {
                sendHistoryRequest("HCD")
            }
        }

        if (cot.lastIndex != -1) {
            coolantTemperatureParameterView.setValue((round(cot[cot.lastIndex].y)).toString())
            coolantTemperatureParameterView.findViewById<Button>(R.id.parameter_history_button).setOnClickListener {
                sendHistoryRequest("HCT")
            }
        }

        if (ata.lastIndex != -1) {
            airIntakeTemperature1ParameterView.setValue((round(ata[ata.lastIndex].y)).toString())
            airIntakeTemperature1ParameterView.findViewById<Button>(R.id.parameter_history_button).setOnClickListener {
                sendHistoryRequest("HAA")
            }
        }
        if (atb.lastIndex != -1) {
            airIntakeTemperature2ParameterView.setValue((round(atb[atb.lastIndex].y)).toString())
            airIntakeTemperature2ParameterView.findViewById<Button>(R.id.parameter_history_button).setOnClickListener {
                sendHistoryRequest("HAB")
            }
        }

        if (bv.lastIndex != -1) {
            batteryVoltageParameterView.setValue((round(bv[bv.lastIndex].y)).toString())
            batteryVoltageParameterView.findViewById<Button>(R.id.parameter_history_button).setOnClickListener {
                sendHistoryRequest("HBV")
            }
        }

    }

    fun displayBluetoothGraphData(num: Float, vals: MutableList<PointValue>, buffer: MutableList<Float>/*, chartView: LineChartView, textView: TextView, paramName: String*/) {
        if (buffer.size < 10) {
            buffer.add(num)
        }
        if (vals.size >= 50) {
            vals.removeAt(0)
            for (i in 0 until vals.size) {
                vals[i] = PointValue(vals[i].x - 1, vals[i].y)
            }
        }
        vals.add(PointValue(vals.size.toFloat(), (buffer.reduce { acc: Float, i -> acc + i } / buffer.size).toFloat()))
//        vals.add(PointValue(vals.size.toFloat(), num))
        buffer.removeAll(buffer)

//        if (vals.size > 4) {
////            var sum: Float = 0.0f
////            for (i in 0 until 3) {
////                sum += vals[vals.size - 1 - i].y
////            }
////            sum /= 60
//            var sum = vals[vals.size - 1].y
//            textView.text = "$paramName $sum"
//        }
//        else {
//            textView.text = paramName
//        }

        val line: Line = Line(vals).setColor(Color.BLUE).setCubic(true)
        val lines: MutableList<Line> = mutableListOf<Line>()
        lines.add(line)

        val data = LineChartData()
        data.lines = lines

//        chartView.lineChartData = data
    }

    fun BluetoothGattCharacteristic.isIndicatable(): Boolean =
            containsProperty(BluetoothGattCharacteristic.PROPERTY_INDICATE)

    fun BluetoothGattCharacteristic.isNotifiable(): Boolean =
            containsProperty(BluetoothGattCharacteristic.PROPERTY_NOTIFY)

    fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean =
            properties and property != 0

    fun writeDescriptor(descriptor: BluetoothGattDescriptor, payload: ByteArray) {
        myBluetoothGatt?.let { gatt ->
            descriptor.value = payload
            gatt.writeDescriptor(descriptor)
        } ?: error("Not connected to a BLE device!")
    }

    fun enableNotifications(characteristic: BluetoothGattCharacteristic) {
        val cccdUuid = UUID.fromString(CCC_DESCRIPTOR_UUID)
        val payload = when {
            characteristic.isIndicatable() -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            characteristic.isNotifiable() -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else -> {
                Log.e("ConnectionManager", "${characteristic.uuid} doesn't support notifications/indications")
                return
            }
        }

        characteristic.getDescriptor(cccdUuid)?.let { cccDescriptor ->
            if (myBluetoothGatt?.setCharacteristicNotification(characteristic, true) == false) {
                Log.e("ConnectionManager", "setCharacteristicNotification failed for ${characteristic.uuid}")
                return
            }
            writeDescriptor(cccDescriptor, payload)
            Log.i("ConnectionManager", "Wrote descriptor")
        } ?: Log.e("ConnectionManager", "${characteristic.uuid} doesn't contain the CCC descriptor!")
    }

    fun disableNotifications(characteristic: BluetoothGattCharacteristic) {
        if (!characteristic.isNotifiable() && !characteristic.isIndicatable()) {
            Log.e("ConnectionManager", "${characteristic.uuid} doesn't support indications/notifications")
            return
        }

        val cccdUuid = UUID.fromString(CCC_DESCRIPTOR_UUID)
        characteristic.getDescriptor(cccdUuid)?.let { cccDescriptor ->
            if (myBluetoothGatt?.setCharacteristicNotification(characteristic, false) == false) {
                Log.e("ConnectionManager", "setCharacteristicNotification failed for ${characteristic.uuid}")
                return
            }
            writeDescriptor(cccDescriptor, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
        } ?: Log.e("ConnectionManager", "${characteristic.uuid} doesn't contain the CCC descriptor!")
    }

    override fun onDestroy() {
        super.onDestroy()
        runBtDisconnect()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        mainMenu = menu!!
        return true
    }

    override fun onRestart() {
        super.onRestart()
        realTimeTimer = Timer()
        realTimeTimer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                sendBtData("RX")
            }
        }, 0, 2000)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setClickFunction()
        // Initializes Bluetooth adapter.
        bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager?.adapter

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (bluetoothAdapter != null && !bluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 4005)
        }
        else {
            if (!runBtReconnect()) {
                findViewById<Button>(R.id.button).visibility = View.VISIBLE
                findViewById<Button>(R.id.cancel_button).visibility = View.GONE
                findViewById<TextView>(R.id.textView).text = getText(R.string.hello)
                if (this@MainActivity::mainMenu.isInitialized && setMenuToConnectedState) {
                    mainMenu.findItem(R.id.connect_new_device).isEnabled = true
                    mainMenu.findItem(R.id.disconnect).isEnabled = false
                    setMenuToConnectedState = false
                }
            }
        }

        findViewById<Button>(R.id.cancel_button).setOnClickListener {
            findViewById<Button>(R.id.button).visibility = View.VISIBLE
            findViewById<Button>(R.id.cancel_button).visibility = View.GONE
            findViewById<TextView>(R.id.textView).text = getText(R.string.hello)
            if (this@MainActivity::mainMenu.isInitialized && setMenuToConnectedState) {
                mainMenu.findItem(R.id.connect_new_device).isEnabled = true
                mainMenu.findItem(R.id.disconnect).isEnabled = false
                setMenuToConnectedState = false
            }
        }

        powerParameterView = findViewById(R.id.power_parameter_view)
        batteryPercentageParameterView = findViewById(R.id.battery_percentage_parameter_view)
        speedParameterView = findViewById(R.id.speed_parameter_view)
        accelerationParameterView = findViewById(R.id.acceleration_parameter_view)
        batteryTemperatureParameterView = findViewById(R.id.battery_temperature_parameter_view)

        cylinderTemperature1ParameterView = findViewById(R.id.cylinder_temperature_1)
        cylinderTemperature2ParameterView = findViewById(R.id.cylinder_temperature_2)
        cylinderTemperature3ParameterView = findViewById(R.id.cylinder_temperature_3)
        cylinderTemperature4ParameterView = findViewById(R.id.cylinder_temperature_4)
        airIntakeTemperature1ParameterView = findViewById(R.id.air_temperature_1)
        airIntakeTemperature2ParameterView = findViewById(R.id.air_temperature_2)
        coolantTemperatureParameterView = findViewById(R.id.coolant_temperature)
        batteryVoltageParameterView = findViewById(R.id.battery_voltage)

        powerParameterView.setText("Power")
        powerParameterView.setIcon(R.drawable.ic_outline_power_24)
        powerParameterView.setUnit("W")

        batteryPercentageParameterView.setText("Battery")
        batteryPercentageParameterView.setIcon(R.drawable.ic_outline_battery_std_24)
        batteryPercentageParameterView.setUnit("%")

        speedParameterView.setText("Speed")
        speedParameterView.setIcon(R.drawable.ic_baseline_speed_24)
        speedParameterView.setUnit("km/h")

        accelerationParameterView.setText("Acceleration")
        accelerationParameterView.setIcon(R.drawable.ic_outline_trending_up_24)
        accelerationParameterView.setUnit("G")

        batteryTemperatureParameterView.setText("Battery temperature")
        batteryTemperatureParameterView.setIcon(R.drawable.ic_baseline_battery_saver_24)
        batteryTemperatureParameterView.setUnit("deg C")

        cylinderTemperature1ParameterView.setText("Exhaust Temp #1")
        cylinderTemperature1ParameterView.setIcon(R.drawable.ic_baseline_battery_saver_24)
        cylinderTemperature1ParameterView.setUnit("°C")

        cylinderTemperature2ParameterView.setText("Exhaust Temp #2")
        cylinderTemperature2ParameterView.setIcon(R.drawable.ic_baseline_battery_saver_24)
        cylinderTemperature2ParameterView.setUnit("°C")

        cylinderTemperature3ParameterView.setText("Exhaust Temp #3")
        cylinderTemperature3ParameterView.setIcon(R.drawable.ic_baseline_battery_saver_24)
        cylinderTemperature3ParameterView.setUnit("°C")

        cylinderTemperature4ParameterView.setText("Exhaust Temp #4")
        cylinderTemperature4ParameterView.setIcon(R.drawable.ic_baseline_battery_saver_24)
        cylinderTemperature4ParameterView.setUnit("°C")

        airIntakeTemperature1ParameterView.setText("Air Intake Temp")
        airIntakeTemperature1ParameterView.setIcon(R.drawable.ic_baseline_battery_saver_24)
        airIntakeTemperature1ParameterView.setUnit("°C")

        airIntakeTemperature2ParameterView.setText("Ambient Temp")
        airIntakeTemperature2ParameterView.setIcon(R.drawable.ic_baseline_battery_saver_24)
        airIntakeTemperature2ParameterView.setUnit("°C")

        coolantTemperatureParameterView.setText("Coolant Temp")
        coolantTemperatureParameterView.setIcon(R.drawable.ic_baseline_battery_saver_24)
        coolantTemperatureParameterView.setUnit("°C")

        batteryVoltageParameterView.setText("Battery Voltage")
        batteryVoltageParameterView.setIcon(R.drawable.ic_baseline_battery_saver_24)
        batteryVoltageParameterView.setUnit("V")
    }

    private fun sendHistoryRequest(command: String) {
        sendBtData(command)
    }

    private fun getBatteryPercentage(context: Context): Int {
        return if (Build.VERSION.SDK_INT >= 21) {
            val bm = context.getSystemService(BATTERY_SERVICE) as BatteryManager
            bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        } else {
            val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus: Intent = context.registerReceiver(null, iFilter)!!
            val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryPct = level / scale.toDouble()
            (batteryPct * 100).toInt()
        }
    }

    private fun setClickFunction() {
        val btn = findViewById<Button>(R.id.button)
        btn.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                runBtConnect()
            }
        }
        val rcnBtn = findViewById<Button>(R.id.reconnect_button)
        rcnBtn.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                rcnBtn.isEnabled = false
                runBtReconnect()
            }
        }
        val dscBtn = findViewById<Button>(R.id.disconnect_button)
        dscBtn.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                runBtDisconnect()
            }
        }
//        val sendBtn = findViewById<Button>(R.id.send_button)
//        sendBtn.setOnClickListener {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                sendBtData()
//            }
//        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.getItemId()) {
            R.id.connect_new_device -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    runBtConnect()
                }
                true
            }
            R.id.disconnect -> {
                runBtDisconnect()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun runBtDisconnect() {
        myBluetoothGatt?.close()
        myBluetoothGatt?.disconnect()
        myBluetoothGatt = null
        myBluetoothConnected = false
        runOnUiThread(Runnable {
//                    findViewById<Button>(R.id.button).text = resources.getText(R.string.connect)
            findViewById<Button>(R.id.button).isEnabled = true
            findViewById<Button>(R.id.reconnect_button).visibility = View.VISIBLE
            findViewById<Button>(R.id.reconnect_button).isEnabled = true
            findViewById<Button>(R.id.disconnect_button).isEnabled = false
            findViewById<TextView>(R.id.textView).text = resources.getText(R.string.hello)
            findViewById<TextView>(R.id.textView).visibility = View.VISIBLE
            if (this@MainActivity::mainMenu.isInitialized && setMenuToConnectedState) {
                mainMenu.findItem(R.id.connect_new_device).isEnabled = true
                mainMenu.findItem(R.id.disconnect).isEnabled = false
                setMenuToConnectedState = false
            }


            cylinderTemperature1ParameterView.visibility = View.GONE
            cylinderTemperature2ParameterView.visibility = View.GONE
            cylinderTemperature3ParameterView.visibility = View.GONE
            cylinderTemperature4ParameterView.visibility = View.GONE

            airIntakeTemperature1ParameterView.visibility = View.GONE
            airIntakeTemperature2ParameterView.visibility = View.GONE

            coolantTemperatureParameterView.visibility = View.GONE
            batteryVoltageParameterView.visibility = View.GONE

            powerParameterView.visibility = View.GONE
            batteryPercentageParameterView.visibility = View.GONE
            speedParameterView.visibility = View.GONE
            accelerationParameterView.visibility = View.GONE
            batteryTemperatureParameterView.visibility = View.GONE

        })
        timer?.cancel()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun runBtReconnect(): Boolean {
        val deviceManager: CompanionDeviceManager = getSystemService(Context.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager
        // val connectedDevices: List<BluetoothDevice> = bluetoothManager?.getConnectedDevices(BluetoothProfile.GATT) as List<BluetoothDevice>
        // From this you can see if the device is perhaps already connected, but don't bother for now...
        var associations = deviceManager.associations
        Log.i("BT Reconnect", "Running");
        if (associations.size > 0) {
            Log.i("BT Reconnect", "Associations size > 0");
            myDevice = bluetoothAdapter?.getRemoteDevice(associations[0]) //[0] means only one (first) device can be re-connected to
            Log.i("BT Reconnect", "Does my device exist?");
            Log.i("BT Reconnect", myDevice.toString());
            myBluetoothGatt = myDevice?.connectGatt(this@MainActivity, true, mGattCallback)
            return true
        }
        else {
            Toast.makeText(this@MainActivity, "No device to reconnect to.", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun runBtConnect() {
        Toast.makeText(this@MainActivity, "Attempting BT now", Toast.LENGTH_SHORT).show()

        // To skip filters based on names and supported feature flags (UUIDs),
        // omit calls to setNamePattern() and addServiceUuid()
        // respectively, as shown in the following  Bluetooth example.
        val deviceFilter: BluetoothDeviceFilter = BluetoothDeviceFilter.Builder()
                //.setNamePattern(Pattern.compile("BT05"))
//                .setNamePattern(Pattern.compile("JDY-33-BLE"))
                .build()

        // The argument provided in setSingleDevice() determines whether a single
        // device name or a list of them appears.
        val pairingRequest: AssociationRequest = AssociationRequest.Builder()
//                .setSingleDevice(true)
                .addDeviceFilter(deviceFilter)
                .build()

        val deviceManager: CompanionDeviceManager = getSystemService(Context.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager

        if (deviceManager.associations.count() > 0) {
            deviceManager.disassociate(deviceManager.associations[0])
        }

        // When the app tries to pair with a Bluetooth device, show the
        // corresponding dialog box to the user.
        deviceManager.associate(pairingRequest,
                object : CompanionDeviceManager.Callback() {
                    override fun onDeviceFound(chooserLauncher: IntentSender) {
                        Toast.makeText(this@MainActivity, "OnDeviceFound", Toast.LENGTH_SHORT).show()
                        startIntentSenderForResult(chooserLauncher,
                                2506, null, 0, 0, 0
                        )
                    }

                    override fun onFailure(error: CharSequence?) {
                        // Handle the failure.
                    }
                }, null
        )
    }

    @SuppressLint("SetTextI18n")
    private fun sendBtData(strVal: String) {
        for (i in 0 until myBluetoothGatt?.services?.size!!) {
            var uuidString = myBluetoothGatt!!.services[i].uuid.toString()
            if (uuidString.contains("ffe0")) {
                for (j in 0 until myBluetoothGatt!!.services[i].characteristics.size) {
                    var characteristicUuidString = myBluetoothGatt!!.services[i].characteristics[j].uuid.toString()
                    if (characteristicUuidString.contains("ffe1")) {
                        myBluetoothGatt!!.services[i].characteristics[j].value = strVal.toByteArray()
                        myBluetoothGatt!!.writeCharacteristic(myBluetoothGatt!!.services[i].characteristics[j])
                    }
                }
            }
        }
    }

    private fun receiveBtData() {
        Log.i("BT Read", "Receving data ---------------")
        for (i in 0 until myBluetoothGatt?.services?.size!!) {
            var uuidString = myBluetoothGatt!!.services[i].uuid.toString()
            Log.i("Service ID", uuidString)
            if (uuidString.contains("ffe0")) {
                for (j in 0 until myBluetoothGatt!!.services[i].characteristics.size) {
                    Log.i(
                        "Characteristic ID",
                        myBluetoothGatt!!.services[i].characteristics[j].uuid.toString()
                    )
                    for (k in 0 until myBluetoothGatt!!.services[i].characteristics[j].descriptors.count()) {
                        Log.i(
                            "Descriptor ID",
                            myBluetoothGatt!!.services[i].characteristics[j].descriptors[k].uuid.toString()
                        )
                    }
                    if (myBluetoothGatt!!.services[i].characteristics[j].uuid.toString().contains("ffe1")) {
                        Log.i("Found CHAR", "Enabling notifications")
                        enableNotifications(myBluetoothGatt!!.services[i].characteristics[j])
                         realTimeTimer.scheduleAtFixedRate(object : TimerTask() {
                            override fun run() {
                                sendBtData("RX")
                            }
                        }, 0, 2000)
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            2506 -> when(resultCode) {
                Activity.RESULT_OK -> {
                    // The user chose to pair the app with a Bluetooth device.
                    val deviceToPair: BluetoothDevice? =
                            data?.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE)
                    myDevice = deviceToPair
                    myDevice?.createBond()
                    myBluetoothGatt = myDevice?.connectGatt(this@MainActivity, true, mGattCallback)
                }
            }
            4005 -> when(resultCode) {
                Activity.RESULT_OK -> {
                    runOnUiThread {
                        if (!runBtReconnect()) {
                            findViewById<Button>(R.id.button).visibility = View.VISIBLE
                            mainMenu.getItem(R.id.connect_new_device).isEnabled = true
                        }
                    }
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }
}