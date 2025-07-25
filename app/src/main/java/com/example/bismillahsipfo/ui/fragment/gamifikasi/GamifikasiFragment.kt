package com.example.bismillahsipfo.ui.fragment.gamifikasi

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.bismillahsipfo.R
import com.example.bismillahsipfo.data.repository.GamifikasiUiState
import com.example.bismillahsipfo.data.repository.GamifikasiViewModel
import com.example.bismillahsipfo.data.repository.GamifikasiViewModelFactory
import com.example.bismillahsipfo.databinding.FragmentGamifikasiBinding
import com.example.bismillahsipfo.adapter.RowDiskonAdapter
import com.example.bismillahsipfo.data.model.Voucher
import java.text.NumberFormat
import java.util.*

class GamifikasiFragment : Fragment(R.layout.fragment_gamifikasi) {

    private lateinit var binding: FragmentGamifikasiBinding
    private lateinit var rowDiskonAdapter: RowDiskonAdapter
    private val viewModel: GamifikasiViewModel by viewModels {
        GamifikasiViewModelFactory(requireContext())
    }
    private var isLevelInfoExpanded = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentGamifikasiBinding.bind(view)

        setupRecyclerView()
        observeViewModel()
        viewModel.loadGamifikasiData()
        setupExpandableLevel()
    }

    private fun setupExpandableLevel() {
        binding.layoutLevelHeader.setOnClickListener {
            toggleLevelInfo()
        }
    }

    private fun toggleLevelInfo() {
        if (isLevelInfoExpanded) {
            // Collapse
            collapseLevelInfo()
        } else {
            // Expand
            expandLevelInfo()
        }
        isLevelInfoExpanded = !isLevelInfoExpanded
    }

    private fun expandLevelInfo() {
        binding.layoutLevelContent.visibility = View.VISIBLE

        // Animate arrow rotation
        val rotateAnimator = ObjectAnimator.ofFloat(binding.ivExpandArrow, "rotation", 0f, 180f)
        rotateAnimator.duration = 300

        // Animate expand
        val slideAnimator = slideDown(binding.layoutLevelContent)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(rotateAnimator, slideAnimator)
        animatorSet.start()
    }

    private fun collapseLevelInfo() {
        // Animate arrow rotation
        val rotateAnimator = ObjectAnimator.ofFloat(binding.ivExpandArrow, "rotation", 180f, 0f)
        rotateAnimator.duration = 300

        // Animate collapse
        val slideAnimator = slideUp(binding.layoutLevelContent)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(rotateAnimator, slideAnimator)
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                binding.layoutLevelContent.visibility = View.GONE
            }
        })
        animatorSet.start()
    }

    private fun slideDown(view: View): ValueAnimator {
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val targetHeight = view.measuredHeight

        view.layoutParams.height = 0
        val animator = ValueAnimator.ofInt(0, targetHeight)
        animator.addUpdateListener { animation ->
            view.layoutParams.height = animation.animatedValue as Int
            view.requestLayout()
        }
        animator.duration = 300
        return animator
    }

    private fun slideUp(view: View): ValueAnimator {
        val initialHeight = view.measuredHeight
        val animator = ValueAnimator.ofInt(initialHeight, 0)
        animator.addUpdateListener { animation ->
            view.layoutParams.height = animation.animatedValue as Int
            view.requestLayout()
        }
        animator.duration = 300
        return animator
    }

    private fun setupRecyclerView() {
        rowDiskonAdapter = RowDiskonAdapter()
        binding.rvDiskon.apply {
            adapter = rowDiskonAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }
    }

    private fun observeViewModel() {
        viewModel.gamifikasiData.observe(viewLifecycleOwner) { state ->
            when (state) {
                is GamifikasiUiState.Success -> updateUI(state)
                is GamifikasiUiState.Error -> showError(state.message)
                is GamifikasiUiState.Loading -> showLoading()
            }
        }
    }

    private fun updateUI(state: GamifikasiUiState.Success) {
        hideLoading()
        with(state) {
            // Update trophy
            Glide.with(this@GamifikasiFragment)
                .load(gamifikasi.tropi)
                .into(binding.trophy)
            Log.d("GamifikasiFragment", "Trophy URL: ${gamifikasi.tropi}")
    
            // Update level
            binding.tvLevel.text = "Level ${gamifikasi.level}"
    
            // Update jumlah peminjaman
            val remainingAmount = nextLevelGamifikasi.jumlahPeminjamanMinimal - totalPembayaran
            val formattedAmount = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(remainingAmount)
            binding.tvJumlahPeminjaman.text = "$formattedAmount transaksi lagi"
            Log.d("GamifikasiFragment", "Remaining amount: $remainingAmount")
    
            // Update progress indicator
            val progress = ((totalPembayaran / nextLevelGamifikasi.jumlahPeminjamanMinimal) * 100).toInt()
            binding.progressIndicator.progress = progress
            Log.d("GamifikasiFragment", "Progress: $progress, Total Pembayaran: $totalPembayaran, Jumlah Minimal: ${nextLevelGamifikasi.jumlahPeminjamanMinimal}")
    
            // Update voucher list
            val activeVoucherId = gamifikasi.idVoucher
//            rowDiskonAdapter.submitList(vouchers.map { it to true }) // Setiap voucher aktif, tampilkan semuanya
            rowDiskonAdapter.submitList(vouchers.map { it to (it.idVoucher == activeVoucherId) })
            Log.d("GamifikasiFragment", "Active voucher ID: $activeVoucherId")
            Log.d("GamifikasiFragment", "Vouchers: ${vouchers.map { "${it.idVoucher}: ${it.kodeVoucher}" }}")
        }
    }

    private fun showError(message: String) {
        hideLoading()
        // Implementasi untuk menampilkan pesan error, misalnya dengan Snackbar atau Toast
    }

    private fun showLoading() {
        // Implementasi untuk menampilkan loading indicator
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
    }

    private fun getDrawableResourceByName(name: String): Int {
        return resources.getIdentifier(name, "drawable", requireContext().packageName)
    }
}