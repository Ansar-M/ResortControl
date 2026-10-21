package com.resort.control

import android.content.Context
import androidx.room.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.flow.Flow

// --- ENTITIES (Таблицы) ---

@Entity(tableName = "centers")
data class CenterEntity(
    @PrimaryKey val name: String = "",
    val lastModified: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "cabins",
    foreignKeys = [
        ForeignKey(
            entity = CenterEntity::class,
            parentColumns = ["name"],
            childColumns = ["centerName"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["centerName"])]
)
data class CabinEntity(
    @PrimaryKey val id: String = "",
    val number: String = "",
    val centerName: String = "",
    val photos: List<String> = emptyList(),
    val hasShower: Boolean = false,
    val hasToilet: Boolean = false,
    val roomsCount: Int = 1,
    val bedsCount: Int = 1,
    val comment: String = "",
    val price: Int = 0,
    val currency: String = "₸",
    val lastModified: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)

@Entity(tableName = "bookings",
    foreignKeys = [
        ForeignKey(
            entity = CabinEntity::class,
            parentColumns = ["id"],
            childColumns = ["cabinId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["cabinId"])]
)
data class BookingEntity(
    @PrimaryKey val id: String = "",
    val cabinId: String = "",
    val startDate: Long = 0,
    val endDate: Long = 0,
    val note: String = "",
    val colorArgb: Int = 0,
    val lastModified: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)

@Entity(tableName = "pending_deletions")
data class PendingDeletionEntity(
    @PrimaryKey val url: String = ""
)

// --- CONVERTERS ---

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(",")

    @TypeConverter
    fun toStringList(value: String): List<String> = if (value.isBlank()) emptyList() else value.split(",")

    @TypeConverter
    fun fromColor(color: Color): Int = color.toArgb()

    @TypeConverter
    fun toColor(colorArgb: Int): Color = Color(colorArgb)
}

// --- DAO (Интерфейс для запросов) ---

@Dao
abstract class ResortDao {
    @Query("SELECT * FROM centers WHERE isDeleted = 0 ORDER BY name ASC")
    abstract fun getAllCentersFlow(): Flow<List<CenterEntity>>

    @Query("SELECT * FROM centers")
    abstract suspend fun getAllCentersWithDeleted(): List<CenterEntity>

    @Query("SELECT * FROM centers WHERE name = :name")
    abstract suspend fun getCenterByName(name: String): CenterEntity?

    @Query("SELECT * FROM centers WHERE isSynced = 0")
    abstract suspend fun getUnsyncedCenters(): List<CenterEntity>

    @Upsert
    abstract suspend fun insertCenter(center: CenterEntity): Long

    @Query("DELETE FROM centers WHERE name = :name")
    abstract suspend fun deleteCenter(name: String): Int

    @Query("UPDATE cabins SET centerName = :newName WHERE centerName = :oldName")
    abstract suspend fun updateCabinsCenterName(oldName: String, newName: String): Int

    @Transaction
    open suspend fun updateCenterName(oldName: String, newName: String): Int {
        val oldCenter = getCenterByName(oldName) ?: return 0
        insertCenter(oldCenter.copy(name = newName, isSynced = false, lastModified = System.currentTimeMillis()))
        updateCabinsCenterName(oldName, newName)
        deleteCenter(oldName)
        return 1
    }

    @Query("SELECT * FROM cabins WHERE isDeleted = 0 ORDER BY number ASC")
    abstract fun getAllCabinsFlow(): Flow<List<CabinEntity>>

    @Query("SELECT * FROM cabins WHERE isDeleted = 0 ORDER BY number ASC")
    abstract suspend fun getAllCabins(): List<CabinEntity>

    @Query("SELECT * FROM cabins")
    abstract suspend fun getAllCabinsWithDeleted(): List<CabinEntity>

    @Query("SELECT * FROM cabins WHERE id = :id")
    abstract suspend fun getCabinById(id: String): CabinEntity?

    @Query("SELECT * FROM cabins WHERE isSynced = 0")
    abstract suspend fun getUnsyncedCabins(): List<CabinEntity>

    @Upsert
    abstract suspend fun insertCabin(cabin: CabinEntity): Long

    @Query("SELECT * FROM bookings WHERE isDeleted = 0 ORDER BY startDate ASC")
    abstract fun getAllBookingsFlow(): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings")
    abstract suspend fun getAllBookingsWithDeleted(): List<BookingEntity>

    @Query("SELECT * FROM bookings WHERE id = :id")
    abstract suspend fun getBookingById(id: String): BookingEntity?

    @Query("SELECT * FROM bookings WHERE isSynced = 0")
    abstract suspend fun getUnsyncedBookings(): List<BookingEntity>

    @Upsert
    abstract suspend fun insertBooking(booking: BookingEntity): Long

    @Query("UPDATE bookings SET isDeleted = 1, isSynced = 0, lastModified = :time WHERE id = :bookingId")
    abstract suspend fun markBookingDeleted(bookingId: String, time: Long): Int

    @Transaction
    open suspend fun softDeleteBooking(bookingId: String) {
        markBookingDeleted(bookingId, System.currentTimeMillis())
    }

    @Query("UPDATE cabins SET isDeleted = 1, isSynced = 0, lastModified = :time WHERE id = :cabinId")
    abstract suspend fun markCabinDeleted(cabinId: String, time: Long): Int

    @Query("UPDATE bookings SET isDeleted = 1, isSynced = 0, lastModified = :time WHERE cabinId = :cabinId")
    abstract suspend fun markBookingsDeletedByCabin(cabinId: String, time: Long): Int

    @Transaction
    open suspend fun softDeleteCabin(cabinId: String) {
        val time = System.currentTimeMillis()
        markCabinDeleted(cabinId, time)
        markBookingsDeletedByCabin(cabinId, time)
    }

    @Query("UPDATE centers SET isDeleted = 1, isSynced = 0, lastModified = :time WHERE name = :name")
    abstract suspend fun markCenterDeleted(name: String, time: Long): Int

    @Query("UPDATE cabins SET isDeleted = 1, isSynced = 0, lastModified = :time WHERE centerName = :name")
    abstract suspend fun markCabinsDeletedByCenter(name: String, time: Long): Int

    @Query("UPDATE bookings SET isDeleted = 1, isSynced = 0, lastModified = :time WHERE cabinId IN (SELECT id FROM cabins WHERE centerName = :name)")
    abstract suspend fun markBookingsDeletedByCenter(name: String, time: Long): Int

    @Transaction
    open suspend fun softDeleteCenter(name: String) {
        val time = System.currentTimeMillis()
        markCenterDeleted(name, time)
        markCabinsDeletedByCenter(name, time)
        markBookingsDeletedByCenter(name, time)
    }

    @Delete
    abstract suspend fun hardDeleteBooking(booking: BookingEntity): Int

    @Delete
    abstract suspend fun hardDeleteCabin(cabin: CabinEntity): Int

    @Delete
    abstract suspend fun hardDeleteCenter(center: CenterEntity): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertPendingDeletion(deletion: PendingDeletionEntity)

    @Query("SELECT * FROM pending_deletions")
    abstract suspend fun getAllPendingDeletions(): List<PendingDeletionEntity>

    @Delete
    abstract suspend fun deletePendingDeletion(deletion: PendingDeletionEntity)
}

// --- DATABASE CLASS ---

@Database(
    entities = [CenterEntity::class, CabinEntity::class, BookingEntity::class, PendingDeletionEntity::class],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun resortDao(): ResortDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "resort_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
