import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class Base32CrockfordTest {

    @Test
    fun `encode and decode roundtrip`() {
        for (id in 0L..1000L) {
            assertEquals(id, Base32Crockford.decode(Base32Crockford.encode(id, 6)))
        }
    }

    @Test
    fun `encoded values use only valid base32 characters`() {
        val ids = listOf(0L, 1L, 42L, 999L, 100_000L)
        for (id in ids) {
            val encoded = Base32Crockford.encode(id, 6)
            assertTrue(encoded.matches(Regex("[0-9A-HJKMNP-TV-Z]+")),
                "Encoded value '$encoded' for ID $id contains invalid characters")
        }
    }

    @Test
    fun `encode pads to requested length`() {
        val encoded = Base32Crockford.encode(1, 6)
        assertEquals(6, encoded.length)

        val longer = Base32Crockford.encode(1, 10)
        assertEquals(10, longer.length)
    }

    @Test
    fun `encode rejects value that exceeds length`() {
        assertFailsWith<IllegalArgumentException> {
            Base32Crockford.encode(32L * 32 * 32, 3)
        }
    }

    @Test
    fun `decode is case-insensitive`() {
        val id = 42L
        val encoded = Base32Crockford.encode(id, 6)
        assertEquals(id, Base32Crockford.decode(encoded.lowercase()))
    }

    @Test
    fun `decode handles confusable characters`() {
        val id = 42L
        val encoded = Base32Crockford.encode(id, 6)
        val confused = encoded
            .replace('0', 'O')
            .replace('1', 'l')
            .replace('V', 'u')
        assertEquals(id, Base32Crockford.decode(confused))
    }

    @Test
    fun `canonicalise normalises case and confusable characters`() {
        val encoded = Base32Crockford.encode(42, 6)
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
            Base32Crockford.encode(-1, 6)
        }
    }
}
