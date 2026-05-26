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
        val encoded = value.drop(prefix.length + 1)
        require(encoded.length == length) { "Token must have $length Base32 characters after prefix" }
        this.value = "${prefix}_${Base32Crockford.canonicalise(encoded)}"
    }

    constructor(prefix: String, length: Int, seed: Long, value: String) :
        this(prefix, length, FeistelCipher(key = seed, domainSize = domainSize(length)), value)

    constructor(prefix: String, length: Int, seed: Long, id: Long) :
        this(prefix, length, FeistelCipher(key = seed, domainSize = domainSize(length)), id)

    private constructor(prefix: String, length: Int, cipher: FeistelCipher, id: Long) :
        this(prefix, length, cipher, "${prefix}_${Base32Crockford.encode(cipher.encrypt(id), length)}")

    /** Decodes this token back to its original ID. */
    fun toId(): Long {
        return cipher.decrypt(Base32Crockford.decode(value.substringAfter('_')))
    }

    override fun toString(): String = value

    override fun equals(other: Any?): Boolean = other is Token && this.value == other.value

    override fun hashCode(): Int = value.hashCode()

    override fun compareTo(other: Token) = this.value.compareTo(other.value)

    companion object {
        private fun domainSize(length: Int): Long {
            require(length in 1..12) { "Length must be between 1 and 12, got $length" }
            var size = 1L
            repeat(length) { size *= 32 }
            return size
        }
    }
}
