import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class Base32CrockfordTest {

    @Test
    fun `encode and decode roundtrip`() {
        for (id in 0..1000) {
            assertEquals(id, Base32Crockford.decode(Base32Crockford.encode(id)))
        }
    }

    @Test
    fun `encoded values use only valid base32 characters`() {
        val ids = listOf(0, 1, 42, 999, 100_000)
        for (id in ids) {
            val encoded = Base32Crockford.encode(id)
            assertTrue(encoded.matches(Regex("[0-9A-HJKMNP-TV-Z]+")),
                "Encoded value '$encoded' for ID $id contains invalid characters")
        }
    }

    @Test
    fun `decode is case-insensitive`() {
        val id = 42
        val encoded = Base32Crockford.encode(id)
        assertEquals(id, Base32Crockford.decode(encoded.lowercase()))
    }

    @Test
    fun `decode handles confusable characters`() {
        val id = 42
        val encoded = Base32Crockford.encode(id)
        val confused = encoded
            .replace('0', 'O')
            .replace('1', 'l')
            .replace('V', 'u')
        assertEquals(id, Base32Crockford.decode(confused))
    }

    @Test
    fun `canonicalise normalises case and confusable characters`() {
        val encoded = Base32Crockford.encode(42)
        assertEquals(encoded, Base32Crockford.canonicalise(encoded.lowercase()))

        val withConfusables = encoded
            .replace('0', 'o')
            .replace('1', 'I')
            .replace('V', 'U')
        assertEquals(encoded, Base32Crockford.canonicalise(withConfusables))
    }

    @Test
    fun `canonicalise rejects invalid characters`() {
        assertFailsWith<IllegalArgumentException> {
            Base32Crockford.canonicalise("!!!!!!")
        }
    }

    @Test
    fun `encode rejects negative input`() {
        assertFailsWith<IllegalArgumentException> {
            Base32Crockford.encode(-1)
        }
    }
}
