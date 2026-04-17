package com.example.tsumaps.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tsumaps.domain.models.Place
import com.example.tsumaps.ui.theme.TsuBrand
import java.time.format.DateTimeFormatter

@Composable
fun PlaceDetailDialog(
    place: Place,
    timeFormatter: DateTimeFormatter,
    onDismiss: () -> Unit,
    onBuildRoute: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = place.name,
                    color = Color.White,
                    fontSize = 18.sp
                )
                Text(
                    text = "Товары: ${place.menu.joinToString()}",
                    color = Color(0xFFC7D2FE),
                    fontSize = 12.sp
                )
                Text(
                    text = "Открытие: ${place.openingAt.format(timeFormatter)}",
                    color = Color(0xFFBFDBFE),
                    fontSize = 12.sp
                )
                Text(
                    text = "Закрытие: ${place.closingAt.format(timeFormatter)}",
                    color = Color(0xFFFECACA),
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onBuildRoute,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TsuBrand.AccentBlue
                )
            ) {
                Text("Маршрут")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF334155)
                )
            ) {
                Text("Закрыть")
            }
        }
    )
}
