
package com.example.attendanceai.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos local de Attendance AI.
 *
 * version = 1 representa la primera versión
 * del esquema de la base de datos.
 */
@Database(
    entities = [Attendance::class],
    version = 1,
    exportSchema = false
)
abstract class AttendanceDatabase : RoomDatabase() {

    /*
     * Permite acceder a las operaciones del DAO.
     */
    abstract fun attendanceDao(): AttendanceDao

    companion object {

        /*
         * Mantiene una sola instancia de la base
         * de datos durante la ejecución.
         */
        @Volatile
        private var INSTANCE: AttendanceDatabase? = null

        fun getDatabase(context: Context): AttendanceDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AttendanceDatabase::class.java,
                    "attendance_database"
                ).build()

                INSTANCE = instance

                instance
            }
        }
    }
}