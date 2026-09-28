package dev.kiritoxd.miaopu.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.kiritoxd.miaopu.data.RatingStage
import dev.kiritoxd.miaopu.data.RatingTarget
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

@Composable
fun MatchStageCard(
    stage: RatingStage,
    onClick: () -> Unit,
    onPlayerClick: (RatingTarget) -> Unit,
) {
    val playerPreviews = stage.targets.filter { it.category == "player" }.ifEmpty { stage.targets }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 7.dp)
            .semantics {
                role = Role.Button
                contentDescription = "查看${stage.name}，${stage.targetCount} 个评分对象"
            },
        insideMargin = PaddingValues(vertical = 16.dp),
        cornerRadius = 22.dp,
        onClick = onClick,
        pressFeedbackType = PressFeedbackType.None,
        showIndication = false,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stage.name,
                    style = MiuixTheme.textStyles.title3,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${stage.targetCount} 个评分对象",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = "查看",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Icon(LucideIcons.ChevronRight, null, Modifier.size(16.dp), tint = MiuixTheme.colorScheme.primary)
        }
        if (playerPreviews.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = playerPreviews,
                    key = { target -> "${target.outBizType}:${target.outBizNo}" },
                    contentType = { "stage-player-preview" },
                ) { target ->
                    StagePlayerPreview(target = target, onClick = { onPlayerClick(target) })
                }
            }
        }
    }
}

@Composable
private fun StagePlayerPreview(target: RatingTarget, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(72.dp)
            .semantics {
                role = Role.Button
                contentDescription = "查看 ${target.name} 的评论"
            },
        insideMargin = PaddingValues(horizontal = 7.dp, vertical = 8.dp),
        cornerRadius = 14.dp,
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.surfaceVariant,
            contentColor = MiuixTheme.colorScheme.onSurface,
        ),
        onClick = onClick,
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RatingTargetPortrait(
                target = target,
                size = 46.dp,
                cornerRadius = 11.dp,
                championSize = 18.dp,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = target.name,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = MiuixTheme.textStyles.footnote2,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (target.scoreCount > 0) "%.1f".format(target.scoreAverage) else "—",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
