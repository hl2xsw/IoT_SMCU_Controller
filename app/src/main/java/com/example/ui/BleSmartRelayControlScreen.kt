package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ble.BleConnectionState
import com.example.ble.BleCurrentScreen
import com.example.ble.BleViewModel

// Theme colors
private val RelayColorOn = Color(0xFFF59E0B)       // Bright Amber
private val RelayColorOnBg = Color(0x33F59E0B)
private val RelayColorOff = Color(0xFF64748B)      // Dim Slate Gray
private val RelayColorOffBg = Color(0x1A64748B)

private val CardDarkBg = Color(0xFF1E1E2E)
private val BorderDark = Color(0xFF313244)
private val TextMain = Color(0xFFF5F5F7)
private val TextMuted = Color(0xFFA6ADC8)
private val MintGreen = Color(0xFF10B981)

@Composable
fun BleSmartRelayControlScreen(viewModel: BleViewModel) {
    val relayStates by viewModel.relayStates.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val isConnected = connectionState is BleConnectionState.Connected

    // 128 Relays total
    val totalRelays = 128

    // Tab filter options: All, or 16-channel chunks (0-15, 16-31, ... 112-127)
    var selectedTab by remember { mutableStateOf(0) } // 0: All, 1: 0-15, 2: 16-31, ...
    val tabLabels = listOf(
        "전체 (128)",
        "CH 0-15",
        "CH 16-31",
        "CH 32-47",
        "CH 48-63",
        "CH 64-79",
        "CH 80-95",
        "CH 96-111",
        "CH 112-127"
    )

    // Request initial relay states upon screen entry
    LaunchedEffect(Unit) {
        viewModel.requestRelayStatus()
    }

    val onCount = remember(relayStates) {
        var count = 0
        for (i in 0 until minOf(totalRelays, relayStates.size)) {
            if (relayStates[i]) count++
        }
        count
    }
    val offCount = totalRelays - onCount

    val displayedIndices = remember(selectedTab) {
        if (selectedTab == 0) {
            (0 until totalRelays).toList()
        } else {
            val start = (selectedTab - 1) * 16
            val end = (start + 16).coerceAtMost(totalRelays)
            (start until end).toList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Top Header Bar
        Surface(
            color = CardDarkBg,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.setScreen(BleCurrentScreen.DASHBOARD) },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("relay_control_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "대시보드로 돌아가기",
                            tint = TextMain
                        )
                    }
                    Column {
                        Text(
                            text = "SmartRelay 제어",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMain
                        )
                        Text(
                            text = "128채널 릴레이 제어 및 실시간 모니터링",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                // Refresh Status Button
                IconButton(
                    onClick = { viewModel.requestRelayStatus() },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("relay_control_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "상태 조회 재요청",
                        tint = if (isConnected) MintGreen else TextMuted
                    )
                }
            }
        }

        // 2. Summary Status Banner Card
        Surface(
            color = CardDarkBg,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) MintGreen else RelayColorOff)
                        )
                        Text(
                            text = if (isConnected) "BLE 연결됨 (128CH 제어)" else "BLE 연결 안 됨",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isConnected) MintGreen else TextMuted
                        )
                    }

                    // Packet Protocol Info Badge
                    Surface(
                        color = Color(0x2210B981),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "02 63 FB 21 00 03 80...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MintGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Packet Protocol Detailed Banner
                Surface(
                    color = Color(0x15FFFFFF),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "TX: 02 63 FB 21 00 03 80 00 00 3A 03 03 FC FC",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = MintGreen
                        )
                        Text(
                            text = "10번째 데이터 ➔ Relay 0부터 128개 상태 수신 통합 (FF: ON, 00: OFF)",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }

                // Stats Counters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ON Counter
                    Surface(
                        color = RelayColorOnBg,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, RelayColorOn.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = RelayColorOn,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("ON", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RelayColorOn)
                            }
                            Text(
                                text = "$onCount / $totalRelays",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = RelayColorOn
                            )
                        }
                    }

                    // OFF Counter
                    Surface(
                        color = RelayColorOffBg,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, RelayColorOff.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("OFF", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            }
                            Text(
                                text = "$offCount / $totalRelays",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }

        // 3. Fast Channel Filter Bar (Horizontal Scrollable Chips)
        val tabScrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(tabScrollState),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tabLabels.forEachIndexed { idx, label ->
                val isSelected = selectedTab == idx
                Surface(
                    color = if (isSelected) RelayColorOn else CardDarkBg,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isSelected) RelayColorOn else BorderDark),
                    modifier = Modifier
                        .clickable { selectedTab = idx }
                        .testTag("relay_filter_tab_$idx")
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color(0xFF1E1E2E) else TextMuted,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // 4. 128 Relay Control Items (Scrollable List with fast recycling)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("relay_items_lazy_column"),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = displayedIndices, key = { it }) { index ->
                val isOn = if (index < relayStates.size) relayStates[index] else false
                RelayItemCard(
                    index = index,
                    isOn = isOn,
                    onToggle = {
                        viewModel.toggleRelay(index, isOn)
                    }
                )
            }
        }
    }
}

@Composable
fun RelayItemCard(
    index: Int,
    isOn: Boolean,
    onToggle: () -> Unit
) {
    val borderColor = if (isOn) RelayColorOn.copy(alpha = 0.6f) else BorderDark
    val containerBg = if (isOn) Color(0xFF262335) else CardDarkBg

    Surface(
        color = containerBg,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .testTag("relay_item_$index")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Icon + Relay Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Lightbulb Icon Container
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isOn) RelayColorOnBg else RelayColorOffBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "Relay $index ${if (isOn) "ON" else "OFF"}",
                        tint = if (isOn) RelayColorOn else RelayColorOff,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Relay $index",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMain
                        )
                        Surface(
                            color = if (isOn) RelayColorOnBg else RelayColorOffBg,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "CH %02d".format(index),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOn) RelayColorOn else TextMuted,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Packet command indicator
                    Text(
                        text = if (isOn) "02 63 FB 41 ... 00 (클릭 시 OFF)" else "02 63 FB 41 ... FF (클릭 시 ON)",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isOn) RelayColorOn.copy(alpha = 0.8f) else TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right: Status Chip / Toggle Switch
            Surface(
                color = if (isOn) RelayColorOn else RelayColorOffBg,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, if (isOn) RelayColorOn else BorderDark),
                modifier = Modifier
                    .height(34.dp)
                    .widthIn(min = 64.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isOn) "FF : ON" else "00 : OFF",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isOn) Color(0xFF1E1E2E) else TextMuted
                    )
                }
            }
        }
    }
}
