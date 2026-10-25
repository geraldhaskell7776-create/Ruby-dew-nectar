package com.rubydew.nectar.ui.care

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rubydew.nectar.ui.theme.AlertRed
import com.rubydew.nectar.ui.theme.Neutral
import com.rubydew.nectar.ui.theme.RaisedCard
import com.rubydew.nectar.ui.theme.RubyDewTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dateFormat = DateTimeFormatter.ofPattern("MMM d, yyyy")

private val cleaningSteps = listOf(
    "Empty out all old nectar. Never top off old nectar with fresh nectar.",
    "Take the feeder apart so you can reach every port and corner.",
    "Scrub with hot water and a bottle brush. Use a small port brush or pipe cleaner on the feeding holes.",
    "Skip dish soap and detergents. They leave residue birds can taste.",
    "For mold or black spots, soak for 1 hour in 1 part white vinegar to 4 parts water, then scrub.",
    "Rinse very well with clean water and let everything air dry before refilling.",
    "Clean every 2–3 days in hot weather (over 80°F) and about once a week in cool weather.",
)

private val mixingSteps = listOf(
    "Boil the water, then turn off the heat.",
    "Stir in plain, refined white sugar until fully dissolved.",
    "Let it cool to room temperature before filling the feeder.",
    "Keep extra nectar in a covered container in the fridge for up to 1 week.",
)

private val warnings = listOf(
    "No red dye or food coloring. It isn't needed, and the red on the feeder already attracts birds.",
    "No honey. It ferments quickly and can grow fungus that harms hummingbirds.",
    "No brown, raw, organic or powdered sugar. They can contain iron or additives that hurt birds.",
    "No artificial sweeteners. They have no calories, so birds can starve.",
)

@Composable
fun CareScreen(viewModel: CareViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CareContent(
        state = state,
        onLogCleaning = viewModel::logCleaning,
        onChangeWater = viewModel::changeWater,
    )
}

@Composable
fun CareContent(state: CareUiState, onLogCleaning: () -> Unit, onChangeWater: (Double) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CleaningStatusButton(needsCleaning = state.needsCleaning, onClick = onLogCleaning)

        Column {
            Text(
                "Last cleaned: " + (state.lastCleaned?.let {
                    "${it.format(dateFormat)} (${daysAgoText(state.daysSinceCleaned ?: 0)})"
                } ?: "Not logged yet"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                state.nextDue?.let { "Next cleaning due ${it.format(dateFormat)} · every ${state.intervalDays} days" }
                    ?: "Tap the button above after you clean and refill.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        InfoCard(title = "How to clean your feeder") {
            cleaningSteps.forEachIndexed { i, step -> Text("${i + 1}. $step") }
        }

        InfoCard(title = "Nectar mixing guide") {
            Text("4 parts water : 1 part white sugar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            NectarCalculator(waterCups = state.waterCups, sugarCups = state.sugarCups, onChangeWater = onChangeWater)
            mixingSteps.forEachIndexed { i, step -> Text("${i + 1}. $step") }
        }

        RaisedCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Never use", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                warnings.forEach { Text("• $it") }
            }
        }
    }
}

/** Gray when the feeder is fresh, bright red when cleaning is due or overdue. Tap to log a cleaning. */
@Composable
fun CleaningStatusButton(needsCleaning: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (needsCleaning) AlertRed else Neutral,
            contentColor = Color.White,
        ),
    ) {
        Icon(if (needsCleaning) Icons.Filled.Warning else Icons.Filled.CheckCircle, contentDescription = null)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                if (needsCleaning) "Time to clean & refill" else "Feeder is fresh",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text("Tap after cleaning to log it", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun NectarCalculator(waterCups: Double, sugarCups: Double, onChangeWater: (Double) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { onChangeWater(-0.5) }) { Text("−") }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("${formatCups(waterCups)} water", fontWeight = FontWeight.Bold)
            Text("${formatCups(sugarCups)} sugar (${formatNumber(sugarCups * 16)} tbsp)")
        }
        OutlinedButton(onClick = { onChangeWater(0.5) }) { Text("+") }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
    RaisedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

private fun daysAgoText(days: Long): String = when (days) {
    0L -> "today"
    1L -> "1 day ago"
    else -> "$days days ago"
}

private fun formatCups(cups: Double): String = "${formatNumber(cups)} ${if (cups == 1.0) "cup" else "cups"}"

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString()
    else "%.2f".format(value).trimEnd('0').trimEnd('.')

@Preview(showBackground = true)
@Composable
private fun CareOverduePreview() {
    RubyDewTheme {
        CareContent(
            state = CareUiState(LocalDate.now().minusDays(5), 5, 3, 2.0),
            onLogCleaning = {},
            onChangeWater = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CareFreshPreview() {
    RubyDewTheme {
        CareContent(
            state = CareUiState(LocalDate.now(), 0, 3, 1.0),
            onLogCleaning = {},
            onChangeWater = {},
        )
    }
}
