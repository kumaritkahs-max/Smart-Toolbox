package com.githubcontrol.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "command_history")
data class CommandHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: String,
    val command: String,
    val output: String,
    val ts: Long = System.currentTimeMillis(),
    val success: Boolean = true
)

@Entity(tableName = "upload_history")
data class UploadHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: String,
    val owner: String,
    val repo: String,
    val branch: String,
    val totalFiles: Int,
    val totalBytes: Long,
    val message: String,
    val state: String, // pending|uploading|paused|failed|completed
    val ts: Long = System.currentTimeMillis()
)

@Entity(tableName = "sync_jobs")
data class SyncJobEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: String,
    val owner: String,
    val repo: String,
    val branch: String,
    val localUri: String,
    val remotePath: String,
    val intervalMinutes: Int,
    val enabled: Boolean = true,
    val lastRun: Long = 0L
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val owner: String,
    val repo: String,
    val path: String,
    val localPath: String,
    val sizeBytes: Long,
    val ts: Long = System.currentTimeMillis()
)

@Dao
interface CommandHistoryDao {
    @Insert suspend fun insert(e: CommandHistoryEntity): Long
    @Query("SELECT * FROM command_history WHERE accountId = :acc ORDER BY ts DESC LIMIT 200")
    fun observe(acc: String): Flow<List<CommandHistoryEntity>>
    @Query("DELETE FROM command_history WHERE accountId = :acc") suspend fun clear(acc: String)
}

@Dao
interface UploadHistoryDao {
    @Insert suspend fun insert(e: UploadHistoryEntity): Long
    @Update suspend fun update(e: UploadHistoryEntity)
    @Query("SELECT * FROM upload_history ORDER BY ts DESC") fun observeAll(): Flow<List<UploadHistoryEntity>>
}

@Dao
interface SyncJobDao {
    @Insert suspend fun insert(e: SyncJobEntity): Long
    @Update suspend fun update(e: SyncJobEntity)
    @Delete suspend fun delete(e: SyncJobEntity)
    @Query("SELECT * FROM sync_jobs WHERE enabled = 1") fun observeEnabled(): Flow<List<SyncJobEntity>>
    @Query("SELECT * FROM sync_jobs") fun observeAll(): Flow<List<SyncJobEntity>>
}

@Dao
interface DownloadDao {
    @Insert suspend fun insert(e: DownloadEntity): Long
    @Query("SELECT * FROM downloads ORDER BY ts DESC") fun observe(): Flow<List<DownloadEntity>>
    @Query("DELETE FROM downloads") suspend fun clear()
}

@Database(
    entities = [CommandHistoryEntity::class, UploadHistoryEntity::class, SyncJobEntity::class, DownloadEntity::class],
    version = 1, exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun commandHistory(): CommandHistoryDao
    abstract fun uploadHistory(): UploadHistoryDao
    abstract fun syncJobs(): SyncJobDao
    abstract fun downloads(): DownloadDao
}
