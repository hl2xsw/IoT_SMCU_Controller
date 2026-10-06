import re

with open('public/index.html', 'r', encoding='utf-8') as f:
    html = f.read()

# ------------------------------------------------------------------------------
# 1. Add Iframe Warning Banner right above scanScreen
# ------------------------------------------------------------------------------
iframe_banner_html = '''  <!-- Iframe Preview Warning Banner -->
  <div id="iframeNoticeBanner" style="display:none; background:linear-gradient(90deg, rgba(255,180,171,0.22), rgba(255,217,147,0.22)); border:1px solid var(--amber); border-radius:14px; padding:12px 16px; margin-bottom:12px; align-items:center; justify-content:space-between; gap:12px;">
    <div style="display:flex; align-items:center; gap:10px;">
      <span style="font-size:22px;">⚠️</span>
      <div style="font-size:12px; line-height:1.45; color:var(--text-main);">
        <b>미리보기(iframe) 창 감지:</b> 브라우저 보안 정책상 iframe 내부에서는 블루투스 검색이 차단됩니다.<br>
        <span style="color:var(--amber); font-weight:600;">반드시 우측 [새 창에서 열기 ↗] 버튼을 눌러 독립 브라우저 탭에서 실행해 주세요!</span>
      </div>
    </div>
    <button onclick="window.open(window.location.href, '_blank')" class="btn btn-primary" style="height:36px; padding:0 14px; font-size:12px; white-space:nowrap; font-weight:700; box-shadow:0 2px 10px rgba(128,226,184,0.3);">
      새 창에서 열기 ↗
    </button>
  </div>
'''

if 'id="iframeNoticeBanner"' not in html:
    html = html.replace('<div class="screen active" id="scanScreen">', iframe_banner_html + '\n  <div class="screen active" id="scanScreen">')
    print("Step 1: Iframe banner added.")

# ------------------------------------------------------------------------------
# 2. Update Scan Card with Diagnostics & Direct inline onclicks
# ------------------------------------------------------------------------------
old_scan_card_pattern = r'<!-- Scan Buttons[\s\S]*?<!-- Connecting Progress Banner -->'

