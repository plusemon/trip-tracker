package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.entity.CategoryIconHelper
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.MemberEntity
import com.example.data.entity.TripStatus
import java.io.File

@Composable
fun MemberAvatar(
    member: MemberEntity,
    size: Dp = 38.dp,
    modifier: Modifier = Modifier
) {
    val bgColor = runCatching { Color(android.graphics.Color.parseColor(member.colorHex)) }.getOrDefault(MaterialTheme.colorScheme.primary)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = member.getDisplayInitials(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.4f).sp
        )
    }
}

@Composable
fun CategoryChip(
    category: ExpenseCategory,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = category.getIcon(),
                contentDescription = category.displayName,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = " ${category.displayName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun CustomCategoryChip(
    name: String,
    iconName: String,
    colorHex: String,
    modifier: Modifier = Modifier
) {
    val tintColor = runCatching { Color(android.graphics.Color.parseColor(colorHex)) }.getOrDefault(MaterialTheme.colorScheme.primary)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = tintColor.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = CategoryIconHelper.getIconForName(iconName),
                contentDescription = name,
                modifier = Modifier.size(14.dp),
                tint = tintColor
            )
            Text(
                text = " $name",
                style = MaterialTheme.typography.labelSmall,
                color = tintColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun TripStatusBadge(
    status: TripStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (status) {
        TripStatus.ACTIVE -> Color(0xFFE8F5E9) to Color(0xFF1B5E20)
        TripStatus.UPCOMING -> Color(0xFFE3F2FD) to Color(0xFF0D47A1)
        TripStatus.COMPLETED -> Color(0xFFECEFF1) to Color(0xFF455A64)
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Text(
            text = status.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun ReceiptThumbnail(
    filePath: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = File(filePath),
            contentDescription = "Receipt photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
    }
}
