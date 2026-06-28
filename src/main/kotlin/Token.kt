import java.math.BigInteger

/** Pairs a type [prefix] with an encoded ID, so tokens are self-describing and can't be mixed across entity types. */
abstract class Token private constructor(
    prefix: String,
    length: Int,
    private val cipher: FeistelCipher,
    value: String,
) : Comparable<Token> {

    val value: String

    init {
        require(value.startsWith("${prefix}_")) { "Token must start with '${prefix}_'" }
        val afterPrefix = value.drop(prefix.length + 1)
        require(afterPrefix.length == length + 1) { "Token must have $length Base32 characters plus check symbol after prefix" }
        val encoded = afterPrefix.dropLast(1)
        val checkChar = afterPrefix.last()
        val canonicalEncoded = Base32Crockford.canonicalise(encoded)
        val numericValue = Base32Crockford.decode(canonicalEncoded)
        val expectedCheck = Base32Crockford.checkSymbol(numericValue)
        val normalizedCheck = Base32Crockford.canonicaliseCheckSymbol(checkChar)
        require(normalizedCheck == expectedCheck) { "Invalid check symbol" }
        this.value = "${prefix}_${canonicalEncoded}${expectedCheck}"
    }

    constructor(prefix: String, length: Int, seed: Long, value: String) :
        this(prefix, length, FeistelCipher(key = seed, domainSize = domainSize(length)), value)

    constructor(prefix: String, length: Int, seed: Long, id: BigInteger) :
        this(prefix, length, FeistelCipher(key = seed, domainSize = domainSize(length)), id)

    private constructor(prefix: String, length: Int, cipher: FeistelCipher, id: BigInteger) :
        this(prefix, length, cipher, cipher.encrypt(id).let { encrypted ->
            "${prefix}_${Base32Crockford.encode(encrypted, length)}${Base32Crockford.checkSymbol(encrypted)}"
        })

    /** Decodes this token back to its original ID. */
    fun toId(): BigInteger {
        val encoded = value.substringAfter('_').dropLast(1)
        return cipher.decrypt(Base32Crockford.decode(encoded))
    }

    override fun toString(): String = value

    override fun equals(other: Any?): Boolean = other is Token && this.value == other.value

    override fun hashCode(): Int = value.hashCode()

    override fun compareTo(other: Token) = this.value.compareTo(other.value)

    companion object {
        private fun domainSize(length: Int): BigInteger {
            require(length >= 1) { "Length must be at least 1, got $length" }
            return BigInteger.valueOf(32).pow(length)
        }
    }
}
