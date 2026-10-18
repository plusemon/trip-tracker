package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.PayerType
import com.example.data.entity.SplitMode

class Converters {
    @TypeConverter
    fun fromExpenseCategory(value: ExpenseCategory?): String? = value?.name

    @TypeConverter
    fun toExpenseCategory(value: String?): ExpenseCategory? =
        value?.let { runCatching { ExpenseCategory.valueOf(it) }.getOrDefault(ExpenseCategory.FOOD) }

    @TypeConverter
    fun fromPayerType(value: PayerType?): String? = value?.name

    @TypeConverter
    fun toPayerType(value: String?): PayerType? =
        value?.let { runCatching { PayerType.valueOf(it) }.getOrDefault(PayerType.COMMON_FUND) }

    @TypeConverter
    fun fromSplitMode(value: SplitMode?): String? = value?.name

    @TypeConverter
    fun toSplitMode(value: String?): SplitMode? =
        value?.let { runCatching { SplitMode.valueOf(it) }.getOrDefault(SplitMode.EQUAL) }
}
