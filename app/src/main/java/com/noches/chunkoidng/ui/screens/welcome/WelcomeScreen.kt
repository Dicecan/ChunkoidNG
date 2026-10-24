package com.noches.chunkoidng.ui.screens.welcome

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.noches.chunkoidng.core.permission.PermissionHelper
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.noches.chunkoidng.R
import com.noches.chunkoidng.core.settings.DeviceHardwareInfo
import com.noches.chunkoidng.core.settings.DevicePerformanceEvaluator
import com.noches.chunkoidng.core.settings.DeviceTier
import com.noches.chunkoidng.core.settings.PerformanceLimits
import com.noches.chunkoidng.core.settings.PerformanceMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WelcomeScreen(
    onFinished: () -> Unit,
    onLanguageSelected: ((String) -> Unit)? = null,
    isRevisit: Boolean = false,
    isUpdate: Boolean = false,
    viewModel: WelcomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentStep by rememberSaveable { mutableIntStateOf(0) }

    BackHandler(enabled = currentStep > 0) {
        currentStep--
    }

    val tunedLimits = remember(uiState.hardwareInfo) {
        DevicePerformanceEvaluator.getTunedLimits(uiState.hardwareInfo)
    }
    val strictLimits = remember(uiState.hardwareInfo) {
        DevicePerformanceEvaluator.getStrictLimits(uiState.hardwareInfo)
    }

    Scaffold(
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    
                    Box(modifier = Modifier.width(108.dp)) {
                        if (currentStep > 0) {
                            OutlinedButton(
                                onClick = { currentStep-- },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    stringResource(R.string.welcome_step_prev),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(4) { step ->
                            val isCurrent = step == currentStep
                            val width by animateDpAsState(
                                targetValue = if (isCurrent) 28.dp else 8.dp,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "dotWidth"
                            )
                            val color by animateColorAsState(
                                targetValue = if (isCurrent) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                label = "dotColor"
                            )
                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .width(width)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { currentStep = step }
                            )
                        }
                    }

                    Box(modifier = Modifier.width(136.dp), contentAlignment = Alignment.CenterEnd) {
                        Button(
                            onClick = {
                                if (currentStep < 3) {
                                    currentStep++
                                } else {
                                    viewModel.completeOnboarding()
                                    onFinished()
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.height(44.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(
                                text = if (currentStep < 3) {
                                    stringResource(R.string.welcome_step_next)
                                } else {
                                    if (isRevisit) stringResource(R.string.welcome_action_save)
                                    else if (isUpdate) stringResource(R.string.welcome_action_update_finish)
                                    else stringResource(R.string.welcome_step_start)
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (currentStep < 3) {
                                    Icons.AutoMirrored.Outlined.ArrowForward
                                } else {
                                    if (isRevisit) Icons.Outlined.Save else Icons.Outlined.RocketLaunch
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { width -> width / 3 } + fadeIn(tween(260))) togetherWith
                            (slideOutHorizontally { width -> -width / 3 } + fadeOut(tween(200)))
                } else {
                    (slideInHorizontally { width -> -width / 3 } + fadeIn(tween(260))) togetherWith
                            (slideOutHorizontally { width -> width / 3 } + fadeOut(tween(200)))
                }.using(SizeTransform(clip = false))
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            label = "stepTransition"
        ) { step ->
            when (step) {
                0 -> StepIntroPage(isUpdate = isUpdate)
                1 -> StepLanguagePage(
                    selectedLang = uiState.selectedLanguage,
                    onSelect = { code ->
                        viewModel.updateLanguage(code)
                        onLanguageSelected?.invoke(code)
                    }
                )
                2 -> StepPermissionsAndEnvPage(viewModel = viewModel)
                3 -> StepPerformancePage(
                    uiState = uiState,
                    tunedLimits = tunedLimits,
                    strictLimits = strictLimits,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun StepIntroPage(isUpdate: Boolean = false) {
    val logoScale = remember { Animatable(0f) }
    val logoOffsetY = remember { Animatable(32f) }
    val logoGlowAlpha = remember { Animatable(0f) }

    val letterCScale = remember { Animatable(0f) }
    val letterCAlpha = remember { Animatable(0f) }

    val textExpansion = remember { Animatable(0f) }

    val introScale = remember { Animatable(0.85f) }
    val introAlpha = remember { Animatable(0f) }
    val introOffsetY = remember { Animatable(24f) }

    val infiniteTransition = rememberInfiniteTransition(label = "idleBreath")
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idlePulse"
    )

    LaunchedEffect(Unit) {
        
        launch {
            logoGlowAlpha.animateTo(
                targetValue = 0.55f,
                animationSpec = tween(500, easing = FastOutSlowInEasing)
            )
        }
        logoScale.animateTo(
            targetValue = 1.25f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )

        launch {
            logoScale.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        logoOffsetY.animateTo(
            targetValue = 0f,
            animationSpec = tween(420, easing = FastOutSlowInEasing)
        )

        launch {
            letterCAlpha.animateTo(1f, tween(180))
        }
        letterCScale.animateTo(
            targetValue = 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )

        textExpansion.animateTo(
            targetValue = 1.0f,
            animationSpec = spring(
                dampingRatio = 0.72f,
                stiffness = Spring.StiffnessLow
            )
        )

        launch {
            introOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            introScale.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        introAlpha.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(380, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.offset(y = (-24).dp)
        ) {
            
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        val baseScale = logoScale.value * if (logoScale.value >= 1.0f) idlePulse else 1.0f
                        scaleX = baseScale
                        scaleY = baseScale
                        translationY = logoOffsetY.value
                    }
            ) {
                
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = logoGlowAlpha.value),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp,
                    modifier = Modifier.size(88.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_app_logo),
                            contentDescription = "Chunkoid NG Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                
                Text(
                    text = "C",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.graphicsLayer {
                        scaleX = letterCScale.value
                        scaleY = letterCScale.value
                        alpha = letterCAlpha.value
                    }
                )

                Box(
                    modifier = Modifier
                        .clipToBounds()
                        .graphicsLayer {
                            alpha = textExpansion.value.coerceIn(0f, 1f)
                        }
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            val targetWidth = (placeable.width * textExpansion.value.coerceIn(0f, 1f)).toInt()
                            layout(targetWidth, placeable.height) {
                                placeable.place(0, 0)
                            }
                        }
                ) {
                    Text(
                        text = "hunkoid NG",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                        letterSpacing = (-0.5).sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = introAlpha.value
                    scaleX = introScale.value
                    scaleY = introScale.value
                    translationY = introOffsetY.value
                }
            ) {
                Text(
                    text = stringResource(R.string.welcome_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = if (isUpdate) stringResource(R.string.welcome_badge_update, "Beta 0.6")
                               else "Beta 0.6 · EncoreTeam Recode",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StepLanguagePage(
    selectedLang: String,
    onSelect: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Translate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = stringResource(R.string.welcome_language_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.welcome_step_indicator, 2, 4),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        val languages = listOf(
            Triple("system", stringResource(R.string.welcome_lang_system), "Auto-detect from OS language"),
            Triple("zh", stringResource(R.string.welcome_lang_zh), "中国大陆地区规范汉字"),
            Triple("en", stringResource(R.string.welcome_lang_en), "Standard International English"),
            Triple("ja", stringResource(R.string.welcome_lang_ja), "日本語ローカライズ")
        )

        languages.forEach { (code, title, subtitle) ->
            val isSelected = selectedLang.equals(code, ignoreCase = true)
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.surfaceContainerLow
                ),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onSelect(code) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isSelected) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    } else {
                        RadioButton(
                            selected = false,
                            onClick = { onSelect(code) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun StepPermissionsAndEnvPage(viewModel: WelcomeViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasNotification by remember {
        mutableStateOf(PermissionHelper.hasNotificationPermission(context))
    }
    var isIgnoringBattery by remember {
        mutableStateOf(PermissionHelper.isIgnoringBatteryOptimizations(context))
    }

    val isRootfsReady by viewModel.isRootfsReady.collectAsState()
    val isDeploying by viewModel.isDeployingEnvironment.collectAsState()
    val deploymentProgress by viewModel.deploymentProgress.collectAsState()
    val deploymentStatusText by viewModel.deploymentStatusText.collectAsState()

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotification = isGranted || PermissionHelper.hasNotificationPermission(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotification = PermissionHelper.hasNotificationPermission(context)
                isIgnoringBattery = PermissionHelper.isIgnoringBatteryOptimizations(context)
                viewModel.checkEnvironmentReady()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = stringResource(R.string.welcome_step_env_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.welcome_step_indicator, 3, 4),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Text(
            text = stringResource(R.string.welcome_step_env_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isRootfsReady) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.tertiaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Terminal,
                            contentDescription = null,
                            tint = if (isRootfsReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.welcome_env_runtime_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isRootfsReady) stringResource(R.string.welcome_env_status_ready)
                            else if (isDeploying) stringResource(R.string.welcome_env_deploying, deploymentProgress)
                            else stringResource(R.string.welcome_env_status_not_ready),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isRootfsReady) Color(0xFF10B981)
                            else if (isDeploying) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                    }

                    if (isRootfsReady && !isDeploying) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Ready",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.welcome_env_runtime_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (isDeploying) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            progress = { deploymentProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (deploymentStatusText.isNotEmpty()) deploymentStatusText
                            else stringResource(R.string.welcome_env_deploying, deploymentProgress),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (isRootfsReady) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.welcome_env_ready_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = { viewModel.deployEnvironment() },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.welcome_env_redeploy),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.welcome_env_not_ready_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.deployEnvironment() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Outlined.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.welcome_env_action_deploy),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isIgnoringBattery) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.errorContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BatteryChargingFull,
                            contentDescription = null,
                            tint = if (isIgnoringBattery) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.welcome_env_battery_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isIgnoringBattery) stringResource(R.string.welcome_env_battery_allowed)
                            else stringResource(R.string.welcome_env_battery_restricted),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isIgnoringBattery) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.welcome_env_battery_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!isIgnoringBattery) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            PermissionHelper.requestIgnoreBatteryOptimizations(context)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Outlined.Power,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.welcome_env_battery_action))
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (hasNotification) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.secondaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = null,
                            tint = if (hasNotification) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.welcome_env_notification_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (hasNotification) stringResource(R.string.welcome_env_notification_allowed)
                            else stringResource(R.string.welcome_env_notification_denied),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (hasNotification) Color(0xFF10B981) else MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.welcome_env_notification_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!hasNotification) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                PermissionHelper.openNotificationSettings(context)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Outlined.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.welcome_env_notification_action))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.welcome_env_tip),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun StepPerformancePage(
    uiState: WelcomeUiState,
    tunedLimits: PerformanceLimits,
    strictLimits: PerformanceLimits,
    viewModel: WelcomeViewModel
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = stringResource(R.string.welcome_profile_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.welcome_step_indicator, 4, 4),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.welcome_hardware_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.welcome_hardware_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val tierColor = when (uiState.hardwareInfo.tier) {
                        DeviceTier.FLAGSHIP -> MaterialTheme.colorScheme.primary
                        DeviceTier.HIGH_END -> MaterialTheme.colorScheme.secondary
                        DeviceTier.MID_RANGE -> MaterialTheme.colorScheme.tertiary
                        DeviceTier.ENTRY_LEVEL -> MaterialTheme.colorScheme.error
                    }
                    Surface(
                        shape = CircleShape,
                        color = tierColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = stringResource(uiState.hardwareInfo.tier.titleRes),
                            color = tierColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(10.dp))

                BenchmarkDetailRow(
                    icon = Icons.Outlined.Smartphone,
                    label = stringResource(R.string.welcome_device_model),
                    value = uiState.hardwareInfo.model
                )
                BenchmarkDetailRow(
                    icon = Icons.Outlined.Memory,
                    label = stringResource(R.string.welcome_device_cpu),
                    value = "${uiState.hardwareInfo.cpuCores} Cores (${if (uiState.hardwareInfo.is64Bit) "64-bit" else "32-bit"})"
                )
                BenchmarkDetailRow(
                    icon = Icons.Outlined.Speed,
                    label = stringResource(R.string.welcome_device_benchmark),
                    value = "${uiState.hardwareInfo.computeScore} pts (${String.format(java.util.Locale.US, "%.1f", uiState.hardwareInfo.cpuMaxFreqMhz / 1000f)} GHz)"
                )
                val availGb = String.format(java.util.Locale.US, "%.1f", uiState.hardwareInfo.availRamMb / 1024f)
                BenchmarkDetailRow(
                    icon = Icons.Outlined.DeveloperBoard,
                    label = stringResource(R.string.welcome_device_ram),
                    value = "${uiState.hardwareInfo.totalRamMb} MB (~${availGb}G 可用)"
                )
                BenchmarkDetailRow(
                    icon = Icons.Outlined.Storage,
                    label = stringResource(R.string.welcome_device_storage),
                    value = "${uiState.hardwareInfo.freeStorageMb / 1024} GB"
                )
            }
        }

        ProfileModeSelectableCard(
            title = stringResource(R.string.welcome_mode_tuned_title),
            description = stringResource(R.string.welcome_mode_tuned_desc),
            icon = Icons.Outlined.AutoAwesome,
            isSelected = uiState.selectedMode == PerformanceMode.TUNED,
            onClick = { viewModel.selectMode(PerformanceMode.TUNED) },
            metrics = listOf(
                "JVM: ${tunedLimits.jvmMaxMemoryMb} MB",
                "${tunedLimits.parallelism} 线程并发",
                if (tunedLimits.useSerialGc) "SerialGC" else "并行 GC"
            )
        )

        ProfileModeSelectableCard(
            title = stringResource(R.string.welcome_mode_strict_title),
            description = stringResource(R.string.welcome_mode_strict_desc),
            icon = Icons.Outlined.Security,
            isSelected = uiState.selectedMode == PerformanceMode.STRICT,
            onClick = { viewModel.selectMode(PerformanceMode.STRICT) },
            metrics = listOf(
                "JVM: ${strictLimits.jvmMaxMemoryMb} MB",
                "单线程 (1)",
                "SerialGC 轻量回收"
            )
        )

        ProfileModeSelectableCard(
            title = stringResource(R.string.welcome_mode_custom_title),
            description = stringResource(R.string.welcome_mode_custom_desc),
            icon = Icons.Outlined.SettingsApplications,
            isSelected = uiState.selectedMode == PerformanceMode.CUSTOM,
            onClick = {
                viewModel.selectMode(PerformanceMode.CUSTOM)
                if (!uiState.isCustomExpanded) viewModel.toggleCustomExpanded()
            },
            trailingContent = {
                IconButton(onClick = { viewModel.toggleCustomExpanded() }) {
                    Icon(
                        imageVector = if (uiState.isCustomExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }
            }
        )

        AnimatedVisibility(
            visible = uiState.selectedMode == PerformanceMode.CUSTOM && uiState.isCustomExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_submodule_limits_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.welcome_custom_jvm_heap),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "${uiState.customMaxMemoryMb.toInt()} MB",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = uiState.customMaxMemoryMb,
                        onValueChange = { viewModel.updateCustomMaxMemory(it) },
                        valueRange = 1024f..8192f,
                        steps = 6
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.welcome_custom_parallelism),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "${uiState.customParallelism} Threads",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = uiState.customParallelism.toFloat(),
                        onValueChange = { viewModel.updateCustomParallelism(it.toInt()) },
                        valueRange = 1f..uiState.hardwareInfo.cpuCores.toFloat().coerceAtLeast(1f),
                        steps = (uiState.hardwareInfo.cpuCores - 2).coerceAtLeast(0)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.welcome_custom_serial_gc),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.welcome_custom_serial_gc_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.customUseSerialGc,
                        onCheckedChange = { viewModel.updateCustomUseSerialGc(it) }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.welcome_custom_bomb_entry),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${uiState.archiveMaxEntryMb} MB",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                CompactSegmentedRow(
                    items = listOf(256 to "256M", 512 to "512M", 1024 to "1G", 2048 to "2G"),
                    selectedValue = uiState.archiveMaxEntryMb,
                    onSelected = { viewModel.updateArchiveMaxEntryMb(it) }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.welcome_custom_bomb_total),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${uiState.archiveMaxTotalMb} MB",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                CompactSegmentedRow(
                    items = listOf(1024 to "1G", 2048 to "2G", 4096 to "4G", 8192 to "8G"),
                    selectedValue = uiState.archiveMaxTotalMb,
                    onSelected = { viewModel.updateArchiveMaxTotalMb(it) }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.welcome_custom_nbt_page),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${uiState.nbtArrayPageSize} / page",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                CompactSegmentedRow(
                    items = listOf(50 to "50", 100 to "100", 200 to "200", 500 to "500"),
                    selectedValue = uiState.nbtArrayPageSize,
                    onSelected = { viewModel.updateNbtArrayPageSize(it) }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.welcome_custom_pack_frames),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${uiState.packMaxFrames} Frames",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                CompactSegmentedRow(
                    items = listOf(30_000 to "30k", 60_000 to "60k", 100_000 to "100k", 200_000 to "200k"),
                    selectedValue = uiState.packMaxFrames,
                    onSelected = { viewModel.updatePackMaxFrames(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun <T> CompactSegmentedRow(
    items: List<Pair<T, String>>,
    selectedValue: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { (value, label) ->
            val isSelected = (value == selectedValue)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelected(value) }
                    .then(
                        if (isSelected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                        else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun BenchmarkDetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileModeSelectableCard(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    metrics: List<String> = emptyList(),
    trailingContent: (@Composable () -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp))
                else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (trailingContent != null) {
                    trailingContent()
                } else {
                    RadioButton(
                        selected = isSelected,
                        onClick = onClick
                    )
                }
            }

            if (metrics.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    metrics.forEach { metric ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        ) {
                            Text(
                                text = metric,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
