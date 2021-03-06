package com.applicaster.cleeng.sample

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.applicaster.cleeng.CleengLoginPlugin
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.nio.charset.Charset

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        button_mock_start.setOnClickListener {
            CleengLoginPlugin().mockStartProcess(this, getMockPluginConfiguration())
        }
    }

    private fun getMockPluginConfiguration(): Map<String, String> =
        Gson().fromJson(getConfigFromAssets(this), Map::class.java) as Map<String, String>

    private fun getConfigFromAssets(context: Context): String {
        val inputStream =
            context.resources.openRawResource(R.raw.mock_config)
        val size = inputStream.available()

        val json: String?

        try {
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()

            json = String(buffer, Charset.forName("UTF-8"))
        } catch (e: IOException) {
            e.printStackTrace()
            return ""
        }

        return json
    }
}
