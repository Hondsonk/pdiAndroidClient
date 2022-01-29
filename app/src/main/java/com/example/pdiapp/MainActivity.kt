package com.example.pdiapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.pdiapp.CarDataWebSocketListener.Companion.NORMAL_CLOSURE_STATUS
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket


class MainActivity : AppCompatActivity() {
    private val requestDataButton: Button by lazy { findViewById(R.id.requestDataButton) }
    private val progressBar: ProgressBar by lazy { findViewById(R.id.progressBar) }
    private val graphView: GraphView by lazy { findViewById(R.id.graphView) }
    private val output: TextView by lazy { findViewById(R.id.textView) }
    private val client by lazy { OkHttpClient() }

    private var ws: WebSocket? = null

    private var pointsPlotted: Int = 1
    private val xOffset: Double = 500.0       // Offsets the graph so the axes don't show
    private val dataArray = arrayOf( DataPoint(0.0,0.0) );
    private val series = LineGraphSeries<DataPoint>(dataArray)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestDataButton.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            requestCarData()
        }

        initGraphView()
    }

    override fun onResume() {
        super.onResume()
        start()
    }

    override fun onPause() {
        super.onPause()
        stop()
    }

    private fun start() {
        val request: Request = Request.Builder().url("ws://10.0.0.203:8082/").build()
        val listener = CarDataWebSocketListener(this::output, this::ping) { ws = null }
        ws = client.newWebSocket(request, listener)
    }

    private fun stop() {
        ws?.close(NORMAL_CLOSURE_STATUS, "Bye!")
    }

    override fun onDestroy() {
        super.onDestroy()
        client.dispatcher.executorService.shutdown()
    }

    private fun output(dataVal: Double) {
        runOnUiThread {
            //output.text = txt
            //"${output.text}\n$txt".also { output.text = it }

            pointsPlotted++

            if (pointsPlotted > 500) {
                pointsPlotted = 1
                series.resetData(
                    arrayOf<DataPoint>(DataPoint(xOffset, dataVal))
                )
            }

            series.appendData(DataPoint(pointsPlotted.toDouble() + xOffset, dataVal)
                ,true
                , pointsPlotted)

            graphView.viewport.setMaxX(pointsPlotted.toDouble() + xOffset)
            graphView.viewport.setMinX((pointsPlotted-200).toDouble() + xOffset)
        }
    }

    private fun ping(txt: String) {
        runOnUiThread {
            Toast.makeText(this, txt, Toast.LENGTH_SHORT).show()
        }
    }

    private fun initGraphView() {
        graphView.viewport.isXAxisBoundsManual = true
        graphView.viewport.isScrollable = true
        graphView.viewport.setMaxX(10.0)

        graphView.title = "Live Throttle";
        graphView.titleColor = R.color.white;
        graphView.titleTextSize = 18f;

        graphView.gridLabelRenderer.padding = 64
        graphView.gridLabelRenderer.isHorizontalLabelsVisible = false

        graphView.addSeries(series);
    }

    private fun requestCarData() {
        val carDataApi = RetrofitHelper.getInstance().create(CarDataApi::class.java)
        // launching a new coroutine
        GlobalScope.launch {
            val result = carDataApi.getCarData()
            if (result != null) {
                Log.d("Hondson ---", result.body().toString())
                result.body()?.let { plotEngineData(it) }
                progressBar.visibility = View.INVISIBLE
            }
        }
    }

    private fun plotEngineData(carData: CarData) {
        for(item in carData.results) {
            Log.d("Hondson ---", item.engineForce.toString())
            pointsPlotted++

            if (pointsPlotted > 1000) {
                pointsPlotted = 1
                series.resetData(
                    arrayOf<DataPoint>(DataPoint(xOffset, item.engineForce))
                )
            }

            series.appendData(DataPoint(pointsPlotted.toDouble() + xOffset, item.engineForce)
                ,true
                , pointsPlotted)

            graphView.viewport.setMaxX(pointsPlotted.toDouble() + xOffset)
            graphView.viewport.setMinX((pointsPlotted-200).toDouble() + xOffset)
        }
    }
}