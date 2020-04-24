package com.carpa.simple_scarlet_websocket_kotlin.main

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.carpa.simple_scarlet_websocket_kotlin.R
import com.carpa.simple_scarlet_websocket_kotlin.databinding.FragmentMainBinding
import com.carpa.simple_scarlet_websocket_kotlin.network.MyWebSocketAPI

class MainFragment : Fragment() {

    /**
     * We create the viewModel only once.
     */
    private val viewModel by lazy {
        ViewModelProvider(
            this,
            MainViewModelFactory(MyWebSocketAPI.getInstance(requireActivity().application).socketService)
        ).get(MainViewModel::class.java)
    }

    @SuppressLint("CheckResult")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment using data binding
        val binding = DataBindingUtil.inflate<FragmentMainBinding>(
            inflater,
            R.layout.fragment_main,
            container,
            false
        )

        binding.viewModel = viewModel

        binding.lifecycleOwner = this

        return binding.root
    }
}
