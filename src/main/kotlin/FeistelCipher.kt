import kotlin.math.ceil
import kotlin.math.sqrt

/** Permutes integers reversibly within `[0, [domainSize])` using [key], so that sequential inputs produce unrelated outputs. */
class FeistelCipher(
    private val key: Long,
    private val domainSize: Long,
) {
    private val rounds = 8
    private val halfModulus = ceil(sqrt(domainSize.toDouble())).toLong()

    init {
        require(halfModulus * halfModulus >= domainSize) {
            "Domain split failed: $halfModulus * $halfModulus < $domainSize"
        }
    }

    private fun roundFunction(value: Long, round: Int): Long {
        var h = value
        h = h * 2654435761L + key
        h = h * 2246822519L + round
        h = h xor (h ushr 16)
        h = h * 2654435761L
        h = h xor (h ushr 13)
        h = h * 3266489917L
        h = h xor (h ushr 16)
        return h.mod(halfModulus)
    }

    /** Maps [input] to a unique, unrelated integer within the same domain. */
    fun encrypt(input: Long): Long {
        require(input in 0 until domainSize) { "Input $input outside domain [0, $domainSize)" }
        var value = input
        do {
            var left = value / halfModulus
            var right = value % halfModulus
            for (round in 0 until rounds) {
                val newLeft = right
                val newRight = (left + roundFunction(right, round)) % halfModulus
                left = newLeft
                right = newRight
            }
            value = left * halfModulus + right
        } while (value >= domainSize)
        return value
    }

    /** Recovers the original integer from an encrypted [input]. */
    fun decrypt(input: Long): Long {
        require(input in 0 until domainSize) { "Input $input outside domain [0, $domainSize)" }
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
