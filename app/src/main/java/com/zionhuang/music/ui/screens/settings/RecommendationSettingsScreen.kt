package com.zionhuang.music.ui.screens.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.zionhuang.music.LocalPlayerAwareWindowInsets
import com.zionhuang.music.R
import com.zionhuang.music.constants.ActivityAwareRecsKey
import com.zionhuang.music.data.recommendation.ActivityContext
import com.zionhuang.music.ui.component.IconButton
import com.zionhuang.music.ui.component.PreferenceEntry
import com.zionhuang.music.ui.component.PreferenceGroupTitle
import com.zionhuang.music.ui.component.SwitchPreference
import com.zionhuang.music.ui.utils.backToMain
import com.zionhuang.music.utils.rememberPreference
import com.zionhuang.music.viewmodels.ExploreViewModel
import com.zionhuang.music.viewmodels.FeedbackViewModel
import com.zionhuang.music.viewmodels.RecommendationSettingsViewModel
import kotlinx.coroutines.launch

/**
 * Settings screen for the hybrid recommendation engine.
 *
 * Allows the user to:
 *  - View an explanation of how recommendations work.
 *  - **Enable/disable activity-aware recommendations** with a permission
 *    rationale dialog shown on first activation.
 *  - Reset all interaction data (play history, likes, dislikes) to start fresh.
 *
 * ## Activity-aware permission flow
 * 1. User taps the toggle.
 * 2. If [ActivityRecognitionManager.hasPermission] is `false`, show the
 *    rationale dialog first.
 * 3. If the user accepts the rationale, launch the system permission request.
 * 4. If granted → store preference `true`, call [ExploreViewModel.startActivityAwareMode].
 * 5. If denied → show a snack / note, keep preference `false`.
 * 6. Toggling off → store `false`, call [ExploreViewModel.stopActivityAwareMode].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationSettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: RecommendationSettingsViewModel = hiltViewModel(),
    feedbackViewModel: FeedbackViewModel = hiltViewModel(),
    exploreViewModel: ExploreViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activityRecognitionManager = viewModel.activityRecognitionManager

    // ── Preferences ───────────────────────────────────────────────────────────
    var activityAwareEnabled by rememberPreference(ActivityAwareRecsKey, defaultValue = false)

    // ── Live activity context (for status chip) ───────────────────────────────
    val currentActivity by exploreViewModel.activityContext.collectAsState()

    // ── Dialog states ─────────────────────────────────────────────────────────
    var showResetDialog   by remember { mutableStateOf(false) }
    var showRationale     by remember { mutableStateOf(false) }
    var showDeniedNotice  by remember { mutableStateOf(false) }

    // ── Permission launcher ───────────────────────────────────────────────────
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            activityAwareEnabled = true
            activityRecognitionManager.start()
            exploreViewModel.startActivityAwareMode()
        } else {
            activityAwareEnabled = false
            showDeniedNotice = true
        }
    }

    // ── Reset confirmation dialog ─────────────────────────────────────────────
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.rec_settings_reset_title)) },
            text  = { Text(stringResource(R.string.rec_settings_reset_description)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.resetAllInteractions()
                            feedbackViewModel.clearCache()
                        }
                        showResetDialog = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.reset),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    // ── Permission rationale dialog ───────────────────────────────────────────
    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text(stringResource(R.string.rec_settings_activity_permission_rationale_title)) },
            text  = { Text(stringResource(R.string.rec_settings_activity_permission_rationale_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRationale = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                        } else {
                            // Pre-Q: permission is granted at install time
                            activityAwareEnabled = true
                            activityRecognitionManager.start()
                            exploreViewModel.startActivityAwareMode()
                        }
                    }
                ) {
                    Text(stringResource(R.string.allow))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    // ── Permission permanently denied dialog ──────────────────────────────────
    if (showDeniedNotice) {
        AlertDialog(
            onDismissRequest = { showDeniedNotice = false },
            title = { Text(stringResource(R.string.rec_settings_activity_permission_rationale_title)) },
            text  = { Text(stringResource(R.string.rec_settings_activity_permission_denied)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeniedNotice = false
                        // Deep-link to app settings so the user can grant manually
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                        )
                    }
                ) {
                    Text(stringResource(R.string.open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeniedNotice = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    Column(
        Modifier
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
            )
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
            )
        )

        // ── How it works ──────────────────────────────────────────────────────
        PreferenceGroupTitle(title = stringResource(R.string.rec_settings_group_how_it_works))

        PreferenceEntry(
            title = { Text(stringResource(R.string.rec_settings_how_it_works_title)) },
            description = stringResource(R.string.rec_settings_how_it_works_body),
            icon = { Icon(painterResource(R.drawable.info), contentDescription = null) },
        )

        // ── Activity-aware music ──────────────────────────────────────────────
        PreferenceGroupTitle(title = stringResource(R.string.rec_settings_group_activity))

        SwitchPreference(
            title = { Text(stringResource(R.string.rec_settings_activity_toggle_title)) },
            description = if (activityAwareEnabled) {
                // Show current detected activity as status
                when (currentActivity) {
                    ActivityContext.Running -> stringResource(R.string.rec_settings_activity_status_running)
                    ActivityContext.Walking -> stringResource(R.string.rec_settings_activity_status_walking)
                    ActivityContext.Still   -> stringResource(R.string.rec_settings_activity_status_still)
                    ActivityContext.Vehicle -> stringResource(R.string.rec_settings_activity_status_vehicle)
                    ActivityContext.Unknown -> stringResource(R.string.rec_settings_activity_status_unknown)
                }
            } else {
                stringResource(R.string.rec_settings_activity_toggle_subtitle)
            },
            icon = { Icon(painterResource(R.drawable.tune), contentDescription = null) },
            checked = activityAwareEnabled,
            onCheckedChange = { wantsEnabled ->
                if (wantsEnabled) {
                    if (activityRecognitionManager.hasPermission()) {
                        // Already granted — enable immediately
                        activityAwareEnabled = true
                        activityRecognitionManager.start()
                        exploreViewModel.startActivityAwareMode()
                    } else {
                        // Show rationale before requesting permission
                        showRationale = true
                    }
                } else {
                    activityAwareEnabled = false
                    activityRecognitionManager.stop()
                    exploreViewModel.stopActivityAwareMode()
                }
            }
        )

        // ── Your data ─────────────────────────────────────────────────────────
        PreferenceGroupTitle(title = stringResource(R.string.rec_settings_group_data))

        PreferenceEntry(
            title   = { Text(stringResource(R.string.rec_settings_reset_title)) },
            description = stringResource(R.string.rec_settings_reset_subtitle),
            icon    = { Icon(painterResource(R.drawable.restore), contentDescription = null) },
            onClick = { showResetDialog = true }
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.rec_settings_title)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
            }
        },
        scrollBehavior = scrollBehavior,
    )
}
