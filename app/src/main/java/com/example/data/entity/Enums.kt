package com.example.data.entity

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.ShoppingBag
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
            "bus" -> Icons.Default.DirectionsBus
            "flight" -> Icons.Default.Flight
            "car" -> Icons.Default.DirectionsCar
            "boat" -> Icons.Default.DirectionsBoat
            "hotel" -> Icons.Default.Hotel
            "food" -> Icons.Default.Fastfood
            "cafe" -> Icons.Default.LocalCafe
            "snack" -> Icons.Default.ShoppingBag
            "shopping" -> Icons.Default.ShoppingCart
            "ticket" -> Icons.Default.ConfirmationNumber
            "policy" -> Icons.Default.Policy
            "gas" -> Icons.Default.LocalGasStation
            "camera" -> Icons.Default.PhotoCamera
            "nature" -> Icons.Default.Park
            "hospital" -> Icons.Default.LocalHospital
            "gift" -> Icons.Default.CardGiftcard
            "money" -> Icons.Default.AttachMoney
            else -> Icons.Default.ShoppingBag
        }
    }
}
