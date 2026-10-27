package com.example.data.model

data class BirdSpecies(
    val id: String,
    val commonName: String,
    val rarityTag: String? = null // "R", "V", "?", "Feral"
) {
    val rarityLabel: String?
        get() = when (rarityTag) {
            "R" -> "Rare"
            "V" -> "Vagrant"
            "?" -> "Confirmation Req."
            "Feral" -> "Feral"
            else -> rarityTag
        }
}
