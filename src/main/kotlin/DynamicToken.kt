/** A [Token] whose type parameters are supplied at runtime rather than compiled in. */
class DynamicToken : Token {
    constructor(prefix: String, length: Int, seed: Long, value: String) : super(prefix, length, seed, value)
    constructor(prefix: String, length: Int, seed: Long, id: Long) : super(prefix, length, seed, id)
}
