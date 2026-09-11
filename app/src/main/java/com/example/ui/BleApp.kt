package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ble.BleConnectionState
import com.example.ble.BleCurrentScreen
import com.example.ble.BleDeviceItem
import com.example.ble.BleLogEntry
import com.example.ble.BleLogType
import com.example.ble.BleProtocol
import com.example.ble.BleViewModel
import com.example.ui.theme.BleAmber
import com.example.ui.theme.BleCyan
import com.example.ui.theme.BleDarkBg
import com.example.ui.theme.BleMint
import com.example.ui.theme.BlePrimary
import com.example.ui.theme.BlePrimaryContainer
import com.example.ui.theme.BlePrimaryContainerActive
import com.example.ui.theme.BlePrimaryLight
import com.example.ui.theme.BleRed
import com.example.ui.theme.BleRxLight
import com.example.ui.theme.BleSurface
import com.example.ui.theme.BleSurfaceVariant
import com.example.ui.theme.BleTerminalBg
import com.example.ui.theme.BleTextDim
import com.example.ui.theme.BleTextMain
import com.example.ui.theme.BleTextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleMainApp(
    viewModel: BleViewModel,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val statusType by viewModel.statusType.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BleDarkBg,
        bottomBar = {
            BleBottomStatusBar(
                message = statusMessage,
                type = statusType
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            if (currentScreen == BleCurrentScreen.SMART_RELAY_CONTROL) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 520.dp)
                ) {
                    BleSmartRelayControlScreen(viewModel = viewModel)
                }
            } else {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 520.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Screen Switcher
                    when (currentScreen) {
                        BleCurrentScreen.SCAN -> {
                            BleScanScreen(viewModel = viewModel)
                        }
                        BleCurrentScreen.DASHBOARD -> {
                            BleDashboardScreen(viewModel = viewModel)
                        }
                        BleCurrentScreen.SMCU_SETTING -> {
                            BleSmcuScreen(viewModel = viewModel)
                        }
                        BleCurrentScreen.TCP_IP_SETTING -> {
                            BleTcpIpScreen(viewModel = viewModel)
                        }
                        BleCurrentScreen.SMART_RELAY_SETTING -> {
                            BleSmartRelayScreen(viewModel = viewModel)
                        }
                        BleCurrentScreen.SMART_RELAY_CONTROL -> {
                            // Rendered outside verticalScroll
                        }
                        BleCurrentScreen.WEB_BLUETOOTH_VIEW -> {
                            BleDashboardScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Header Bar
// -----------------------------------------------------------------------------
@Composable
fun BleHeaderBar() {
    Surface(
        color = BleSurface,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BleSurfaceVariant),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(BlePrimaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = "BLE Icon",
                        tint = BlePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "IoT SMCU Controller",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = BlePrimary,
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        text = "HEX PROTOCOL ENGINE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.5.sp,
                        color = BleTextMuted
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Connected Device Banner
// -----------------------------------------------------------------------------
@Composable
fun BleConnectedBanner(
    device: BleDeviceItem,
    onDisconnect: () -> Unit
) {
    Surface(
        color = BleSurface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BleSurfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(BleMint)
                )
                Column {
                    Text(
                        text = device.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = BleTextMain
                    )
                    Text(
                        text = "UUID: ${device.address}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = BleTextMuted
                    )
                }
            }

            Button(
                onClick = onDisconnect,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BleRed.copy(alpha = 0.15f),
                    contentColor = BleRed
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BleRed.copy(alpha = 0.35f)),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("banner_disconnect_button")
            ) {
                Text("연결 해제", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -----------------------------------------------------------------------------
// SCREEN 1: Bluetooth Scan Screen
// -----------------------------------------------------------------------------
@Composable
fun BleScanScreen(viewModel: BleViewModel) {
    val connectionState by viewModel.connectionState.collectAsState()
    val scannedDevices by viewModel.scannedDevices.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isScanning = connectionState is BleConnectionState.Scanning

    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = BleSurface),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BleSurfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Radar Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(BlePrimaryContainer)
                    .border(
                        2.dp,
                        if (isScanning) BlePrimary else BleSurfaceVariant,
                        CircleShape
                    )
                    .then(if (isScanning) Modifier.scale(pulseScale) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BluetoothSearching,
                    contentDescription = "Search Radar",
                    tint = BlePrimary,
                    modifier = Modifier.size(38.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "기기 검색 및 연결",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BleTextMain
                )
                Text(
                    text = "주변의 SMCU 및 SmartRelay 블루투스 기기를 스캔합니다.",
                    fontSize = 13.sp,
                    color = BleTextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Scan Button
            Button(
                onClick = { viewModel.startScan() },
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BlePrimary,
                    contentColor = BlePrimaryContainer
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("scan_button")
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        color = BlePrimaryContainer,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("검색 중...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                } else {
                    Icon(
                        imageVector = Icons.Default.BluetoothSearching,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("블루투스 기기 검색", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            // Connection & Verification Progress Card
            if (connectionState is BleConnectionState.Connecting) {
                Surface(
                    color = BlePrimaryContainer.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BlePrimary.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = BlePrimary,
                            strokeWidth = 2.5.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "기기 연결 및 통신 상태 검증 중...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BleTextMain
                            )
                            Text(
                                text = if (statusMessage.isNotBlank()) statusMessage else "릴레이 상태 패킷(TX: 02 63 FB 21 00 03 80...)으로 응답을 확인하고 있습니다.",
                                fontSize = 11.sp,
                                color = BleTextMuted
                            )
                        }
                        TextButton(
                            onClick = { viewModel.disconnect() }
                        ) {
                            Text("취소", color = BleRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Devices List Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AVAILABLE DEVICES",
                        fontSize = 11.sp,
                        color = BleTextMuted,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${scannedDevices.size} DEPLOYED",
                        fontSize = 10.sp,
                        color = BlePrimary,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                if (scannedDevices.isEmpty()) {
                    Surface(
                        color = BleTerminalBg,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BleSurfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isScanning) "기기를 탐색하는 중입니다..." else "[블루투스 기기 검색]을 눌러 스캔을 시작하세요.",
                            color = BleTextDim,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        scannedDevices.forEach { dev ->
                            BleDeviceListItem(
                                device = dev,
                                onConnect = { viewModel.connectToDevice(dev) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BleDeviceListItem(
    device: BleDeviceItem,
    onConnect: () -> Unit
) {
    Surface(
        color = BleSurfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BleSurfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onConnect() }
            .testTag("device_item_${device.address}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(BlePrimary)
                )
                Column {
                    Text(
                        text = device.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = BleTextMain
                    )
                    Text(
                        text = "UUID: ${device.address}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = BleTextMuted
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${device.rssi} dBm",
                    color = BlePrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "CONNECT →",
                    color = BleTextMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// SCREEN 2: Dashboard (3 Main Buttons)
// -----------------------------------------------------------------------------
@Composable
fun BleDashboardScreen(viewModel: BleViewModel) {
    val logs by viewModel.logs.collectAsState()

    Card(
        colors = CardDefaults.cardColors(containerColor = BleSurface),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BleSurfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header: IoT SMCU Controller (moved from HeaderBar)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BlePrimaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = BlePrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column {
                    Text(
                        text = "IoT SMCU Controller",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = BleTextMain
                    )
                    Text(
                        text = "READY TO CONFIGURE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlePrimary,
                        letterSpacing = 1.2.sp
                    )
                }
            }
            
            // Dashboard Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "기능 설정 대시보드",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BleTextMuted
                )
                Surface(
                    color = BleMint.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "● CONNECTED",
                        color = BleMint,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // 3 Main Menu Buttons
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // ... (existing menu cards) ...
                // Button 1: SMCU ID 설정
                DashboardMenuCard(
                    title = "SMCU ID 설정",
                    subtitle = "0~255 ID 설정, 체크섬 다운로드 및 업로드",
                    icon = Icons.Default.Tune,
                    iconBg = BlePrimaryContainer,
                    iconTint = BlePrimary,
                    testTag = "menu_smcu_button",
                    onClick = { viewModel.setScreen(BleCurrentScreen.SMCU_SETTING) }
                )

                // Button 1-1: TCP/IP 설정
                DashboardMenuCard(
                    title = "TCP/IP 설정",
                    subtitle = "IP, GW, SUB, MAC 설정, 다운로드 및 업로드",
                    icon = Icons.Default.Settings,
                    iconBg = BlePrimaryContainer,
                    iconTint = BlePrimary,
                    testTag = "menu_tcp_ip_button",
                    onClick = { viewModel.setScreen(BleCurrentScreen.TCP_IP_SETTING) }
                )

                // Button 2: SmartRelay ID 설정
                DashboardMenuCard(
                    title = "SmartRelay ID 설정",
                    subtitle = "실시간 모니터링 및 스트림 제어",
                    icon = Icons.Default.Memory,
                    iconBg = BlePrimaryContainer,
                    iconTint = BlePrimary,
                    testTag = "menu_smart_relay_button",
                    onClick = { viewModel.setScreen(BleCurrentScreen.SMART_RELAY_SETTING) }
                )

                // Button 2-1: SmartRelay 제어
                DashboardMenuCard(
                    title = "SmartRelay 제어",
                    subtitle = "128개 릴레이 상태 제어 및 모니터링",
                    icon = Icons.Default.Lightbulb,
                    iconBg = BlePrimaryContainer,
                    iconTint = BlePrimary,
                    testTag = "menu_smart_relay_control_button",
                    onClick = {
                        viewModel.setScreen(BleCurrentScreen.SMART_RELAY_CONTROL)
                        viewModel.requestRelayStatus()
                    }
                )

                // Button 3: 종료 (연결 해제)
                DashboardMenuCard(
                    title = "종료 (연결 해제)",
                    subtitle = "블루투스 연결을 완전히 해제하고 초기 검색 화면으로 복귀",
                    icon = Icons.Default.PowerSettingsNew,
                    iconBg = BleSurfaceVariant.copy(alpha = 0.6f),
                    iconTint = BleTextMuted,
                    borderColor = BleSurfaceVariant,
                    titleColor = BleTextMain,
                    testTag = "menu_exit_button",
                    onClick = { viewModel.disconnect() }
                )
            }
        }
    }
}

@Composable
fun DashboardMenuCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconTint: Color,
    borderColor: Color = BleSurfaceVariant,
    titleColor: Color = BleTextMain,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        color = BleSurfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = titleColor
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = BleTextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = "→",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = BlePrimary
            )
        }
    }
}

// -----------------------------------------------------------------------------
// SCREEN 3: SMCU ID Setting Screen
// -----------------------------------------------------------------------------
@Composable
fun BleSmcuScreen(viewModel: BleViewModel) {
    val smcuId by viewModel.smcuId.collectAsState()
    val uploadedId by viewModel.smcuUploadedId.collectAsState()
    val logs by viewModel.logs.collectAsState()

    var inputIdStr by remember { mutableStateOf(smcuId.toString()) }

    LaunchedEffect(smcuId) {
        inputIdStr = smcuId.toString()
    }

    val currentIntId = inputIdStr.toIntOrNull()?.coerceIn(0, 255) ?: 0
    val hexValueStr = "0x" + "%02X".format(currentIntId)

    Card(
        colors = CardDefaults.cardColors(containerColor = BleSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BleSurfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(BlePrimaryLight)
                    )
                    Text(
                        text = "SMCU ID 설정",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BleTextMain
                    )
                }

                Button(
                    onClick = { viewModel.setScreen(BleCurrentScreen.DASHBOARD) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFF4A4458),
                        contentColor = androidx.compose.ui.graphics.Color(0xFFF5EEFA)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF7D7588)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("smcu_back_button")
                ) {
                    Text("← 대시보드", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Input Field with Up / Down Stepper Buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "설정할 SMCU ID (0 ~ 255)",
                        fontSize = 13.sp,
                        color = BleTextMuted
                    )
                    Text(
                        text = "HEX: $hexValueStr",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = BleCyan,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Stepper Row (- / Input / +)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ID 다운 버튼 (-)
                    Button(
                        onClick = { viewModel.decrementSmcuId() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BleSurfaceVariant,
                            contentColor = BleTextMain
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("smcu_id_down_button")
                    ) {
                        Text("-", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = inputIdStr,
                        onValueChange = { str ->
                            if (str.length <= 3) {
                                inputIdStr = str
                                str.toIntOrNull()?.let { num ->
                                    if (num in 0..255) viewModel.setSmcuId(num)
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            color = BleTextMain
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = BleDarkBg,
                            unfocusedContainerColor = BleDarkBg,
                            focusedBorderColor = BleCyan,
                            unfocusedBorderColor = BleSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("smcu_id_input")
                    )

                    // ID 업 버튼 (+)
                    Button(
                        onClick = { viewModel.incrementSmcuId() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BleSurfaceVariant,
                            contentColor = BleTextMain
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("smcu_id_up_button")
                    ) {
                        Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Quick presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0, 64, 128, 192, 255).forEach { preset ->
                        Surface(
                            color = BleSurfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    inputIdStr = preset.toString()
                                    viewModel.setSmcuId(preset)
                                }
                        ) {
                            Text(
                                text = "ID $preset",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = BleTextMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Action Buttons (다운로드 & 업로드)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 다운로드 (전송)
                Button(
                    onClick = {
                        viewModel.downloadSmcuId(currentIntId)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BlePrimaryContainer,
                        contentColor = BlePrimary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("smcu_download_button")
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = BlePrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("다운로드", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                // 업로드 (조회)
                Button(
                    onClick = {
                        viewModel.uploadSmcuId()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BleSurfaceVariant.copy(alpha = 0.5f),
                        contentColor = BleTextMain
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BleSurfaceVariant),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("smcu_upload_button")
                ) {
                    Icon(imageVector = Icons.Default.Upload, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("업로드", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            // Uploaded ID Display
            Surface(
                color = BleTerminalBg,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BleSurfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "기기 수신 SMCU ID (13번째 바이트 / Index 12)",
                        fontSize = 12.sp,
                        color = BleTextMuted
                    )
                    Text(
                        text = if (uploadedId != null) "0x${"%02X".format(uploadedId)} (십진수: $uploadedId)" else "--",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = BlePrimary,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .testTag("smcu_uploaded_text")
                    )
                }
            }

            // Hex Logs (송·수신 패킷 화면 크기 최적화)
            HexLogBox(logs = logs, height = 230.dp)
        }
    }
}

// -----------------------------------------------------------------------------
// SCREEN 3-1: TCP/IP ID Setting Screen
// -----------------------------------------------------------------------------
@Composable
fun BleTcpIpScreen(viewModel: BleViewModel) {
    val currentIp by viewModel.tcpIp.collectAsState()
    val currentGw by viewModel.tcpGw.collectAsState()
    val currentSub by viewModel.tcpSub.collectAsState()
    val currentMac by viewModel.tcpMac.collectAsState()
    val logs by viewModel.logs.collectAsState()

    var ipText by remember { mutableStateOf(currentIp) }
    var gwText by remember { mutableStateOf(currentGw) }
    var subText by remember { mutableStateOf(currentSub) }
    var macText by remember { mutableStateOf(currentMac) }

    LaunchedEffect(currentIp, currentGw, currentSub, currentMac) {
        ipText = currentIp
        gwText = currentGw
        subText = currentSub
        macText = currentMac
    }

    // 업로드된 데이터가 변경되면 자동으로 입력창에 반영
    LaunchedEffect(currentIp, currentGw, currentSub, currentMac) {
        viewModel.setTcpIp(currentIp)
        viewModel.setTcpGw(currentGw)
        viewModel.setTcpSub(currentSub)
        viewModel.setTcpMac(currentMac)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = BleSurface),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BleSurfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(BlePrimaryLight)
                    )
                    Text(
                        text = "TCP/IP 설정",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BleTextMain
                    )
                }

                Button(
                    onClick = { viewModel.setScreen(BleCurrentScreen.DASHBOARD) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFF4A4458),
                        contentColor = androidx.compose.ui.graphics.Color(0xFFF5EEFA)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF7D7588)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("tcp_ip_back_button")
                ) {
                    Text("← 대시보드", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Input 1: IP 주소
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "1. IP 주소",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BleTextMuted
                )
                OutlinedTextField(
                    value = ipText,
                    onValueChange = { 
                        ipText = it
                        viewModel.setTcpIp(it)
                    },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BleTextMain
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = BleTerminalBg,
                        unfocusedContainerColor = BleTerminalBg,
                        focusedBorderColor = BlePrimary,
                        unfocusedBorderColor = BleSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tcp_ip_input_ip")
                )
            }

            // Input 2: 게이트웨이 (GW)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "2. GW (게이트웨이)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BleTextMuted
                )
                OutlinedTextField(
                    value = gwText,
                    onValueChange = { 
                        gwText = it
                        viewModel.setTcpGw(it)
                    },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BleTextMain
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = BleTerminalBg,
                        unfocusedContainerColor = BleTerminalBg,
                        focusedBorderColor = BlePrimary,
                        unfocusedBorderColor = BleSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tcp_ip_input_gw")
                )
            }

            // Input 3: 서브넷 마스크 (SUB)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "3. SUB (서브넷 마스크)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BleTextMuted
                )
                OutlinedTextField(
                    value = subText,
                    onValueChange = { 
                        subText = it
                        viewModel.setTcpSub(it)
                    },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BleTextMain
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = BleTerminalBg,
                        unfocusedContainerColor = BleTerminalBg,
                        focusedBorderColor = BlePrimary,
                        unfocusedBorderColor = BleSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tcp_ip_input_sub")
                )
            }

            // Input 4: MAC 주소
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "4. MAC 주소",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BleTextMuted
                )
                OutlinedTextField(
                    value = macText,
                    onValueChange = { 
                        macText = it
                        viewModel.setTcpMac(it)
                    },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BleTextMain
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = BleTerminalBg,
                        unfocusedContainerColor = BleTerminalBg,
                        focusedBorderColor = BlePrimary,
                        unfocusedBorderColor = BleSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tcp_ip_input_mac")
                )
            }

            // Action Buttons: 다운로드 & 업로드 (SMCU ID 설정과 동일한 레이아웃/스타일)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 다운로드 (SMCU ID 설정 다운로드 색상과 동일하게 통일)
                Button(
                    onClick = {
                        viewModel.downloadTcpIpSettings(ipText, gwText, subText, macText)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BlePrimaryContainer,
                        contentColor = BlePrimary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("tcp_ip_download_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "다운로드",
                        tint = BlePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "다운로드",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // 업로드 (서피스 버튼)
                Button(
                    onClick = {
                        viewModel.uploadTcpIpSettings()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BleSurfaceVariant,
                        contentColor = BleTextMain
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BleSurfaceVariant),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("tcp_ip_upload_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = "업로드",
                        tint = BleTextMain,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "업로드",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // 기본값 복원 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        ipText = BleProtocol.DEFAULT_IP
                        gwText = BleProtocol.DEFAULT_GW
                        subText = BleProtocol.DEFAULT_SUB
                        macText = BleProtocol.DEFAULT_MAC
                        viewModel.resetTcpIpDefaults()
                    },
                    modifier = Modifier.testTag("tcp_ip_reset_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "기본값 복원",
                        tint = BleTextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "기본값으로 복원",
                        fontSize = 12.sp,
                        color = BleTextMuted
                    )
                }
            }

            // Hex Logs (송·수신 패킷 화면 크기 최적화)
            HexLogBox(logs = logs, height = 230.dp)
        }
    }
}

// -----------------------------------------------------------------------------
// SCREEN 4: SmartRelay ID Setting Screen
// -----------------------------------------------------------------------------
@Composable
fun BleSmartRelayScreen(viewModel: BleViewModel) {
    val isStreaming by viewModel.isSmartRelayStreaming.collectAsState()
    val realtimeId by viewModel.smartRelayRealtimeId.collectAsState()
    val realtimeHex by viewModel.smartRelayRealtimeHex.collectAsState()
    val realtimeByte10 by viewModel.smartRelayRealtimeByte10.collectAsState()
    val realtimeByte11 by viewModel.smartRelayRealtimeByte11.collectAsState()
    val targetId by viewModel.smartRelayTargetId.collectAsState()
    val logs by viewModel.logs.collectAsState()

    var inputSrIdStr by remember { mutableStateOf(targetId.toString()) }

    LaunchedEffect(targetId) {
        inputSrIdStr = targetId.toString()
    }

    val currentSrIntId = inputSrIdStr.toIntOrNull()?.coerceIn(0, 65535) ?: 0
    val byte8 = (currentSrIntId shr 8) and 0xFF
    val byte9 = currentSrIntId and 0xFF
    // Start 체크섬: 11번째 데이터가 0xFF -> (0x40 xor byte8 xor byte9 xor 0xFF) = (0xBF xor byte8 xor byte9)
    val startChecksumVal = (0xBF xor byte8 xor byte9) and 0xFF
    val startChecksumStr = "0x" + "%02X".format(startChecksumVal)
    // Stop 체크섬: 11번째 데이터가 0x00 -> (0x40 xor byte8 xor byte9 xor 0x00) = (0x40 xor byte8 xor byte9)
    val stopChecksumVal = (0x40 xor byte8 xor byte9) and 0xFF
    val stopChecksumStr = "0x" + "%02X".format(stopChecksumVal)

    Card(
        colors = CardDefaults.cardColors(containerColor = BleSurface),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BleSurfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(BlePrimaryLight)
                    )
                    Text(
                        text = "SmartRelay ID 설정",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BleTextMain
                    )
                }

                Button(
                    onClick = {
                        if (isStreaming) {
                            viewModel.stopSmartRelayStream()
                        }
                        viewModel.setScreen(BleCurrentScreen.DASHBOARD)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFF4A4458),
                        contentColor = androidx.compose.ui.graphics.Color(0xFFF5EEFA)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF7D7588)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("sr_back_button")
                ) {
                    Text("← 대시보드", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // -------------------------------------------------------------
            // 실시간 수신 및 ID 변경 영역 (+, - 버튼 양 옆에 배치)
            // -------------------------------------------------------------
            Surface(
                color = BleTerminalBg,
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isStreaming) BleMint else BleSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "실시간 수신 & ID 변경",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BleTextMain
                        )
                        if (isStreaming) {
                            Surface(
                                color = BleMint.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BleMint.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "● LIVE STREAMING (1초 주기)",
                                    color = BleMint,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // 실시간 수신 데이터 (10, 11번째 바이트) 표시 카드
                    Surface(
                        color = if (realtimeId != null) BleMint.copy(alpha = 0.08f) else BleDarkBg,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (realtimeId != null) BleMint.copy(alpha = 0.35f) else BleSurfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "실시간 수신 데이터 (10, 11번째 바이트)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BleTextMuted
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (realtimeByte10 != null && realtimeByte11 != null) {
                                        "10번째: 0x%02X | 11번째: 0x%02X".format(realtimeByte10, realtimeByte11)
                                    } else {
                                        "10번째: -- | 11번째: --"
                                    },
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = BleTextMuted
                                )
                            }

                            Text(
                                text = if (realtimeId != null) "$realtimeId (0x$realtimeHex)" else "수신 대기중...",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (realtimeId != null) BleMint else BleTextMuted,
                                modifier = Modifier.testTag("sr_live_id_text")
                            )
                        }
                    }

                    // Stepper Row: [-] 버튼 + ID 입력/표시창 + [+] 버튼 (8, 9번째 바이트 송신)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // - 버튼 (좌측)
                        Button(
                            onClick = { viewModel.decrementSmartRelayId() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BleSurfaceVariant,
                                contentColor = BleTextMain
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .size(56.dp)
                                .testTag("sr_id_down_button")
                        ) {
                            Text("-", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // 중앙: ID 입력 및 대형 숫자 표시창
                        OutlinedTextField(
                            value = inputSrIdStr,
                            onValueChange = { str ->
                                if (str.length <= 5) {
                                    inputSrIdStr = str
                                    str.toIntOrNull()?.let { num ->
                                        if (num in 0..65535) viewModel.setSmartRelayTargetId(num)
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center,
                                color = BleCyan
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = BleDarkBg,
                                unfocusedContainerColor = BleDarkBg,
                                focusedBorderColor = BleCyan,
                                unfocusedBorderColor = BleSurfaceVariant
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .width(140.dp)
                                .testTag("sr_id_input")
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        // + 버튼 (우측)
                        Button(
                            onClick = { viewModel.incrementSmartRelayId() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BleSurfaceVariant,
                                contentColor = BleTextMain
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .size(56.dp)
                                .testTag("sr_id_up_button")
                        ) {
                            Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // ID 상태 표시 (8, 9번째 바이트 송신 정보 & Start/Stop 체크섬)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "송신 (8,9번째: 0x%02X %02X | Start 11번째: FF)".format(byte8, byte9),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = BleCyan,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Start 체크섬: $startChecksumStr",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = BleTextMuted,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Quick presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0, 1, 64, 128, 255).forEach { preset ->
                            Surface(
                                color = BleSurfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        inputSrIdStr = preset.toString()
                                        viewModel.setSmartRelayTargetId(preset)
                                    }
                            ) {
                                Text(
                                    text = "ID $preset",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = BleTextMuted,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                    }
                    // 송신 패킷 안내 (Start 11번째: FF / Stop 11번째: 00 / 1초 주기: DD)
                    Surface(
                        color = BleDarkBg,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BleSurfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Start (11번째: FF)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BleTextMuted
                                )
                                Text(
                                    text = "02 63 FB DC 00 05 01 %02X %02X 00 FF %02X 03 03 FC FC".format(byte8, byte9, startChecksumVal),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = BlePrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Stop (11번째: 00)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BleTextMuted
                                )
                                Text(
                                    text = "02 63 FB DC 00 05 01 %02X %02X 00 00 %02X 03 03 FC FC".format(byte8, byte9, stopChecksumVal),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = BleTextMuted,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "1초 주기 (DD)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BleTextMuted
                                )
                                Text(
                                    text = "02 63 FB DD 00 03 01 00 00 47 03 03 FC FC",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = BleMint,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Start & Stop 스트림 제어 버튼 (적색 제거, 일관된 스타일)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Start
                Button(
                    onClick = { viewModel.startSmartRelayStream() },
                    enabled = !isStreaming,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BlePrimaryContainer,
                        contentColor = BlePrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("sr_start_button")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "시작", tint = BlePrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start (스트림 시작)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                // Stop (적색 스타일 제거, 차분한 서피스 스타일 적용)
                Button(
                    onClick = { viewModel.stopSmartRelayStream() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BleSurfaceVariant,
                        contentColor = BleTextMain
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BleSurfaceVariant),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("sr_stop_button")
                ) {
                    Icon(imageVector = Icons.Default.Stop, contentDescription = "중지", tint = BleTextMain)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Stop (스트림 중지)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // Realtime packet logs
            HexLogBox(logs = logs)
        }
    }
}

// -----------------------------------------------------------------------------
// Reusable Hex Log Box (송·수신 패킷 화면)
// -----------------------------------------------------------------------------
@Composable
fun HexLogBox(
    logs: List<BleLogEntry>,
    modifier: Modifier = Modifier,
    height: Dp = 230.dp,
    title: String = "송·수신 패킷 (TX / RX)"
) {
    // logs 리스트는 최신 패킷이 index 0(맨 위)에 위치하며, 이전 패킷은 밑으로 배치됩니다.
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Surface(
        color = BleTerminalBg,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BleSurfaceVariant),
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BleSurface.copy(alpha = 0.7f))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(BlePrimary)
                    )
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = BleTextMain
                    )
                    Surface(
                        color = BlePrimaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "최신순",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlePrimary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = "${logs.size}건",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = BleTextDim
                )
            }

            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "송·수신된 패킷 로그가 없습니다.",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = BleTextDim
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(logs) { index, entry ->
                        val isLatest = (index == 0)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isLatest) Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(BlePrimary.copy(alpha = 0.08f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                    else Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                ),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "[${entry.timestamp}]",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = BleTextDim
                            )
                            val (tagText, tagColor, textColor) = when (entry.type) {
                                BleLogType.TX -> Triple("[TX]", BlePrimary, BlePrimary)
                                BleLogType.RX -> Triple("[RX]", BleRxLight, BleRxLight)
                                BleLogType.ERROR -> Triple("[ERR]", BleRed, BleRed)
                                BleLogType.INFO -> Triple("[INF]", BleTextMuted, BleTextMuted)
                            }
                            Text(
                                text = tagText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = tagColor
                            )
                            if (isLatest) {
                                Surface(
                                    color = BlePrimary,
                                    shape = RoundedCornerShape(3.dp)
                                ) {
                                    Text(
                                        text = "NEW",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = androidx.compose.ui.graphics.Color.Black,
                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = entry.text,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = textColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Monospace bottom status inside terminal
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            BleSurfaceVariant
                        )
                    )
                    .background(BleSurface.copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "MTU: 247 BYTES",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = BleTextMuted
                    )
                    Text(
                        text = "READY",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlePrimary
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Bottom Status Bar
// -----------------------------------------------------------------------------
@Composable
fun BleBottomStatusBar(message: String, type: String) {
    val dotColor = when (type) {
        "busy" -> BleAmber
        "success" -> BleMint
        "error" -> BleRed
        else -> BlePrimary
    }

    Surface(
        color = BleSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, BleSurfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = message,
                fontSize = 12.sp,
                color = BleTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Web Bluetooth Standalone HTML Viewer & Copy Modal
// -----------------------------------------------------------------------------
@Composable
fun WebBluetoothHtmlViewer(onClose: () -> Unit) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    val htmlContent = remember {
        try {
            context.assets.open("index.html").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "<!-- index.html load failed: ${e.message} -->"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Code, contentDescription = null, tint = BlePrimary)
                Text(
                    text = "Web Bluetooth 단일 HTML 파일",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BleTextMain
                )
            }

            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("index.html", htmlContent))
                    copied = true
                    Toast.makeText(context, "HTML 코드가 클립보드에 복사되었습니다!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BlePrimaryContainer,
                    contentColor = BlePrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (copied) "복사됨!" else "HTML 복사", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Text(
            text = "Web Bluetooth API를 사용한 단일 HTML(HTML+CSS+JS) 소스코드입니다. PC나 모바일 브라우저(Chrome/Edge)에서 즉시 실행 가능합니다.",
            fontSize = 12.sp,
            color = BleTextMuted
        )

        Surface(
            color = BleTerminalBg,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BleSurfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        ) {
            LazyColumn(modifier = Modifier.padding(12.dp)) {
                item {
                    Text(
                        text = htmlContent,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = BleRxLight
                    )
                }
            }
        }

        Button(
            onClick = onClose,
            colors = ButtonDefaults.buttonColors(containerColor = BleSurfaceVariant.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(46.dp)
        ) {
            Text("닫기", color = BleTextMain, fontWeight = FontWeight.SemiBold)
        }
    }
}
