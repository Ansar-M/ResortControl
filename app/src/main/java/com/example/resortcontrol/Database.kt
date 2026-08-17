package com.example.resortcontrol

import android.content.Context
import androidx.room.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.time.LocalDate

// --- ENTITIES (Таблицы в базе данных) ---

@Entity(tableName = "centers")
data class CenterEntity(
    @PrimaryKey val name: String
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
    val currency: String
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
    val colorArgb: Int
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
    @Query("SELECT * FROM centers")
    suspend fun getAllCenters(): List<CenterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCenter(center: CenterEntity): Long

    @Query("SELECT * FROM cabins")
    suspend fun getAllCabins(): List<CabinEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCabin(cabin: CabinEntity): Long

    @Query("SELECT * FROM bookings")
    suspend fun getAllBookings(): List<BookingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: BookingEntity): Long

    @Query("DELETE FROM bookings WHERE id = :bookingId")
    suspend fun deleteBooking(bookingId: String): Int
}

// --- DATABASE CLASS ---

@Database(
    entities = [CenterEntity::class, CabinEntity::class, BookingEntity::class],
    version = 1,
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
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
