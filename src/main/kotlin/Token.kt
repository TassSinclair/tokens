/** Pairs a type [prefix] with an encoded ID, so tokens are self-describing and can't be mixed across entity types. */
open class Token private constructor(
    prefix: Char,
    private val cipher: FeistelCipher,
    value: String,
) : Comparable<Token> {

    val value: String

    init {
        require(value.startsWith("${prefix}_")) { "Token must start with '${prefix}_'" }
        require(value.length == 8) { "Token must be 8 characters long" }
        this.value = "${prefix}_${Base32Crockford.canonicalise(value.drop(2))}"
    }

    constructor(prefix: Char, seed: Long, value: String) :
        this(prefix, FeistelCipher(key = seed, domainSize = DOMAIN_SIZE), value)

    constructor(prefix: Char, seed: Long, id: Int) :
        this(prefix, FeistelCipher(key = seed, domainSize = DOMAIN_SIZE), id)

    private constructor(prefix: Char, cipher: FeistelCipher, id: Int) :
        this(prefix, cipher, "${prefix}_${Base32Crockford.encode(cipher.encrypt(id))}")

    /** Decodes this token back to its original integer ID. */
    fun toId(): Int {
        return cipher.decrypt(Base32Crockford.decode(value.drop(2)))
    }

    override fun toString(): String = value

    override fun equals(other: Any?): Boolean = other is Token && this.value == other.value

    override fun hashCode(): Int = value.hashCode()

    override fun compareTo(other: Token) = this.value.compareTo(other.value)

    companion object {
        private const val DOMAIN_SIZE = 32 * 32 * 32 * 32 * 32 * 32
    }
}
