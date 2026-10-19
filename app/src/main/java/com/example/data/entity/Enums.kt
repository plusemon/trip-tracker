package com.example.data.entity

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

enum class TripStatus(val displayName: String) {
    ACTIVE("Active"),
    UPCOMING("Upcoming"),
    COMPLETED("Completed")
}

enum class ExpenseCategory(
    val displayName: String,
    val description: String,
    val iconName: String,
    val defaultColorHex: String = "#006C4C"
) {
    TRANSPORTATION(
        displayName = "Transportation",
        description = "Bus, Chander Gari, Boat, CNG, Fuel",
        iconName = "bus",
        defaultColorHex = "#00658E"
    ),
    LODGING(
        displayName = "Lodging & Resort",
        description = "Hotel, Cottage, Tent, Homestay",
        iconName = "hotel",
        defaultColorHex = "#7B1FA2"
    ),
    FOOD(
        displayName = "Food & Dining",
        description = "Breakfast, Lunch, Dinner, Hill Cafe",
        iconName = "food",
        defaultColorHex = "#E65100"
    ),
    GUIDE_PERMISSIONS(
        displayName = "Guide & Permissions",
        description = "Army/Admin entry fees, Guide honorarium",
        iconName = "policy",
        defaultColorHex = "#00796B"
    ),
    SIGHTSEEING(
        displayName = "Sightseeing & Tickets",
        description = "Waterfalls, Viewpoints, Entry tickets",
        iconName = "ticket",
        defaultColorHex = "#006C4C"
    ),
    SNACKS_PERSONAL(
        displayName = "Snacks & Refreshment",
        description = "Cha, mineral water, fruits, local snacks",
        iconName = "snack",
        defaultColorHex = "#835400"
    );

    fun getIcon(): ImageVector = CategoryIconHelper.getIconForName(iconName)
}

enum class PayerType {
    COMMON_FUND,
    MEMBER,
    MULTI_PAYER
}

enum class SplitMode {
    EQUAL,
    SELECTIVE,
    CUSTOM
}

object CategoryIconHelper {
    val AVAILABLE_ICONS = listOf(
        "bus" to "Bus / Transit",
        "flight" to "Flight / Airline",
        "car" to "Car / Rental",
        "boat" to "Boat / Launch",
        "hotel" to "Hotel / Resort",
        "food" to "Food & Dining",
        "cafe" to "Tea & Cafe",
        "snack" to "Snacks & Market",
        "shopping" to "Shopping & Souvenir",
        "ticket" to "Tickets & Entry",
        "policy" to "Guide & Permit",
        "gas" to "Fuel & Gas",
        "camera" to "Photography",
        "nature" to "Trekking & Nature",
        "hospital" to "Medical / First Aid",
        "gift" to "Gift / Tip",
        "money" to "Income / Pool"
    )

    val PRESET_COLORS = listOf(
        "#006C4C", // Emerald
        "#006A6A", // Teal
        "#00658E", // Ocean Blue
        "#303F9F", // Indigo
        "#7B1FA2", // Purple
        "#C2185B", // Pink
        "#D32F2F", // Red
        "#E65100", // Orange
        "#835400", // Amber / Ochre
        "#F57F17", // Gold
        "#33691E", // Forest Green
        "#455A64"  // Slate Gray
    )

    fun getIconForName(name: String): ImageVector {
        return when (name.lowercase()) {
            "bus" -> Icons.Default.Star
            "flight" -> Icons.Default.Star
            "car" -> Icons.Default.Star
            "boat" -> Icons.Default.Star
            "hotel" -> Icons.Default.Star
            "food" -> Icons.Default.Star
            "cafe" -> Icons.Default.Star
            "snack" -> Icons.Default.Star
            "shopping" -> Icons.Default.ShoppingCart
            "ticket" -> Icons.Default.Star
            "policy" -> Icons.Default.Star
            "gas" -> Icons.Default.Star
            "camera" -> Icons.Default.Star
            "nature" -> Icons.Default.Star
            "hospital" -> Icons.Default.Star
            "gift" -> Icons.Default.Star
            "money" -> Icons.Default.Star
            else -> Icons.Default.Star
        }
    }
}
