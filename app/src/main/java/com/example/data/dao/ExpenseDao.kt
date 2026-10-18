package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.ExpenseSplitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE tripId = :tripId ORDER BY date DESC")
    fun getExpensesForTrip(tripId: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE tripId = :tripId ORDER BY date DESC")
    suspend fun getExpensesForTripOnce(tripId: Long): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE id = :expenseId")
    suspend fun getExpenseById(expenseId: Long): ExpenseEntity?

    @Query("SELECT SUM(amount) FROM expenses WHERE tripId = :tripId")
    fun getTotalSpentForTrip(tripId: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM expenses WHERE tripId = :tripId AND payerType = 'COMMON_FUND'")
    fun getCommonFundSpentForTrip(tripId: Long): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    // Splits
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplits(splits: List<ExpenseSplitEntity>)

    @Query("SELECT * FROM expense_splits WHERE expenseId = :expenseId")
    fun getSplitsForExpense(expenseId: Long): Flow<List<ExpenseSplitEntity>>

    @Query("SELECT * FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun getSplitsForExpenseOnce(expenseId: Long): List<ExpenseSplitEntity>

    @Query("SELECT es.* FROM expense_splits es INNER JOIN expenses e ON es.expenseId = e.id WHERE e.tripId = :tripId")
    fun getAllSplitsForTrip(tripId: Long): Flow<List<ExpenseSplitEntity>>

    @Query("SELECT es.* FROM expense_splits es INNER JOIN expenses e ON es.expenseId = e.id WHERE e.tripId = :tripId")
    suspend fun getAllSplitsForTripOnce(tripId: Long): List<ExpenseSplitEntity>

    @Query("DELETE FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun deleteSplitsForExpense(expenseId: Long)

    @Transaction
    suspend fun insertExpenseWithSplits(expense: ExpenseEntity, splits: List<ExpenseSplitEntity>): Long {
        val expenseId = insertExpense(expense)
        val splitsWithId = splits.map { it.copy(expenseId = expenseId) }
        insertSplits(splitsWithId)
        return expenseId
    }

    @Transaction
    suspend fun deleteExpenseAndSplits(expense: ExpenseEntity) {
        deleteSplitsForExpense(expense.id)
        deleteExpense(expense)
    }
}
