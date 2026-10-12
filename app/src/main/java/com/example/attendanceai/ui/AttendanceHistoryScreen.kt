
package com.example.attendanceai.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.attendanceai.data.Attendance
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AttendanceHistoryScreen(
    attendances: List<Attendance>,
    onTakeAttendance: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            text = "Attendance AI",
            style = MaterialTheme.typography.headlineLarge
        )

        Text(
            text = "Sistema de asistencia mediante IA",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp)
        )

        Button(
            onClick = onTakeAttendance,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
        ) {
            Text("Tomar asistencia")
        }

        Text(
            text = "Historial de asistencias",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
        )

        if (attendances.isEmpty()) {
            Text(
                text = "Todavía no hay asistencias guardadas.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = attendances,
                    key = { it.id }
                ) { attendance ->
                    AttendanceCard(attendance)
                }
            }
        }
    }
}

@Composable
private fun AttendanceCard(
    attendance: Attendance
) {
    val formattedDate = SimpleDateFormat(
        "dd/MM/yyyy · HH:mm",
        Locale.getDefault()
    ).format(Date(attendance.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Asistencia ${attendance.id} — Salón ${attendance.classroom}",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(Modifier.padding(horizontal = 4.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = attendance.peopleCount.toString(),
                    style = MaterialTheme.typography.headlineMedium
                )

                Text(
                    text = "personas",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}