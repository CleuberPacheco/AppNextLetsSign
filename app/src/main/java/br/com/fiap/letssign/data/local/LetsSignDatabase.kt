package br.com.fiap.letssign.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        UserEntity::class,
        DocumentEntity::class,
        DocumentSignerEntity::class,
        SignatureFieldEntity::class,
        AuditEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LetsSignDatabase : RoomDatabase() {

    abstract fun letsSignDao(): LetsSignDao

    companion object {
        @Volatile
        private var INSTANCE: LetsSignDatabase? = null

        fun getInstance(context: Context): LetsSignDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LetsSignDatabase::class.java,
                    "letssign_juridico.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
