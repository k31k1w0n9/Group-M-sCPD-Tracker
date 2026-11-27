package com.example.scpd_tracker.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.scpd_tracker.ScpdApplication
import com.example.scpd_tracker.model.AttendanceClaim
import com.example.scpd_tracker.model.Stage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.util.concurrent.TimeUnit

/*
 * Scans saved on the phone while offline. They live in a Room database, so they survive the app being closed,
 * the phone restarting, and Android killing the app when a permission is switched off in Settings
 * (the lecturer's 24 Nov question). WorkManager uploads them once there is a connection, even if the app is closed.
 */

@Entity(tableName = "pending_scans")
data class PendingScan(
    @PrimaryKey val requestId: String,
    val userId: String,
    val eventId: String,
    val stage: String,
    val token: String,
    val deviceId: String,
    val capturedAtMillis: Long,
    val viaBackupCode: Boolean,
) {
    fun toClaim() = AttendanceClaim(
        requestId = requestId, eventId = eventId, stage = Stage.valueOf(stage), token = token,
        deviceId = deviceId, capturedAt = Instant.ofEpochMilli(capturedAtMillis), viaBackupCode = viaBackupCode,
    )

    companion object {
        fun from(userId: String, claim: AttendanceClaim) = PendingScan(
            claim.requestId, userId, claim.eventId, claim.stage.name, claim.token, claim.deviceId,
            claim.capturedAt.toEpochMilli(), claim.viaBackupCode,
        )
    }
}

/** An upload the server refused. The token is gone, so the phone keeps only the reason to show the student. */
@Entity(tableName = "rejected_uploads")
data class RejectedUpload(
    /** "userId|eventId|stage" */
    @PrimaryKey val key: String,
    val userId: String,
    val eventId: String,
    val stage: String,
    val reason: String,
)

@Dao
interface ScanQueueDao {
    @Query("SELECT * FROM pending_scans WHERE userId = :userId ORDER BY capturedAtMillis")
    fun pending(userId: String): Flow<List<PendingScan>>

    @Query("SELECT * FROM pending_scans WHERE userId = :userId ORDER BY capturedAtMillis")
    suspend fun pendingNow(userId: String): List<PendingScan>

    /** A double tap or a retry of the same scan keeps the first copy. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(scan: PendingScan)

    @Query("DELETE FROM pending_scans WHERE requestId = :requestId")
    suspend fun remove(requestId: String)

    @Query("SELECT * FROM rejected_uploads WHERE userId = :userId")
    fun rejected(userId: String): Flow<List<RejectedUpload>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addRejected(rejected: RejectedUpload)

    /** Saved scans belong to the account: signing out clears them (the sign-out screen warns first). */
    @Query("DELETE FROM pending_scans WHERE userId = :userId")
    suspend fun clearPending(userId: String)

    @Query("DELETE FROM rejected_uploads WHERE userId = :userId")
    suspend fun clearRejected(userId: String)
}

@Database(entities = [PendingScan::class, RejectedUpload::class], version = 1, exportSchema = false)
abstract class ScpdDatabase : RoomDatabase() {
    abstract fun scanQueue(): ScanQueueDao

    companion object {
        fun create(context: Context): ScpdDatabase =
            Room.databaseBuilder(context.applicationContext, ScpdDatabase::class.java, "scpd.db").build()
    }
}

/** Uploads saved scans when the phone is online. WorkManager runs it even if the app has been closed. */
class ScanSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as ScpdApplication).container
        // In a fresh process (after Android killed the app) the saved sign-in takes a moment to come back from disk.
        // Wait for it, or this run would see "nobody signed in" and finish without uploading.
        withTimeoutOrNull(20_000) { container.auth.currentUser.filterNotNull().first() } ?: return Result.success()
        val attendance = container.attendance
        attendance.syncPending()
        // Anything left failed for a network reason; try again later with backoff.
        val left = container.pendingScanCount()
        return if (left > 0 && runAttemptCount < 20) Result.retry() else Result.success()
    }

    companion object {
        private const val NAME = "scan-sync"

        fun schedule(context: Context) {
            val request = OneTimeWorkRequestBuilder<ScanSyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(NAME, ExistingWorkPolicy.KEEP, request)
        }
    }
}
