import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class FeistelCipherTest {

    private val domainSize = BigInteger.valueOf(32).pow(6)
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
        val maxId = domainSize.toLong() - 1
        val encrypted = cipher.encrypt(maxId)
        assertTrue(encrypted >= 0 && encrypted < domainSize.toLong(), "Encrypted value $encrypted outside domain")
        assertEquals(maxId, cipher.decrypt(encrypted))
    }

    @Test
    fun `larger domain size works`() {
        val largeDomain = BigInteger.valueOf(32).pow(8)
        val largeCipher = FeistelCipher(key = 0x12345L, domainSize = largeDomain)
        for (id in 0L..20L) {
            assertEquals(id, largeCipher.decrypt(largeCipher.encrypt(id)))
        }
        val maxId = largeDomain.toLong() - 1
        assertEquals(maxId, largeCipher.decrypt(largeCipher.encrypt(maxId)))
    }

    @Test
    fun `capped long domain works`() {
        val longDomain = BigInteger.ONE.shiftLeft(63)
        val longCipher = FeistelCipher(key = 0xABCDEFL, domainSize = longDomain)
        for (id in 0L..20L) {
            assertEquals(id, longCipher.decrypt(longCipher.encrypt(id)))
        }
        assertEquals(Long.MAX_VALUE, longCipher.decrypt(longCipher.encrypt(Long.MAX_VALUE)))
    }

    @Test
    fun `encrypt rejects negative input`() {
        assertFailsWith<IllegalArgumentException> {
            cipher.encrypt(-1L)
        }
    }

    @Test
    fun `encrypt rejects input at domain size`() {
        assertFailsWith<IllegalArgumentException> {
            cipher.encrypt(domainSize.toLong())
        }
    }

    @Test
    fun `decrypt rejects negative input`() {
        assertFailsWith<IllegalArgumentException> {
            cipher.decrypt(-1L)
        }
    }

    @Test
    fun `decrypt rejects input at domain size`() {
        assertFailsWith<IllegalArgumentException> {
            cipher.decrypt(domainSize.toLong())
        }
    }
}
