package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ExpenseTrackerApp
import com.example.ui.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ExpenseViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: ExpenseViewModel = viewModel()
        ExpenseTrackerApp(viewModel = viewModel)
      }
    }
  }
}
