import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class TokenTest {

    class TenantToken : Token {
        constructor(value: String) : super(PREFIX, SEED, value)
        constructor(id: Int) : super(PREFIX, SEED, id)

        companion object {
            const val PREFIX = "T"
            const val SEED = 0x948d928L
        }
    }

    class InvoiceToken : Token {
        constructor(value: String) : super(PREFIX, SEED, value)
        constructor(id: Int) : super(PREFIX, SEED, id)

        companion object {
            const val PREFIX = "INV"
            const val SEED = 0x0000000L
        }
    }

    class UserToken : Token {
        constructor(value: String) : super(PREFIX, SEED, value)
        constructor(id: Int) : super(PREFIX, SEED, id)

        companion object {
            const val PREFIX = "U"
            const val SEED = 0x2783dabL
        }
    }

    @Test
    fun `UserToken roundtrips`() {
        for (id in 0..20) {
            val token = UserToken(id)
            assertEquals(id, token.toId())
        }
    }

    @Test
    fun `TenantToken roundtrips`() {
        for (id in 0..20) {
            val token = TenantToken(id)
            assertEquals(id, token.toId())
        }
    }

    @Test
    fun `InvoiceToken roundtrips`() {
        for (id in 0..20) {
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
        assertTrue(token.value.matches(Regex("INV_[0-9A-HJKMNP-TV-Z]{6}")),
            "Token '${token.value}' does not match expected format")
    }

    @Test
    fun `token constructed from string matches token constructed from ID`() {
        val userFromId = UserToken(42)
        val userFromString = UserToken(userFromId.value)
        assertEquals(userFromId, userFromString)
        assertEquals(42, userFromString.toId())

        val tenantFromId = TenantToken(42)
        val tenantFromString = TenantToken(tenantFromId.value)
        assertEquals(tenantFromId, tenantFromString)
        assertEquals(42, tenantFromString.toId())
    }

    @Test
    fun `different token types with same ID produce different values`() {
        val user = UserToken(1)
        val tenant = TenantToken(1)
        assertTrue(user.value != tenant.value)
    }

    @Test
    fun `adjacent IDs produce tokens that differ in most characters`() {
        val pairs = listOf(0 to 1, 99 to 100, 999 to 1000, 12345 to 12346)
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
        assertEquals(42, fromConfused.toId())
    }

    @Test
    fun `token canonicalises lowercase input`() {
        val canonical = UserToken(42)
        val lower = "U_${canonical.value.drop(2).lowercase()}"
        val fromLower = UserToken(lower)
        assertEquals(canonical, fromLower)
    }

    @Test
    fun `maximum ID roundtrips`() {
        val maxId = 32 * 32 * 32 * 32 * 32 * 32 - 1
        val token = UserToken(maxId)
        assertTrue(token.value.matches(Regex("U_[0-9A-HJKMNP-TV-Z]{6}")),
            "Token '${token.value}' does not match expected format")
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
}
