/** Encodes and decodes integers as human-friendly strings using [Crockford's Base32](https://www.crockford.com/base32.html). */
object Base32Crockford {

    /** Encodes [input] as a 6-character Base32 string. */
    fun encode(input: Int): String {
        require(input >= 0) { "Input must be non-negative" }

        var number = input.toLong()
        val response = StringBuilder()

        do {
            val remainder = (number % 32).toInt()
            response.insert(0, CHARACTER_TABLE[remainder])
            number /= 32
        } while (number > 0)

        while (response.length < 6) {
            response.insert(0, CHARACTER_TABLE[0])
        }

        return response.toString()
    }

    /** Decodes [string] back to an integer. Case-insensitive. */
    fun decode(string: String): Int {
        require(string.isNotEmpty()) { "Input must not be empty" }

        var result: Long = 0

        for (i in string.indices) {
            val ch = string[i]
            val value = REVERSE_TABLE[ch]
                ?: throw IllegalArgumentException("Invalid character '$ch'")
            result = result * 32 + value
        }

        return result.toInt()
    }

    /** Converts [string] to its canonical representation, replacing confusable characters and normalising case. */
    fun canonicalise(string: String): String {
        return string.map { ch ->
            val index = REVERSE_TABLE[ch]
                ?: throw IllegalArgumentException("Invalid character '$ch'")
            CHARACTER_TABLE[index]
        }.joinToString("")
    }

    private val CHARACTER_TABLE = charArrayOf(
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'M',
        'N', 'P', 'Q', 'R', 'S', 'T', 'V', 'W', 'X', 'Y', 'Z'
    )

    private val REVERSE_TABLE = buildMap {
        CHARACTER_TABLE.forEachIndexed { index, ch ->
            put(ch, index)
            put(ch.lowercaseChar(), index)
        }
        put('O', 0); put('o', 0)
        put('I', 1); put('i', 1)
        put('L', 1); put('l', 1)
        put('U', 27); put('u', 27)
    }
}
