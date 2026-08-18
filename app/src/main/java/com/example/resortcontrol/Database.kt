package com.example.resortcontrol

import android.content.Context
import androidx.room.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.time.LocalDate

// --- ENTITIES (Таблицы в базе данных) ---

@Entity(tableName = "centers")
data class CenterEntity(
    @PrimaryKey val name: String,
    val lastModified: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)

@Entity(tableName = "cabins")
data class CabinEntity(
    @PrimaryKey val id: String,
    val number: String,
    val centerName: String,
    val photos: List<String>,
    val hasShower: Boolean,
    val hasToilet: Boolean,
    val roomsCount: Int,
    val bedsCount: Int,
    val comment: String,
    val price: Int,
    val currency: String,
    val lastModified: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "bookings",
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
    @PrimaryKey val id: String,
    val cabinId: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val note: String,
    val colorArgb: Int,
    val lastModified: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)

// --- CONVERTERS (Обучаем Room сохранять даты, списки и цвета) ---

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDate? = value?.let { LocalDate.ofEpochDay(it) }

    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun fromStringList(value: String?): List<String> = if (value.isNullOrEmpty()) emptyList() else value.split("||")

    @TypeConverter
    fun toStringList(list: List<String>?): String = list?.joinToString("||") ?: ""

    @TypeConverter
    fun fromColor(color: Color): Int = color.toArgb()

    @TypeConverter
    fun toColor(colorArgb: Int): Color = Color(colorArgb)
}

// --- DAO (Интерфейс для запросов) ---

@Dao
interface ResortDao {
    @Query("SELECT * FROM centers WHERE isDeleted = 0")
    suspend fun getAllCenters(): List<CenterEntity>

    @Query("SELECT * FROM centers WHERE isSynced = 0")
    suspend fun getUnsyncedCenters(): List<CenterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCenter(center: CenterEntity): Long

    @Query("DELETE FROM centers WHERE name = :name")
    suspend fun deleteCenter(name: String): Int

    @Query("UPDATE cabins SET centerName = :newName WHERE centerName = :oldName")
    suspend fun updateCabinsCenterName(oldName: String, newName: String): Int

    @Transaction
    suspend fun updateCenterName(oldName: String, newName: String): Int {
        val oldCenter = getAllCenters().find { it.name == oldName } ?: return 0
        insertCenter(oldCenter.copy(name = newName, isSynced = false, lastModified = System.currentTimeMillis()))
        updateCabinsCenterName(oldName, newName)
        deleteCenter(oldName)
        return 1
    }

    @Query("SELECT * FROM cabins WHERE isDeleted = 0")
    suspend fun getAllCabins(): List<CabinEntity>

    @Query("SELECT * FROM cabins WHERE isSynced = 0")
    suspend fun getUnsyncedCabins(): List<CabinEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCabin(cabin: CabinEntity): Long

    @Query("SELECT * FROM bookings WHERE isDeleted = 0")
    suspend fun getAllBookings(): List<BookingEntity>

    @Query("SELECT * FROM bookings WHERE isSynced = 0")
    suspend fun getUnsyncedBookings(): List<BookingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: BookingEntity): Long

    @Query("UPDATE bookings SET isDeleted = 1, isSynced = 0, lastModified = :time WHERE id = :bookingId")
    suspend fun softDeleteBooking(bookingId: String, time: Long = System.currentTimeMillis()): Int

    @Query("UPDATE cabins SET isDeleted = 1, isSynced = 0, lastModified = :time WHERE id = :cabinId")
    suspend fun softDeleteCabin(cabinId: String, time: Long = System.currentTimeMillis()): Int

    @Query("UPDATE centers SET isDeleted = 1, isSynced = 0, lastModified = :time WHERE name = :name")
    suspend fun softDeleteCenter(name: String, time: Long = System.currentTimeMillis()): Int

    @Delete
    suspend fun hardDeleteBooking(booking: BookingEntity): Int

    @Delete
    suspend fun hardDeleteCabin(cabin: CabinEntity): Int

    @Delete
    suspend fun hardDeleteCenter(center: CenterEntity): Int
}

// --- DATABASE CLASS ---

@Database(
    entities = [CenterEntity::class, CabinEntity::class, BookingEntity::class],
    version = 3,
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
                    "resort_db.sqlite" // Имя файла базы на устройстве
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
