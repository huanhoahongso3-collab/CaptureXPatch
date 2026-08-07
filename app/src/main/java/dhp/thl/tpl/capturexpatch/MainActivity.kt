package dhp.thl.tpl.capturexpatch

import android.Manifest
import android.app.Activity.ScreenCaptureCallback
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.WindowManager.SCREEN_RECORDING_STATE_VISIBLE
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getString
import androidx.core.graphics.drawable.toBitmap
import dhp.thl.tpl.capturexpatch.ui.theme.APPTheme
import dhp.thl.tpl.capturexpatch.utils.PrefsUtils
import dhp.thl.tpl.capturexpatch.utils.XposedChecker
import java.util.function.Consumer

class MainActivity : ComponentActivity() {

    private var screenshotCounter = mutableIntStateOf(0)
    private var screenRecordingActive = mutableStateOf("")
    private var detectionTestResult = mutableStateOf<String?>(null)

    // Mirrors how a real detector app finds screenshots: query the most recently added images
    // and look for one whose path/name mentions "screenshot". If the hooks in
    // ScreenCaptureDetectionHooker are active in this process, that row is filtered out before
    // it ever reaches this code, so the query returns nothing even right after a screenshot.
    // Returns null on failure (permission denied, provider error, etc.) instead of throwing,
    // since a raw MediaStore query can fail for reasons outside our control (e.g. Android 14
    // partial photo-access grants) and must never crash the host app.
    private fun mediaStoreShowsRecentScreenshot(): Boolean? {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA
        )
        try {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC LIMIT 10"
            )?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                val dataIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                while (cursor.moveToNext()) {
                    val name = if (nameIndex >= 0) cursor.getString(nameIndex) else null
                    val path = if (dataIndex >= 0) cursor.getString(dataIndex) else null
                    if (name?.contains("screenshot", ignoreCase = true) == true ||
                        path?.contains("screenshot", ignoreCase = true) == true
                    ) {
                        return true
                    }
                }
            } ?: return null
        } catch (e: Throwable) {
            android.util.Log.e("CaptureXPatch", "Screenshot detection test query failed", e)
            return null
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        setTheme(R.style.Theme_APP)
        super.onCreate(savedInstanceState)
        screenshotCounter.intValue = savedInstanceState?.getInt("counter") ?: 0
        PrefsUtils.loadPrefs()

        setContent {
            APPTheme {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    AppUI()
                }
            }
        }
    }

    private val screenCaptureCallback = ScreenCaptureCallback {
        screenshotCounter.intValue++
    }

    private val screenRecordCallback = Consumer<Int> { state ->
        if (state == SCREEN_RECORDING_STATE_VISIBLE) {
            screenRecordingActive.value = "YES"
        }
        else {
            screenRecordingActive.value = "NO"
        }
    }

    override fun onStart() {
        super.onStart()
        registerScreenCaptureCallback(mainExecutor, screenCaptureCallback)

        // If Android version is 15 or newer, add screen record callback.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            screenRecordCallback.accept(
                windowManager.addScreenRecordingCallback(mainExecutor, screenRecordCallback))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("counter", screenshotCounter.intValue)
    }

    override fun onStop() {
        super.onStop()
        unregisterScreenCaptureCallback(screenCaptureCallback)

        // If Android version is 15 or newer, remove screen record callback.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            windowManager.removeScreenRecordingCallback(screenRecordCallback)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AppUI(modifier: Modifier = Modifier) {
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

        Scaffold(
            topBar = {
                LargeTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                bitmap = loadXmlDrawable(R.drawable.ic_launcher_round)!!,
                                contentDescription = stringResource(R.string.xposed_name)
                            )
                            Spacer(Modifier.padding(horizontal = 5.dp))
                            Text(
                                text = stringResource(R.string.xposed_name),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            },
            modifier = modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.displayCutout)
        ) { p ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(p)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.Start
            ) {
                MainCard()
                TestCard()
                DetectionTestCard()
            }
        }
    }

    @Composable
    fun MainCard() {
        OutlinedCard(modifier = Modifier.fillMaxWidth()){
            Column(modifier = Modifier.padding(16.dp)){
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ){
                    if(XposedChecker.isEnabled()){
                        Icon(painterResource(R.drawable.checklist_24), getString(R.string.status))
                    }else{
                        Icon(painterResource(R.drawable.error_24), getString(R.string.error))
                    }
                    Text(getString(R.string.status_title), fontSize = 24.sp)
                }

                if (!XposedChecker.isEnabled()) {
                    Text(
                        text = getString(LocalContext.current, R.string.module_disabled),
                        fontSize = 20.sp,
                        textAlign = TextAlign.Start,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                else {
                    Text(
                        text = getString(LocalContext.current, R.string.detection_blocked),
                        fontSize = 20.sp,
                        textAlign = TextAlign.Start,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }

    @Composable
    fun TestCard(){
        OutlinedCard(modifier = Modifier.fillMaxWidth()){
            Column(modifier = Modifier.padding(16.dp)){
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ){
                    Icon(painterResource(R.drawable.test_tube_24), getString(R.string.card_title_testing))
                    Text(getString(R.string.card_title_testing), fontSize = 24.sp)
                }
                Text(
                    text = "Screenshot Counter: ${screenshotCounter.intValue}",
                    fontSize = 20.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(10.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                // If Android version is 15 or newer, show recording status.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    Text(
                        text = "Recording in Progress: ${screenRecordingActive.value}",
                        fontSize = 20.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.padding(10.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    @Composable
    fun DetectionTestCard() {
        val context = LocalContext.current
        val readImagesPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                detectionTestResult.value = when (mediaStoreShowsRecentScreenshot()) {
                    true -> "Screenshot found — MediaStore is NOT filtered. Make sure the module is patched into this app and Xposed/LSPatch is active."
                    false -> "No screenshot found in the last 10 images — screenshot detection is blocked."
                    null -> "Could not run the test query (check permission or Logcat for 'CaptureXPatch')."
                }
            } else {
                detectionTestResult.value = "Storage permission is required to run this test."
            }
        }

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(painterResource(R.drawable.test_tube_24), getString(R.string.card_title_testing))
                    Text("Screenshot Detection Test", fontSize = 24.sp)
                }
                Text(
                    text = "1. Take a screenshot now (e.g. power + volume down).\n" +
                        "2. Come back and tap the button below.\n" +
                        "This repeats the same MediaStore query a real detector app would use.",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(top = 10.dp, bottom = 10.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Button(onClick = {
                    val granted = ContextCompat.checkSelfPermission(
                        context, readImagesPermission
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        detectionTestResult.value = when (mediaStoreShowsRecentScreenshot()) {
                            true -> "Screenshot found — MediaStore is NOT filtered. Make sure the module is patched into this app and Xposed/LSPatch is active."
                            false -> "No screenshot found in the last 10 images — screenshot detection is blocked."
                            null -> "Could not run the test query (check permission or Logcat for 'CaptureXPatch')."
                        }
                    } else {
                        permissionLauncher.launch(readImagesPermission)
                    }
                }) {
                    Text("Check for Screenshot Detection")
                }
                detectionTestResult.value?.let { result ->
                    Text(
                        text = result,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.padding(top = 10.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    /*
        loadXmlDrawable() was sourced from
        https://slack-chats.kotlinlang.org/t/506477/hello-i-am-trying-to-load-a-layer-list-drawable-with-this-co#707c4aef-021c-421b-b873-ea7ca453b61e
     */
    @Composable
    fun loadXmlDrawable(@DrawableRes resId: Int): ImageBitmap? =
        ContextCompat.getDrawable(
            LocalContext.current,
            resId
        )?.toBitmap()?.asImageBitmap()
}