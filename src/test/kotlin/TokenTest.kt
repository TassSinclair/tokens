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
        val token = UserToken(42)
        assertTrue(token.value.matches(Regex("U_[0-9A-HJKMNP-TV-Z]{6}")),
            "Token '${token.value}' does not match expected format")
    }

    @Test
    fun `InvoiceToken value matches expected format`() {
        val token = InvoiceToken(42)
        assertTrue(token.value.matches(Regex("INV_[0-9A-HJKMNP-TV-Z]{8}")),
            "Token '${token.value}' does not match expected format")
    }

    @Test
    fun `token constructed from string matches token constructed from ID`() {
        val userFromId = UserToken(42)
        val userFromString = UserToken(userFromId.value)
        assertEquals(userFromId, userFromString)
        assertEquals(42L, userFromString.toId())

        val invoiceFromId = InvoiceToken(42)
        val invoiceFromString = InvoiceToken(invoiceFromId.value)
        assertEquals(invoiceFromId, invoiceFromString)
        assertEquals(42L, invoiceFromString.toId())
    }

    @Test
    fun `different token types with same ID produce different values`() {
        val user = UserToken(1)
        val invoice = InvoiceToken(1)
        assertTrue(user.value != invoice.value)
    }

    @Test
    fun `adjacent IDs produce tokens that differ in most characters`() {
        val pairs = listOf(0L to 1L, 99L to 100L, 999L to 1000L, 12345L to 12346L)
        for ((a, b) in pairs) {
            val ea = UserToken(a).value.substringAfter('_')
            val eb = UserToken(b).value.substringAfter('_')
            val diffs = ea.zip(eb).count { (c1, c2) -> c1 != c2 }
            assertTrue(diffs >= 3, "IDs $a and $b only differ in $diffs/6 chars: $ea vs $eb")
        }
    }

    @Test
    fun `token canonicalises confusable characters`() {
        val canonical = UserToken(42)
        val confused = canonical.value
            .replace('0', 'O')
            .replace('1', 'l')
            .replace('V', 'u')
        val fromConfused = UserToken(confused)
        assertEquals(canonical, fromConfused)
        assertEquals(canonical.value, fromConfused.value)
        assertEquals(42L, fromConfused.toId())
    }

    @Test
    fun `token canonicalises lowercase input`() {
        val canonical = UserToken(42)
        val lower = "U_${canonical.value.drop(2).lowercase()}"
        val fromLower = UserToken(lower)
        assertEquals(canonical, fromLower)
    }

    @Test
    fun `maximum ID roundtrips for short token`() {
        val maxId = 32L * 32 * 32 * 32 * 32 * 32 - 1
        val token = UserToken(maxId)
        assertTrue(token.value.matches(Regex("U_[0-9A-HJKMNP-TV-Z]{6}")))
        assertEquals(maxId, token.toId())
    }

    @Test
    fun `maximum ID roundtrips for long token`() {
        val maxId = 32L * 32 * 32 * 32 * 32 * 32 * 32 * 32 - 1
        val token = InvoiceToken(maxId)
        assertTrue(token.value.matches(Regex("INV_[0-9A-HJKMNP-TV-Z]{8}")))
        assertEquals(maxId, token.toId())
    }

    @Test
    fun `token rejects wrong prefix`() {
        val token = UserToken(0)
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
    fun `token rejects encoded length beyond 12`() {
        class OversizedToken : Token {
            constructor(id: Long) : super("X", 13, 0x1L, id)
        }
        assertFailsWith<IllegalArgumentException> {
            OversizedToken(0)
        }
    }
}
