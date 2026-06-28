import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class TokenTest {

    class UserToken : Token {
        constructor(value: String) : super(PREFIX, LENGTH, SEED, value)
        constructor(id: BigInteger) : super(PREFIX, LENGTH, SEED, id)

        companion object {
            const val PREFIX = "U"
            const val LENGTH = 6
            const val SEED = 0x2783dabL
        }
    }

    class InvoiceToken : Token {
        constructor(value: String) : super(PREFIX, LENGTH, SEED, value)
        constructor(id: BigInteger) : super(PREFIX, LENGTH, SEED, id)

        companion object {
            const val PREFIX = "INV"
            const val LENGTH = 8
            const val SEED = 0x3b4ff74L
        }
    }

    class HugeToken : Token {
        constructor(value: String) : super(PREFIX, LENGTH, SEED, value)
        constructor(id: BigInteger) : super(PREFIX, LENGTH, SEED, id)

        companion object {
            const val PREFIX = "H"
            const val LENGTH = 20
            const val SEED = 0x99887766L
        }
    }

    @Test
    fun `UserToken roundtrips`() {
        for (id in 0L..20L) {
            val token = UserToken(BigInteger.valueOf(id))
            assertEquals(BigInteger.valueOf(id), token.toId())
        }
    }

    @Test
    fun `InvoiceToken roundtrips`() {
        for (id in 0L..20L) {
            val token = InvoiceToken(BigInteger.valueOf(id))
            assertEquals(BigInteger.valueOf(id), token.toId())
        }
    }

    @Test
    fun `UserToken value matches expected format`() {
        val token = UserToken(BigInteger.valueOf(42))
        assertTrue(token.value.matches(Regex("U_[0-9A-HJKMNP-TV-Z]{6}[0-9A-HJKMNP-TV-Z*~\$=U]")),
            "Token '${token.value}' does not match expected format")
    }

    @Test
    fun `InvoiceToken value matches expected format`() {
        val token = InvoiceToken(BigInteger.valueOf(42))
        assertTrue(token.value.matches(Regex("INV_[0-9A-HJKMNP-TV-Z]{8}[0-9A-HJKMNP-TV-Z*~\$=U]")),
            "Token '${token.value}' does not match expected format")
    }

    @Test
    fun `token constructed from string matches token constructed from ID`() {
        val userFromId = UserToken(BigInteger.valueOf(42))
        val userFromString = UserToken(userFromId.value)
        assertEquals(userFromId, userFromString)
        assertEquals(BigInteger.valueOf(42), userFromString.toId())

        val invoiceFromId = InvoiceToken(BigInteger.valueOf(42))
        val invoiceFromString = InvoiceToken(invoiceFromId.value)
        assertEquals(invoiceFromId, invoiceFromString)
        assertEquals(BigInteger.valueOf(42), invoiceFromString.toId())
    }

    @Test
    fun `different token types with same ID produce different values`() {
        val user = UserToken(BigInteger.valueOf(1))
        val invoice = InvoiceToken(BigInteger.valueOf(1))
        assertTrue(user.value != invoice.value)
    }

    @Test
    fun `adjacent IDs produce tokens that differ in most characters`() {
        val pairs = listOf(0L to 1L, 99L to 100L, 999L to 1000L, 12345L to 12346L)
        for ((a, b) in pairs) {
            val ea = UserToken(BigInteger.valueOf(a)).value.substringAfter('_').dropLast(1)
            val eb = UserToken(BigInteger.valueOf(b)).value.substringAfter('_').dropLast(1)
            val diffs = ea.zip(eb).count { (c1, c2) -> c1 != c2 }
            assertTrue(diffs >= 3, "IDs $a and $b only differ in $diffs/6 chars: $ea vs $eb")
        }
    }

    @Test
    fun `token canonicalises confusable characters`() {
        val canonical = UserToken(BigInteger.valueOf(42))
        val encodedPart = canonical.value.substringAfter('_').dropLast(1)
        val checkPart = canonical.value.last()
        val confused = "U_${encodedPart.replace('0', 'O').replace('1', 'l').replace('V', 'u')}$checkPart"
        val fromConfused = UserToken(confused)
        assertEquals(canonical, fromConfused)
        assertEquals(canonical.value, fromConfused.value)
        assertEquals(BigInteger.valueOf(42), fromConfused.toId())
    }

    @Test
    fun `token canonicalises lowercase input`() {
        val canonical = UserToken(BigInteger.valueOf(42))
        val encodedPart = canonical.value.substringAfter('_').dropLast(1)
        val checkPart = canonical.value.last()
        val lower = "U_${encodedPart.lowercase()}$checkPart"
        val fromLower = UserToken(lower)
        assertEquals(canonical, fromLower)
    }

    @Test
    fun `maximum ID roundtrips for short token`() {
        val maxId = BigInteger.valueOf(32).pow(6) - BigInteger.ONE
        val token = UserToken(BigInteger.valueOf(maxId.toLong()))
        assertTrue(token.value.matches(Regex("U_[0-9A-HJKMNP-TV-Z]{6}[0-9A-HJKMNP-TV-Z*~\$=U]")))
        assertEquals(maxId, token.toId())
    }

    @Test
    fun `maximum ID roundtrips for long token`() {
        val maxId = BigInteger.valueOf(32).pow(8) - BigInteger.ONE
        val token = InvoiceToken(BigInteger.valueOf(maxId.toLong()))
        assertTrue(token.value.matches(Regex("INV_[0-9A-HJKMNP-TV-Z]{8}[0-9A-HJKMNP-TV-Z*~\$=U]")))
        assertEquals(maxId, token.toId())
    }

    @Test
    fun `token rejects wrong prefix`() {
        val token = UserToken(BigInteger.valueOf(0))
        val badValue = "X" + token.value.drop(1)
        assertFailsWith<IllegalArgumentException> {
            UserToken(badValue)
        }
    }

    @Test
    fun `token rejects wrong length`() {
        assertFailsWith<IllegalArgumentException> {
            UserToken("U_12345")
        }
    }

    @Test
    fun `token rejects invalid check symbol`() {
        val token = UserToken(BigInteger.valueOf(42))
        val encoded = token.value.dropLast(1)
        val badCheck = if (token.value.last() == '0') '1' else '0'
        assertFailsWith<IllegalArgumentException> {
            UserToken("$encoded$badCheck")
        }
    }

    @Test
    fun `check symbol detects single-character errors`() {
        val token = UserToken(BigInteger.valueOf(99))
        val chars = token.value.substringAfter('_')
        val encoded = chars.dropLast(1)
        val check = chars.last()
        for (i in encoded.indices) {
            val original = encoded[i]
            val replacement = if (original == '0') 'A' else '0'
            val corrupted = encoded.substring(0, i) + replacement + encoded.substring(i + 1)
            assertFailsWith<IllegalArgumentException>("Corruption at position $i was not detected") {
                UserToken("U_$corrupted$check")
            }
        }
    }

    @Test
    fun `check symbol detects transposition errors`() {
        val token = UserToken(BigInteger.valueOf(777))
        val chars = token.value.substringAfter('_')
        val encoded = chars.dropLast(1)
        val check = chars.last()
        for (i in 0 until encoded.length - 1) {
            if (encoded[i] == encoded[i + 1]) continue
            val transposed = encoded.substring(0, i) + encoded[i + 1] + encoded[i] + encoded.substring(i + 2)
            assertFailsWith<IllegalArgumentException>("Transposition at positions $i-${i + 1} was not detected") {
                UserToken("U_$transposed$check")
            }
        }
    }

    @Test
    fun `check symbol from extended set roundtrips`() {
        val extendedSymbols = setOf('*', '~', '$', '=', 'U')
        val found = mutableSetOf<Char>()
        for (id in 0L..1000L) {
            val token = UserToken(BigInteger.valueOf(id))
            val check = token.value.last()
            if (check in extendedSymbols) {
                found.add(check)
                val fromString = UserToken(token.value)
                assertEquals(BigInteger.valueOf(id), fromString.toId())
            }
            if (found == extendedSymbols) break
        }
        assertEquals(extendedSymbols, found, "Not all extended check symbols were exercised")
    }

    @Test
    fun `check symbol is canonicalised from lowercase`() {
        val token = UserToken(BigInteger.valueOf(42))
        val encoded = token.value.substringAfter('_').dropLast(1)
        val check = token.value.last()
        if (check.isLetter()) {
            val withLowerCheck = "U_$encoded${check.lowercaseChar()}"
            val fromLower = UserToken(withLowerCheck)
            assertEquals(token, fromLower)
        }
    }

    class Length13Token : Token {
        constructor(value: String) : super(PREFIX, LENGTH, SEED, value)
        constructor(id: BigInteger) : super(PREFIX, LENGTH, SEED, id)

        companion object {
            const val PREFIX = "T"
            const val LENGTH = 13
            const val SEED = 0x55443322L
        }
    }

    @Test
    fun `arbitrary-length token roundtrips`() {
        for (id in 0L..20L) {
            val token = HugeToken(BigInteger.valueOf(id))
            assertEquals(BigInteger.valueOf(id), token.toId())
        }
    }

    @Test
    fun `arbitrary-length token handles large IDs`() {
        val largeId = BigInteger.valueOf(32).pow(16)
        val token = HugeToken(largeId)
        assertEquals(largeId, token.toId())
        val fromString = HugeToken(token.value)
        assertEquals(token, fromString)
    }

    @Test
    fun `arbitrary-length token max ID roundtrips`() {
        val maxId = BigInteger.valueOf(32).pow(20) - BigInteger.ONE
        val token = HugeToken(maxId)
        assertEquals(maxId, token.toId())
    }

    @Test
    fun `length 13 exceeds old Long limit and roundtrips`() {
        for (id in 0L..20L) {
            val token = Length13Token(BigInteger.valueOf(id))
            assertEquals(BigInteger.valueOf(id), token.toId())
        }
        val maxId = BigInteger.valueOf(32).pow(13) - BigInteger.ONE
        assertTrue(maxId > BigInteger.valueOf(Long.MAX_VALUE), "32^13 - 1 should exceed Long.MAX_VALUE")
        val token = Length13Token(maxId)
        assertEquals(maxId, token.toId())
    }

    @Test
    fun `length 13 token matches expected format`() {
        val token = Length13Token(BigInteger.valueOf(42))
        assertTrue(token.value.matches(Regex("T_[0-9A-HJKMNP-TV-Z]{13}[0-9A-HJKMNP-TV-Z*~\$=U]")),
            "Token '${token.value}' does not match expected format")
    }

    @Test
    fun `arbitrary-length token IDs beyond Long range are unique`() {
        val base = BigInteger.valueOf(Long.MAX_VALUE)
        val tokens = (0L..100L).map { Length13Token(base + BigInteger.valueOf(it)) }
        val values = tokens.map { it.value }.toSet()
        assertEquals(101, values.size, "Expected 101 unique token values")
    }

    @Test
    fun `arbitrary-length token constructed from string roundtrips`() {
        val id = BigInteger.valueOf(Long.MAX_VALUE) + BigInteger.ONE
        val token = Length13Token(id)
        val fromString = Length13Token(token.value)
        assertEquals(token, fromString)
        assertEquals(id, fromString.toId())
    }

    @Test
    fun `arbitrary-length token check symbol detects corruption`() {
        val id = BigInteger.valueOf(Long.MAX_VALUE) + BigInteger.valueOf(42)
        val token = HugeToken(id)
        val encoded = token.value.dropLast(1)
        val badCheck = if (token.value.last() == '0') '1' else '0'
        assertFailsWith<IllegalArgumentException> {
            HugeToken("$encoded$badCheck")
        }
    }
}
