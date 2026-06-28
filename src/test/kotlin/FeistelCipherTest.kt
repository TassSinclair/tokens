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
            val bigId = BigInteger.valueOf(id)
            assertEquals(bigId, cipher.decrypt(cipher.encrypt(bigId)))
        }
    }

    @Test
    fun `first 100_000 encrypted values are unique`() {
        val seen = mutableSetOf<BigInteger>()
        for (id in 0L until 100_000L) {
            val bigId = BigInteger.valueOf(id)
            val encrypted = cipher.encrypt(bigId)
            assertTrue(seen.add(encrypted), "Collision at ID $id: $encrypted")
            assertEquals(bigId, cipher.decrypt(encrypted), "Roundtrip failed for ID $id")
        }
    }

    @Test
    fun `maximum domain value roundtrips`() {
        val maxId = domainSize - BigInteger.ONE
        val encrypted = cipher.encrypt(maxId)
        assertTrue(encrypted >= BigInteger.ZERO && encrypted < domainSize, "Encrypted value $encrypted outside domain")
        assertEquals(maxId, cipher.decrypt(encrypted))
    }

    @Test
    fun `larger domain size works`() {
        val largeDomain = BigInteger.valueOf(32).pow(8)
        val largeCipher = FeistelCipher(key = 0x12345L, domainSize = largeDomain)
        for (id in 0L..20L) {
            val bigId = BigInteger.valueOf(id)
            assertEquals(bigId, largeCipher.decrypt(largeCipher.encrypt(bigId)))
        }
        val maxId = largeDomain - BigInteger.ONE
        assertEquals(maxId, largeCipher.decrypt(largeCipher.encrypt(maxId)))
    }

    @Test
    fun `arbitrary large domain size works`() {
        val hugeDomain = BigInteger.valueOf(32).pow(20)
        val hugeCipher = FeistelCipher(key = 0xABCDEFL, domainSize = hugeDomain)
        for (id in 0L..20L) {
            val bigId = BigInteger.valueOf(id)
            assertEquals(bigId, hugeCipher.decrypt(hugeCipher.encrypt(bigId)))
        }
        val maxId = hugeDomain - BigInteger.ONE
        assertEquals(maxId, hugeCipher.decrypt(hugeCipher.encrypt(maxId)))
    }

    @Test
    fun `encrypt rejects negative input`() {
        assertFailsWith<IllegalArgumentException> {
            cipher.encrypt(BigInteger.valueOf(-1))
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
            cipher.decrypt(BigInteger.valueOf(-1))
        }
    }

    @Test
    fun `decrypt rejects input at domain size`() {
        assertFailsWith<IllegalArgumentException> {
            cipher.decrypt(domainSize)
        }
    }
}
