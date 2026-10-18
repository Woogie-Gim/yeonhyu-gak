package com.gsw.yeonhyugak

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gsw.yeonhyugak.ui.YhApp
import com.gsw.yeonhyugak.ui.theme.YhTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YhTheme {
                YhApp()
            }
        }
    }
}
