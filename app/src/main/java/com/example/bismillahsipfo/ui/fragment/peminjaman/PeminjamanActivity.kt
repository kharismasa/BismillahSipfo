package com.example.bismillahsipfo.ui.fragment.peminjaman

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.bismillahsipfo.R
import com.example.bismillahsipfo.adapter.PeminjamanPagerAdapter
import com.example.bismillahsipfo.data.repository.SharedPeminjamanViewModel

class PeminjamanActivity : AppCompatActivity() {

    private val sharedViewModel: SharedPeminjamanViewModel by viewModels()
    lateinit var viewPager: ViewPager2
    private lateinit var pagerAdapter: PeminjamanPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_peminjaman)

        viewPager = findViewById(R.id.viewPager)
        pagerAdapter = PeminjamanPagerAdapter(this)
        viewPager.adapter = pagerAdapter

        // Disable swipe
        viewPager.isUserInputEnabled = false
    }

    fun navigateToNextPage(data: Bundle) {
        // Update shared data in adapter
        pagerAdapter.updateSharedData(data)

        // Navigate to next page
        val nextItem = viewPager.currentItem + 1
        if (nextItem < pagerAdapter.itemCount) {
            viewPager.setCurrentItem(nextItem, true)
        }
    }

    fun navigateToPreviousPage() {
        val previousItem = viewPager.currentItem - 1
        if (previousItem >= 0) {
            viewPager.setCurrentItem(previousItem, true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clear SharedViewModel data saat activity selesai
        if (isFinishing) {
            Log.d("PeminjamanActivity", "Activity finishing, clearing SharedViewModel data")
            sharedViewModel.clearData()
        }
    }
}