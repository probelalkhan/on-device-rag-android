package dev.belalkhan.myapplication.ui.components

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import dev.belalkhan.myapplication.R
import dev.belalkhan.myapplication.ui.ChatMessage
import io.noties.markwon.Markwon

@Composable
fun ChatMessageItem(message: ChatMessage) {
    if (message.isUser) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 310.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(22.dp, 22.dp, 6.dp, 22.dp),
            ) {
                Column(modifier = Modifier.padding(5.dp)) {
                    message.imageUri?.let {
                        AsyncImage(
                            model = it,
                            contentDescription = "Attached image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(4f / 3f)
                                .clip(RoundedCornerShape(17.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    if (message.text.isNotBlank()) {
                        Text(
                            text = message.text,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                modifier = Modifier.size(30.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(R.drawable.pocket_ai_mark),
                        contentDescription = null,
                        modifier = Modifier.size(27.dp),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 3.dp),
            ) {
                if (message.text.isNotBlank()) MarkdownText(message.text)
                if (message.isStreaming) {
                    Text(
                        text = "●  ●  ●",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkdownText(markdown: String) {
    val context = LocalContext.current
    val markwon = remember { Markwon.create(context) }
    val textColor = MaterialTheme.colorScheme.onBackground.toArgb()

    AndroidView(
        factory = {
            TextView(it).apply {
                textSize = 16f
                includeFontPadding = false
                movementMethod = LinkMovementMethod.getInstance()
            }
        },
        update = {
            it.setTextColor(textColor)
            markwon.setMarkdown(it, markdown)
        },
        modifier = Modifier.fillMaxWidth(),
    )
}
