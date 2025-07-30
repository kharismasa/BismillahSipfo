package com.example.bismillahsipfo.ui.fragment.gamifikasi

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
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
        val targetView = binding.layoutLevelContent

        // ✅ PERBAIKAN: Pastikan visibility terlebih dahulu
        targetView.visibility = View.VISIBLE

        // ✅ PERBAIKAN: Gunakan post() untuk memastikan view sudah ter-render
        targetView.post {
            // Measure dengan parent constraint yang tepat
            val widthSpec = View.MeasureSpec.makeMeasureSpec(
                targetView.parent.let { (it as ViewGroup).width },
                View.MeasureSpec.EXACTLY
            )
            val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            targetView.measure(widthSpec, heightSpec)

            val targetHeight = targetView.measuredHeight
            Log.d("GamifikasiFragment", "Target height for expand: $targetHeight")

            // Set height to 0 untuk animasi
            targetView.layoutParams.height = 0
            targetView.requestLayout()

            // Animate arrow rotation
            val rotateAnimator = ObjectAnimator.ofFloat(binding.ivExpandArrow, "rotation", 0f, 180f)
            rotateAnimator.duration = 300

            // ✅ PERBAIKAN: Animate expand dengan ValueAnimator yang lebih reliable
            val expandAnimator = ValueAnimator.ofInt(0, targetHeight)
            expandAnimator.duration = 300
            expandAnimator.addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Int
                targetView.layoutParams.height = animatedValue
                targetView.requestLayout()
            }

            // ✅ PERBAIKAN: Tambahkan listener untuk cleanup
            expandAnimator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // Reset to wrap_content setelah animasi selesai
                    targetView.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    targetView.requestLayout()
                }
            })

            val animatorSet = AnimatorSet()
            animatorSet.playTogether(rotateAnimator, expandAnimator)
            animatorSet.start()
        }
    }

    private fun collapseLevelInfo() {
        val targetView = binding.layoutLevelContent
        val initialHeight = targetView.height

        Log.d("GamifikasiFragment", "Initial height for collapse: $initialHeight")

        // Animate arrow rotation
        val rotateAnimator = ObjectAnimator.ofFloat(binding.ivExpandArrow, "rotation", 180f, 0f)
        rotateAnimator.duration = 300

        // ✅ PERBAIKAN: Animate collapse dengan handling yang lebih baik
        val collapseAnimator = ValueAnimator.ofInt(initialHeight, 0)
        collapseAnimator.duration = 300
        collapseAnimator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Int
            targetView.layoutParams.height = animatedValue
            targetView.requestLayout()
        }

        collapseAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                targetView.visibility = View.GONE
                // Reset height parameter
                targetView.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        })

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(rotateAnimator, collapseAnimator)
        animatorSet.start()
    }

    // ✅ ALTERNATIVE SOLUTION: Gunakan simple visibility toggle tanpa animasi tinggi
    // Jika masalah masih persisting, gunakan method ini sebagai fallback
    private fun toggleLevelInfoSimple() {
        val targetView = binding.layoutLevelContent

        if (isLevelInfoExpanded) {
            // Collapse - simple
            targetView.visibility = View.GONE
            binding.ivExpandArrow.rotation = 0f
        } else {
            // Expand - simple
            targetView.visibility = View.VISIBLE
            binding.ivExpandArrow.rotation = 180f
        }

        isLevelInfoExpanded = !isLevelInfoExpanded
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