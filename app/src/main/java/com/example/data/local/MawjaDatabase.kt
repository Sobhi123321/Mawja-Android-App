package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import com.example.data.model.AccessSchedule
import com.example.data.model.ConnectionMode
import com.example.data.model.DeviceCategory
import com.example.data.model.DeviceProfile
import com.example.data.model.NetworkDevice
import com.example.data.model.SecurityAlert
import com.example.data.model.SpeedTestRecord
import kotlinx.coroutines.flow.Flow

class MawjaTypeConverters {
    @TypeConverter
    fun fromCategory(value: DeviceCategory): String = value.name

    @TypeConverter
    fun toCategory(value: String): DeviceCategory =
        runCatching { DeviceCategory.valueOf(value) }.getOrDefault(DeviceCategory.UNKNOWN)

    @TypeConverter
    fun fromProfile(value: DeviceProfile): String = value.name

    @TypeConverter
    fun toProfile(value: String): DeviceProfile =
        runCatching { DeviceProfile.valueOf(value) }.getOrDefault(DeviceProfile.UNKNOWN)

    @TypeConverter
    fun fromMode(value: ConnectionMode): String = value.name

    @TypeConverter
    fun toMode(value: String): ConnectionMode =
        runCatching { ConnectionMode.valueOf(value) }.getOrDefault(ConnectionMode.LOCAL_LAN)
}

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices WHERE isDemoDevice = :isDemo ORDER BY isBlocked ASC, isTrusted ASC, currentDownloadMbps DESC, name ASC")
    fun observeDevices(isDemo: Boolean): Flow<List<NetworkDevice>>

    @Query("SELECT * FROM devices WHERE isDemoDevice = :isDemo")
    suspend fun getDevicesSnapshot(isDemo: Boolean): List<NetworkDevice>

    @Query("SELECT * FROM devices WHERE macAddress = :mac LIMIT 1")
    suspend fun getByMac(mac: String): NetworkDevice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(devices: List<NetworkDevice>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(device: NetworkDevice)

    @Update
    suspend fun update(device: NetworkDevice)

    @Query("UPDATE devices SET isBlocked = :blocked WHERE macAddress = :mac")
    suspend fun setBlocked(mac: String, blocked: Boolean)

    @Query("UPDATE devices SET isTrusted = :trusted WHERE macAddress = :mac")
    suspend fun setTrusted(mac: String, trusted: Boolean)

    @Query("UPDATE devices SET name = :name WHERE macAddress = :mac")
    suspend fun rename(mac: String, name: String)

    @Query("UPDATE devices SET downloadLimitMbps = :downMbps, uploadLimitMbps = :upMbps WHERE macAddress = :mac")
    suspend fun setSpeedLimits(mac: String, downMbps: Int, upMbps: Int)

    @Query("UPDATE devices SET currentDownloadMbps = :downMbps, currentUploadMbps = :upMbps, todayUsageGb = :todayGb WHERE macAddress = :mac")
    suspend fun updateLiveMetrics(mac: String, downMbps: Float, upMbps: Float, todayGb: Float)
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY id ASC")
    fun observeAll(): Flow<List<AccessSchedule>>

    @Query("SELECT COUNT(*) FROM schedules")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(schedule: AccessSchedule): Long

    @Query("UPDATE schedules SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM schedules WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface SpeedTestDao {
    @Query("SELECT * FROM speed_tests ORDER BY timestampMillis DESC LIMIT 12")
    fun observeRecent(): Flow<List<SpeedTestRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: SpeedTestRecord): Long
}

@Dao
interface SecurityAlertDao {
    @Query("SELECT * FROM security_alerts ORDER BY timestampMillis DESC LIMIT 25")
    fun observeAlerts(): Flow<List<SecurityAlert>>

    @Query("SELECT COUNT(*) FROM security_alerts WHERE isRead = 0 AND isResolved = 0")
    fun observeUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: SecurityAlert): Long

    @Query("UPDATE security_alerts SET isResolved = 1, wasBlocked = :blocked, isRead = 1 WHERE id = :alertId")
    suspend fun resolveAlert(alertId: Long, blocked: Boolean)

    @Query("UPDATE security_alerts SET isResolved = 1, wasBlocked = :blocked, isRead = 1 WHERE deviceMac = :mac AND isResolved = 0")
    suspend fun resolveByMac(mac: String, blocked: Boolean)

    @Query("UPDATE security_alerts SET isRead = 1")
    suspend fun markAllRead()
}

@Database(
    entities = [
        NetworkDevice::class,
        AccessSchedule::class,
        SpeedTestRecord::class,
        SecurityAlert::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(MawjaTypeConverters::class)
abstract class MawjaDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun speedTestDao(): SpeedTestDao
    abstract fun securityAlertDao(): SecurityAlertDao

    companion object {
        @Volatile
        private var INSTANCE: MawjaDatabase? = null

        fun getInstance(context: Context): MawjaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MawjaDatabase::class.java,
                    "mawja_router_db"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
