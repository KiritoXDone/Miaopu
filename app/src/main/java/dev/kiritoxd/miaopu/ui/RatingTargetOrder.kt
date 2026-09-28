package dev.kiritoxd.miaopu.ui

import dev.kiritoxd.miaopu.data.RatingTarget

internal fun orderRatingTargets(targets: List<RatingTarget>, order: StageTargetOrder): List<RatingTarget> = when (order) {
    StageTargetOrder.HOT -> targets
    StageTargetOrder.LATEST -> targets.sortedByDescending { it.nodeId ?: Long.MIN_VALUE }
    StageTargetOrder.HIGH_SCORE -> targets.sortedWith(
        compareByDescending<RatingTarget> { it.scoreAverage }.thenByDescending { it.scoreCount },
    )
    StageTargetOrder.LOW_SCORE -> targets.sortedWith(
        compareBy<RatingTarget> { if (it.scoreCount == 0) 1 else 0 }
            .thenBy { it.scoreAverage }.thenByDescending { it.scoreCount },
    )
}
