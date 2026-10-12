
package com.example.attendanceai.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    /*
     * Guarda una asistencia.
     * Devuelve el ID del registro creado.
     */
    @Insert
    suspend fun insert(attendance: Attendance): Long

    /*
     * Obtiene todas las asistencias,
     * mostrando primero las más recientes.
     *
     * Flow actualiza el historial cuando cambian
     * los datos de la tabla.
     */
    @Query("SELECT * FROM attendances ORDER BY timestamp DESC")
    fun getAll(): Flow<List<Attendance>>

    /*
     * Elimina un registro específico.
     */
    @Delete
    suspend fun delete(attendance: Attendance)
}