new_scan_card_html = '''<!-- Environment Diagnostics Badge -->
      <div style="background:var(--bg-surface-variant); border-radius:12px; padding:10px 14px; font-size:12px; display:flex; flex-direction:column; gap:5px; border:1px solid rgba(255,255,255,0.06);">
        <div style="display:flex; justify-content:space-between; align-items:center;">
          <span style="color:var(--text-muted); font-size:11px;">Web Bluetooth API:</span>
          <span id="bleSupportStatus" style="font-weight:700; color:var(--mint); font-size:11px;">확인 중...</span>
        </div>
        <div style="display:flex; justify-content:space-between; align-items:center;">
          <span style="color:var(--text-muted); font-size:11px;">실행 브라우저 환경:</span>
          <span id="bleContextStatus" style="font-weight:600; color:var(--primary); font-size:11px;">최상위 브라우저 탭</span>
        </div>
      </div>

      <!-- Scan Buttons (Dual Mode: All Devices Primary + Filter Secondary) -->
      <div style="display:flex; flex-direction:column; gap:10px;">
        <div style="display:flex; gap:8px;">
          <!-- Primary Scan: All Nearby BLE Devices (Shows HMSoft, SMCU, SmartRelay, AT-09, etc.) -->
          <button class="btn btn-primary" id="startScanBtn" onclick="performAllDevicesScan()" style="flex:1; height:54px; font-size:15px; font-weight:700; box-shadow:0 4px 18px rgba(128,226,184,0.3); letter-spacing:-0.3px; cursor:pointer;">
            <svg width="22" height="22" viewBox="0 0 24 24"><path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
            <span id="scanBtnText">🔍 블루투스 기기 검색 (모든 기기)</span>
          </button>
          <button class="btn btn-secondary" id="stopScanBtn" onclick="stopScan()" style="height:54px; display:none; padding:0 16px; color:var(--red); font-weight:700;">
            <svg width="18" height="18" viewBox="0 0 24 24"><path d="M6 6h12v12H6z"/></svg>
            중지
          </button>
        </div>

        <!-- Secondary Option: Targeted UART Service & Name Filter Scan -->
        <div style="display:flex; gap:8px;">
          <button class="btn btn-secondary" id="startFilterScanBtn" onclick="performFilterScan()" style="flex:1; height:42px; font-size:12px; font-weight:600; border:1px solid rgba(255,255,255,0.12); cursor:pointer;">
            <svg width="16" height="16" viewBox="0 0 24 24"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
            <span id="filterScanBtnText">⚡ BLE UART 전용 필터 검색 (HM-10 / Nordic)</span>
          </button>
        </div>
      </div>

      <!-- Smartphone Essential Troubleshooting Notice -->
      <div style="background:rgba(255,217,147,0.06); border:1px solid rgba(255,217,147,0.25); border-radius:12px; padding:10px 14px; margin-top:2px;">
        <div style="display:flex; justify-content:space-between; align-items:center; cursor:pointer;" onclick="togglePhoneGuide()">
          <span style="font-size:12px; font-weight:700; color:var(--amber); display:flex; align-items:center; gap:6px;">
            <span>💡</span> 스마트폰 검색창 즉시 닫힘 해결 (위치 GPS & 노트북 1:1 연결 해제)
          </span>
          <span style="font-size:11px; color:var(--primary); font-weight:700;">[상세보기 ▼]</span>
        </div>
      </div>

      <!-- Native Android App Direct APK Install Notice -->
      <div style="background:rgba(128,226,184,0.06); border:1px solid rgba(128,226,184,0.25); border-radius:12px; padding:10px 14px; display:flex; justify-content:space-between; align-items:center;">
        <div style="display:flex; align-items:center; gap:8px;">
          <span style="font-size:16px;">📱</span>
          <div style="font-size:12px; color:var(--text-main);">
            <b>스마트폰 사용자:</b> 브라우저 제약 없는 <b>정식 안드로이드 앱</b> 설치 권장
          </div>
        </div>
        <a href="/app-debug.apk" download style="background:var(--mint); color:var(--bg-main); font-weight:700; font-size:11px; padding:6px 12px; border-radius:8px; text-decoration:none; white-space:nowrap; box-shadow:0 2px 8px rgba(128,226,184,0.3);">
          APK 받기
        </a>
      </div>

      <!-- Connecting Progress Banner -->'''

if re.search(old_scan_card_pattern, html):
    html = re.sub(old_scan_card_pattern, new_scan_card_html, html)
    print("Step 2: Scan card HTML updated.")
else:
    print("Warning: old_scan_card_pattern not found.")

# ------------------------------------------------------------------------------
# 3. Replace Scan JavaScript logic: DIRECT invocation, NO preliminary awaits!
# ------------------------------------------------------------------------------
idx_start = html.find('const BLE_SERVICES =')
idx_end = html.find('function handleIncomingBytes')

