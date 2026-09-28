package dev.kiritoxd.miaopu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.kiritoxd.miaopu.data.RatingTarget
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun PlayerIdentityCard(
    target: RatingTarget, matchLabel: String?, selectedScore: Int,
    canSelectScore: Boolean, onScoreChange: (Int) -> Unit, onLogin: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.fillMaxWidth(), insideMargin = PaddingValues(14.dp), cornerRadius = 18.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RatingTargetPortrait(target, 62.dp, 10.dp, 20.dp, "${target.name}头像")
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(target.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    listOfNotNull(matchLabel, target.stageName?.takeUnless { it == "评分" }, target.description)
                        .filter(String::isNotBlank).distinct().forEach {
                            Text(it, fontSize = 11.sp, color = colors.onSurfaceVariantSummary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(if (target.scoreCount > 0) "%.1f".format(target.scoreAverage) else "—",
                        fontSize = 30.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                    Text("${target.scoreCount}人评分", fontSize = 10.sp, color = colors.onSurfaceVariantSummary)
                }
            }
        }
        Card(Modifier.fillMaxWidth(), insideMargin = PaddingValues(14.dp), cornerRadius = 18.dp) {
            Text("评分分布", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            val total = target.scoreDistribution.values.sum().coerceAtLeast(0)
            listOf(10, 8, 6, 4, 2).forEach { score ->
                val count = (target.scoreDistribution[score] ?: 0).coerceAtLeast(0)
                val fraction = if (total > 0) (count.toFloat() / total).coerceIn(0f, 1f) else 0f
                Row(Modifier.fillMaxWidth().height(19.dp).semantics { contentDescription = "$score 分，$count 人" }, verticalAlignment = Alignment.CenterVertically) {
                    Text("${score}分", Modifier.width(34.dp), fontSize = 11.sp, color = colors.onSurfaceVariantSummary)
                    Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(colors.surfaceVariant)) {
                        if (fraction > 0) Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(colors.primary))
                    }
                    Text(if (total > 0) "${(fraction * 100).toInt()}%" else "—", Modifier.width(36.dp), fontSize = 10.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End, color = colors.onSurfaceVariantSummary)
                }
            }
        }
        if (target.canScore) {
            Card(Modifier.fillMaxWidth(), insideMargin = PaddingValues(horizontal = 14.dp, vertical = 8.dp), cornerRadius = 18.dp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("我的评分", Modifier.weight(1f), fontSize = 13.sp)
                    (1..5).forEach { stars ->
                        IconButton(onClick = { if (canSelectScore) onScoreChange(stars * 2) else onLogin() }, minWidth = 32.dp, minHeight = 36.dp,
                            modifier = Modifier.semantics { contentDescription = "$stars 星"; selected = selectedScore == stars * 2 }) {
                            Icon(if (selectedScore >= stars * 2) LucideIcons.StarFilled else LucideIcons.Star, null, Modifier.size(25.dp), tint = colors.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CommentInputBar(
    value: String, target: RatingTarget, loggedIn: Boolean, publishing: Boolean, selectedScore: Int,
    onValueChange: (String) -> Unit, onPublish: () -> Unit, onLogin: () -> Unit,
    focusKey: String? = null,
) {
    CommentComposer(
        focusKey = focusKey,
        value = value,
        placeholder = if (!loggedIn) "登录后发表评论" else if (!target.canComment) "暂不可发表评论" else "说说你的看法…",
        action = if (!loggedIn) "登录" else if (publishing) "提交中" else if (value.isBlank() && hasPendingScore(selectedScore, target.userScore)) "评分" else "发布",
        inputEnabled = loggedIn && target.canComment && !publishing,
        actionEnabled = !publishing && (!loggedIn || canSubmitCommentOrScore(value, selectedScore, target.userScore)),
        onValueChange = onValueChange, onSend = if (loggedIn) onPublish else onLogin,
    )
}

internal fun canSubmitCommentOrScore(comment: String, selectedScore: Int, userScore: Int): Boolean =
    comment.isNotBlank() || hasPendingScore(selectedScore, userScore)

private fun hasPendingScore(selectedScore: Int, userScore: Int): Boolean = selectedScore > 0 && selectedScore != userScore
