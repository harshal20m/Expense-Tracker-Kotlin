package com.example.paisatracker.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromBudgetPeriod(period: BudgetPeriod): String {
        return period.name
    }

    @TypeConverter
    fun toBudgetPeriod(value: String): BudgetPeriod {
        return try {
            BudgetPeriod.valueOf(value)
        } catch (e: Exception) {
            BudgetPeriod.MONTHLY
        }
    }
}