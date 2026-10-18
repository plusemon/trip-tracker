package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.ReceiptAttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM receipt_attachments WHERE expenseId = :expenseId ORDER BY timestamp ASC")
    fun getAttachmentsForExpense(expenseId: Long): Flow<List<ReceiptAttachmentEntity>>

    @Query("SELECT * FROM receipt_attachments WHERE expenseId = :expenseId ORDER BY timestamp ASC")
    suspend fun getAttachmentsForExpenseOnce(expenseId: Long): List<ReceiptAttachmentEntity>

    @Query("SELECT a.* FROM receipt_attachments a INNER JOIN expenses e ON a.expenseId = e.id WHERE e.tripId = :tripId")
    fun getAllAttachmentsForTrip(tripId: Long): Flow<List<ReceiptAttachmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: ReceiptAttachmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachments(attachments: List<ReceiptAttachmentEntity>)

    @Delete
    suspend fun deleteAttachment(attachment: ReceiptAttachmentEntity)

    @Query("DELETE FROM receipt_attachments WHERE expenseId = :expenseId")
    suspend fun deleteAttachmentsForExpense(expenseId: Long)
}
