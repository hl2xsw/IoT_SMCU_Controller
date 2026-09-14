package com.example.ble

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class BleDeviceItem(
    val name: String,
    val address: String,
    val rssi: Int = -60,
    val realDevice: BluetoothDevice? = null
)

enum class BleLogType { TX, RX, INFO, ERROR }

data class BleLogEntry(
    val timestamp: String,
    val type: BleLogType,
    val text: String
)

sealed interface BleConnectionState {
    object Disconnected : BleConnectionState
    object Scanning : BleConnectionState
    data class Connecting(val deviceName: String) : BleConnectionState
    data class Connected(val device: BleDeviceItem) : BleConnectionState
}

enum class BleCurrentScreen {
    SCAN,
    DASHBOARD,
    SMCU_SETTING,
    TCP_IP_SETTING,
    SMART_RELAY_SETTING,
    SMART_RELAY_CONTROL,
    WEB_BLUETOOTH_VIEW
}

class BleViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothManager =
        application.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BleDeviceItem>>(emptyList())
    val scannedDevices: StateFlow<List<BleDeviceItem>> = _scannedDevices.asStateFlow()

    private val _currentScreen = MutableStateFlow(BleCurrentScreen.SCAN)
    val currentScreen: StateFlow<BleCurrentScreen> = _currentScreen.asStateFlow()

    private val _statusMessage = MutableStateFlow("대기 중: 기기 검색을 시작해 주세요.")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _statusType = MutableStateFlow("ready")
    val statusType: StateFlow<String> = _statusType.asStateFlow()

    private val _logs = MutableStateFlow<List<BleLogEntry>>(emptyList())
    val logs: StateFlow<List<BleLogEntry>> = _logs.asStateFlow()

    // Connection Verification State
    private var verificationJob: Job? = null
    private var isVerifyingConnection = false
    private var pendingConnectedDevice: BleDeviceItem? = null

    // SMCU State
    private val _smcuId = MutableStateFlow(1)
    val smcuId: StateFlow<Int> = _smcuId.asStateFlow()

    private val _smcuUploadedId = MutableStateFlow<Int?>(null)
    val smcuUploadedId: StateFlow<Int?> = _smcuUploadedId.asStateFlow()
    private var isWaitingForSmcuUpload = false

    // TCP/IP State
    private val _tcpIp = MutableStateFlow(BleProtocol.DEFAULT_IP)
    val tcpIp: StateFlow<String> = _tcpIp.asStateFlow()

    private val _tcpGw = MutableStateFlow(BleProtocol.DEFAULT_GW)
    val tcpGw: StateFlow<String> = _tcpGw.asStateFlow()

    private val _tcpSub = MutableStateFlow(BleProtocol.DEFAULT_SUB)
    val tcpSub: StateFlow<String> = _tcpSub.asStateFlow()

    private val _tcpMac = MutableStateFlow(BleProtocol.DEFAULT_MAC)
    val tcpMac: StateFlow<String> = _tcpMac.asStateFlow()

    // SmartRelay State
    private val _isSmartRelayStreaming = MutableStateFlow(false)
    val isSmartRelayStreaming: StateFlow<Boolean> = _isSmartRelayStreaming.asStateFlow()

    private val _smartRelayRealtimeId = MutableStateFlow<Int?>(null)
    val smartRelayRealtimeId: StateFlow<Int?> = _smartRelayRealtimeId.asStateFlow()

    private val _smartRelayRealtimeHex = MutableStateFlow<String>("-- --")
    val smartRelayRealtimeHex: StateFlow<String> = _smartRelayRealtimeHex.asStateFlow()

    private val _smartRelayRealtimeByte10 = MutableStateFlow<Int?>(null)
    val smartRelayRealtimeByte10: StateFlow<Int?> = _smartRelayRealtimeByte10.asStateFlow()

    private val _smartRelayRealtimeByte11 = MutableStateFlow<Int?>(null)
    val smartRelayRealtimeByte11: StateFlow<Int?> = _smartRelayRealtimeByte11.asStateFlow()

    private val _smartRelayTargetId = MutableStateFlow(1)
    val smartRelayTargetId: StateFlow<Int> = _smartRelayTargetId.asStateFlow()

    private var smartRelayStreamJob: Job? = null
    private var activeScanCallback: ScanCallback? = null
    private var scanJob: Job? = null

    // Internal BLE handles
    private var bluetoothGatt: BluetoothGatt? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null
    private var lastReceivedPacket: ByteArray? = null
    private var packetBuffer = mutableListOf<Byte>()
    private var maxPayloadSize: Int = 20

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    // Standard client characteristic config UUID for notifications
    private val CLIENT_CHARACTERISTIC_CONFIG =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    init {
        addLog(BleLogType.INFO, "BLE Controller 엔진 초기화됨.")
    }

    fun setScreen(screen: BleCurrentScreen) {
        if (screen == BleCurrentScreen.SMCU_SETTING) {
            isWaitingForSmcuUpload = false
        }
        _currentScreen.value = screen
    }

    fun setSmcuId(id: Int) {
        _smcuId.value = id.coerceIn(0, 255)
    }

    fun incrementSmcuId() {
        setSmcuId(_smcuId.value + 1)
    }

    fun decrementSmcuId() {
        setSmcuId(_smcuId.value - 1)
    }

    fun setSmartRelayTargetId(id: Int) {
        val newId = id.coerceIn(0, 65535)
        _smartRelayTargetId.value = newId
        if (_isSmartRelayStreaming.value) {
            val packet = BleProtocol.buildSmartRelaySetIdPacket(newId)
            sendPacket(packet, "SmartRelay 변경 ID 설정 송신 (ID: $newId, 8,9번째 바이트)")
        }
    }

    fun incrementSmartRelayId() {
        setSmartRelayTargetId(_smartRelayTargetId.value + 1)
    }

    fun decrementSmartRelayId() {
        setSmartRelayTargetId(_smartRelayTargetId.value - 1)
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun addLog(type: BleLogType, message: String) {
        val entry = BleLogEntry(
            timestamp = timeFormat.format(Date()),
            type = type,
            text = message
        )
        _logs.value = listOf(entry) + _logs.value.take(49)
    }

    private fun setStatus(msg: String, type: String = "ready") {
        _statusMessage.value = msg
        _statusType.value = type
    }

    // -------------------------------------------------------------
    // Scanning Flow
    // -------------------------------------------------------------
    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        activeScanCallback?.let { cb ->
            try {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(cb)
            } catch (_: Exception) {}
        }
        activeScanCallback = null
        if (_connectionState.value is BleConnectionState.Scanning) {
            _connectionState.value = BleConnectionState.Disconnected
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        // Always cleanly stop any prior scan before starting a new one
        stopScan()

        _connectionState.value = BleConnectionState.Scanning
        setStatus("주변 BLE 기기를 검색하는 중입니다...", "busy")
        _scannedDevices.value = emptyList()

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            setStatus("블루투스가 꺼져 있거나 지원되지 않습니다.", "error")
            addLog(BleLogType.ERROR, "블루투스 비활성화 상태")
            _connectionState.value = BleConnectionState.Disconnected
            return
        }

        // Real BLE Scan
        try {
            val scanner = bluetoothAdapter.bluetoothLeScanner
            if (scanner == null) {
                setStatus("BLE 스캐너를 가져올 수 없습니다.", "error")
                addLog(BleLogType.ERROR, "블루투스 LE 스캐너 초기화 실패")
                _connectionState.value = BleConnectionState.Disconnected
                return
            }

            val foundList = mutableListOf<BleDeviceItem>()

            fun updateSortedDevices() {
                val sorted = foundList.sortedWith(
                    compareBy<BleDeviceItem> { it.name.startsWith("알 수 없는 기기") }
                        .thenBy { it.name.lowercase(Locale.getDefault()) }
                )
                _scannedDevices.value = sorted
            }

            // Add already bonded (paired) devices so user can immediately connect if previously paired
            try {
                bluetoothAdapter.bondedDevices?.forEach { dev ->
                    val devName = dev.name ?: "페어링된 기기 (${dev.address.takeLast(5)})"
                    val item = BleDeviceItem(
                        name = devName,
                        address = dev.address,
                        rssi = -50,
                        realDevice = dev
                    )
                    if (foundList.none { it.address == dev.address }) {
                        foundList.add(item)
                    }
                }
                if (foundList.isNotEmpty()) {
                    updateSortedDevices()
                }
            } catch (_: Exception) {}

            val callback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult?) {
                    result?.device?.let { dev ->
                        val recordName = result.scanRecord?.deviceName
                        val resolvedName = dev.name ?: recordName ?: "알 수 없는 기기 (${dev.address.takeLast(5)})"
                        val existingIndex = foundList.indexOfFirst { it.address == dev.address }
                        if (existingIndex >= 0) {
                            // Update RSSI or name if discovered
                            val existing = foundList[existingIndex]
                            val isExistingUnknown = existing.name.startsWith("알 수 없는 기기")
                            val isNewKnown = !resolvedName.startsWith("알 수 없는 기기")
                            if (isExistingUnknown && isNewKnown) {
                                foundList[existingIndex] = existing.copy(name = resolvedName, rssi = result.rssi)
                                updateSortedDevices()
                            } else if (result.rssi != existing.rssi) {
                                foundList[existingIndex] = existing.copy(rssi = result.rssi)
                                updateSortedDevices()
                            }
                        } else {
                            val item = BleDeviceItem(
                                name = resolvedName,
                                address = dev.address,
                                rssi = result.rssi,
                                realDevice = dev
                            )
                            foundList.add(item)
                            updateSortedDevices()
                        }
                    }
                }

                override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                    results?.forEach { res ->
                        onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, res)
                    }
                }

                override fun onScanFailed(errorCode: Int) {
                    setStatus("스캔 실패: 코드 $errorCode", "error")
                    addLog(BleLogType.ERROR, "스캔 실패: $errorCode")
                    stopScan()
                }
            }

            activeScanCallback = callback

            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .build()

            scanner.startScan(null, scanSettings, callback)

            scanJob = viewModelScope.launch {
                delay(12000)
                stopScan()
                setStatus("검색 완료: ${foundList.size}개 기기 발견 (다시 검색 가능)", "success")
            }
        } catch (e: SecurityException) {
            stopScan()
            setStatus("블루투스 검색 권한이 필요합니다.", "error")
            addLog(BleLogType.ERROR, "블루투스 권한 오류: ${e.message}")
        } catch (e: Exception) {
            stopScan()
            setStatus("스캔 예외 발생: ${e.message}", "error")
            addLog(BleLogType.ERROR, "스캔 오류: ${e.message}")
        }
    }

    // -------------------------------------------------------------
    // Connection Flow
    // -------------------------------------------------------------
    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BleDeviceItem) {
        _connectionState.value = BleConnectionState.Connecting(device.name)
        setStatus("${device.name} 연결 시도 중...", "busy")

        if (device.realDevice == null) {
            setStatus("연결 실패: 실제 블루투스 장치 정보를 찾을 수 없습니다.", "error")
            _connectionState.value = BleConnectionState.Disconnected
            return
        }

        // Real GATT Connection
        try {
            bluetoothGatt?.close()
            bluetoothGatt = device.realDevice.connectGatt(
                getApplication(),
                false,
                gattCallback
            )
        } catch (e: SecurityException) {
            setStatus("연결 권한 오류: ${e.message}", "error")
            addLog(BleLogType.ERROR, "권한 누락으로 연결 실패")
            _connectionState.value = BleConnectionState.Disconnected
        } catch (e: Exception) {
            setStatus("연결 실패: ${e.message}", "error")
            _connectionState.value = BleConnectionState.Disconnected
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                val dev = gatt?.device
                val item = BleDeviceItem(
                    name = dev?.name ?: "Bluetooth Device",
                    address = dev?.address ?: "00:00:00:00:00:00",
                    realDevice = dev
                )
                pendingConnectedDevice = item
                isVerifyingConnection = true
                _connectionState.value = BleConnectionState.Connecting("${item.name} (통신 검증 중)")
                viewModelScope.launch {
                    setStatus("BLE 연결됨: ${item.name}. 서비스 탐색 진행 중...", "busy")
                    addLog(BleLogType.INFO, "GATT 연결 완료. MTU 협상 및 서비스 탐색 진행...")
                    try {
                        gatt?.requestMtu(512)
                    } catch (_: Exception) {}
                    // 300ms delay between MTU request and service discovery avoids Android GATT collisions
                    delay(300)
                    gatt?.discoverServices()
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                viewModelScope.launch {
                    disconnect()
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && mtu > 3) {
                maxPayloadSize = (mtu - 3).coerceAtLeast(20)
                viewModelScope.launch {
                    addLog(BleLogType.INFO, "MTU 협상 완료: $mtu bytes (최대 청크: $maxPayloadSize bytes)")
                }
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            viewModelScope.launch {
                addLog(BleLogType.INFO, "GATT 알림 설정 완료 (status: $status)")
                if (isVerifyingConnection && verificationJob == null) {
                    startConnectionVerification()
                }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                viewModelScope.launch {
                    addLog(BleLogType.ERROR, "GATT 쓰기 콜백 오류: status=$status")
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                val (tx, rx) = resolveUartCharacteristics(gatt)
                txCharacteristic = tx
                rxCharacteristic = rx

                if (rx != null) {
                    enableNotification(gatt, rx)
                }

                viewModelScope.launch {
                    val txStr = if (tx != null) {
                        val noResp = (tx.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
                        val wr = (tx.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0
                        "TX=${tx.uuid.toString().take(8)} (Write=$wr, NoResp=$noResp)"
                    } else "TX 미발견"
                    val rxStr = if (rx != null) "RX=${rx.uuid.toString().take(8)}" else "RX 미발견"
                    addLog(BleLogType.INFO, "BLE 채널 등록 완료: $txStr, $rxStr")

                    // If notification descriptor callback does not fire within 1000ms, fallback to trigger verification
                    delay(1000)
                    if (isVerifyingConnection && verificationJob == null) {
                        startConnectionVerification()
                    }
                }
            }
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            characteristic?.value?.let { bytes ->
                processIncomingBytes(bytes)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            processIncomingBytes(value)
        }
    }

    private fun startConnectionVerification() {
        verificationJob?.cancel()
        verificationJob = viewModelScope.launch {
            isVerifyingConnection = true

            // BLE 연결 직후 MCU UART 버퍼 및 무선 통신 안정화를 위한 충분한 딜레이 (1500ms)
            setStatus("BLE 연결 완료. 기기 통신 안정화 대기 중... (1.5초)", "busy")
            addLog(BleLogType.INFO, "기기 연결 및 채널 준비 완료. 통신 안정화 대기 (1500ms 지연) 후 상태 검증 패킷 송신 예정...")
            delay(1500)

            var attempts = 0
            val maxAttempts = 3
            while (isVerifyingConnection && attempts < maxAttempts) {
                attempts++
                addLog(BleLogType.INFO, "기기 통신 검증 ($attempts/$maxAttempts 차): 릴레이 상태 패킷 송신 (02 63 FB 21 00 03 80 00 00 3A...)")
                setStatus("기기 통신 상태 검증 중 ($attempts/$maxAttempts 차)...", "busy")

                // TX : 02 63 FB 21 00 03 80 00 00 3A 03 03 FC FC
                sendPacket(
                    BleProtocol.SMART_RELAY_STATUS_REQUEST_PACKET,
                    "기기 연결 검증 (상태 패킷: 80 00 00 3A)"
                )

                // Wait up to 2000ms for incoming RX response
                delay(2000)
            }

            if (isVerifyingConnection) {
                isVerifyingConnection = false
                setStatus("기기 통신 검증 실패: 릴레이 상태 응답 없음. 기기 전원 또는 BLE 상태를 확인하세요.", "error")
                addLog(BleLogType.ERROR, "기기 상태 응답 미수신으로 통신 검증 실패 -> 연결 해제")
                disconnect()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableNotification(gatt: BluetoothGatt, char: BluetoothGattCharacteristic) {
        try {
            gatt.setCharacteristicNotification(char, true)
            val descriptor = char.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
            if (descriptor != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                } else {
                    @Suppress("DEPRECATION")
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    @Suppress("DEPRECATION")
                    gatt.writeDescriptor(descriptor)
                }
            }
        } catch (_: Exception) {}
    }

    private fun resolveUartCharacteristics(gatt: BluetoothGatt): Pair<BluetoothGattCharacteristic?, BluetoothGattCharacteristic?> {
        var tx: BluetoothGattCharacteristic? = null
        var rx: BluetoothGattCharacteristic? = null

        // 1. Priority: HM-10 / CC2541 / MLT-BT05 style (FFE0 / FFE5)
        for (service in gatt.services) {
            val sUuid = service.uuid.toString().lowercase(Locale.getDefault())
            if (sUuid.contains("ffe0") || sUuid.contains("ffe5")) {
                for (char in service.characteristics) {
                    val cUuid = char.uuid.toString().lowercase(Locale.getDefault())
                    val props = char.properties
                    if (cUuid.contains("ffe1") || cUuid.contains("ffe9") || cUuid.contains("ffe4") ||
                        ((props and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0)
                    ) {
                        tx = char
                    }
                    if ((props and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE)) != 0) {
                        rx = char
                    }
                }
                if (tx != null) return Pair(tx, rx)
            }
        }

        // 2. Priority: Nordic UART Service (NUS 6e400001...)
        for (service in gatt.services) {
            val sUuid = service.uuid.toString().lowercase(Locale.getDefault())
            if (sUuid.contains("6e400001")) {
                for (char in service.characteristics) {
                    val cUuid = char.uuid.toString().lowercase(Locale.getDefault())
                    if (cUuid.contains("6e400002")) tx = char
                    if (cUuid.contains("6e400003")) rx = char
                }
                if (tx != null) return Pair(tx, rx)
            }
        }

        // 3. Custom Vendor Services (skip standard 000018xx Bluetooth SIG services)
        for (service in gatt.services) {
            val sUuid = service.uuid.toString().lowercase(Locale.getDefault())
            if (sUuid.startsWith("000018")) continue
            for (char in service.characteristics) {
                val props = char.properties
                if (tx == null && ((props and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0)) {
                    tx = char
                }
                if (rx == null && ((props and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE)) != 0)) {
                    rx = char
                }
            }
            if (tx != null && rx != null) return Pair(tx, rx)
        }

        // 4. Fallback: Any service
        for (service in gatt.services) {
            for (char in service.characteristics) {
                val props = char.properties
                if (tx == null && ((props and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0)) {
                    tx = char
                }
                if (rx == null && ((props and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE)) != 0)) {
                    rx = char
                }
            }
        }

        return Pair(tx, rx)
    }

    private fun processIncomingBytes(bytes: ByteArray) {
        // TCP/IP & SmartRelay handling: assemble if split (starts with 0x02, ends with 0xFC 0xFC)
        packetBuffer.addAll(bytes.toList())

        while (packetBuffer.size >= 4) {
            val startIdx = packetBuffer.indexOf(0x02.toByte())
            if (startIdx == -1) {
                packetBuffer.clear()
                break
            }
            if (startIdx > 0) {
                packetBuffer = packetBuffer.subList(startIdx, packetBuffer.size).toMutableList()
            }
            // Check for FC FC tail
            var endIdx = -1
            for (i in 3 until packetBuffer.size) {
                if (packetBuffer[i - 1] == 0xFC.toByte() && packetBuffer[i] == 0xFC.toByte()) {
                    endIdx = i
                    break
                }
            }

            // 128개 릴레이 상태 패킷 (142바이트 등) FC FC 지연 수신 대비 보완
            if (endIdx == -1 && packetBuffer.size >= 142 &&
                (packetBuffer.getOrNull(3) == 0x21.toByte() || packetBuffer.getOrNull(2) == 0x21.toByte() || packetBuffer.getOrNull(1) == 0x21.toByte())
            ) {
                endIdx = 141
            }
            if (endIdx != -1) {
                val fullPacket = packetBuffer.subList(0, endIdx + 1).toByteArray()
                packetBuffer = packetBuffer.subList(endIdx + 1, packetBuffer.size).toMutableList()

                lastReceivedPacket = fullPacket
                val hexStr = BleProtocol.toHexString(fullPacket)

                viewModelScope.launch {
                    addLog(BleLogType.RX, hexStr)

                    // 1. Connection Verification Handling (백그라운드 통신 상태 검증)
                    if (isVerifyingConnection) {
                        isVerifyingConnection = false
                        verificationJob?.cancel()
                        verificationJob = null

                        val verifiedDev = pendingConnectedDevice
                        if (verifiedDev != null) {
                            _connectionState.value = BleConnectionState.Connected(verifiedDev)
                        }
                        handleRelayStatusPacket(fullPacket)
                        _currentScreen.value = BleCurrentScreen.DASHBOARD
                        setStatus("기기 연결 및 통신 검증 완료!", "success")
                        addLog(BleLogType.INFO, "기기 응답 패킷 수신 성공! 정상 통신 확인되어 대시보드로 이동합니다.")
                    }

                    // Real-time SmartRelay stream parsing (10, 11번째 데이터)
                    if (_isSmartRelayStreaming.value) {
                        BleProtocol.parseSmartRelayStreamData(fullPacket)?.let { streamData ->
                            _smartRelayRealtimeId.value = streamData.value
                            _smartRelayRealtimeHex.value = streamData.hexString
                            _smartRelayRealtimeByte10.value = streamData.byte10
                            _smartRelayRealtimeByte11.value = streamData.byte11
                            setStatus("실시간 수신: 10번째=0x%02X, 11번째=0x%02X (값: %d)".format(streamData.byte10, streamData.byte11, streamData.value), "success")
                            addLog(BleLogType.INFO, "SmartRelay 실시간 수신 (10, 11번째): ${streamData.value} (HEX: ${streamData.hexString})")
                        }
                    }

                    // TCP/IP packet parsing (Index 9..26)
                    if (_currentScreen.value == BleCurrentScreen.TCP_IP_SETTING && fullPacket.size >= 27) {
                        BleProtocol.parseTcpIpPacket(fullPacket)?.let { parsed ->
                            _tcpIp.value = parsed.ip
                            _tcpGw.value = parsed.gw
                            _tcpSub.value = parsed.sub
                            _tcpMac.value = parsed.mac
                            setStatus("TCP/IP 데이터 수신 및 적용 완료: IP=${parsed.ip}", "success")
                            addLog(BleLogType.INFO, "TCP/IP 파싱 적용: IP=${parsed.ip}, GW=${parsed.gw}, SUB=${parsed.sub}, MAC=${parsed.mac}")
                        }
                    }

                    // SMCU ID packet parsing (13th byte, Index 12) - Only during upload
                    if (_currentScreen.value == BleCurrentScreen.SMCU_SETTING) {
                        if (isWaitingForSmcuUpload) {
                            handleSmcuUploadedPacket(fullPacket)
                            isWaitingForSmcuUpload = false
                        } else {
                            setStatus("SMCU 다운로드 응답 수신 완료", "success")
                        }
                    }

                    // SmartRelay Status packet parsing (0x21, 0x41 또는 SMART_RELAY_CONTROL 화면)
                    val isRelayPacket = (_currentScreen.value == BleCurrentScreen.SMART_RELAY_CONTROL) ||
                            (fullPacket.size >= 10 && (
                                    fullPacket.getOrNull(3) == 0x21.toByte() || fullPacket.getOrNull(3) == 0x41.toByte() ||
                                    fullPacket.getOrNull(2) == 0x21.toByte() || fullPacket.getOrNull(1) == 0x21.toByte()))
                    if (isRelayPacket) {
                        handleRelayStatusPacket(fullPacket)
                    }
                }
            } else {
                if (packetBuffer.size > 512) {
                    packetBuffer.clear()
                }
                break
            }
        }
    }

    // -------------------------------------------------------------
    // Generic Send Function (Reliable Transmission Engine)
    // -------------------------------------------------------------
    // Queue-based transmission with retry and chunk synchronization
    private val sendQueue = mutableListOf<() -> Unit>()
    private var isSending = false

    @SuppressLint("MissingPermission")
    private fun sendPacket(packet: ByteArray, description: String) {
        val hexStr = BleProtocol.toHexString(packet)
        addLog(BleLogType.TX, hexStr)

        val isConnectedOrVerifying = (_connectionState.value is BleConnectionState.Connected) ||
                (isVerifyingConnection && bluetoothGatt != null)
        if (!isConnectedOrVerifying) {
            setStatus("전송 실패: 기기가 연결되어 있지 않습니다.", "error")
            return
        }

        sendQueue.add {
            executeSendPacket(packet, description)
        }
        processQueue()
    }

    private fun processQueue() {
        if (isSending || sendQueue.isEmpty()) return
        isSending = true
        val task = sendQueue.removeAt(0)
        task()
    }

    @SuppressLint("MissingPermission")
    private fun executeSendPacket(packet: ByteArray, description: String) {
        viewModelScope.launch {
            try {
                val gatt = bluetoothGatt
                if (gatt == null) {
                    setStatus("전송 실패: 블루투스 연결(GATT) 객체가 없습니다.", "error")
                    isSending = false
                    processQueue()
                    return@launch
                }

                var tx = txCharacteristic
                if (tx == null) {
                    val (resolvedTx, resolvedRx) = resolveUartCharacteristics(gatt)
                    tx = resolvedTx
                    txCharacteristic = resolvedTx
                    if (rxCharacteristic == null && resolvedRx != null) {
                        rxCharacteristic = resolvedRx
                        enableNotification(gatt, resolvedRx)
                    }
                }

                if (tx == null) {
                    setStatus("전송 실패: 쓰기 Characteristic을 찾을 수 없습니다.", "error")
                    gatt.discoverServices()
                    isSending = false
                    processQueue()
                    return@launch
                }

                // Default standard BLE MTU packet chunking (safe 20 bytes for standard BLE MCUs)
                val chunkSize = 20
                val chunks = packet.toList().chunked(chunkSize).map { it.toByteArray() }

                var overallSuccess = true
                for ((index, chunk) in chunks.withIndex()) {
                    var chunkSuccess = false
                    // Retry up to 3 times per chunk with backoff
                    for (attempt in 1..3) {
                        chunkSuccess = writeRawChunk(gatt, tx, chunk)
                        if (chunkSuccess) break
                        delay(40L * attempt)
                    }

                    if (!chunkSuccess) {
                        overallSuccess = false
                        addLog(BleLogType.ERROR, "GATT 쓰기 실패 (청크 ${index + 1}/${chunks.size}, 3회 재시도 실패)")
                        break
                    }

                    // Inter-chunk delay to allow MCU BLE stack buffer clearance
                    if (index < chunks.size - 1) {
                        delay(35)
                    }
                }

                if (overallSuccess) {
                    setStatus("$description 전송 성공", "success")
                } else {
                    setStatus("$description 전송 실패 (재시도 초과)", "error")
                }
            } catch (e: Exception) {
                setStatus("전송 오류: ${e.message}", "error")
            } finally {
                // Short inter-packet guard delay to prevent BLE queue congestion
                delay(30)
                isSending = false
                processQueue()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun writeRawChunk(
        gatt: BluetoothGatt,
        tx: BluetoothGattCharacteristic,
        chunk: ByteArray
    ): Boolean {
        return try {
            val canNoResp = (tx.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
            val canWrite = (tx.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0

            // Prefer WRITE_TYPE_NO_RESPONSE for high throughput UART if available, otherwise WRITE_TYPE_DEFAULT
            val writeType = if (canNoResp) {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            } else {
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            }

            @Suppress("DEPRECATION")
            tx.value = chunk
            tx.writeType = writeType

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val res = gatt.writeCharacteristic(tx, chunk, writeType)
                res == BluetoothGatt.GATT_SUCCESS
            } else {
                @Suppress("DEPRECATION")
                gatt.writeCharacteristic(tx)
            }
        } catch (e: Exception) {
            addLog(BleLogType.ERROR, "writeRawChunk 예외: ${e.message}")
            false
        }
    }

    fun setTcpIp(ip: String) { _tcpIp.value = ip }
    fun setTcpGw(gw: String) { _tcpGw.value = gw }
    fun setTcpSub(sub: String) { _tcpSub.value = sub }
    fun setTcpMac(mac: String) { _tcpMac.value = mac }

    fun resetTcpIpDefaults() {
        _tcpIp.value = BleProtocol.DEFAULT_IP
        _tcpGw.value = BleProtocol.DEFAULT_GW
        _tcpSub.value = BleProtocol.DEFAULT_SUB
        _tcpMac.value = BleProtocol.DEFAULT_MAC
    }

    // -------------------------------------------------------------
    // Feature 1: SMCU ID Settings
    // -------------------------------------------------------------
    fun downloadSmcuId(id: Int) {
        isWaitingForSmcuUpload = false
        _smcuUploadedId.value = null
        val packet = BleProtocol.buildSmcuDownloadPacket(id)
        sendPacket(packet, "SMCU ID $id 설정 다운로드")
    }

    fun uploadSmcuId() {
        isWaitingForSmcuUpload = true
        _smcuUploadedId.value = null
        val packet = BleProtocol.SMCU_ID_UPLOAD_PACKET
        sendPacket(packet, "SMCU ID 업로드 조회 명령")
    }

    // -------------------------------------------------------------
    // Feature 1-1: TCP/IP ID Settings
    // -------------------------------------------------------------
    fun downloadTcpIpSettings(
        ip: String = _tcpIp.value,
        gw: String = _tcpGw.value,
        sub: String = _tcpSub.value,
        mac: String = _tcpMac.value
    ) {
        val packet = BleProtocol.buildTcpIpDownloadPacket(ip, gw, sub, mac)
        sendPacket(packet, "TCP/IP 설정 전송")
    }

    fun uploadTcpIpSettings() {
        val packet = BleProtocol.TCP_IP_UPLOAD_PACKET
        sendPacket(packet, "TCP/IP 업로드 조회 명령 (14 바이트)")
    }

    fun handleSmcuUploadedPacket(packet: ByteArray) {
        if (packet.size >= 13) {
            val idValue = packet[12].toInt() and 0xFF
            _smcuUploadedId.value = idValue
            setStatus("SMCU 업로드 완료: ID = $idValue", "success")
            addLog(BleLogType.INFO, "SMCU 업로드 파싱: $idValue")
        } else {
            setStatus("업로드 실패: 패킷 길이가 부족합니다.", "error")
        }
    }

    // -------------------------------------------------------------
    // Feature 2: SmartRelay ID Settings
    // -------------------------------------------------------------
    fun downloadSmartRelayId(id: Int = _smartRelayTargetId.value) {
        val packet = BleProtocol.buildSmartRelayStartPacket(id)
        val checksum = packet[11].toInt() and 0xFF
        sendPacket(packet, "SmartRelay ID $id 설정 다운로드 (11번째: FF, 체크섬 0x${"%02X".format(checksum)})")
    }

    fun startSmartRelayStream() {
        smartRelayStreamJob?.cancel()
        _isSmartRelayStreaming.value = true
        setStatus("SmartRelay 스트림 시작 (1초 주기 패킷 송신 중...)", "busy")

        val currentId = _smartRelayTargetId.value
        val startPacket = BleProtocol.buildSmartRelayStartPacket(currentId)
        val startChecksum = startPacket[11].toInt() and 0xFF
        sendPacket(startPacket, "SmartRelay Start 송신 (ID: $currentId, 11번째: FF, 체크섬: 0x%02X)".format(startChecksum))
        addLog(BleLogType.INFO, "SmartRelay Start: ID=$currentId, 11번째=0xFF, 체크섬=0x%02X 송신 완료. 1초 주기로 DD 패킷 송신 시작".format(startChecksum))

        smartRelayStreamJob = viewModelScope.launch {
            delay(1000L)
            while (isActive && _isSmartRelayStreaming.value) {
                sendPacket(BleProtocol.SMART_RELAY_PERIODIC_STREAM_PACKET, "SmartRelay 1초 주기 패킷 송신 (DD)")
                delay(1000L)
            }
        }
    }

    fun stopSmartRelayStream() {
        smartRelayStreamJob?.cancel()
        smartRelayStreamJob = null
        _isSmartRelayStreaming.value = false
        val currentId = _smartRelayTargetId.value
        val stopPacket = BleProtocol.buildSmartRelayStopPacket(currentId)
        val stopChecksum = stopPacket[11].toInt() and 0xFF
        sendPacket(stopPacket, "SmartRelay Stop 송신 (ID: $currentId, 11번째: 00, 체크섬: 0x%02X)".format(stopChecksum))
        setStatus("SmartRelay 스트림 중지됨 (Stop 패킷 송신 완료)", "success")
        addLog(BleLogType.INFO, "SmartRelay Stop: ID=$currentId, 11번째=0x00, 체크섬=0x%02X 송신 및 1초 주기 송신 중단".format(stopChecksum))
    }

    // -------------------------------------------------------------
    // Feature 2-1: SmartRelay Control (128 Relays)
    // -------------------------------------------------------------
    private val _relayStates = MutableStateFlow<BooleanArray>(BooleanArray(128) { false })
    val relayStates: StateFlow<BooleanArray> = _relayStates.asStateFlow()

    private var lastRelayRequestTime = 0L
    private var accumulatedRelayOffset = 0

    fun requestRelayStatus() {
        lastRelayRequestTime = System.currentTimeMillis()
        accumulatedRelayOffset = 0
        // TX : 02 63 FB 21 00 03 80 00 00 3A 03 03 FC FC (7번째 데이터 01 -> 80)
        sendPacket(BleProtocol.SMART_RELAY_STATUS_REQUEST_PACKET, "SmartRelay 128개 릴레이 상태 조회 (80 00 00)")
        setStatus("SmartRelay 128개 상태 조회 패킷 송신 완료", "busy")
        addLog(BleLogType.INFO, "SmartRelay 128개 상태 요청 송신: 02 63 FB 21 00 03 80 00 00 3A 03 03 FC FC")
    }

    fun toggleRelay(index: Int, currentState: Boolean) {
        val newState = !currentState
        val relayValue: Byte = if (newState) 0xFF.toByte() else 0x00.toByte()

        // 15-byte Packet Format:
        // 02 63 FB 41 00 04 01 [INDEX_H] [INDEX_L] [VAL:00/FF] [CHECKSUM] 03 03 FC FC
        val packet = ByteArray(15)
        packet[0] = 0x02.toByte()
        packet[1] = 0x63.toByte()
        packet[2] = 0xFB.toByte()
        packet[3] = 0x41.toByte()
        packet[4] = 0x00.toByte()
        packet[5] = 0x04.toByte()
        packet[6] = 0x01.toByte()
        packet[7] = ((index shr 8) and 0xFF).toByte()
        packet[8] = (index and 0xFF).toByte()
        packet[9] = relayValue

        // Checksum: XOR from 2nd byte (index 1) to 10th byte (index 9)
        var checksum = 0
        for (i in 1..9) {
            checksum = checksum xor (packet[i].toInt() and 0xFF)
        }
        packet[10] = (checksum and 0xFF).toByte()
        packet[11] = 0x03.toByte()
        packet[12] = 0x03.toByte()
        packet[13] = 0xFC.toByte()
        packet[14] = 0xFC.toByte()

        // Send packet
        sendPacket(packet, "릴레이 $index ${if (newState) "FF(ON)" else "00(OFF)"}")

        // Optimistic local state update
        try {
            if (index in 0 until _relayStates.value.size) {
                val states = _relayStates.value.copyOf()
                states[index] = newState
                _relayStates.value = states
            }
        } catch (e: Exception) {
            android.util.Log.e("BleViewModel", "Error toggling relay state: ${e.message}")
        }
    }

    /**
     * 여러 수신데이터를 조합 및 통합하고 10번째 데이터를 Relay 0번째에 표시하여 128개의 상태 표시
     * (FF : ON, 00 : OFF)
     */
    fun handleRelayStatusPacket(packet: ByteArray) {
        try {
            if (packet.size >= 10) {
                // 1) Single Relay Control Response: 02 63 FB 41 00 04 01 [INDEX_H] [INDEX_L] [VAL] [CS] 03 03 FC FC
                if (packet.size == 15 && packet.getOrNull(3) == 0x41.toByte()) {
                    val relayIdx = ((packet[7].toInt() and 0xFF) shl 8) or (packet[8].toInt() and 0xFF)
                    val valByte = packet[9].toInt() and 0xFF
                    val isRelayOn = (valByte == 0xFF || valByte != 0x00)
                    if (relayIdx in 0 until 128) {
                        val states = _relayStates.value.copyOf()
                        states[relayIdx] = isRelayOn
                        _relayStates.value = states
                        val stateStr = if (isRelayOn) "FF(ON)" else "00(OFF)"
                        setStatus("Relay $relayIdx 상태: $stateStr", "success")
                        addLog(BleLogType.INFO, "Relay $relayIdx 제어 응답 확인: $stateStr (0x%02X)".format(valByte))
                    }
                    return
                }

                // 2) Bulk / Multi-packet Relay Status Response (0x21 등 상태 패킷):
                // 10번째 데이터(index 9)를 Relay 0번째 (또는 오프셋)에 매핑하여 128개 상태 조합 및 통합
                val hasFcTail = (packet.size >= 14 &&
                        packet[packet.size - 2] == 0xFC.toByte() &&
                        packet[packet.size - 1] == 0xFC.toByte())
                val tailLength = if (hasFcTail) {
                    if (packet.size >= 15 && packet[packet.size - 4] == 0x03.toByte() && packet[packet.size - 3] == 0x03.toByte()) 5 else 4
                } else 0

                val dataStartIndex = 9 // 10번째 바이트 (0-indexed 9)
                val availableBytes = maxOf(0, packet.size - dataStartIndex - tailLength)
                if (availableBytes <= 0) return

                // 8, 9번째 바이트(index 7, 8)에 명시적 오프셋이 있는지 확인
                val explicitOffset = if (packet.size >= 9) {
                    ((packet[7].toInt() and 0xFF) shl 8) or (packet[8].toInt() and 0xFF)
                } else 0

                val startRelayIndex: Int
                val now = System.currentTimeMillis()
                if (explicitOffset in 1..127) {
                    startRelayIndex = explicitOffset
                } else if (availableBytes >= 128) {
                    // 단일 패킷으로 128개 전체 수신
                    startRelayIndex = 0
                    accumulatedRelayOffset = 128
                } else {
                    // 여러 분할 수신데이터 순차 조합 및 통합
                    val isRecentRequest = (now - lastRelayRequestTime < 5000L)
                    if (isRecentRequest && accumulatedRelayOffset in 1..127) {
                        startRelayIndex = accumulatedRelayOffset
                    } else {
                        startRelayIndex = 0
                    }
                }

                val states = _relayStates.value.copyOf()
                val countToUpdate = minOf(availableBytes, 128 - startRelayIndex)
                for (i in 0 until countToUpdate) {
                    val rIndex = startRelayIndex + i
                    val b = packet[dataStartIndex + i].toInt() and 0xFF
                    // FF : ON, 00 : OFF
                    states[rIndex] = (b == 0xFF || b != 0x00)
                }
                _relayStates.value = states
                accumulatedRelayOffset = minOf(128, startRelayIndex + countToUpdate)

                val onCount = states.count { it }
                val offCount = 128 - onCount
                setStatus("128개 릴레이 상태 통합 (Relay 0~${accumulatedRelayOffset - 1} 완료 | ON: $onCount, OFF: $offCount)", "success")
                addLog(
                    BleLogType.INFO,
                    "SmartRelay 상태 수신 통합: Relay $startRelayIndex..${startRelayIndex + countToUpdate - 1} 업데이트 (10번째 데이터->Relay 0 기준, 총 누적 ${accumulatedRelayOffset}/128, ON: $onCount, OFF: $offCount)"
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("BleViewModel", "Error handling relay status packet: ${e.message}")
        }
    }
    // -------------------------------------------------------------
    @SuppressLint("MissingPermission")
    fun disconnect() {
        isVerifyingConnection = false
        verificationJob?.cancel()
        verificationJob = null
        pendingConnectedDevice = null
        smartRelayStreamJob?.cancel()
        smartRelayStreamJob = null
        _isSmartRelayStreaming.value = false
        _smartRelayRealtimeId.value = null
        _smartRelayRealtimeHex.value = "-- --"
        _smartRelayRealtimeByte10.value = null
        _smartRelayRealtimeByte11.value = null
        isWaitingForSmcuUpload = false
        _smcuUploadedId.value = null

        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (_: Exception) {}
        bluetoothGatt = null
        txCharacteristic = null
        rxCharacteristic = null

        _connectionState.value = BleConnectionState.Disconnected
        _currentScreen.value = BleCurrentScreen.SCAN
        setStatus("블루투스 연결이 해제되었습니다.", "ready")
        addLog(BleLogType.INFO, "연결 종료됨 (초기 검색 화면으로 복귀)")
    }

    override fun onCleared() {
        super.onCleared()
        stopScan()
        disconnect()
    }
}
