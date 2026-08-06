package fr.solremi.minerspace.domain.economy

import java.math.BigInteger

object FixedPointMath {
    private val scale = BigInteger.valueOf(MULTIPLIER_SCALE)

    fun floorMultiply(value: Long, multiplierMillionths: Long): Long {
        require(value >= 0L)
        require(multiplierMillionths >= 0L)
        return BigInteger.valueOf(value)
            .multiply(BigInteger.valueOf(multiplierMillionths))
            .divide(scale)
            .longValueExact()
    }

    fun addExact(left: Long, right: Long): Long = Math.addExact(left, right)

    fun multiplyExact(left: Long, right: Long): Long = Math.multiplyExact(left, right)
}
