package app.multinest.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query(
        "SELECT * FROM profiles " +
            "WHERE name LIKE '%' || :q || '%' OR url LIKE '%' || :q || '%' " +
            "ORDER BY createdAt ASC, id ASC"
    )
    fun observe(q: String): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun get(id: String): ProfileEntity?

    @Insert
    suspend fun insert(p: ProfileEntity)

    @Insert
    suspend fun insertAll(list: List<ProfileEntity>)

    @Update
    suspend fun update(p: ProfileEntity)

    @Query("UPDATE profiles SET lastUrl = :url WHERE id = :id")
    suspend fun setLastUrl(id: String, url: String)

    @Query("UPDATE profiles SET url = :url WHERE id = :id")
    suspend fun setStartUrl(id: String, url: String)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun count(): Int
}
