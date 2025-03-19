package com.example.voicesend
// all done by lingaa
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
        super.onCreate(savedInstanceState)
        setContent {
            VoiceSenderApp()
        }
    }
}

@Composable
fun VoiceSenderApp() {
    var receiverEmail by remember { mutableStateOf("Press button to speak email...") }
    var subject by remember { mutableStateOf("Press button to speak subject...") }
    var body by remember { mutableStateOf("Press button to speak message...") }
    var inputType by remember { mutableStateOf("email") } // Tracks which input is being spoken

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            val spokenText = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0) ?: ""

            when (inputType) {
                "email" -> receiverEmail = validateEmail(spokenText)
                "subject" -> subject = spokenText
                "body" -> body = spokenText
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Receiver Email: $receiverEmail", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(10.dp))
        Button(onClick = { inputType = "email"; startVoiceRecognition(speechLauncher) }) {
            Text(text = "Speak Receiver Email")
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "Subject: $subject", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(10.dp))
        Button(onClick = { inputType = "subject"; startVoiceRecognition(speechLauncher) }) {
            Text(text = "Speak Subject")
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "Body: $body", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(10.dp))
        Button(onClick = { inputType = "body"; startVoiceRecognition(speechLauncher) }) {
            Text(text = "Speak Message Body")
        }

        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = { sendToServer(receiverEmail, subject, body) },
            enabled = receiverEmail.isNotBlank() && subject.isNotBlank() && body.isNotBlank()
                    && receiverEmail.contains("@")
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

// Function to validate spoken email and format it correctly
private fun validateEmail(spokenText: String): String {
    return spokenText.lowercase(Locale.getDefault()) // Convert to lowercase for better accuracy
        .replace(" at ", "@")
        .replace(" dot ", ".")
        .replace(" underscore ", "_")
        .replace(" dash ", "-")
        .replace(" space ", "")
        .replace(" ", "") // Remove any remaining spaces
        .trim() // Ensure no leading or trailing spaces
}

// Function to send email data to Flask server
private fun sendToServer(email: String, subject: String, body: String) {
    val client = OkHttpClient()
    val finalemail=validateEmail(email)
    val requestBody = FormBody.Builder()
        .add("email", finalemail)
        .add("subject", subject)
        .add("body", body)
        .build()

    val request = Request.Builder()
        .url("http://192.168.157.85:5000/sendmail")
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
