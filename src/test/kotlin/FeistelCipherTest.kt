import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class FeistelCipherTest {

    private val domainSize = 32L * 32 * 32 * 32 * 32 * 32
    private val cipher = FeistelCipher(key = 0x2783dabL, domainSize = domainSize)

    @Test
    fun `encrypt and decrypt roundtrip`() {
        for (id in 0L..20L) {
            assertEquals(id, cipher.decrypt(cipher.encrypt(id)))
        }
    }

    @Test
    fun `first 100_000 encrypted values are unique`() {
        val seen = mutableSetOf<Long>()
        for (id in 0L until 100_000L) {
            val encrypted = cipher.encrypt(id)
            assertTrue(seen.add(encrypted), "Collision at ID $id: $encrypted")
            assertEquals(id, cipher.decrypt(encrypted), "Roundtrip failed for ID $id")
        }
    }

    @Test
    fun `maximum domain value roundtrips`() {
        val maxId = domainSize - 1
        val encrypted = cipher.encrypt(maxId)
        assertTrue(encrypted in 0 until domainSize, "Encrypted value $encrypted outside domain")
        assertEquals(maxId, cipher.decrypt(encrypted))
    }

    @Test
    fun `larger domain size works`() {
        val largeDomain = 32L * 32 * 32 * 32 * 32 * 32 * 32 * 32
        val largeCipher = FeistelCipher(key = 0x12345L, domainSize = largeDomain)
        for (id in 0L..20L) {
            assertEquals(id, largeCipher.decrypt(largeCipher.encrypt(id)))
        }
        val maxId = largeDomain - 1
        assertEquals(maxId, largeCipher.decrypt(largeCipher.encrypt(maxId)))
    }

    @Test
    fun `encrypt rejects negative input`() {
        assertFailsWith<IllegalArgumentException> {
            cipher.encrypt(-1)
        }
    }

    @Test
    fun `encrypt rejects input at domain size`() {
        assertFailsWith<IllegalArgumentException> {
            cipher.encrypt(domainSize)
        }
    }

    @Test
    fun `decrypt rejects negative input`() {
        assertFailsWith<IllegalArgumentException> {
            cipher.decrypt(-1)
        }
    }

    @Test
    fun `decrypt rejects input at domain size`() {
        assertFailsWith<IllegalArgumentException> {
            cipher.decrypt(domainSize)
        }
    }
}
