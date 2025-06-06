import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object LocalDateSerializer : KSerializer<LocalDate> {
    @RequiresApi(Build.VERSION_CODES.O)
    private val formatter = DateTimeFormatter.ISO_DATE

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun serialize(encoder: Encoder, value: LocalDate) {
        val string = value.format(formatter)
        encoder.encodeString(string)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun deserialize(decoder: Decoder): LocalDate {
        val string = decoder.decodeString()
        return try {
            LocalDate.parse(string, formatter)
        } catch (e: DateTimeParseException) {
            // Jika format ISO gagal, coba format alternatif
            LocalDate.parse(string, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        }
    }
}