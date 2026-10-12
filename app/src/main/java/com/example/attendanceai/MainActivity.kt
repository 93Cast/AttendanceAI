
package com.example.attendanceai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.ContextCompat
import com.example.attendanceai.camera.CameraScreen
import com.example.attendanceai.data.Attendance
import com.example.attendanceai.data.AttendanceDatabase
import com.example.attendanceai.ui.AttendanceHistoryScreen
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

class MainActivity : ComponentActivity() {

    private var showCamera by mutableStateOf(false)

    private val cameraPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                showCamera = true
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {

                // Obtenemos el acceso a la base de datos.
                val dao = remember {
                    AttendanceDatabase
                        .getDatabase(applicationContext)
                        .attendanceDao()
                }

                // Observamos los registros guardados.
                val attendances by dao.getAll()
                    .collectAsState(initial = emptyList())

                // Ejecutamos las operaciones de guardado
                // fuera del hilo principal.
                val scope = rememberCoroutineScope()

                if (showCamera) {
                    CameraScreen(
                        onBack = {
                            showCamera = false
                        },
                        onSaveAttendance = { classroom, count ->

                            scope.launch {
                                dao.insert(
                                    Attendance(
                                        classroom = classroom,
                                        peopleCount = count
                                    )
                                )

                                // Regresamos al inicio una vez
                                // guardado el registro.
                                showCamera = false
                            }
                        }
                    )
                } else {
                    AttendanceHistoryScreen(
                        attendances = attendances,
                        onTakeAttendance = {
                            if (
                                ContextCompat.checkSelfPermission(
                                    this@MainActivity,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                showCamera = true
                            } else {
                                cameraPermission.launch(
                                    Manifest.permission.CAMERA
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}