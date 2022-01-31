package com.example.pdiapp

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.fragment.app.activityViewModels
import com.example.pdiapp.databinding.FragmentThreeBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject

class FragmentThree : Fragment() {

    private val viewModel by activityViewModels<SocketViewModel>()

    private var _binding: FragmentThreeBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentThreeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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

    private fun handleOpenEvent() {
        Toast.makeText(activity, "WS Connected!", Toast.LENGTH_SHORT).show()
    }

    private fun handleCloseEvent(code: Int, reason: String) {
        Toast.makeText(activity, "WS Closing : $code / $reason", Toast.LENGTH_SHORT).show()
    }

    private fun handleStringMessageEvent(dataString: String) {
        val carDataJSON = JSONObject(dataString)

        val suspensionForce = carDataJSON.getJSONObject("suspensionForce")
        val driverFront = suspensionForce.getDouble("driverFront")
        val passengerFront = suspensionForce.getDouble("passengerFront")
        val driverRear = suspensionForce.getDouble("driverRear")
        val passengerRear = suspensionForce.getDouble("passengerRear")

/*
        Log.d("Hondson ---", "DriverFront: $driverFront" +
                ", passengerFront: $passengerFront" +
                ", driverRear: $driverRear" +
                ", passengerRear: $passengerRear")
*/
        val driverFrontStress = (driverFront / 10).toInt()
        val passengerFrontStress = (passengerFront / 10).toInt()
        val driverRearStress = (driverRear / 10).toInt()
        val passengerRearStress = (passengerRear / 10).toInt()

        binding.driverFront.progress = if (driverFrontStress > 100) 100 else driverFrontStress
        binding.passengerFront.progress = if (passengerFrontStress > 100) 100 else passengerFrontStress
        binding.driverRear.progress = if (driverRearStress > 100) 100 else driverRearStress
        binding.passengerRear.progress = if (passengerRearStress > 100) 100 else passengerRearStress

        setSpringColor(driverFrontStress, binding.driverFront)
        setSpringColor(passengerFrontStress, binding.passengerFront)
        setSpringColor(driverRearStress, binding.driverRear)
        setSpringColor(passengerRearStress, binding.passengerRear)
    }

    private fun setSpringColor(stress: Int, spring: ProgressBar) {
        when (stress) {
            in 0..33 -> spring.progressTintList = ColorStateList.valueOf(Color.GREEN)
            in 34..66 -> spring.progressTintList = ColorStateList.valueOf(Color.YELLOW)
            in 67..100 -> spring.progressTintList = ColorStateList.valueOf(Color.RED)
            else -> spring.progressTintList = ColorStateList.valueOf(Color.RED)
        }
    }

    private fun handleErrorEvent(error: Throwable) {
        Toast.makeText(activity, "WS Error : ${error.message}", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}