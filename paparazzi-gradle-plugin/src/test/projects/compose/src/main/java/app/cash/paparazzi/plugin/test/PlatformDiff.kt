package app.cash.paparazzi.plugin.test

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PlatformDiff() {
  Column(
    Modifier
      .background(Color.White)
      .fillMaxSize()
  ) {
    Box(
      Modifier
        .fillMaxSize()
        .padding(48.dp)
        .background(Color.Black.copy(alpha = 0.5f))
    ) {
    }
  }
}
