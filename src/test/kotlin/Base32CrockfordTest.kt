import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class Base32CrockfordTest {

    private val b32 = Base32Crockford(0x2783dabL)

    @Test
    fun `encode and decode roundtrip`() {
        for (id in 0..20) {
            assertEquals(id, b32.decode(b32.encode(id)))
        }
    }

    @Test
    fun `encoded values use only valid base32 characters`() {
        val ids = listOf(0, 1, 42, 999, 100_000)
        for (id in ids) {
            val encoded = b32.encode(id)
            assertTrue(encoded.matches(Regex("[0-9A-HJKMNP-TV-Z]+")),
                "Encoded value '$encoded' for ID $id contains invalid characters")
        }
    }

    @Test
    fun `decode is case-insensitive`() {
        val id = 42
        val encoded = b32.encode(id)
        assertEquals(id, b32.decode(encoded.lowercase()))
    }

    @Test
    fun `adjacent IDs produce strings that differ in most characters`() {
        val pairs = listOf(0 to 1, 99 to 100, 999 to 1000, 12345 to 12346)
        for ((a, b) in pairs) {
            val ea = b32.encode(a)
            val eb = b32.encode(b)
            val diffs = ea.zip(eb).count { (c1, c2) -> c1 != c2 }
            assertTrue(diffs >= 3, "IDs $a and $b only differ in $diffs/6 chars: $ea vs $eb")
        }
    }

    @Test
    fun `encode rejects negative input`() {
        assertFailsWith<IllegalArgumentException> {
            b32.encode(-1)
        }
    }
}
