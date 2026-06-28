import java.math.BigInteger

/** Permutes integers reversibly within `[0, [domainSize])` using [key], so that sequential inputs produce unrelated outputs. */
class FeistelCipher(
    private val key: Long,
    private val domainSize: BigInteger,
) {
    private val rounds = 8
    private val halfModulus: BigInteger

    init {
        var sqrt = domainSize.sqrt()
        if (sqrt * sqrt < domainSize) sqrt += BigInteger.ONE
        halfModulus = sqrt
        require(halfModulus * halfModulus >= domainSize) {
            "Domain split failed: $halfModulus * $halfModulus < $domainSize"
        }
    }

    private fun roundFunction(value: BigInteger, round: Int): BigInteger {
        var h = value
        h = h * C1 + BigInteger.valueOf(key)
        h = h * C2 + BigInteger.valueOf(round.toLong())
        h = h xor (h shr 16)
        h *= C1
        h = h xor (h shr 13)
        h *= C3
        h = h xor (h shr 16)
        return h.mod(halfModulus)
    }

    companion object {
        private val C1 = BigInteger.valueOf(2654435761L)
        private val C2 = BigInteger.valueOf(2246822519L)
        private val C3 = BigInteger.valueOf(3266489917L)
    }

    /** Maps [input] to a unique, unrelated integer within the same domain. */
    fun encrypt(input: BigInteger): BigInteger {
        require(input >= BigInteger.ZERO && input < domainSize) { "Input $input outside domain [0, $domainSize)" }
        var value = input
        do {
            var left = value / halfModulus
            var right = value % halfModulus
            for (round in 0 until rounds) {
                val newLeft = right
                val newRight = (left + roundFunction(right, round)).mod(halfModulus)
                left = newLeft
                right = newRight
            }
            value = left * halfModulus + right
        } while (value >= domainSize)
        return value
    }

    /** Recovers the original integer from an encrypted [input]. */
    fun decrypt(input: BigInteger): BigInteger {
        require(input >= BigInteger.ZERO && input < domainSize) { "Input $input outside domain [0, $domainSize)" }
        var value = input
        do {
            var left = value / halfModulus
            var right = value % halfModulus
            for (round in (rounds - 1) downTo 0) {
                val newRight = left
                val newLeft = (right - roundFunction(left, round)).mod(halfModulus)
                left = newLeft
                right = newRight
            }
            value = left * halfModulus + right
        } while (value >= domainSize)
        return value
    }
}
