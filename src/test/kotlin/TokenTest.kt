import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class TokenTest {

    class UserToken : Token {
        constructor(value: String) : super(PREFIX, LENGTH, SEED, value)
        constructor(id: Long) : super(PREFIX, LENGTH, SEED, id)

        companion object {
            const val PREFIX = "U"
            const val LENGTH = 6
            const val SEED = 0x2783dabL
        }
    }

    class InvoiceToken : Token {
        constructor(value: String) : super(PREFIX, LENGTH, SEED, value)
        constructor(id: Long) : super(PREFIX, LENGTH, SEED, id)

        companion object {
            const val PREFIX = "INV"
            const val LENGTH = 8
            const val SEED = 0x3b4ff74L
        }
    }

    class HugeToken : Token {
        constructor(value: String) : super(PREFIX, LENGTH, SEED, value)
        constructor(id: Long) : super(PREFIX, LENGTH, SEED, id)

        companion object {
            const val PREFIX = "H"
            const val LENGTH = 20
            const val SEED = 0x99887766L
        }
    }

    @Test
    fun `UserToken roundtrips`() {
        for (id in 0L..20L) {
            val token = UserToken(id)
            assertEquals(id, token.toId())
        }
    }

    @Test
    fun `InvoiceToken roundtrips`() {
        for (id in 0L..20L) {
            val token = InvoiceToken(id)
            assertEquals(id, token.toId())
        }
    }

    @Test
    fun `UserToken value matches expected format`() {
        val token = UserToken(42L)
        assertTrue(token.value.matches(Regex("U_[0-9A-HJKMNP-TV-Z]{6}[0-9A-HJKMNP-TV-Z*~\$=U]")),
            "Token '${token.value}' does not match expected format")
    }

    @Test
    fun `InvoiceToken value matches expected format`() {
        val token = InvoiceToken(42L)
        assertTrue(token.value.matches(Regex("INV_[0-9A-HJKMNP-TV-Z]{8}[0-9A-HJKMNP-TV-Z*~\$=U]")),
            "Token '${token.value}' does not match expected format")
    }

    @Test
    fun `token constructed from string matches token constructed from ID`() {
        val userFromId = UserToken(42L)
        val userFromString = UserToken(userFromId.value)
        assertEquals(userFromId, userFromString)
        assertEquals(42L, userFromString.toId())

        val invoiceFromId = InvoiceToken(42L)
        val invoiceFromString = InvoiceToken(invoiceFromId.value)
        assertEquals(invoiceFromId, invoiceFromString)
        assertEquals(42L, invoiceFromString.toId())
    }

    @Test
    fun `different token types with same ID produce different values`() {
        val user = UserToken(1L)
        val invoice = InvoiceToken(1L)
        assertTrue(user.value != invoice.value)
    }

    @Test
    fun `adjacent IDs produce tokens that differ in most characters`() {
        val pairs = listOf(0L to 1L, 99L to 100L, 999L to 1000L, 12345L to 12346L)
        for ((a, b) in pairs) {
            val ea = UserToken(a).value.substringAfter('_').dropLast(1)
            val eb = UserToken(b).value.substringAfter('_').dropLast(1)
            val diffs = ea.zip(eb).count { (c1, c2) -> c1 != c2 }
            assertTrue(diffs >= 3, "IDs $a and $b only differ in $diffs/6 chars: $ea vs $eb")
        }
    }

    @Test
    fun `token canonicalises confusable characters`() {
        val canonical = UserToken(42L)
        val encodedPart = canonical.value.substringAfter('_').dropLast(1)
        val checkPart = canonical.value.last()
        val confused = "U_${encodedPart.replace('0', 'O').replace('1', 'l').replace('V', 'u')}$checkPart"
        val fromConfused = UserToken(confused)
        assertEquals(canonical, fromConfused)
        assertEquals(canonical.value, fromConfused.value)
        assertEquals(42L, fromConfused.toId())
    }

    @Test
    fun `token canonicalises lowercase input`() {
        val canonical = UserToken(42L)
        val encodedPart = canonical.value.substringAfter('_').dropLast(1)
        val checkPart = canonical.value.last()
        val lower = "U_${encodedPart.lowercase()}$checkPart"
        val fromLower = UserToken(lower)
        assertEquals(canonical, fromLower)
    }

    @Test
    fun `maximum ID roundtrips for short token`() {
        val maxId = BigInteger.valueOf(32).pow(6).toLong() - 1
        val token = UserToken(maxId)
        assertTrue(token.value.matches(Regex("U_[0-9A-HJKMNP-TV-Z]{6}[0-9A-HJKMNP-TV-Z*~\$=U]")))
        assertEquals(maxId, token.toId())
    }

    @Test
    fun `maximum ID roundtrips for long token`() {
        val maxId = BigInteger.valueOf(32).pow(8).toLong() - 1
        val token = InvoiceToken(maxId)
        assertTrue(token.value.matches(Regex("INV_[0-9A-HJKMNP-TV-Z]{8}[0-9A-HJKMNP-TV-Z*~\$=U]")))
        assertEquals(maxId, token.toId())
    }

    @Test
    fun `token rejects wrong prefix`() {
        val token = UserToken(0L)
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
        val token = UserToken(42L)
        val encoded = token.value.dropLast(1)
        val badCheck = if (token.value.last() == '0') '1' else '0'
        assertFailsWith<IllegalArgumentException> {
            UserToken("$encoded$badCheck")
        }
    }

    @Test
    fun `check symbol detects single-character errors`() {
        val token = UserToken(99L)
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
        val token = UserToken(777L)
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
            val token = UserToken(id)
            val check = token.value.last()
            if (check in extendedSymbols) {
                found.add(check)
                val fromString = UserToken(token.value)
                assertEquals(id, fromString.toId())
            }
            if (found == extendedSymbols) break
        }
        assertEquals(extendedSymbols, found, "Not all extended check symbols were exercised")
    }

    @Test
    fun `check symbol is canonicalised from lowercase`() {
        val token = UserToken(42L)
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
        constructor(id: Long) : super(PREFIX, LENGTH, SEED, id)

        companion object {
            const val PREFIX = "T"
            const val LENGTH = 13
            const val SEED = 0x55443322L
        }
    }

    @Test
    fun `arbitrary-length token roundtrips`() {
        for (id in 0L..20L) {
            val token = HugeToken(id)
            assertEquals(id, token.toId())
        }
    }

    @Test
    fun `arbitrary-length token handles large IDs`() {
        val largeId = BigInteger.valueOf(32).pow(16).toLong()
        val token = HugeToken(largeId)
        assertEquals(largeId, token.toId())
        val fromString = HugeToken(token.value)
        assertEquals(token, fromString)
    }

    @Test
    fun `arbitrary-length token max ID roundtrips`() {
        val token = HugeToken(Long.MAX_VALUE)
        assertEquals(Long.MAX_VALUE, token.toId())
    }

    @Test
    fun `length 13 supports full long range`() {
        for (id in 0L..20L) {
            val token = Length13Token(id)
            assertEquals(id, token.toId())
        }
        val token = Length13Token(Long.MAX_VALUE)
        assertEquals(Long.MAX_VALUE, token.toId())
    }

    @Test
    fun `length 13 token matches expected format`() {
        val token = Length13Token(42L)
        assertTrue(token.value.matches(Regex("T_[0-9A-HJKMNP-TV-Z]{13}[0-9A-HJKMNP-TV-Z*~\$=U]")),
            "Token '${token.value}' does not match expected format")
    }

    @Test
    fun `long range IDs are unique`() {
        val base = Long.MAX_VALUE - 100
        val tokens = (0L..100L).map { Length13Token(base + it) }
        val values = tokens.map { it.value }.toSet()
        assertEquals(101, values.size, "Expected 101 unique token values")
    }

    @Test
    fun `token constructed from string roundtrips at long boundary`() {
        val id = Long.MAX_VALUE
        val token = Length13Token(id)
        val fromString = Length13Token(token.value)
        assertEquals(token, fromString)
        assertEquals(id, fromString.toId())
    }

    @Test
    fun `arbitrary-length token check symbol detects corruption`() {
        val token = HugeToken(Long.MAX_VALUE - 42)
        val encoded = token.value.dropLast(1)
        val badCheck = if (token.value.last() == '0') '1' else '0'
        assertFailsWith<IllegalArgumentException> {
            HugeToken("$encoded$badCheck")
        }
    }
}
