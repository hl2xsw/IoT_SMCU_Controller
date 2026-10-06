import re

with open('public/index.html', 'r', encoding='utf-8') as f:
    html = f.read()

# ------------------------------------------------------------------------------
# 1. Remove the Red Box elements (Smartphone Notice & APK Notice & Trouble Guide)
# ------------------------------------------------------------------------------
# Pattern for the two notice boxes
notices_pattern = r'<!-- Smartphone Essential Troubleshooting Notice -->[\s\S]*?<!-- Connecting Progress Banner -->'
replacement_connecting = '<!-- Connecting Progress Banner -->'

if re.search(notices_pattern, html):
    html = re.sub(notices_pattern, replacement_connecting, html)
    print("Step 1a: Red box notice cards removed.")
else:
    print("Warning: notices_pattern not found.")

# Also remove phoneTroubleGuide card if present
guide_pattern = r'<!-- Smartphone Troubleshooting & Help Card -->[\s\S]*?</div>\s*</div>\s*</div>\s*<!-- ============================================================= -->\s*<!-- SCREEN 2: DASHBOARD SCREEN -->'
guide_replacement = '</div>\n  </div>\n\n  <!-- ============================================================= -->\n  <!-- SCREEN 2: DASHBOARD SCREEN -->'

if re.search(guide_pattern, html):
    html = re.sub(guide_pattern, guide_replacement, html)
    print("Step 1b: phoneTroubleGuide card removed.")
else:
    # Try more relaxed pattern
    guide_pattern2 = r'<!-- Smartphone Troubleshooting & Help Card -->[\s\S]*?</div>\s*</div>\s*</div>'
    if re.search(guide_pattern2, html):
        html = re.sub(guide_pattern2, '</div>\n  </div>', html)
        print("Step 1b: phoneTroubleGuide card removed (alt pattern).")
    else:
        print("Notice: phoneTroubleGuide not matched (may already be removed).")

# ------------------------------------------------------------------------------
# 2. Add isWaitingForSmcuUpload state and restrict SMCU ID screen update to Upload ONLY
# ------------------------------------------------------------------------------
# In protocol state declaration
if 'let isWaitingForSmcuUpload = false;' not in html:
    html = html.replace('let smcuUploadedId = null;', 'let smcuUploadedId = null;\n  let isWaitingForSmcuUpload = false;')
    print("Step 2a: isWaitingForSmcuUpload flag declared.")

# In dispatchIncomingPacket
old_dispatch = '''    // 4) SMCU ID upload response (13th byte, Index 12)
    if (activeScreen === 'smcuScreen' && packet.length >= 13) {
      const readId = packet[12] & 0xFF;
      smcuUploadedId = readId;
      document.getElementById('smcuUploadedBanner').style.display = 'block';
      document.getElementById('smcuUploadedText').textContent = `${readId} (HEX: 0x${('0' + readId.toString(16).toUpperCase()).slice(-2)})`;
      setStatus(`SMCU ID 업로드 완료: ID=${readId}`, "info");
      return;
    }'''

new_dispatch = '''    // 4) SMCU ID upload response (13th byte, Index 12) - 업로드(0xBE) 응답일 때만 데이터 갱신
    if (activeScreen === 'smcuScreen' && isWaitingForSmcuUpload && packet.length >= 13 && packet[3] === 0xBE) {
      isWaitingForSmcuUpload = false;
      const readId = packet[12] & 0xFF;
      smcuUploadedId = readId;
      const banner = document.getElementById('smcuUploadedBanner');
      if (banner) banner.style.display = 'block';
      const textEl = document.getElementById('smcuUploadedText');
      if (textEl) textEl.textContent = `${readId} (HEX: 0x${('0' + readId.toString(16).toUpperCase()).slice(-2)})`;
      setStatus(`SMCU ID 업로드 확인 완료: ID=${readId}`, "info");
      return;
    }'''

if old_dispatch in html:
    html = html.replace(old_dispatch, new_dispatch)
    print("Step 2b: dispatchIncomingPacket SMCU upload logic updated.")
else:
    # Try regex match
    disp_pattern = r'// 4\) SMCU ID upload response[\s\S]*?return;\s*}'
    if re.search(disp_pattern, html):
        html = re.sub(disp_pattern, new_dispatch, html)
        print("Step 2b: dispatchIncomingPacket updated via regex.")
    else:
        print("Warning: dispatchIncomingPacket SMCU block not found.")

# In smcuUploadBtn.onclick: set isWaitingForSmcuUpload = true
old_upload_btn = '''  // Upload (Send 14-byte SMCU Request Packet)
  const smcuUploadBtn = document.getElementById('smcuUploadBtn');
  if (smcuUploadBtn) {
    smcuUploadBtn.onclick = async () => {
      setStatus("SMCU ID 업로드 요청 패킷 송신 중...", "busy");'''

new_upload_btn = '''  // Upload (Send 14-byte SMCU Request Packet)
  const smcuUploadBtn = document.getElementById('smcuUploadBtn');
  if (smcuUploadBtn) {
    smcuUploadBtn.onclick = async () => {
      isWaitingForSmcuUpload = true;
      setStatus("SMCU ID 업로드 요청 패킷 송신 중...", "busy");'''

if old_upload_btn in html:
    html = html.replace(old_upload_btn, new_upload_btn)
    print("Step 2c: smcuUploadBtn updated to set isWaitingForSmcuUpload = true.")
else:
    print("Warning: old_upload_btn pattern not found.")

# In smcuDownloadBtn.onclick: ensure isWaitingForSmcuUpload = false
old_download_btn = '''  const smcuDownloadBtn = document.getElementById('smcuDownloadBtn');
  if (smcuDownloadBtn) {
    smcuDownloadBtn.onclick = async () => {
      if (smcuDownloadBtn.disabled) return;
      smcuDownloadBtn.disabled = true;
      try {
        const { id, cs, packet } = updateSmcuCalculations();'''

new_download_btn = '''  const smcuDownloadBtn = document.getElementById('smcuDownloadBtn');
  if (smcuDownloadBtn) {
    smcuDownloadBtn.onclick = async () => {
      if (smcuDownloadBtn.disabled) return;
      smcuDownloadBtn.disabled = true;
      isWaitingForSmcuUpload = false;
      try {
        const { id, cs, packet } = updateSmcuCalculations();'''

if old_download_btn in html:
    html = html.replace(old_download_btn, new_download_btn)
    print("Step 2d: smcuDownloadBtn updated to ensure isWaitingForSmcuUpload = false.")
else:
    print("Warning: old_download_btn pattern not found.")

# In simulateResponse: remove 0xDE echo triggering handleIncomingBytes
old_sim = '''      // SMCU download / TCP download echo (0xDE)
      else if (txPacket[3] === 0xDE) {
        handleIncomingBytes(txPacket);
      }'''

new_sim = '''      // SMCU download echo (0xDE) - 다운로드 시에는 업로드 결과 화면을 갱신하지 않음
      else if (txPacket[3] === 0xDE) {
        // Download acknowledged, do not update upload banner
      }'''

if old_sim in html:
    html = html.replace(old_sim, new_sim)
    print("Step 2e: simulateResponse updated for 0xDE.")

# ------------------------------------------------------------------------------
# 3. Save to all targets
# ------------------------------------------------------------------------------
with open('public/index.html', 'w', encoding='utf-8') as f:
    f.write(html)
with open('index.html', 'w', encoding='utf-8') as f:
    f.write(html)
with open('app/src/main/assets/index.html', 'w', encoding='utf-8') as f:
    f.write(html)

print("All targets written successfully.")
