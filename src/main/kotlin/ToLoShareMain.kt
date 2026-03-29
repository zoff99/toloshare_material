import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main(args: Array<String>) = application(exitProcessOnExit = true) {
    var isOpen by remember { mutableStateOf(true) }
    if (isOpen)
    {
        Window(onCloseRequest = { isOpen = false }, title = "Info") {
            Column {
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "some text",
                    style = MaterialTheme.typography.body1.copy(
                        fontSize = 22.sp,
                    ),
                )
                Button(onClick = {}) {
                    Text(
                        text = "click me",
                        style = MaterialTheme.typography.body1.copy(
                            fontSize = 22.sp,
                        ),
                    )
                }
                Column(modifier = Modifier.height(500.dp).width(400.dp).background(Color.Red)) {
                }
                Spacer(modifier = Modifier.height(5.dp))
            }
        }
    }
}

