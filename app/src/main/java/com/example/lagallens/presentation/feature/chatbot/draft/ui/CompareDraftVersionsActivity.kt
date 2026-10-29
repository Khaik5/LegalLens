package com.example.lagallens.presentation.feature.chatbot.draft.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.lagallens.databinding.ActivityCompareDraftVersionsBinding
import com.example.lagallens.presentation.common.utils.applySystemBarInsets

class CompareDraftVersionsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCompareDraftVersionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        binding = ActivityCompareDraftVersionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        binding.compareDraftBack.setOnClickListener { finish() }
        binding.compareDraftReturn.setOnClickListener { finish() }
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, CompareDraftVersionsActivity::class.java)
    }
}
