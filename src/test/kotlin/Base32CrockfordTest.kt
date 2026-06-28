import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class Base32CrockfordTest {

    @Test
    fun `encode and decode roundtrip`() {
        for (id in 0L..1000L) {
            assertEquals(BigInteger.valueOf(id), Base32Crockford.decode(Base32Crockford.encode(BigInteger.valueOf(id), 6)))
        }
    }

    @Test
    fun `encoded values use only valid base32 characters`() {
        val ids = listOf(0L, 1L, 42L, 999L, 100_000L)
        for (id in ids) {
            val encoded = Base32Crockford.encode(BigInteger.valueOf(id), 6)
            assertTrue(encoded.matches(Regex("[0-9A-HJKMNP-TV-Z]+")),
                "Encoded value '$encoded' for ID $id contains invalid characters")
        }
    }

    @Test
    fun `encode pads to requested length`() {
        val encoded = Base32Crockford.encode(BigInteger.ONE, 6)
        assertEquals(6, encoded.length)

        val longer = Base32Crockford.encode(BigInteger.ONE, 10)
        assertEquals(10, longer.length)
    }

    @Test
    fun `encode rejects value that exceeds length`() {
        assertFailsWith<IllegalArgumentException> {
            Base32Crockford.encode(BigInteger.valueOf(32L * 32 * 32), 3)
        }
    }

    @Test
    fun `decode is case-insensitive`() {
        val id = BigInteger.valueOf(42)
        val encoded = Base32Crockford.encode(id, 6)
        assertEquals(id, Base32Crockford.decode(encoded.lowercase()))
    }

    @Test
    fun `decode handles confusable characters`() {
        val id = BigInteger.valueOf(42)
        val encoded = Base32Crockford.encode(id, 6)
        val confused = encoded
            .replace('0', 'O')
            .replace('1', 'l')
            .replace('V', 'u')
        assertEquals(id, Base32Crockford.decode(confused))
    }

    @Test
    fun `canonicalise normalises case and confusable characters`() {
        val encoded = Base32Crockford.encode(BigInteger.valueOf(42), 6)
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
            Base32Crockford.encode(BigInteger.valueOf(-1), 6)
        }
    }

    @Test
    fun `check symbol is deterministic`() {
        val value = BigInteger.valueOf(12345)
        assertEquals(Base32Crockford.checkSymbol(value), Base32Crockford.checkSymbol(value))
    }

    @Test
    fun `check symbol uses extended symbol set`() {
        val symbols = (0L..36L).map { Base32Crockford.checkSymbol(BigInteger.valueOf(it)) }.toSet()
        assertEquals(37, symbols.size)
        assertTrue('*' in symbols)
        assertTrue('~' in symbols)
        assertTrue('$' in symbols)
        assertTrue('=' in symbols)
        assertTrue('U' in symbols)
    }

    @Test
    fun `canonicalise check symbol is case-insensitive`() {
        assertEquals('A', Base32Crockford.canonicaliseCheckSymbol('a'))
        assertEquals('Z', Base32Crockford.canonicaliseCheckSymbol('z'))
        assertEquals('U', Base32Crockford.canonicaliseCheckSymbol('u'))
    }

    @Test
    fun `canonicalise check symbol handles confusables`() {
        assertEquals('0', Base32Crockford.canonicaliseCheckSymbol('O'))
        assertEquals('1', Base32Crockford.canonicaliseCheckSymbol('I'))
        assertEquals('1', Base32Crockford.canonicaliseCheckSymbol('L'))
    }

    @Test
    fun `encode and decode roundtrip for large values`() {
        val large = BigInteger.valueOf(32).pow(16) - BigInteger.ONE
        val encoded = Base32Crockford.encode(large, 16)
        assertEquals(16, encoded.length)
        assertEquals(large, Base32Crockford.decode(encoded))
    }
}
