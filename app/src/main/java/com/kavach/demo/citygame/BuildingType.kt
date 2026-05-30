package com.kavach.demo.citygame

import com.kavach.engine.math.Vector3

enum class BuildingType(
    val displayName: String,
    val cost: Int,
    val populationBonus: Int,
    val color: Vector3,
    /** World-space height of the building's visible block. */
    val buildHeight: Float,
    /** XZ footprint scale relative to one grid cell (0–1). */
    val footprint: Float = 0.88f
) {
    ROAD(       "Road",      500,   0,  Vector3(0.42f, 0.42f, 0.42f), 0.06f, 1.00f),
    HOUSE(      "House",    2000,   4,  Vector3(0.80f, 0.55f, 0.25f), 1.00f),
    APARTMENT(  "Apt",      8000,  20,  Vector3(0.50f, 0.62f, 0.88f), 2.60f, 0.82f),
    SHOP(       "Shop",     5000,   0,  Vector3(0.92f, 0.78f, 0.18f), 1.20f),
    PARK(       "Park",     1000,   2,  Vector3(0.22f, 0.78f, 0.28f), 0.12f, 1.00f),
    BUS_STOP(   "Bus Stop", 3000,   0,  Vector3(0.95f, 0.48f, 0.10f), 0.75f);

    val emoji get() = when (this) {
        ROAD       -> "🛣"
        HOUSE      -> "🏠"
        APARTMENT  -> "🏢"
        SHOP       -> "🏪"
        PARK       -> "🌳"
        BUS_STOP   -> "🚌"
    }
}
