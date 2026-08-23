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
import com.example.data.models.Beneficiary
import com.example.data.models.DeviceStatus
import com.example.data.models.ExchangeRate
import com.example.data.models.Notice
import com.example.data.models.Offer
import com.example.data.models.PaymentMethod
import com.example.data.models.RegisteredDevice
import com.example.data.models.RemittanceTransaction
import com.example.data.models.TimelineEvent
import com.example.data.models.TransactionStatus
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow

class Converters {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @TypeConverter
    fun fromTransactionStatus(status: TransactionStatus): String = status.name

    @TypeConverter
    fun toTransactionStatus(value: String): TransactionStatus = try {
        TransactionStatus.valueOf(value)
    } catch (e: Exception) {
        TransactionStatus.PENDING
    }

    @TypeConverter
    fun fromDeviceStatus(status: DeviceStatus): String = status.name

    @TypeConverter
    fun toDeviceStatus(value: String): DeviceStatus = try {
        DeviceStatus.valueOf(value)
    } catch (e: Exception) {
        DeviceStatus.PENDING
    }

    @TypeConverter
    fun fromTimelineList(list: List<TimelineEvent>): String {
        val type = Types.newParameterizedType(List::class.java, TimelineEvent::class.java)
        return moshi.adapter<List<TimelineEvent>>(type).toJson(list)
    }

    @TypeConverter
    fun toTimelineList(value: String): List<TimelineEvent> {
        val type = Types.newParameterizedType(List::class.java, TimelineEvent::class.java)
        return try {
            moshi.adapter<List<TimelineEvent>>(type).fromJson(value) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

@Dao
interface RemittanceDao {
    @Query("SELECT * FROM exchange_rates ORDER BY displayOrder ASC")
    fun getAllExchangeRates(): Flow<List<ExchangeRate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExchangeRates(rates: List<ExchangeRate>)

    @Query("SELECT * FROM beneficiaries ORDER BY name ASC")
    fun getAllBeneficiaries(): Flow<List<Beneficiary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeneficiary(beneficiary: Beneficiary)

    @Query("DELETE FROM beneficiaries WHERE id = :id")
    suspend fun deleteBeneficiary(id: String)

    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun getAllTransactions(): Flow<List<RemittanceTransaction>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): RemittanceTransaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: RemittanceTransaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<RemittanceTransaction>)

    @Query("UPDATE transactions SET status = :status WHERE id = :id")
    suspend fun updateTransactionStatus(id: String, status: TransactionStatus)

    @Query("SELECT * FROM payment_methods WHERE isActive = 1 ORDER BY displayOrder ASC")
    fun getAllPaymentMethods(): Flow<List<PaymentMethod>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentMethods(methods: List<PaymentMethod>)

    @Query("SELECT * FROM offers WHERE isActive = 1")
    fun getAllOffers(): Flow<List<Offer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOffers(offers: List<Offer>)

    @Query("SELECT * FROM notices ORDER BY isPriority DESC, date DESC")
    fun getAllNotices(): Flow<List<Notice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotices(notices: List<Notice>)

    @Query("SELECT * FROM devices")
    fun getAllDevices(): Flow<List<RegisteredDevice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: RegisteredDevice)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevices(devices: List<RegisteredDevice>)

    @Query("UPDATE devices SET status = :status WHERE deviceId = :deviceId")
    suspend fun updateDeviceStatus(deviceId: String, status: DeviceStatus)

    @Query("DELETE FROM devices WHERE deviceId = :deviceId")
    suspend fun deleteDevice(deviceId: String)
}

@Database(
    entities = [
        ExchangeRate::class,
        Beneficiary::class,
        RemittanceTransaction::class,
        PaymentMethod::class,
        Offer::class,
        Notice::class,
        RegisteredDevice::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun remittanceDao(): RemittanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "paisa_remit_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
