package com.example.pdiapp

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.fragment.app.activityViewModels
import com.example.pdiapp.databinding.FragmentOneBinding
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject

class FragmentOne : Fragment() {

    private val viewModel by activityViewModels<SocketViewModel>()

    private var _binding: FragmentOneBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    // Graph Details
    private var pointsPlotted: Int = 1
    private val xOffset: Double = 500.0       // Offsets the graph so the axes don't show
    private val dataArray = arrayOf( DataPoint(0.0,0.0) );
    private val series = LineGraphSeries<DataPoint>(dataArray)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentOneBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initGraphView()
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.socketEventsFlow().collectLatest { value ->
                    //Log.d("Hondson ---","FragmentOne collected value: $value")
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

            title = "Live Throttle";
            titleColor = R.color.white;
            titleTextSize = 18f;

            gridLabelRenderer.padding = 64
            gridLabelRenderer.isHorizontalLabelsVisible = false

        }
        binding.graphView.addSeries(series)
    }

    private fun handleOpenEvent() {
        Toast.makeText(activity, "WS Connected!", Toast.LENGTH_SHORT).show()
    }

    private fun handleCloseEvent(code: Int, reason: String) {
        Toast.makeText(activity, "WS Closing : $code / $reason", Toast.LENGTH_SHORT).show()
    }

    private fun handleStringMessageEvent(dataString: String) {
        val carDataJSON = JSONObject(dataString)
        val engineForce = carDataJSON.getDouble("engineForce")
        //Log.d("Hondson ---", "FragmentOne --- engineForce: $engineForce")

        pointsPlotted++

        if (pointsPlotted > 1000) {
            pointsPlotted = 1
            series.resetData(
                arrayOf<DataPoint>(DataPoint(xOffset, engineForce))
            )
        }

        series.appendData(DataPoint(pointsPlotted.toDouble() + xOffset, engineForce)
            ,true
            , pointsPlotted)

        binding.graphView.viewport.setMaxX(pointsPlotted.toDouble() + xOffset)
        binding.graphView.viewport.setMinX((pointsPlotted-200).toDouble() + xOffset)
    }

    private fun handleErrorEvent(error: Throwable) {
        Toast.makeText(activity, "WS Error : ${error.message}", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}