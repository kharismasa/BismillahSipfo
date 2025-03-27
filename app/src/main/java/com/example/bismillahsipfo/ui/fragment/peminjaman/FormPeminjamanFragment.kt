package com.example.bismillahsipfo.ui.fragment.peminjaman

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.bismillahsipfo.R
import com.example.bismillahsipfo.data.model.Fasilitas
import com.example.bismillahsipfo.data.model.Lapangan
import com.example.bismillahsipfo.data.model.PenggunaKhusus
import com.example.bismillahsipfo.data.repository.FasilitasRepository
import com.example.bismillahsipfo.data.repository.FormPeminjamanViewModel
import com.example.bismillahsipfo.data.repository.FormPeminjamanViewModelFactory
import com.example.bismillahsipfo.data.repository.UserRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class FormPeminjamanFragment : Fragment() {

    private lateinit var viewModel: FormPeminjamanViewModel
    private lateinit var spinnerFasilitas: Spinner
    private lateinit var editTextTanggalMulai: EditText
    private lateinit var editTextTanggalSelesai: EditText
    private lateinit var editTextJamMulai: EditText
    private lateinit var editTextJamSelesai: EditText
    private lateinit var editTextNamaAcara: EditText
    private lateinit var editTextNamaOrganisasi: EditText
    private lateinit var containerJenisLapangan: LinearLayout
    private lateinit var containerPenggunaKhusus: RadioGroup
    private lateinit var buttonNext: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_form_peminjaman, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fasilitasRepository = FasilitasRepository() // Inisialisasi sesuai kebutuhan
        val userRepository = UserRepository(requireContext()) // Inisialisasi dengan context // Inisialisasi sesuai kebutuhan
        val factory = FormPeminjamanViewModelFactory(fasilitasRepository, userRepository)
        viewModel = ViewModelProvider(this, factory)[FormPeminjamanViewModel::class.java]

        initViews(view)
        setupListeners()
        observeViewModel()
    }

    private fun initViews(view: View) {
        spinnerFasilitas = view.findViewById(R.id.spinner_fasilitas)
        editTextTanggalMulai = view.findViewById(R.id.edittext_tanggal_mulai)
        editTextTanggalSelesai = view.findViewById(R.id.edittext_tanggal_selesai)
        editTextJamMulai = view.findViewById(R.id.edittext_jam_mulai)
        editTextJamSelesai = view.findViewById(R.id.edittext_jam_selesai)
        editTextNamaAcara = view.findViewById(R.id.edittext_nama_acara)
        editTextNamaOrganisasi = view.findViewById(R.id.edittext_nama_organisasi)
        containerJenisLapangan = view.findViewById(R.id.container_jenis_lapangan)
        containerPenggunaKhusus = view.findViewById(R.id.container_pengguna_khusus)
        buttonNext = view.findViewById(R.id.button_next)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupListeners() {
        spinnerFasilitas.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedFasilitas = parent.getItemAtPosition(position) as Fasilitas
                containerJenisLapangan.removeAllViews()  // Hapus tampilan lapangan lama
                containerPenggunaKhusus.visibility = View.GONE  // Sembunyikan pengguna khusus
                viewModel.onFasilitasSelected(selectedFasilitas)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        editTextTanggalMulai.setOnClickListener { showDatePicker(true) }
        editTextTanggalSelesai.setOnClickListener { showDatePicker(false) }
        editTextJamMulai.setOnClickListener { showTimePicker(true) }
        editTextJamSelesai.setOnClickListener { showTimePicker(false) }

        // Listener untuk tombol Next
        buttonNext.setOnClickListener {
            val activity = requireActivity() as PeminjamanActivity
            val nextItem = (activity.viewPager.currentItem + 1) % activity.viewPager.adapter?.itemCount!!
            activity.viewPager.setCurrentItem(nextItem, true)  // Navigasi ke fragment berikutnya
        }

        // Listener untuk edittext yang akan menghilangkan hint saat diketik
        editTextNamaAcara.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                editTextNamaAcara.hint = "" // Hilangkan hint saat fokus
            }
        }

        editTextNamaOrganisasi.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                editTextNamaOrganisasi.hint = "" // Hilangkan hint saat fokus
            }
        }

        buttonNext.setOnClickListener { submitForm() }
    }

    private fun observeViewModel() {
        viewModel.fasilitasList.observe(viewLifecycleOwner) { fasilitas ->
            val fasilitasList = listOf(
                Fasilitas(
                    idFasilitas = -1,
                    namaFasilitas = "Pilih fasilitas disini",
                    deskripsi = "",
                    fasilitasPlus = "",
                    photo = "",
                    alamat = "",
                    prosedurPeminjaman = "",
                    tatatertib = "",
                    ketentuanTarif = "",
                    kontak = "",
                    maps = ""
                )
            ) + fasilitas
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, fasilitasList)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerFasilitas.adapter = adapter
        }

        viewModel.lapanganList.observe(viewLifecycleOwner) { lapangan ->
            updateLapanganCheckboxes(lapangan)
        }

        viewModel.showPenggunaKhusus.observe(viewLifecycleOwner) { show ->
            containerPenggunaKhusus.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH)
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = LocalDate.of(selectedYear, selectedMonth + 1, selectedDay)
            val formattedDate = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            if (isStartDate) {
                editTextTanggalMulai.setText(formattedDate)
                viewModel.setTanggalMulai(selectedDate)
            } else {
                editTextTanggalSelesai.setText(formattedDate)
                viewModel.setTanggalSelesai(selectedDate)
            }
        }, year, month, day).show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showTimePicker(isStartTime: Boolean) {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = calendar.get(java.util.Calendar.MINUTE)

        TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            val selectedTime = LocalTime.of(selectedHour, selectedMinute)
            val formattedTime = selectedTime.format(DateTimeFormatter.ofPattern("HH:mm"))
            if (isStartTime) {
                editTextJamMulai.setText(formattedTime)
                viewModel.setJamMulai(selectedTime)
            } else {
                editTextJamSelesai.setText(formattedTime)
                viewModel.setJamSelesai(selectedTime)
            }
        }, hour, minute, true).show()
    }

    private fun updateLapanganCheckboxes(lapanganList: List<Lapangan>) {
        containerJenisLapangan.removeAllViews()
        if (lapanganList.isNotEmpty()) {
            lapanganList.forEach { lapangan ->
                val checkbox = CheckBox(context)
                checkbox.text = lapangan.namaLapangan
                checkbox.setOnCheckedChangeListener { _, isChecked ->
                    viewModel.onLapanganChecked(lapangan, isChecked)
                }
                containerJenisLapangan.addView(checkbox)
            }
        } else {
            // Tambahkan TextView untuk menampilkan pesan ketika tidak ada lapangan
            val textView = TextView(context)
            textView.text = "Tidak ada lapangan tersedia"
            containerJenisLapangan.addView(textView)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun submitForm() {
        val namaAcara = editTextNamaAcara.text.toString()
        val namaOrganisasi = editTextNamaOrganisasi.text.toString()
        val penggunaKhusus = when (containerPenggunaKhusus.checkedRadioButtonId) {
            R.id.radio_internal_uii -> PenggunaKhusus.INTERNAL_UII
            R.id.radio_internal_vs_eksternal -> PenggunaKhusus.INTERNAL_VS_EKSTERNAL
            R.id.radio_eksternal_uii -> PenggunaKhusus.EKSTERNAL_UII
            else -> null
        }

        viewModel.submitForm(namaAcara, namaOrganisasi, penggunaKhusus)
    }

}