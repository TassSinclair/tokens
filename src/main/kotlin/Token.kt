/** Pairs a type [prefix] with an encoded ID, so tokens are self-describing and can't be mixed across entity types. */
open class Token(
    prefix: Char,
    private val base32Crockford: Base32Crockford,
    val value: String,
) : Comparable<Token> {

    init {
        require(value.startsWith("${prefix}_")) { "Token must start with '${prefix}_'" }
        require(value.length == 8) { "Token must be 8 characters long" }
        require(base32Crockford.check(value.drop(2))) {
            "'${value}' does not contain a valid base32 payload"
        }
    }

    constructor(prefix: Char, base32Crockford: Base32Crockford, id: Int) :
        this(prefix, base32Crockford, "${prefix}_${base32Crockford.encode(id)}")

    /** Decodes this token back to its original integer ID. */
    fun toId(): Int {
        return base32Crockford.decode(value.drop(2))
    }

    override fun toString(): String = value

    override fun equals(other: Any?): Boolean = other is Token && this.value == other.value

    override fun hashCode(): Int = value.hashCode()

    override fun compareTo(other: Token) = this.value.compareTo(other.value)
}
