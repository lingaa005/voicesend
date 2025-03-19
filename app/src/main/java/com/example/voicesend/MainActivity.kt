package com.example.voicesend

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import okhttp3.*
import java.io.IOException
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // ✅ Ensure this calls super correctly
        setContent {
            VoiceSenderApp()
        }
    }
}

@Composable
fun VoiceSenderApp() {
    var recognizedText by remember { mutableStateOf("Press the button and speak...") }
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            val resultText = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0) ?: ""
            recognizedText = resultText
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = recognizedText, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = { startVoiceRecognition(speechLauncher) }) {
            Text(text = "Start Voice Input")
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = { sendToServer(recognizedText) },
            enabled = recognizedText.isNotBlank() && recognizedText != "Press the button and speak..."
        ) {
            Text(text = "Send to Server")
        }
    }
}

private fun startVoiceRecognition(launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
    }
    launcher.launch(intent)
}

private fun sendToServer(text: String) {
    val client = OkHttpClient()
    val requestBody = FormBody.Builder().add("text", text).build()
    val request = Request.Builder()
        .url("http://192.168.157.85:5000/receive_text") // ✅ Replace with your actual Flask server URL
        .post(requestBody)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace()
        }

        override fun onResponse(call: Call, response: Response) {
            response.close()
        }
    })
}
