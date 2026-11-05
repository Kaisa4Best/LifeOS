package com.lifeos.app

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "entries")
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val detail: String = "",
    val kind: String = "TASK",
    val done: Boolean = false,
    val xp: Int = 10,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Entry>>
    @Insert suspend fun insert(entry: Entry)
    @Update suspend fun update(entry: Entry)
    @Delete suspend fun delete(entry: Entry)
    @Query("SELECT COUNT(*) FROM entries WHERE done = 1")
    suspend fun completedCount(): Int
}

@Database(entities = [Entry::class], version = 1, exportSchema = false)
abstract class LifeDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    companion object {
        @Volatile private var INSTANCE: LifeDatabase? = null
        fun get(context: Context): LifeDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, LifeDatabase::class.java, "lifeos.db").build().also { INSTANCE = it }
        }
    }
}
