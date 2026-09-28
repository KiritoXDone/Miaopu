package dev.kiritoxd.miaopu

import android.os.Bundle
import android.os.Build
import android.graphics.Color
import androidx.activity.SystemBarStyle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import dev.kiritoxd.miaopu.widget.WidgetUpdates
import androidx.activity.viewModels
import dev.kiritoxd.miaopu.ui.MiaopuApp
import dev.kiritoxd.miaopu.ui.MiaopuViewModel
import dev.kiritoxd.miaopu.data.EsportCatalog
import dev.kiritoxd.miaopu.widget.WidgetIntents
import dev.kiritoxd.miaopu.widget.WidgetScheduleStore

class MainActivity : ComponentActivity() {
    private val viewModel: MiaopuViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT))
        if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        if (Build.VERSION.SDK_INT >= 28) window.navigationBarDividerColor = Color.TRANSPARENT
        if (savedInstanceState == null) handleWidgetIntent(intent)
        setContent { MiaopuApp(viewModel) }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch { WidgetUpdates.onAppForeground(applicationContext) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent(intent)
    }

    private fun handleWidgetIntent(intent: Intent) {
        if (intent.action != WidgetIntents.OPEN) return
        val businessId = intent.getStringExtra(WidgetIntents.BUSINESS_ID)
        val match = WidgetScheduleStore(this).find(businessId, intent.getStringExtra(WidgetIntents.MATCH_ID))
        viewModel.openWidgetDestination(match?.toModel(), EsportCatalog.byBusinessId(businessId))
    }
}
