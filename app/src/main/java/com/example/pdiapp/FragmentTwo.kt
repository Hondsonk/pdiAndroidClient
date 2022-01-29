package com.example.pdiapp

import android.content.Context.SENSOR_SERVICE
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.pdiapp.databinding.FragmentOneBinding
import com.example.pdiapp.databinding.FragmentTwoBinding
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.sqrt


class FragmentTwo : Fragment(), SensorEventListener {

    private val viewModel by activityViewModels<SocketViewModel>()
    private var _binding: FragmentTwoBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    // Graph Details
    private var pointsPlotted: Int = 1
    private val xOffset: Double = 500.0       // Offsets the graph so the axes don't show
    private val dataArray = arrayOf( DataPoint(0.0,0.0) );

    // Ideally for track use, want phone accelerometer normalized value to be 0 at all times,
    // In a real situation, the phone (when in the car) will show acceleration data
    // that correlates to steering input b/c suspension is doing things
    private val steeringDataSeries = LineGraphSeries<DataPoint>(dataArray)
    private val accelerometerDataSeries = LineGraphSeries<DataPoint>(dataArray)

    private var mSensorManager: SensorManager? = null
    private var mAccelerometer: Sensor? = null

    private var accelerationCurrentValue: Float = 0.0F
    private var accelerationPreviousValue: Float = 0.0F
    private var changeInAcceleration: Double = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        mSensorManager = activity?.getSystemService(SENSOR_SERVICE) as SensorManager?
        mAccelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        _binding = FragmentTwoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        mSensorManager?.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        mSensorManager?.unregisterListener(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initGraphView()
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.socketEventsFlow().collectLatest { value ->
                    when (value) {
                        is SocketEvent.OpenEvent -> handleOpenEvent()
                        is SocketEvent.StringMessage -> handleStringMessageEvent(value.content)
                        is SocketEvent.CloseEvent -> handleCloseEvent(value.code, value.reason)
                        is SocketEvent.Error -> handleErrorEvent(value.error)
                    }
                }
            }
        }
    }

    private fun initGraphView() {
        binding.graphView.apply {
            viewport.isXAxisBoundsManual = true
            viewport.isScrollable = true
            viewport.setMaxX(10.0)

            title = "Steering Input vs Accelerometer";
            titleColor = R.color.white;
            titleTextSize = 18f;

            gridLabelRenderer.padding = 64
            gridLabelRenderer.isHorizontalLabelsVisible = false
        }
        binding.graphView.addSeries(steeringDataSeries)
        steeringDataSeries.color = Color.BLUE

        binding.graphView.addSeries(accelerometerDataSeries)
        accelerometerDataSeries.color = Color.RED
    }

    private fun handleOpenEvent() {
        //Toast.makeText(activity, "WS Connected!", Toast.LENGTH_SHORT).show()
    }

    private fun handleCloseEvent(code: Int, reason: String) {
        //Toast.makeText(activity, "WS Closing : $code / $reason", Toast.LENGTH_SHORT).show()
    }

    private fun handleStringMessageEvent(dataString: String) {
        val carDataJSON = JSONObject(dataString)
        val steeringValue = carDataJSON.getDouble("steeringValue")
        //val suspensionForce = carDataJSON.getJSONObject("suspensionForce")

        pointsPlotted++

        if (pointsPlotted > 1000) {
            pointsPlotted = 1
            steeringDataSeries.resetData(
                arrayOf<DataPoint>(DataPoint(xOffset, steeringValue))
            )
            accelerometerDataSeries.resetData(
                arrayOf<DataPoint>(DataPoint(xOffset, changeInAcceleration))
            )

        }

        steeringDataSeries.appendData(
            DataPoint(pointsPlotted.toDouble() + xOffset, steeringValue)
            ,true
            , pointsPlotted)

        accelerometerDataSeries.appendData(
            DataPoint(pointsPlotted.toDouble() + xOffset, changeInAcceleration)
            ,true
            ,pointsPlotted)

        binding.graphView.viewport.setMaxX(pointsPlotted.toDouble() + xOffset)
        binding.graphView.viewport.setMinX((pointsPlotted-200).toDouble() + xOffset)
    }

    private fun handleErrorEvent(error: Throwable) {
        Toast.makeText(activity, "WS Error : ${error.message}", Toast.LENGTH_SHORT).show()
    }

    override fun onSensorChanged(p0: SensorEvent?) {
        if (p0 != null) {
            if(p0.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                // alpha is calculated as t / (t + dT)
                // with t, the low-pass filter's time-constant
                // and dT, the event delivery rate

                val x = p0.values[0]
                val y = p0.values[1]
                val z = p0.values[2]

                accelerationCurrentValue = sqrt(x*x + y*y + z*z)
                changeInAcceleration = abs(accelerationCurrentValue - accelerationPreviousValue).toDouble()
                accelerationPreviousValue = accelerationCurrentValue
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}