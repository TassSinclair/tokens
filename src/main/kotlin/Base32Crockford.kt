import java.math.BigInteger

/** Encodes and decodes integers as human-friendly strings using [Crockford's Base32](https://www.crockford.com/base32.html). */
object Base32Crockford {

    private val BASE = BigInteger.valueOf(32)
    private val CHECK_MOD = BigInteger.valueOf(37)

    /** Encodes [input] as a [length]-character Base32 string. */
    fun encode(input: BigInteger, length: Int): String {
        require(input >= BigInteger.ZERO) { "Input must be non-negative" }

        var number = input
        val response = StringBuilder()

        do {
            val remainder = number.mod(BASE).toInt()
            response.insert(0, CHARACTER_TABLE[remainder])
            number /= BASE
        } while (number > BigInteger.ZERO)

        require(response.length <= length) { "Value $input requires more than $length Base32 characters" }

        while (response.length < length) {
            response.insert(0, CHARACTER_TABLE[0])
        }

        return response.toString()
    }

    /** Decodes [string] back to an integer. Case-insensitive. */
    fun decode(string: String): BigInteger {
        require(string.isNotEmpty()) { "Input must not be empty" }

        var result = BigInteger.ZERO

        for (ch in string) {
            val value = REVERSE_TABLE[ch]
                ?: throw IllegalArgumentException("Invalid character '$ch'")
            result = result * BASE + BigInteger.valueOf(value.toLong())
        }

        return result
    }

    /** Computes the check symbol for [value] as `value mod 37`, mapped to one of 37 symbols. */
    fun checkSymbol(value: BigInteger): Char {
        return CHECK_SYMBOL_TABLE[value.mod(CHECK_MOD).toInt()]
    }

    /** Normalises a check symbol to its canonical form. */
    fun canonicaliseCheckSymbol(ch: Char): Char {
        val index = CHECK_REVERSE_TABLE[ch]
            ?: throw IllegalArgumentException("Invalid check symbol '$ch'")
        return CHECK_SYMBOL_TABLE[index]
    }

    /** Converts [string] to its canonical representation, replacing confusable characters and normalising case. */
    fun canonicalise(string: String): String = buildString(string.length) {
        for (ch in string) {
            val index = REVERSE_TABLE[ch]
                ?: throw IllegalArgumentException("Invalid character '$ch'")
            append(CHARACTER_TABLE[index])
        }
    }

    private val CHARACTER_TABLE = charArrayOf(
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'M',
        'N', 'P', 'Q', 'R', 'S', 'T', 'V', 'W', 'X', 'Y', 'Z'
    )

    private val CHECK_SYMBOL_TABLE = charArrayOf(
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'M',
        'N', 'P', 'Q', 'R', 'S', 'T', 'V', 'W', 'X', 'Y', 'Z',
        '*', '~', '$', '=', 'U'
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

    private val CHECK_REVERSE_TABLE = buildMap {
        CHECK_SYMBOL_TABLE.forEachIndexed { index, ch ->
            put(ch, index)
            if (ch.isLetter()) put(ch.lowercaseChar(), index)
        }
        put('O', 0); put('o', 0)
        put('I', 1); put('i', 1)
        put('L', 1); put('l', 1)
    }
}
