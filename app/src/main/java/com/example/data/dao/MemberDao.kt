package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.MemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {
    @Query("SELECT * FROM members WHERE tripId = :tripId ORDER BY name ASC")
    fun getMembersForTrip(tripId: Long): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE tripId = :tripId ORDER BY name ASC")
    suspend fun getMembersForTripOnce(tripId: Long): List<MemberEntity>

    @Query("SELECT * FROM members WHERE id = :memberId")
    suspend fun getMemberById(memberId: Long): MemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: MemberEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<MemberEntity>): List<Long>

    @Update
    suspend fun updateMember(member: MemberEntity)

    @Delete
    suspend fun deleteMember(member: MemberEntity)

    @Query("DELETE FROM members WHERE tripId = :tripId")
    suspend fun deleteMembersForTrip(tripId: Long)
}
