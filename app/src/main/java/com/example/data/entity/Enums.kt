package com.example.data.entity

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.ui.graphics.vector.ImageVector

enum class ExpenseCategory(
    val displayName: String,
    val description: String,
    val iconName: String
) {
    TRANSPORTATION(
        displayName = "Transportation",
        description = "Bus, Chander Gari, Boat, CNG, Fuel",
        iconName = "bus"
    ),
    LODGING(
        displayName = "Lodging & Resort",
        description = "Hotel, Cottage, Tent, Homestay",
        iconName = "hotel"
    ),
    FOOD(
        displayName = "Food & Dining",
        description = "Breakfast, Lunch, Dinner, Hill Cafe",
        iconName = "food"
    ),
    GUIDE_PERMISSIONS(
        displayName = "Guide & Permissions",
        description = "Army/Admin entry fees, Guide honorarium",
        iconName = "policy"
    ),
    SIGHTSEEING(
        displayName = "Sightseeing & Tickets",
        description = "Waterfalls, Viewpoints, Entry tickets",
        iconName = "ticket"
    ),
    SNACKS_PERSONAL(
        displayName = "Snacks & Refreshment",
        description = "Cha, mineral water, fruits, local snacks",
        iconName = "snack"
    );

    fun getIcon(): ImageVector {
        return when (this) {
            TRANSPORTATION -> Icons.Default.DirectionsBus
            LODGING -> Icons.Default.Hotel
            FOOD -> Icons.Default.Fastfood
            GUIDE_PERMISSIONS -> Icons.Default.Policy
            SIGHTSEEING -> Icons.Default.ConfirmationNumber
            SNACKS_PERSONAL -> Icons.Default.ShoppingBag
        }
    }
}

enum class PayerType {
    COMMON_FUND,
    MEMBER
}

enum class SplitMode {
    EQUAL,
    SELECTIVE,
    CUSTOM
}
