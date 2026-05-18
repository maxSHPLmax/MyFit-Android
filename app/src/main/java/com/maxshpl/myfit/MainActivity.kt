package com.maxshpl.myfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.maxshpl.myfit.navigation.AppNav
import com.maxshpl.myfit.ui.theme.MyFitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyFitTheme {
                AppNav()
            }
        }
    }
}
