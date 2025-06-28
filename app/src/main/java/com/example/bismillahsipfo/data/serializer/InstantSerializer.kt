package com.example.bismillahsipfo.data.serializer

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object InstantSerializer : KSerializer<Instant> {
    @RequiresApi(Build.VERSION_CODES.O)
    private val primaryFormatter = DateTimeFormatter.ISO_INSTANT

    @RequiresApi(Build.VERSION_CODES.O)
    private val fallbackFormatters = listOf(
        DateTimeFormatter.ISO_OFFSET_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
    )

    // SOLUSI 1: Daftar nama formatter untuk logging
    @RequiresApi(Build.VERSION_CODES.O)
    private val formatterNames = listOf(
        "ISO_OFFSET_DATE_TIME",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss'Z'"
    )

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun serialize(encoder: Encoder, value: Instant) {
        val string = primaryFormatter.format(value)
        encoder.encodeString(string)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun deserialize(decoder: Decoder): Instant {
        val string = decoder.decodeString()

        // Log for debugging
        android.util.Log.d("InstantSerializer", "Parsing timestamp: '$string'")

        // Try primary parser first
        try {
            return Instant.parse(string)
        } catch (e: DateTimeParseException) {
            android.util.Log.w("InstantSerializer", "Primary parser failed for '$string': ${e.message}")
        }

        // Try fallback formatters
        for ((index, formatter) in fallbackFormatters.withIndex()) {
            try {
                return Instant.from(formatter.parse(string))
            } catch (e: DateTimeParseException) {
                // SOLUSI 1: Menggunakan nama formatter dari daftar
                val formatterName = formatterNames[index]
                android.util.Log.d("InstantSerializer", "Formatter '$formatterName' failed for '$string'")

                // SOLUSI 2: Atau gunakan toString() (lebih verbose)
                // android.util.Log.d("InstantSerializer", "Formatter ${formatter.toString()} failed for '$string'")

                // SOLUSI 3: Atau tanpa pattern info
                // android.util.Log.d("InstantSerializer", "Formatter ${index + 1} failed for '$string'")
            }
        }

        // If all formatters fail, try to clean the string and parse again
        try {
            val cleanedString = string.replace(Regex("\\+00:00$"), "Z")
            android.util.Log.d("InstantSerializer", "Trying cleaned string: '$cleanedString'")
            return Instant.parse(cleanedString)
        } catch (e: DateTimeParseException) {
            android.util.Log.e("InstantSerializer", "All parsing attempts failed for '$string'", e)
        }

        // Last resort: return current time and log error
        android.util.Log.e("InstantSerializer", "❌ FAILED TO PARSE TIMESTAMP: '$string' - Using current time as fallback")
        return Instant.now()
    }
}