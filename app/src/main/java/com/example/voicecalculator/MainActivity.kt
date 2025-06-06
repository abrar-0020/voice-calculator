package com.example.voicecalculator


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.voicecalculator.ui.theme.VoiceCalculatorTheme
import net.objecthunter.exp4j.ExpressionBuilder


class CalculatorViewModel : androidx.lifecycle.ViewModel() {
    var spokenText by mutableStateOf(TextFieldValue(""))
    var calculationResult by mutableStateOf("")
    var historyList = mutableStateListOf<Pair<String, String>>()

    fun addToHistory(expr: String, res: String) {
        if (expr.isNotBlank() && res != "Invalid Expression") {
            historyList.add(0, expr to res)
        }
    }

    fun clear() {
        spokenText = TextFieldValue("")
        calculationResult = ""
    }
}

class MainActivity : ComponentActivity() {

    private val RECORD_AUDIO_REQUEST_CODE = 101
    private lateinit var speechRecognizer: SpeechRecognizer

    private val viewModel: CalculatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_REQUEST_CODE
            )
        } else {
            initSpeechRecognizer()
        }

        setContent {
            VoiceCalculatorTheme {
                val navController = rememberNavController()
                NavHost(navController, startDestination = "calculator") {
                    composable("calculator") {
                        CalculatorScreen(
                            viewModel = viewModel,
                            onStartSpeech = { startSpeechRecognition() },
                            onNavigateToHistory = { navController.navigate("history") }
                        )
                    }
                    composable("history") {
                        HistoryScreen(
                            history = viewModel.historyList,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    private fun initSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                Toast.makeText(this@MainActivity, "Error recognizing speech: $error", Toast.LENGTH_SHORT)
                    .show()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spoken = matches[0]
                    viewModel.spokenText = TextFieldValue(spoken)
                    val res = calculateExpression(spoken)
                    viewModel.calculationResult = res
                    viewModel.addToHistory(spoken, res)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!partial.isNullOrEmpty()) {
                    viewModel.spokenText = TextFieldValue(partial[0])
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }
        speechRecognizer.startListening(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initSpeechRecognizer()
            } else {
                Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }
    }

    private fun calculateExpression(expr: String): String {
        val processedExpr = expr
            .lowercase()
            .replace("plus", "+")
            .replace("minus", "-")
            .replace("into", "*")
            .replace("multiplied by", "*")
            .replace("times", "*")
            .replace("divided by", "/")
            .replace("by", "/")
            .replace("x", "*")
            .replace(" ", "")

        return try {
            val expression = ExpressionBuilder(processedExpr).build()
            expression.evaluate().toString()
        } catch (e: Exception) {
            "Invalid Expression"
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CalculatorScreen(
        viewModel: CalculatorViewModel,
        onStartSpeech: () -> Unit,
        onNavigateToHistory: () -> Unit
    ) {
        val clipboardManager = LocalClipboardManager.current
        val context = LocalContext.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "🎙️ Voice Calculator",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFBBDEFB)), // light blue
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "You said:", fontWeight = FontWeight.Bold, color = Color.White)
                    OutlinedTextField(
                        value = viewModel.spokenText,
                        onValueChange = {
                            viewModel.spokenText = it
                            viewModel.calculationResult = calculateExpression(it.text)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = Color(0xFF0D47A1), fontSize = 18.sp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0D47A1),
                            unfocusedBorderColor = Color(0xFF0D47A1),
                            cursorColor = Color(0xFF0D47A1),
                            focusedTextColor = Color(0xFF0D47A1),
                            unfocusedTextColor = Color(0xFF0D47A1),
                            focusedContainerColor = Color(0xFFBBDEFB),
                            unfocusedContainerColor = Color(0xFFBBDEFB)
                        )
                    )

                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFC8E6C9)), // light green
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(15.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Result:", fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (viewModel.calculationResult.isNotBlank() &&
                                    viewModel.calculationResult != "Invalid Expression"
                                ) {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(viewModel.calculationResult))
                                    Toast.makeText(context, "Result copied to clipboard", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Nothing to copy", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                        ) {
                            Text("COPY", fontSize = 14.sp, color = Color.White)
                        }
                    }
                    Text(
                        text = viewModel.calculationResult,
                        fontSize = 24.sp,
                        color = Color(0xFF0D47A1)
                    ) // very dark blue
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onStartSpeech,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(56.dp)
            ) {
                Text("🎤 Start Speaking", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Manual input buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Clear", "⌫").forEach { btn ->
                    Button(
                        onClick = {
                            when (btn) {
                                "Clear" -> {
                                    viewModel.clear()
                                }

                                "⌫" -> {
                                    val currentText = viewModel.spokenText.text
                                    if (currentText.isNotEmpty()) {
                                        val newText = currentText.dropLast(1)
                                        viewModel.spokenText = TextFieldValue(newText)
                                        viewModel.calculationResult = calculateExpression(newText)
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .height(50.dp),
                        shape = RoundedCornerShape(10)
                    ) {
                        Text(btn, fontSize = 18.sp, color = Color(0xFF0D47A1))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onNavigateToHistory,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.History, contentDescription = "History")
                Spacer(modifier = Modifier.width(8.dp))
                Text("View History")
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun HistoryScreen(history: List<Pair<String, String>>, onBack: () -> Unit) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("History") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.History, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            if (history.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No history yet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    items(history) { (expr, res) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D47A1)) // dark blue bg
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(expr, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(res, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
