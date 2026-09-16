package dev.belalkhan.myapplication.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.belalkhan.myapplication.R

@Composable
fun ChatComposer(
    enabled: Boolean,
    onSend: (String, Uri?) -> Unit,
) {
    var prompt by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(PickVisualMedia()) { imageUri = it }

    Surface(
        tonalElevation = 4.dp,
        shadowElevation = 10.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            imageUri?.let { uri ->
                Box(modifier = Modifier.padding(bottom = 10.dp)) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Selected image",
                        modifier = Modifier
                            .size(86.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.65f),
                    ) {
                        IconButton(
                            onClick = { imageUri = null },
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = "Remove image",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    IconButton(
                        onClick = {
                            picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                        },
                        enabled = enabled,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add_photo),
                            contentDescription = "Add image",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    BasicTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        enabled = enabled,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp, vertical = 12.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        maxLines = 5,
                        decorationBox = { textField ->
                            Box {
                                if (prompt.isEmpty()) {
                                    Text(
                                        text = "Message Pocket AI…",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                textField()
                            }
                        },
                    )

                    val canSend = enabled && (prompt.isNotBlank() || imageUri != null)
                    Surface(
                        shape = CircleShape,
                        color = if (canSend) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    ) {
                        IconButton(
                            onClick = {
                                onSend(prompt, imageUri)
                                prompt = ""
                                imageUri = null
                            },
                            enabled = canSend,
                            modifier = Modifier.size(44.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_send),
                                contentDescription = "Send",
                                tint = if (canSend) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
