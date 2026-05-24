import kotlin.random.Random

/** Encodes integers as short, human-friendly strings using a [seed]-specific shuffled alphabet to prevent ID guessing and enumeration. */
class Base32Crockford(seed: Long) {

    private val characterTable = charArrayOf(
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'M',
        'N', 'P', 'Q', 'R', 'S', 'T', 'V', 'W', 'X', 'Y', 'Z'
        ).also { arr ->
            val rng = Random(seed)
            for (i in arr.size - 1 downTo 1) {
                val j = rng.nextInt(i + 1)
                val tmp = arr[i]
                arr[i] = arr[j]
                arr[j] = tmp
            }
        }

    private val scrambler = FeistelScrambler(
        key = seed,
        domainSize = 32 * 32 * 32 * 32 * 32 * 32
    )

    private val reverseTable = buildMap {
        characterTable.forEachIndexed { index, ch ->
            put(ch, index)
            put(ch.lowercaseChar(), index)
        }
    }

    /** Encodes [input] as a 6-character Base32 string. */
    fun encode(input: Int): String {
        require(input >= 0) { "ID must be non-negative" }

        val scrambled = scrambler.scramble(input)

        var number = scrambled.toLong()
        val response = StringBuilder()

        do {
            val remainder = (number % 32).toInt()
            response.insert(0, characterTable[remainder])
            number /= 32
        } while (number > 0)

        while (response.length < 6) {
            response.insert(0, characterTable[0])
        }

        return response.toString()
    }

    /** Decodes [string] back to the original integer. Case-insensitive. */
    fun decode(string: String): Int {
        require(string.isNotEmpty()) { "Token string must not be empty" }

        var result: Long = 0

        for (i in string.indices) {
            val ch = string[i]
            val value = reverseTable[ch]
                ?: throw IllegalArgumentException("Invalid character '$ch'")
            result = result * 32 + value
        }

        return scrambler.unscramble(result.toInt())
    }

    /** Returns true if [string] can be decoded successfully. */
    fun check(string: String): Boolean {
        return try {
            decode(string)
            true
        } catch (_: Exception) {
            false
        }
    }
}