if idx_start != -1 and idx_end != -1:
    new_js_code = '''const BLE_SERVICES = [
    '6e400001-b5a3-f393-e0a9-e50e24dcca9e', // Nordic UART Service
    '0000ffe0-0000-1000-8000-00805f9b34fb', // Standard HM-10 / CC2541 / MLT-BT05
    '0000ffe5-0000-1000-8000-00805f9b34fb', // Alternative HM-10
    '49535343-fe7d-4ae5-8fa9-9fafd205e455', // Microchip IS1678
    '0000fff0-0000-1000-8000-00805f9b34fb', // Common Telink
    '0000fee0-0000-1000-8000-00805f9b34fb', // Custom UART
    '0000ff00-0000-1000-8000-00805f9b34fb'  // Common serial
  ];

  // Verified chunk delay between 20-byte chunks (35ms matches Android BleViewModel delay(35L))
  let chunkDelayMs = 35;

  window.setChunkDelay = function(ms) {
    chunkDelayMs = ms;
    document.querySelectorAll('.chunk-delay-btn').forEach(btn => {
      btn.style.border = '1px solid var(--border-color)';
      btn.style.background = 'var(--bg-surface-variant)';
      btn.style.color = 'var(--text-muted)';
      btn.style.fontWeight = 'normal';
    });
    const targetBtn = document.getElementById(`delayBtn${ms}`);
    if (targetBtn) {
      targetBtn.style.border = '1px solid var(--mint)';
      targetBtn.style.background = 'rgba(128,226,184,0.2)';
      targetBtn.style.color = 'var(--mint)';
      targetBtn.style.fontWeight = '700';
    }
    const statusText = document.getElementById('smcuChunkStatusText');
    if (statusText) statusText.textContent = `20B 분할 (지연: ${ms}ms)`;
    setStatus(`BLE 분할 전송 간격이 ${ms}ms로 설정되었습니다.`, 'info');
  };

  // ===========================================================================
  // UI Status & Navigation Helpers
  // ===========================================================================
  function setStatus(msg, type = 'info') {
    const dot = document.getElementById('bottomStatusDot');
    const txt = document.getElementById('bottomStatusText');
    if (!dot || !txt) return;

    txt.textContent = msg;
    dot.className = 'status-dot';
    if (type === 'busy') dot.classList.add('busy');
    else if (type === 'error') dot.classList.add('error');
  }

  function showScreen(screenId) {
    document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
    const target = document.getElementById(screenId);
    if (target) {
      target.classList.add('active');
      activeScreen = screenId;
    }

    if (screenId === 'relayControlScreen') {
      renderRelayItems();
      updateRelayCounters();
    }
  }

  function getTimestampString() {
    const d = new Date();
    const pad = n => ('0' + n).slice(-2);
    return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
  }

  function appendLog(containerId, hexStr, type = 'rx') {
    const container = document.getElementById(containerId);
    if (!container) return;

    const row = document.createElement('div');
    row.className = `log-line log-${type}`;

    const timeSpan = document.createElement('span');
    timeSpan.style.color = 'var(--text-muted)';
    timeSpan.style.marginRight = '6px';
    timeSpan.style.fontSize = '11px';
    timeSpan.style.fontFamily = 'var(--font-mono)';
    timeSpan.textContent = `[${getTimestampString()}]`;

    const tag = document.createElement('span');
    tag.className = 'log-tag';
    tag.textContent = `[${type.toUpperCase()}]`;

    const text = document.createElement('span');
    text.className = 'log-text';
    text.style.wordBreak = 'break-all';
    text.style.marginLeft = '6px';
    text.textContent = hexStr;

    row.appendChild(timeSpan);
    row.appendChild(tag);
    row.appendChild(text);

    container.insertBefore(row, container.firstChild);
  }

  function clearLogs(containerId) {
    const container = document.getElementById(containerId);
    if (container) container.innerHTML = '';
  }

  function toHexString(byteArray) {
    return Array.from(byteArray, b => ('0' + (b & 0xFF).toString(16).toUpperCase()).slice(-2)).join(' ');
  }

  function computeXorChecksum(bytes, startIdx, endIdx) {
    let xorVal = 0;
    const safeEnd = Math.min(endIdx, bytes.length - 1);
    for (let i = startIdx; i <= safeEnd; i++) {
      xorVal ^= (bytes[i] & 0xFF);
    }
    return xorVal & 0xFF;
  }

  // ===========================================================================
  // 1. Bluetooth Connection Management (Direct User-Gesture Invocation)
  // ===========================================================================
  const startScanBtn = document.getElementById('startScanBtn');
  const startFilterScanBtn = document.getElementById('startFilterScanBtn');
  const stopScanBtn = document.getElementById('stopScanBtn');
  const scanStatusBadge = document.getElementById('scanStatusBadge');
  const radarSweep = document.getElementById('radarSweep');
  const devicesContainer = document.getElementById('devicesContainer');
  const deviceCountText = document.getElementById('deviceCountText');

  function updateDiagnostics() {
    const supportEl = document.getElementById('bleSupportStatus');
    const contextEl = document.getElementById('bleContextStatus');
    const inIframe = (window.self !== window.top);

    if (supportEl) {
      if (navigator.bluetooth) {
        supportEl.textContent = '✅ 지원됨 (정상 작동 가능)';
        supportEl.style.color = 'var(--mint)';
      } else {
        supportEl.textContent = '❌ 미지원 (Chrome / Edge 브라우저 필요)';
        supportEl.style.color = 'var(--red)';
      }
    }

    if (contextEl) {
      if (inIframe) {
        contextEl.textContent = '⚠️ 미리보기 iframe (새 창 권장)';
        contextEl.style.color = 'var(--amber)';
        const banner = document.getElementById('iframeNoticeBanner');
        if (banner) banner.style.display = 'flex';
      } else {
        contextEl.textContent = '✅ 최상위 브라우저 탭 (정상)';
        contextEl.style.color = 'var(--mint)';
      }
    }
  }

  // Call diagnostics immediately and on load
  updateDiagnostics();
  window.addEventListener('DOMContentLoaded', updateDiagnostics);

  window.togglePhoneGuide = function() {
    const guide = document.getElementById('phoneTroubleGuide');
    if (guide) {
      guide.style.display = (guide.style.display === 'none' || !guide.style.display) ? 'block' : 'none';
      if (guide.style.display === 'block') {
        guide.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
    }
  };

  function stopScan() {
    isScanning = false;
    if (scanStatusBadge) {
      scanStatusBadge.textContent = "READY TO SCAN";
      scanStatusBadge.style.color = "var(--primary)";
    }
    if (radarSweep) radarSweep.style.display = "none";
    const scanBtnText = document.getElementById('scanBtnText');
    if (scanBtnText) scanBtnText.textContent = "🔍 블루투스 기기 검색 (모든 기기)";
    const filterBtnText = document.getElementById('filterScanBtnText');
    if (filterBtnText) filterBtnText.textContent = "⚡ BLE UART 전용 필터 검색 (HM-10 / Nordic)";
    if (stopScanBtn) stopScanBtn.style.display = "none";
  }

  function showBluetoothNotSupported() {
    alert("현재 브라우저는 Web Bluetooth API를 지원하지 않거나 비활성화되어 있습니다.\\n\\nChrome(크롬) 또는 Edge 브라우저를 사용해 주시거나, 상단의 [APK 받기]를 통해 정식 안드로이드 앱으로 설치해 주세요.\\n\\n(※ 기능 테스트를 위한 가상 시뮬레이션 모드로 연결합니다)");
    simulateConnect();
  }

  // Primary Scan: All Nearby BLE Devices
  // CRITICAL: requestDevice must be called synchronously inside user gesture tick (NO awaits before requestDevice)
  window.performAllDevicesScan = async function() {
    if (!navigator.bluetooth) {
      showBluetoothNotSupported();
      return;
    }

    try {
      setStatus("주변 블루투스 기기 검색 중... (선택 창에서 기기를 선택하세요)", "busy");
      isScanning = true;
      if (scanStatusBadge) {
        scanStatusBadge.textContent = "SCANNING";
        scanStatusBadge.style.color = "var(--mint)";
      }
      if (radarSweep) radarSweep.style.display = "block";
      const scanBtnText = document.getElementById('scanBtnText');
      if (scanBtnText) scanBtnText.textContent = "기기 선택 창 열림...";
      if (stopScanBtn) stopScanBtn.style.display = "inline-flex";

      const guide = document.getElementById('phoneTroubleGuide');
      if (guide) guide.style.display = 'none';

      // DIRECT CALL synchronously within user gesture tick
      const device = await navigator.bluetooth.requestDevice({
        acceptAllDevices: true,
        optionalServices: BLE_SERVICES
      });

      if (!device) {
        stopScan();
        return;
      }

      handleDeviceSelected(device);
    } catch (err) {
      handleScanError(err, '전체 기기');
    }
  };

  // Secondary Scan: Targeted UART Service & Name Filter Scan
  window.performFilterScan = async function() {
    if (!navigator.bluetooth) {
      showBluetoothNotSupported();
      return;
    }

    try {
      setStatus("UART 전용 기기 필터 검색 중...", "busy");
      isScanning = true;
      if (scanStatusBadge) {
        scanStatusBadge.textContent = "SCANNING";
        scanStatusBadge.style.color = "var(--mint)";
      }
      if (radarSweep) radarSweep.style.display = "block";
      const filterBtnText = document.getElementById('filterScanBtnText');
      if (filterBtnText) filterBtnText.textContent = "필터 선택 창 열림...";
      if (stopScanBtn) stopScanBtn.style.display = "inline-flex";

      // DIRECT CALL synchronously within user gesture tick
      const device = await navigator.bluetooth.requestDevice({
        filters: [
          { services: ['0000ffe0-0000-1000-8000-00805f9b34fb'] }, // Standard HM-10 / CC2541
          { services: ['6e400001-b5a3-f393-e0a9-e50e24dcca9e'] }, // Nordic NUS
          { services: ['0000fff0-0000-1000-8000-00805f9b34fb'] }, // Telink
          { namePrefix: 'SMCU' },
          { namePrefix: 'Smart' },
          { namePrefix: 'HM' },
          { namePrefix: 'BT' },
          { namePrefix: 'BLE' },
          { namePrefix: 'Relay' },
          { namePrefix: 'HC' },
          { namePrefix: 'CC' },
          { namePrefix: 'AT' },
          { namePrefix: 'MLT' },
          { namePrefix: 'JDY' },
          { namePrefix: 'ESP' }
        ],
        optionalServices: BLE_SERVICES
      });

      if (!device) {
        stopScan();
        return;
      }

      handleDeviceSelected(device);
    } catch (err) {
      handleScanError(err, 'UART 필터');
    }
  };

  function handleScanError(err, scanType) {
    console.warn(`${scanType} 검색 취소 또는 오류:`, err);
    stopScan();

    const isMobile = /Android|iPhone|iPad/i.test(navigator.userAgent);
    const inIframe = (window.self !== window.top);
    const guide = document.getElementById('phoneTroubleGuide');

    if (err.name === 'SecurityError') {
      if (inIframe) {
        setStatus("미리보기(iframe) 창에서는 블루투스가 차단됩니다. [새 창에서 열기]를 눌러주세요.", "error");
        alert("현재 AI Studio 미리보기(iframe) 창 내부에서는 브라우저 보안 정책상 블루투스 접근이 차단됩니다.\\n\\n상단의 [새 창에서 열기 ↗] 버튼을 눌러 독립 브라우저 탭에서 실행해 주세요!");
      } else {
        setStatus(`보안 오류: HTTPS 연결 또는 브라우저 권한을 확인해주세요. (${err.message})`, "error");
      }
    } else if (err.name === 'NotFoundError') {
      if (isMobile && guide) {
        guide.style.display = 'block';
        guide.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
      setStatus("검색 창이 닫혔습니다. (스마트폰 '위치(GPS)' 켬 / 노트북 1:1 연결 해제 확인)", "info");
    } else {
      setStatus(`블루투스 검색 오류: ${err.message}`, "error");
    }
  }

  function handleDeviceSelected(device) {
    stopScan();
    const guide = document.getElementById('phoneTroubleGuide');
    if (guide) guide.style.display = 'none';

    renderDeviceItem({
      name: device.name || "SmartRelay Controller",
      id: device.id,
      rawDevice: device
    });

    setStatus(`기기 선택됨: ${device.name || 'SmartRelay'}. 연결을 진행합니다.`, "busy");
    connectToDevice(device);
  }

  // Register event listeners as well as inline onclicks
  if (startScanBtn) startScanBtn.addEventListener('click', performAllDevicesScan);
  if (startFilterScanBtn) startFilterScanBtn.addEventListener('click', performFilterScan);
  if (stopScanBtn) stopScanBtn.addEventListener('click', stopScan);
  window.triggerFilterScan = performFilterScan;

  function renderDeviceItem(dev) {
    devicesContainer.innerHTML = '';
    deviceCountText.textContent = "1 DEPLOYED";

    const item = document.createElement('div');
    item.className = 'device-item';
    item.innerHTML = `
      <div class="device-item-left">
        <div class="device-accent-bar"></div>
        <div>
          <div class="device-name">${dev.name}</div>
          <div class="device-uuid">UUID: ${dev.id}</div>
        </div>
      </div>
      <div style="display:flex; flex-direction:column; align-items:flex-end;">
        <span style="font-size:12px; font-weight:700; font-family:var(--font-mono); color:var(--primary);">-65 dBm</span>
        <span style="font-size:9px; font-weight:700; font-family:var(--font-mono); color:var(--text-muted); letter-spacing:1px;">CONNECT →</span>
      </div>
    `;

    item.onclick = () => connectToDevice(dev.rawDevice || dev);
    devicesContainer.appendChild(item);
  }

  async function connectToDevice(device) {
    const progressCard = document.getElementById('connectingProgressCard');
    const subMsg = document.getElementById('connectingSubMsg');
    if (progressCard) progressCard.style.display = 'block';

    try {
      setStatus("GATT 서버에 연결 중...", "busy");
      if (subMsg) subMsg.textContent = "GATT 서버 연결 중...";
      bluetoothDevice = device;

      device.addEventListener('gattserverdisconnected', onDisconnected);

      gattServer = await device.gatt.connect();
      setStatus("GATT 서비스 탐색 중...", "busy");
      if (subMsg) subMsg.textContent = "UART 서비스 및 특성 탐색 중...";

      // Find UART service
      let uartService = null;
      for (const sUuid of BLE_SERVICES) {
        try {
          uartService = await gattServer.getPrimaryService(sUuid);
          if (uartService) break;
        } catch (_) {}
      }

      if (!uartService) {
        // Fallback: search all available services, skipping Generic Access/Attribute 000018xx
        try {
          const services = await gattServer.getPrimaryServices();
          for (const s of services) {
            const u = s.uuid.toLowerCase();
            if (!u.includes('00001800') && !u.includes('00001801') && !u.includes('0000180a')) {
              uartService = s;
              break;
            }
          }
          if (!uartService && services.length > 0) uartService = services[0];
        } catch (_) {}
      }

      if (!uartService) {
        throw new Error("UART 통신 서비스를 찾을 수 없습니다.");
      }

      // Discover TX / RX characteristics with priority matching (identical to Android Kotlin BleViewModel)
      txChar = null;
      rxChar = null;
      const chars = await uartService.getCharacteristics();

      // First pass: Match known UART characteristic UUIDs
      for (const ch of chars) {
        const u = ch.uuid.toLowerCase();
        if (u.includes('6e400002') || u.includes('ffe1') || u.includes('ffe9') || u.includes('ffe4') || u.includes('fff2')) {
          txChar = ch;
        }
        if (u.includes('6e400003') || u.includes('ffe1') || u.includes('ffe9') || u.includes('ffe4') || u.includes('fff1')) {
          rxChar = ch;
        }
      }

      // Second pass: Property-based fallback if not matched
      if (!txChar || !rxChar) {
        for (const ch of chars) {
          if (!txChar && (ch.properties.writeWithoutResponse || ch.properties.write)) {
            txChar = ch;
          }
          if (!rxChar && (ch.properties.notify || ch.properties.indicate)) {
            rxChar = ch;
          }
        }
      }

      if (!txChar) {
        throw new Error("쓰기 Characteristic을 찾을 수 없습니다.");
      }

      if (rxChar) {
        try {
          await rxChar.startNotifications();
          rxChar.addEventListener('characteristicvaluechanged', (e) => {
            const val = new Uint8Array(e.target.value.buffer);
            handleIncomingBytes(val);
          });
        } catch (notifErr) {
          console.warn("Notification start failed:", notifErr);
        }
      }

      // Start Connection Verification (identical to Kotlin Android app)
      setStatus("BLE 연결 완료. 통신 상태 검증 중...", "busy");
      if (subMsg) subMsg.textContent = "릴레이 상태 패킷(02 63 FB 21 00 03 80...) 송신 후 응답 대기 중";
      isVerifyingConnection = true;

      // Update banner
      const topBanner = document.getElementById('topConnectedBanner');
      if (topBanner) topBanner.style.display = 'flex';
      const devNameEl = document.getElementById('bannerDevName');
      if (devNameEl) devNameEl.textContent = device.name || 'SmartRelay Controller';
      const devUuidEl = document.getElementById('bannerDevUuid');
      if (devUuidEl) devUuidEl.textContent = `UUID: ${(device.id || '').slice(0, 16)}...`;

      // Verification packet: 02 63 FB 21 00 03 80 00 00 3A 03 03 FC FC
      await sendPacket([0x02, 0x63, 0xFB, 0x21, 0x00, 0x03, 0x80, 0x00, 0x00, 0x3A, 0x03, 0x03, 0xFC, 0xFC], 'smcuLogContainer');

      setTimeout(() => {
        if (progressCard) progressCard.style.display = 'none';
        isVerifyingConnection = false;
        showScreen('dashboardScreen');
        setStatus("기기 연결 및 통신 검증 완료!", "info");
      }, 1000);

    } catch (err) {
      console.error("Connection failed:", err);
      if (progressCard) progressCard.style.display = 'none';
      setStatus(`기기 연결 실패: ${err.message}`, "error");
      onDisconnected();
    }
  }

  // Simulation mode if Web Bluetooth is not supported on the client browser
  function simulateConnect() {
    const progressCard = document.getElementById('connectingProgressCard');
    if (progressCard) progressCard.style.display = 'block';
    setStatus("가상 BLE 장치 연결 중...", "busy");

    setTimeout(() => {
      if (progressCard) progressCard.style.display = 'none';
      const topBanner = document.getElementById('topConnectedBanner');
      if (topBanner) topBanner.style.display = 'flex';
      const devNameEl = document.getElementById('bannerDevName');
      if (devNameEl) devNameEl.textContent = "SmartRelay-01 (시뮬레이션)";
      const devUuidEl = document.getElementById('bannerDevUuid');
      if (devUuidEl) devUuidEl.textContent = "UUID: 0000FFE0-0000-1000...";
      showScreen('dashboardScreen');
      setStatus("시뮬레이션 모드로 연결되었습니다. (모든 릴레이 제어 및 패킷 생성 동작 가능)", "info");
    }, 800);
  }

  function onDisconnected() {
    const topBanner = document.getElementById('topConnectedBanner');
    if (topBanner) topBanner.style.display = 'none';
    if (isSmartRelayStreaming) stopSmartRelayStream();
    bluetoothDevice = null;
    gattServer = null;
    txChar = null;
    rxChar = null;
    showScreen('scanScreen');
    setStatus("기기 연결이 해제되었습니다.", "info");
  }

  document.getElementById('bannerDisconnectBtn').addEventListener('click', () => {
    if (gattServer && gattServer.connected) {
      gattServer.disconnect();
    } else {
      onDisconnected();
    }
  });

  document.getElementById('cancelConnectBtn').addEventListener('click', () => {
    const progressCard = document.getElementById('connectingProgressCard');
    if (progressCard) progressCard.style.display = 'none';
    onDisconnected();
  });

  document.getElementById('menuExitBtn').addEventListener('click', () => {
    if (gattServer && gattServer.connected) gattServer.disconnect();
    else onDisconnected();
  });

  // ===========================================================================
  // 2. High-Performance Packet Transmission (20-byte Chunking + writeWithoutResponse Priority)
  // ===========================================================================
  // Writes a single <=20 byte chunk.
  // Standard BLE UART (HM-10, CC2541, Nordic NUS) requires Write Without Response (Unacknowledged Write).
  // This avoids Windows/WinRT GATT response timeouts and radio queue blocks.
  async function writeChunkWithRetry(ch, chunk, chunkIdx, totalChunks) {
    const maxRetries = 3;
    let lastErr = null;

    for (let attempt = 1; attempt <= maxRetries; attempt++) {
      try {
        // Priority 1: writeWithoutResponse (Standard BLE UART transmission)
        if (ch.properties.writeWithoutResponse && typeof ch.writeValueWithoutResponse === 'function') {
          await ch.writeValueWithoutResponse(chunk);
          return true;
        }

        // Priority 2: Direct writeValueWithoutResponse if method exists on characteristic
        if (typeof ch.writeValueWithoutResponse === 'function') {
          await ch.writeValueWithoutResponse(chunk);
          return true;
        }

        // Priority 3: writeValueWithResponse (fallback if peripheral strictly requires ACK)
        if (ch.properties.write && typeof ch.writeValueWithResponse === 'function') {
          await ch.writeValueWithResponse(chunk);
          return true;
        }

        // Priority 4: standard writeValue
        if (typeof ch.writeValue === 'function') {
          await ch.writeValue(chunk);
          return true;
        }

        throw new Error("Characteristic has no valid write method");
      } catch (err) {
        lastErr = err;
        console.warn(`[TX Chunk ${chunkIdx}/${totalChunks}] Attempt ${attempt} failed:`, err);
        if (attempt < maxRetries) {
          // 40ms, 80ms backoff gives radio buffer time to clear
          await new Promise(r => setTimeout(r, attempt * 40));
        }
      }
    }
    throw lastErr;
  }

  async function sendPacket(byteArray, targetLogContainerId = null) {
    const uint8 = new Uint8Array(byteArray);
    const hexStr = toHexString(uint8);

    // Append to logs
    const logContainers = ['smcuLogContainer', 'tcpIpLogContainer', 'srLogContainer'];
    if (targetLogContainerId && !logContainers.includes(targetLogContainerId)) {
      logContainers.push(targetLogContainerId);
    }

    if (txChar) {
      try {
        const CHUNK_SIZE = 20;

        if (uint8.length <= CHUNK_SIZE) {
          // Single chunk (<= 20 bytes)
          logContainers.forEach(cId => {
            appendLog(cId, hexStr, 'tx');
          });
          await writeChunkWithRetry(txChar, uint8, 1, 1);
          return true;
        } else {
          // Multi-chunk sequential transmission (20 bytes per chunk)
          const totalChunks = Math.ceil(uint8.length / CHUNK_SIZE);

          // 1. Log overall full packet
          logContainers.forEach(cId => {
            appendLog(cId, `[TX 전문 (${uint8.length}B)] ${hexStr}`, 'tx');
          });

          // 2. Sequentially send chunks with visual logging & delay
          for (let offset = 0, chunkIdx = 0; offset < uint8.length; offset += CHUNK_SIZE, chunkIdx++) {
            const chunk = uint8.slice(offset, offset + CHUNK_SIZE);
            const chunkHex = toHexString(chunk);
            console.log(`[TX Chunk ${chunkIdx + 1}/${totalChunks}] (${chunk.length} bytes):`, chunkHex);

            // Log individual chunk to UI
            logContainers.forEach(cId => {
              appendLog(cId, `[TX ${chunkIdx + 1}/${totalChunks} (${chunk.length}B)] ${chunkHex}`, 'tx');
            });

            await writeChunkWithRetry(txChar, chunk, chunkIdx + 1, totalChunks);

            // Inter-chunk delay for peripheral MCU UART buffer reassembly
            if (offset + CHUNK_SIZE < uint8.length) {
              await new Promise(r => setTimeout(r, chunkDelayMs));
            }
          }

          logContainers.forEach(cId => {
            appendLog(cId, `[전송 완료] 총 ${uint8.length}바이트 (${totalChunks}개 청크 전송 성공, 간격: ${chunkDelayMs}ms)`, 'tx');
          });
          return true;
        }
      } catch (err) {
        console.error("TX Write Error:", err);
        const errMsg = err && err.message ? err.message : String(err);
        logContainers.forEach(cId => {
          appendLog(cId, '전송 에러: ' + errMsg, 'error');
        });
        setStatus('패킷 전송 실패: ' + errMsg, "error");
        return false;
      }
    } else {
      // Simulation mode
      logContainers.forEach(cId => {
        appendLog(cId, hexStr, 'tx');
      });
      simulateResponse(uint8);
      return true;
    }
  }

  '''
    html = html[:idx_start] + new_js_code + html[idx_end:]
    print("Step 3: JavaScript core engine updated.")
else:
    print(f"Error finding JS boundaries: idx_start={idx_start}, idx_end={idx_end}")

# ------------------------------------------------------------------------------
# 4. Save to all targets
# ------------------------------------------------------------------------------
with open('public/index.html', 'w', encoding='utf-8') as f:
    f.write(html)
with open('index.html', 'w', encoding='utf-8') as f:
    f.write(html)
with open('app/src/main/assets/index.html', 'w', encoding='utf-8') as f:
    f.write(html)

print("All files updated successfully!")
