package com.example.pdiapp

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerFragmentAdapter(
    fragmentActivity: FragmentActivity,
    private val listFragmentTitles: ArrayList<String>
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int {
        return listFragmentTitles.size
    }

    override fun createFragment(position: Int): Fragment {
        Log.d("Hondson ---", "ViewPagerFragmentAdapter position: $position")
        when (position) {
            0 -> return FragmentOne()
            1 -> return FragmentTwo()
        }
        return FragmentOne()
    }
}