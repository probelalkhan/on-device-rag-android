package dev.belalkhan.performanceoverlay

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun PerformanceOverlay(
    metricsFlow: StateFlow<PerformanceMetrics>,
    modifier: Modifier = Modifier,
) {
    val metrics by metricsFlow.collectAsState()
    var visible by rememberSaveable { mutableStateOf(true) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val prompt = rememberSaveable { samplePrompts.random() }

    if (!visible) return

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val containerWidth = constraints.maxWidth
        val containerHeight = constraints.maxHeight
        var overlaySize by remember { mutableStateOf(IntSize.Zero) }
        var x by rememberSaveable { mutableFloatStateOf(with(density) { 14.dp.toPx() }) }
        var y by rememberSaveable { mutableFloatStateOf(with(density) { 92.dp.toPx() }) }

        fun moveBy(amount: Offset) {
            x = (x + amount.x).coerceIn(0f, (containerWidth - overlaySize.width).coerceAtLeast(0).toFloat())
            y = (y + amount.y).coerceIn(0f, (containerHeight - overlaySize.height).coerceAtLeast(0).toFloat())
        }

        Surface(
            modifier = Modifier
                .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                .widthIn(max = 380.dp)
                .onSizeChanged {
                    overlaySize = it
                    moveBy(Offset.Zero)
                }
                .animateContentSize()
                .clip(RoundedCornerShape(24.dp))
                .pointerInput(containerWidth, containerHeight, overlaySize) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        moveBy(dragAmount)
                    }
                },
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.98f),
            tonalElevation = 5.dp,
            shadowElevation = 8.dp,
        ) {
            AnimatedContent(targetState = expanded, label = "Performance overlay") { isExpanded ->
                if (isExpanded) {
                    ExpandedMetrics(
                        metrics = metrics,
                        prompt = prompt,
                        onCollapse = { expanded = false },
                        onClose = { visible = false },
                    )
                } else {
                    CollapsedMetrics(metrics, onExpand = { expanded = true })
                }
            }
        }
    }
}

@Composable
private fun CollapsedMetrics(metrics: PerformanceMetrics, onExpand: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(role = Role.Button, onClick = onExpand)
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GaugeIcon(metrics.thermalStatus, metrics.isGenerating)
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (metrics.isGenerating) {
                "${metrics.estimatedTokensPerSecond.oneDecimal()} est. tok/s"
            } else {
                "Performance"
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "  ·  ${metrics.thermalStatus.label}",
            color = thermalColor(metrics.thermalStatus),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun ExpandedMetrics(
    metrics: PerformanceMetrics,
    prompt: String,
    onCollapse: () -> Unit,
    onClose: () -> Unit,
) {
    Column(modifier = Modifier.padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GaugeIcon(metrics.thermalStatus, metrics.isGenerating)
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text("On-device performance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    if (metrics.isGenerating) "Live · CPU inference" else "Last run · drag to move",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onCollapse) { Text("Collapse") }
            IconButton(onClick = onClose) {
                CloseIcon()
            }
        }

        HorizontalDivider(modifier = Modifier.padding(bottom = 14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HeroMetric("EST. SPEED", "${metrics.estimatedTokensPerSecond.oneDecimal()} tok/s", Modifier.weight(1f))
            HeroMetric("TIME TO FIRST OUTPUT", metrics.timeToFirstOutputMs?.formatDuration() ?: "Waiting…", Modifier.weight(1f))
        }

        Spacer(Modifier.height(14.dp))
        MetricRow("Elapsed", metrics.elapsedMs.formatDuration(), "Estimated tokens", "~${metrics.estimatedTokens}")
        MetricRow("App CPU", "${metrics.cpuPercent}% of device", "App memory", "${metrics.memoryMb} MB")
        MetricRow("Peak memory", "${metrics.peakMemoryMb} MB", "Thermal", metrics.thermalStatus.label)
        MetricRow("Thermal headroom", metrics.thermalHeadroom?.oneDecimal() ?: "Unavailable", "Backend", "CPU")

        HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))
        SamplePrompt(prompt)

        Text(
            "Token count is estimated from generated text. Metrics are sampled on this device.",
            modifier = Modifier.padding(top = 12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            lineHeight = 16.sp,
        )
    }
}

@Composable
private fun CloseIcon() {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(
        modifier = Modifier
            .size(20.dp)
            .semantics { contentDescription = "Close performance overlay" }
    ) {
        val strokeWidth = 2.dp.toPx()
        drawLine(color, Offset(4.dp.toPx(), 4.dp.toPx()), Offset(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(size.width - 4.dp.toPx(), 4.dp.toPx()), Offset(4.dp.toPx(), size.height - 4.dp.toPx()), strokeWidth, StrokeCap.Round)
    }
}

@Composable
private fun SamplePrompt(prompt: String) {
    val context = LocalContext.current
    var copied by rememberSaveable(prompt) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .padding(13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "STRESS-TEST PROMPT",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
            )
            TextButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Pocket AI stress-test prompt", prompt))
                    copied = true
                },
            ) { Text(if (copied) "Copied" else "Copy") }
        }
        Text(
            prompt,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.bodySmall,
            lineHeight = 17.sp,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HeroMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f), RoundedCornerShape(16.dp)).padding(13.dp)
    ) {
        Text(label, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
        Text(value, Modifier.padding(top = 5.dp), MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 16.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MetricRow(firstLabel: String, firstValue: String, secondLabel: String, secondValue: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Metric(firstLabel, firstValue, Modifier.weight(1f))
        Metric(secondLabel, secondValue, Modifier.weight(1f))
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun GaugeIcon(thermalStatus: ThermalStatus, active: Boolean) {
    val color = thermalColor(thermalStatus)
    Canvas(Modifier.size(24.dp)) {
        val stroke = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
        drawArc(color.copy(alpha = 0.2f), 140f, 260f, false, style = stroke)
        drawArc(color, 140f, if (active) 205f else 80f, false, style = stroke)
        drawCircle(color, 2.5.dp.toPx())
    }
}

@Composable
private fun thermalColor(status: ThermalStatus): Color = when (status) {
    ThermalStatus.None -> Color(0xFF22C55E)
    ThermalStatus.Light -> Color(0xFF84CC16)
    ThermalStatus.Moderate -> Color(0xFFF59E0B)
    ThermalStatus.Severe -> Color(0xFFF97316)
    ThermalStatus.Critical, ThermalStatus.Emergency, ThermalStatus.Shutdown -> MaterialTheme.colorScheme.error
}

private fun Number.oneDecimal(): String = String.format(Locale.US, "%.1f", toDouble())

private fun Long.formatDuration(): String = if (this < 1_000) "$this ms" else "${(this / 1_000.0).oneDecimal()} s"
