package pw.micmax.callhistory

import android.Manifest
import android.provider.CallLog.Calls
import android.widget.*
import android.os.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.protobuf.*
import java.io.*

class MainActivity : AppCompatActivity() {

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
        requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        val exportButton: Button = findViewById(R.id.exportButton)

        exportButton.setOnClickListener {
            val columns = arrayOf(Calls.NUMBER, Calls.DATE, Calls.DURATION, Calls.TYPE)
            val cursor = contentResolver.query(Calls.CONTENT_URI, columns, null, null)
                ?: throw IOException("Failed to read call history")

            val historyBuilder = calls.Calls.CallHistory.newBuilder()

            val numberIndex = cursor.getColumnIndex(Calls.NUMBER)
            val dateIndex = cursor.getColumnIndex(Calls.DATE)
            val durationIndex = cursor.getColumnIndex(Calls.DURATION)
            val typeIndex = cursor.getColumnIndex(Calls.TYPE)

            while (cursor.moveToNext()) {
                val number = cursor.getString(numberIndex)
                val startMilliseconds = cursor.getLong(dateIndex)
                val durationSeconds = cursor.getLong(durationIndex)
                val type = cursor.getInt(typeIndex)

                historyBuilder.addCalls(calls.Calls.Call.newBuilder().apply {
                    this.number = number
                    this.start = Timestamp.newBuilder().setSeconds(startMilliseconds / 1000).build()
                    this.duration = Duration.newBuilder().setSeconds(durationSeconds).build()
                    this.type = calls.Calls.Call.CallType.forNumber(type)
                }.build())
            }
            cursor.close()
            val history = historyBuilder.build()

            val filename = "call-history.bin"
            try {
                val directory = Environment.DIRECTORY_DOWNLOADS
                val file = File(Environment.getExternalStoragePublicDirectory(directory), filename)
                val fos = FileOutputStream(file)
                history.writeTo(fos)
                fos.close()
                Toast.makeText(this, "Saved: ${directory}/$filename", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to save", Toast.LENGTH_LONG).show()
            }
        }
    }
}