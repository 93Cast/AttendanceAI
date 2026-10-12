
package com.example.attendanceai.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Representa un registro de asistencia.
 *
 * Room creará una tabla llamada "attendances".
 */
@Entity(tableName = "attendances")
data class Attendance(

    /*
     * Identificador único.
     * Room lo genera automáticamente.
     */
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /*
     * Salón donde se realizó el conteo.
     * Ejemplo: 3°C.
     */
    val classroom: String,

    /*
     * Cantidad de personas detectadas por YOLO.
     */
    val peopleCount: Int,

    /*
     * Fecha y hora en milisegundos.
     * Se asigna al crear el registro.
     */
    val timestamp: Long = System.currentTimeMillis()
)