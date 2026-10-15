package pl.cheapskountant.mobile.domain

import java.math.BigDecimal
import java.math.RoundingMode

data class MonthInput(val category: String, val currency: String, val amount: BigDecimal)

data class MonthCategoryRow(
    val category: String,
    val amount: BigDecimal,
    val position: Int,
    val ratio: BigDecimal?,
    val percent: Int?,
    val emptyBar: Boolean,
)

data class MonthFigures(
    val currency: String,
    val total: BigDecimal,
    val categories: List<MonthCategoryRow>,
)

object MonthMath {
    private val zero = BigDecimal.ZERO.setScale(2)

    fun summarize(entries: List<MonthInput>): MonthFigures {
        if (entries.isEmpty()) {
            return MonthFigures("PLN", zero, emptyList())
        }
        val sums = linkedMapOf<String, BigDecimal>()
        entries.forEach { entry ->
            sums[entry.category] = (sums[entry.category] ?: BigDecimal.ZERO).add(entry.amount)
        }
        val nonzero = sums.mapNotNull { (name, sum) ->
            if (sum.compareTo(BigDecimal.ZERO) == 0) null else name to sum
        }
        val total = nonzero.fold(BigDecimal.ZERO) { acc, pair -> acc.add(pair.second) }.setScale(2, RoundingMode.HALF_UP)
        val rows = nonzero.mapIndexed { index, (name, raw) ->
            val scaled = raw.setScale(2, RoundingMode.HALF_UP)
            val positive = raw.signum() > 0
            val percent = if (total.compareTo(BigDecimal.ZERO) == 0) {
                null
            } else {
                scaled.multiply(BigDecimal(100)).divide(total, 0, RoundingMode.HALF_UP).toInt()
            }
            val ratio = if (positive && total.compareTo(BigDecimal.ZERO) != 0) {
                raw.divide(total, 6, RoundingMode.HALF_UP)
            } else {
                null
            }
            MonthCategoryRow(
                category = name,
                amount = raw,
                position = index,
                ratio = ratio,
                percent = percent,
                emptyBar = ratio == null,
            )
        }
        return MonthFigures(entries.first().currency, total, rows)
    }
}
