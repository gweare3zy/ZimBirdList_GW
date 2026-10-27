package com.example

import com.example.data.catalog.ZimbabweBirdCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testZimbabweCatalogSize() {
        val total = ZimbabweBirdCatalog.totalCount
        assertTrue("Checklist should contain at least 400 species, found $total", total >= 400)
    }

    @Test
    fun testKeySpeciesPresent() {
        assertNotNull(ZimbabweBirdCatalog.getSpeciesById("african_fish_eagle"))
        assertNotNull(ZimbabweBirdCatalog.getSpeciesById("lilac_breasted_roller"))
        assertNotNull(ZimbabweBirdCatalog.getSpeciesById("bateleur"))
        assertNotNull(ZimbabweBirdCatalog.getSpeciesById("pels_fishing_owl"))

        val eagle = ZimbabweBirdCatalog.getSpeciesById("african_fish_eagle")
        assertEquals("African Fish-eagle", eagle?.commonName)

        val grebe = ZimbabweBirdCatalog.getSpeciesById("great_crested_grebe")
        assertEquals("Vagrant", grebe?.rarityLabel)

        val kite = ZimbabweBirdCatalog.getSpeciesById("yellow_billed_kite")
        assertNotNull(kite)
        assertEquals("Yellow-billed Kite", kite?.commonName)
    }
}
