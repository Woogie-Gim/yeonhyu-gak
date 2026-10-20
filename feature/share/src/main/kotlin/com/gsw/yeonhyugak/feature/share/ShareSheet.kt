package com.gsw.yeonhyugak.feature.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Picture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gsw.yeonhyugak.core.ui.theme.YhTheme
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val SHARE_SHEET_TAG = "share_sheet"
const val SHARE_ACTION_TAG = "share_action"

fun ratioTag(ratio: ShareRatio): String = "share_ratio_${ratio.name.lowercase()}"

// 공유 시트: 비율 고르기 · 미리보기 · 공유하기. onShare는 테스트에서 바꾼다
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ShareRoute(
    year: Int,
    onDismiss: () -> Unit,
    viewModel: ShareViewModel = hiltViewModel(),
    onShare: suspend (Context, Bitmap) -> Unit = ShareImage::send,
) {
    LaunchedEffect(year) { viewModel.show(year) }
    val card by viewModel.card.collectAsStateWithLifecycle()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = YhTheme.colors.background,
        // 시트는 별도 창이라 Maestro용 id를 여기서 다시 켠다
        modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .testTag(SHARE_SHEET_TAG),
    ) {
        card?.takeIf { it.year == year }?.let { ShareSheetContent(it, onShare) }
    }
}

// 시트 창 밖에서도 그릴 수 있게 분리 (스크린샷 테스트는 이 부분만 띄운다)
@Composable
fun ShareSheetContent(card: ShareCardData, onShare: suspend (Context, Bitmap) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var ratio by rememberSaveable { mutableStateOf(ShareRatio.FEED) }
    var failed by rememberSaveable { mutableStateOf(false) }
    val picture = remember { Picture() }
    // 앱 이름은 app 모듈 app_name 한 곳에서
    val appName = remember(context) {
        context.applicationInfo.loadLabel(context.packageManager).toString()
    }
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.share_title), style = MaterialTheme.typography.titleMedium)
        RatioChips(ratio) { ratio = it }
        RecordedCard(picture) { ShareCard(card, ratio, appName) }
        Text(
            stringResource(if (failed) R.string.share_error else R.string.share_privacy_note),
            style = MaterialTheme.typography.bodySmall,
            color = if (failed) YhTheme.colors.warning else YhTheme.colors.muted,
        )
        ShareButton {
            scope.launch {
                failed = try {
                    onShare(context, picture.toBitmap())
                    false
                } catch (_: IOException) {
                    true
                }
            }
        }
    }
}

@Composable
private fun RatioChips(selected: ShareRatio, onSelect: (ShareRatio) -> Unit) {
    val colors = YhTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ShareRatio.entries.forEach { r ->
            FilterChip(
                selected = selected == r,
                onClick = { onSelect(r) },
                label = {
                    Text(
                        stringResource(
                            when (r) {
                                ShareRatio.FEED -> R.string.share_ratio_feed
                                ShareRatio.STORY -> R.string.share_ratio_story
                            },
                        ),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colors.accent,
                    selectedLabelColor = colors.onAccent,
                ),
                modifier = Modifier.heightIn(min = 48.dp).testTag(ratioTag(r)),
            )
        }
    }
}

// 화면에 보이는 그대로 Picture에 기록해 두고 공유할 때 비트맵으로
@Composable
private fun RecordedCard(picture: Picture, content: @Composable () -> Unit) {
    Box(
        Modifier.drawWithCache {
            val w = size.width.toInt()
            val h = size.height.toInt()
            onDrawWithContent {
                val recording = Canvas(picture.beginRecording(w, h))
                draw(this, layoutDirection, recording, size) {
                    this@onDrawWithContent.drawContent()
                }
                picture.endRecording()
                drawIntoCanvas { it.nativeCanvas.drawPicture(picture) }
            }
        },
    ) {
        content()
    }
}

@Composable
private fun ShareButton(onClick: () -> Unit) {
    val colors = YhTheme.colors
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.accent,
            contentColor = colors.onAccent,
        ),
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(SHARE_ACTION_TAG),
    ) {
        Text(stringResource(R.string.share_action))
    }
}

// 아직 그려지지 않았으면 공유하지 않는다
private fun Picture.toBitmap(): Bitmap {
    if (width <= 0 || height <= 0) throw IOException("카드가 아직 그려지지 않음")
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    android.graphics.Canvas(bitmap).drawPicture(this)
    return bitmap
}

// 기기 안에서 PNG로 만든 뒤 안드로이드 공유 시트
object ShareImage {
    private const val DIR = "share"
    private const val FILE = "yeonhyugak_card.png"
    private const val QUALITY = 100

    suspend fun send(context: Context, bitmap: Bitmap) {
        val file = withContext(Dispatchers.IO) { save(context, bitmap) }
        val uri = FileProvider.getUriForFile(context, authority(context), file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val chooser = Intent.createChooser(send, context.getString(R.string.share_chooser))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun save(context: Context, bitmap: Bitmap): File {
        val dir = File(context.cacheDir, DIR).apply { mkdirs() }
        val file = File(dir, FILE)
        file.outputStream().use {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, QUALITY, it)) {
                throw IOException("PNG 저장 실패")
            }
        }
        return file
    }

    fun authority(context: Context): String = "${context.packageName}.share"
}
