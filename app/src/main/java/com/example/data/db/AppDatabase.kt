package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.dao.DepositDao
import com.example.data.dao.ExpenseDao
import com.example.data.dao.MemberDao
import com.example.data.dao.SettlementDao
import com.example.data.dao.TripDao
import com.example.data.entity.DepositEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.ExpenseSplitEntity
import com.example.data.entity.MemberEntity
import com.example.data.entity.SettlementRecordEntity
import com.example.data.entity.TripEntity

@Database(
    entities = [
        TripEntity::class,
        MemberEntity::class,
        DepositEntity::class,
        ExpenseEntity::class,
        ExpenseSplitEntity::class,
        SettlementRecordEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun memberDao(): MemberDao
    abstract fun depositDao(): DepositDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun settlementDao(): SettlementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tripsplit_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
