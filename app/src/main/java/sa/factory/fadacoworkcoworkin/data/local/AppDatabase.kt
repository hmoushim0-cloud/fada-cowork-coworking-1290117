package sa.factory.fadacoworkcoworkin.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [FadaCoworkEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun coworkingDao(): FadaCoworkDao
}
