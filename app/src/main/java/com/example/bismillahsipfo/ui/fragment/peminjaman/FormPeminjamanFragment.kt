package com.example.bismillahsipfo.ui.fragment.peminjaman

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.example.bismillahsipfo.R

class FormPeminjamanFragment : Fragment() {

//    private lateinit var spinnerFasilitas: Spinner
//    private lateinit var containerJenisLapangan: LinearLayout
//
//    private val fasilitasList = listOf("Fasilitas 1", "Fasilitas 2", "Fasilitas 3")
//    private val jenisLapanganMap = mapOf(
//        "Fasilitas 1" to listOf("Lapangan A", "Lapangan B"),
//        "Fasilitas 2" to listOf("Lapangan C", "Lapangan D"),
//        "Fasilitas 3" to listOf("Lapangan E", "Lapangan F")
//    )
//
//    @SuppressLint("MissingInflatedId")
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        val view = inflater.inflate(R.layout.fragment_form_peminjaman, container, false)
//        spinnerFasilitas = view.findViewById(R.id.spinner_fasilitas)
//        containerJenisLapangan = view.findViewById(R.id.container_jenis_lapangan)
//
//        setupSpinner()
//        return view
//    }
//
//    private fun setupSpinner() {
//        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, fasilitasList)
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        spinnerFasilitas.adapter = adapter
//
//        spinnerFasilitas.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
//                val selectedFasilitas = fasilitasList[position]
//                updateJenisLapangan(selectedFasilitas)
//            }
//
//            override fun onNothingSelected(parent: AdapterView<*>) {
//                // Do nothing
//            }
//        }
//    }
//
//    private fun updateJenisLapangan(fasilitas: String) {
//        containerJenisLapangan.removeAllViews()
//        val jenisLapanganList = jenisLapanganMap[fasilitas] ?: emptyList()
//
//        for (jenis in jenisLapanganList) {
//            val checkBox = CheckBox(requireContext())
//            checkBox.text = jenis
//            containerJenisLapangan.addView(checkBox)
//        }
//    }
}