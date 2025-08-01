package com.example.bismillahsipfo.ui.fragment.profile

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.bismillahsipfo.R
import com.example.bismillahsipfo.ui.fragment.login.LoginActivity

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.GINGERBREAD)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", AppCompatActivity.MODE_PRIVATE)
        val userName = sharedPreferences.getString("nama", "Nama Pengguna")
        val userProfileImage = sharedPreferences.getString("foto_profil", null)
        val userEmail = sharedPreferences.getString("email", "")
        Log.d("ProfileFragment", "Retrieved user data: $userEmail")

        view.findViewById<TextView>(R.id.profile_name).text = userName

        val profileImageView = view.findViewById<ImageView>(R.id.profile_image)
        if (userProfileImage != null) {
            // Load image using Glide
            Glide.with(this)
                .load(userProfileImage)
                .placeholder(R.drawable.ic_profile)
                .circleCrop()
                .into(profileImageView)
        }

        view.findViewById<TextView>(R.id.profile_info).setOnClickListener {
            val intent = Intent(requireContext(), DetailProfileActivity::class.java)
            startActivity(intent)
        }

        // Tambahkan logika untuk tombol logout
        view.findViewById<TextView>(R.id.logout).setOnClickListener {

            // Hapus status login dari SharedPreferences
            with(sharedPreferences.edit()) {
                remove("is_logged_in") // Hanya menghapus flag login
                apply()
            }


            // Arahkan pengguna ke LoginActivity
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }
    }

    override fun onResume() {
        super.onResume()

        // ✅ REFRESH data setiap kali user kembali ke fragment
        refreshUserData()
    }

    private fun refreshUserData() {
        val sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", AppCompatActivity.MODE_PRIVATE)
        val userName = sharedPreferences.getString("nama", "Nama Pengguna")
        val userProfileImage = sharedPreferences.getString("foto_profil", null)

        // Update nama
        view?.findViewById<TextView>(R.id.profile_name)?.text = userName

        // Update profile picture
        val profileImageView = view?.findViewById<ImageView>(R.id.profile_image)
        if (userProfileImage != null && userProfileImage.isNotEmpty()) {
            profileImageView?.let { imageView ->
                Glide.with(this)
                    .load(userProfileImage)
                    .placeholder(R.drawable.ic_profile)
                    .circleCrop()
                    .into(imageView)
            }
        } else {
            // Set default image jika tidak ada foto profil
            profileImageView?.setImageResource(R.drawable.ic_profile)
        }

        Log.d("ProfileFragment", "Data refreshed - Profile image: $userProfileImage")
    }

}