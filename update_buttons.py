with open('public/index.html', 'r', encoding='utf-8') as f:
    html = f.read()

# 1. Update delay buttons HTML
old_delay_html = '''<div style="display:flex; gap:6px; margin-top:8px; align-items:center; flex-wrap:wrap;">
          <span style="color:var(--text-muted); font-size:11px;">전송 간격:</span>
          <button type="button" class="chunk-delay-btn active" id="delayBtn50" onclick="setChunkDelay(50)" style="padding:4px 10px; font-size:11px; border-radius:8px; border:1px solid var(--border-color); background:var(--bg-surface-variant); color:var(--text-muted); font-weight:normal;">50ms (권장)</button>
          <button type="button" class="chunk-delay-btn" id="delayBtn80" onclick="setChunkDelay(80)" style="padding:4px 10px; font-size:11px; border-radius:8px; border:1px solid var(--border-color); background:var(--bg-surface-variant); color:var(--text-muted); cursor:pointer;">80ms (안정)</button>
          <button type="button" class="chunk-delay-btn" id="delayBtn30" onclick="setChunkDelay(30)" style="padding:4px 10px; font-size:11px; border-radius:8px; border:1px solid var(--border-color); background:var(--bg-surface-variant); color:var(--text-muted); cursor:pointer;">30ms</button>
        </div>'''

new_delay_html = '''<div style="display:flex; gap:6px; margin-top:8px; align-items:center; flex-wrap:wrap;">
          <span style="color:var(--text-muted); font-size:11px;">전송 간격:</span>
          <button type="button" class="chunk-delay-btn active" id="delayBtn35" onclick="setChunkDelay(35)" style="padding:4px 10px; font-size:11px; border-radius:8px; border:1px solid var(--mint); background:rgba(128,226,184,0.2); color:var(--mint); cursor:pointer; font-weight:700;">35ms (기본/검증)</button>
          <button type="button" class="chunk-delay-btn" id="delayBtn50" onclick="setChunkDelay(50)" style="padding:4px 10px; font-size:11px; border-radius:8px; border:1px solid var(--border-color); background:var(--bg-surface-variant); color:var(--text-muted); cursor:pointer; font-weight:normal;">50ms (권장)</button>
          <button type="button" class="chunk-delay-btn" id="delayBtn80" onclick="setChunkDelay(80)" style="padding:4px 10px; font-size:11px; border-radius:8px; border:1px solid var(--border-color); background:var(--bg-surface-variant); color:var(--text-muted); cursor:pointer; font-weight:normal;">80ms (안정)</button>
          <button type="button" class="chunk-delay-btn" id="delayBtn20" onclick="setChunkDelay(20)" style="padding:4px 10px; font-size:11px; border-radius:8px; border:1px solid var(--border-color); background:var(--bg-surface-variant); color:var(--text-muted); cursor:pointer; font-weight:normal;">20ms (고속)</button>
        </div>'''

if old_delay_html in html:
    html = html.replace(old_delay_html, new_delay_html)
    print("Delay buttons updated successfully.")
else:
    # try regex replacement
    import re
    p = r'<div style="display:flex; gap:6px; margin-top:8px; align-items:center; flex-wrap:wrap;">[\s\S]*?전송 간격:[\s\S]*?</div>\s*</div>'
    m = re.search(p, html)
    if m:
        html = html[:m.start()] + new_delay_html + '</div>' + html[m.end():]
        print("Delay buttons regex replaced.")
    else:
        print("Warning: old_delay_html pattern not found.")

# 2. Update smcuDownloadBtn & tcpIpDownloadBtn handlers with lock & status
old_handlers = '''  // Download (Send 32-byte SMCU ID Packet with BLE chunking)
  const smcuDownloadBtn = document.getElementById('smcuDownloadBtn');
  if (smcuDownloadBtn) {
    smcuDownloadBtn.onclick = async () => {
      const { id, cs, packet } = updateSmcuCalculations();
      setStatus(`SMCU ID ${id} 다운로드 패킷 전송 중...`, "busy");
      const ok = await sendPacket(packet, 'smcuLogContainer');
      if (ok) {
        setStatus(`SMCU ID ${id} 다운로드 완료 (체크섬: 0x${('0' + cs.toString(16).toUpperCase()).slice(-2)})`, "info");
      }
    };
  }'''

new_handlers = '''  // Download (Send 32-byte SMCU ID Packet with BLE 20B chunking)
  const smcuDownloadBtn = document.getElementById('smcuDownloadBtn');
  if (smcuDownloadBtn) {
    smcuDownloadBtn.onclick = async () => {
      if (smcuDownloadBtn.disabled) return;
      smcuDownloadBtn.disabled = true;
      try {
        const { id, cs, packet } = updateSmcuCalculations();
        setStatus(`SMCU ID ${id} 다운로드 패킷 (32바이트) 20B 분할 전송 중...`, "busy");
        const ok = await sendPacket(packet, 'smcuLogContainer');
        if (ok) {
          setStatus(`SMCU ID ${id} 다운로드 완료! (32바이트 분할 전송 성공, 체크섬: 0x${('0' + cs.toString(16).toUpperCase()).slice(-2)})`, "info");
        }
      } catch (err) {
        console.error("다운로드 오류:", err);
        setStatus(`다운로드 실패: ${err.message}`, "error");
      } finally {
        smcuDownloadBtn.disabled = false;
      }
    };
  }'''

if old_handlers in html:
    html = html.replace(old_handlers, new_handlers)
    print("Download handler updated successfully.")

with open('public/index.html', 'w', encoding='utf-8') as f:
    f.write(html)
with open('index.html', 'w', encoding='utf-8') as f:
    f.write(html)
with open('app/src/main/assets/index.html', 'w', encoding='utf-8') as f:
    f.write(html)

print("Files saved successfully.")
