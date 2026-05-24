import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class TokenTest {

    class TenantToken : Token {
        constructor(value: String) : super(PREFIX, B32, value)
        constructor(id: Int) : super(PREFIX, B32, id)

        companion object {
            const val PREFIX = 'T'
            val B32 = Base32Crockford(0x948d928L)
        }
    }

    class UserToken : Token {
        constructor(value: String) : super(PREFIX, B32, value)
        constructor(id: Int) : super(PREFIX, B32, id)

        companion object {
            const val PREFIX = 'U'
            val B32 = Base32Crockford(0x2783dabL)
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
    fun `UserToken value matches expected format`() {
        val token = UserToken(42)
        assertTrue(token.value.matches(Regex("U_[0-9A-HJKMNP-TV-Z]{6}")),
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
