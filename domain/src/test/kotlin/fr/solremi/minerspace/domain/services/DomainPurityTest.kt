package fr.solremi.minerspace.domain.services

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DomainPurityTest {
    @Test
    fun `domain service contracts are platform neutral`() {
        val referencedPackages = GameServices::class.java.declaredFields
            .map { it.type.packageName }
            .toSet()

        assertTrue(referencedPackages.none { it.startsWith("android.") })
        assertTrue(referencedPackages.none { it.startsWith("com.badlogic.gdx") })
    }
}
