package com.example.ble

/**
 * BLE Hex Communication Protocol Engine
 * Handles packet construction, checksum computation, and stream parsing.
 */
object BleProtocol {

    const val DEFAULT_IP = "192.168.10.1"
    const val DEFAULT_GW = "192.168.10.1"
    const val DEFAULT_SUB = "255.255.255.0"
    const val DEFAULT_MAC = "AA.AA.AA.AA.AA.AA"

    val TAIL_BYTES = byteArrayOf(
        0x03.toByte(), 0x03.toByte(), 0xFC.toByte(), 0xFC.toByte()
    )

    /**
     * Compute XOR Checksum:
     * 패킷 2번째(index 1, 0x63)부터 27번째(index 26, MAC 마지막 바이트)까지 XOR 하여 28번째에 넣음
     */
    fun computeXorChecksum(bytes: ByteArray, startIndex: Int = 1, endIndex: Int = 26): Byte {
        var xorVal = 0
        val safeEnd = minOf(endIndex, bytes.size - 1)
        for (i in startIndex..safeEnd) {
            xorVal = xorVal xor (bytes[i].toInt() and 0xFF)
        }
        return (xorVal and 0xFF).toByte()
    }

    /**
     * Parse IPv4 string (e.g. "192.168.10.1") to 4 bytes
     */
    fun parseIpAddress(ipStr: String): ByteArray {
        val defaultBytes = byteArrayOf(192.toByte(), 168.toByte(), 10.toByte(), 1.toByte())
        val parts = ipStr.trim().split('.').filter { it.isNotBlank() }
        if (parts.size != 4) return defaultBytes

        val result = ByteArray(4)
        for (i in 0 until 4) {
            val num = parts[i].trim().toIntOrNull() ?: return defaultBytes
            if (num !in 0..255) return defaultBytes
            result[i] = num.toByte()
        }
        return result
    }

    /**
     * Parse MAC address string (e.g. "AA.AA.AA.AA.AA.AA", "AA:AA:AA:AA:AA:AA", or "AA AA AA AA AA AA") to 6 bytes
     */
    fun parseMacAddress(macStr: String): ByteArray {
        val defaultBytes = ByteArray(6) { 0xAA.toByte() }
        // Split by dots, colons, hyphens, or spaces
        val parts = macStr.trim().split(Regex("[.:\\-\\s]+")).filter { it.isNotBlank() }
        if (parts.size == 6) {
            val result = ByteArray(6)
            for (i in 0 until 6) {
                val hexVal = parts[i].toIntOrNull(16) ?: return defaultBytes
                if (hexVal !in 0..255) return defaultBytes
                result[i] = hexVal.toByte()
            }
            return result
        }

        // If continuous hex string (e.g. "AAAAAAAAAAAA")
        val clean = macStr.replace(Regex("[^0-9a-fA-F]"), "")
        if (clean.length == 12) {
            val result = ByteArray(6)
            for (i in 0 until 6) {
                val byteHex = clean.substring(i * 2, (i + 1) * 2)
                result[i] = (byteHex.toIntOrNull(16) ?: 0xAA).toByte()
            }
            return result
        }

        return defaultBytes
    }

    /**
     * Build 32-byte TCP/IP ID Download packet
     * Format:
     * 02 63 FB DE 00 15 12 00 00 [IP:4B] [GW:4B] [SUB:4B] [MAC:6B] [Checksum:1B] 03 03 FC FC
     * Checksum: 패킷 2번째(63)부터 27번째(AA)까지 XOR
     */
    fun buildTcpIpDownloadPacket(
        ip: String = DEFAULT_IP,
        gw: String = DEFAULT_GW,
        sub: String = DEFAULT_SUB,
        mac: String = DEFAULT_MAC
    ): ByteArray {
        val ipBytes = parseIpAddress(ip)
        val gwBytes = parseIpAddress(gw)
        val subBytes = parseIpAddress(sub)
        val macBytes = parseMacAddress(mac)

        val packet = ByteArray(32)
        // 1st byte (index 0): STX
        packet[0] = 0x02.toByte()
        // 2nd ~ 9th bytes (index 1..8): Command header
        packet[1] = 0x63.toByte()
        packet[2] = 0xFB.toByte()
        packet[3] = 0xDE.toByte()
        packet[4] = 0x00.toByte()
        packet[5] = 0x15.toByte() // Length (0x15 = 21)
        packet[6] = 0x12.toByte()
        packet[7] = 0x00.toByte()
        packet[8] = 0x00.toByte()

        // 10th ~ 13th bytes (index 9..12): IP Address
        System.arraycopy(ipBytes, 0, packet, 9, 4)
        // 14th ~ 17th bytes (index 13..16): Gateway (GW)
        System.arraycopy(gwBytes, 0, packet, 13, 4)
        // 18th ~ 21st bytes (index 17..20): Subnet Mask (SUB)
        System.arraycopy(subBytes, 0, packet, 17, 4)
        // 22nd ~ 27th bytes (index 21..26): MAC Address
        System.arraycopy(macBytes, 0, packet, 21, 6)

        // 28th byte (index 27): Checksum (XOR from 2nd byte (index 1) to 27th byte (index 26))
        packet[27] = computeXorChecksum(packet, startIndex = 1, endIndex = 26)

        // 29th ~ 32nd bytes (index 28..31): Tail 03 03 FC FC
        packet[28] = 0x03.toByte()
        packet[29] = 0x03.toByte()
        packet[30] = 0xFC.toByte()
        packet[31] = 0xFC.toByte()

        return packet
    }

    /**
     * 14-byte TCP/IP ID Upload Request Packet
     * Fixed packet:
     * 02 63 FB BE 00 03 01 00 00 24 03 03 FC FC
     */
    val TCP_IP_UPLOAD_PACKET = byteArrayOf(
        0x02.toByte(), 0x63.toByte(), 0xFB.toByte(), 0xBE.toByte(),
        0x00.toByte(), 0x03.toByte(), 0x01.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x24.toByte(), 0x03.toByte(), 0x03.toByte(),
        0xFC.toByte(), 0xFC.toByte()
    )

    /**
     * 14-byte SMCU ID Upload Request Packet
     * Fixed packet:
     * 02 63 FB BE 00 03 01 00 00 24 03 03 FC FC
     */
    val SMCU_ID_UPLOAD_PACKET = byteArrayOf(
        0x02.toByte(), 0x63.toByte(), 0xFB.toByte(), 0xBE.toByte(),
        0x00.toByte(), 0x03.toByte(), 0x01.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x24.toByte(), 0x03.toByte(), 0x03.toByte(),
        0xFC.toByte(), 0xFC.toByte()
    )

    data class TcpIpParsedSettings(
        val ip: String,
        val gw: String,
        val sub: String,
        val mac: String
    )

    /**
     * Parse TCP/IP settings from a 32-byte response packet
     */
    fun parseTcpIpPacket(packet: ByteArray): TcpIpParsedSettings? {
        if (packet.size >= 27) {
            val ip = "${packet[9].toInt() and 0xFF}.${packet[10].toInt() and 0xFF}.${packet[11].toInt() and 0xFF}.${packet[12].toInt() and 0xFF}"
            val gw = "${packet[13].toInt() and 0xFF}.${packet[14].toInt() and 0xFF}.${packet[15].toInt() and 0xFF}.${packet[16].toInt() and 0xFF}"
            val sub = "${packet[17].toInt() and 0xFF}.${packet[18].toInt() and 0xFF}.${packet[19].toInt() and 0xFF}.${packet[20].toInt() and 0xFF}"
            val mac = (21..26).joinToString(". ") { "%02X".format(packet[it].toInt() and 0xFF) }
            return TcpIpParsedSettings(ip, gw, sub, mac)
        }
        return null
    }

    /**
     * Build 32-byte SMCU ID Download packet
     * - ID: 0 ~ 255
     * - Byte 13 (Index 12): ID in hex
     * - Byte 28 (Index 27): Checksum (XOR from index 1 to 26)
     */
    fun buildSmcuDownloadPacket(id: Int): ByteArray {
        val clampedId = id.coerceIn(0, 255)

        val packet = byteArrayOf(
            0x02.toByte(), 0x63.toByte(), 0xFB.toByte(), 0xDE.toByte(),
            0x00.toByte(), 0x15.toByte(), 0x12.toByte(), 0x00.toByte(),
            0x00.toByte(), 0xC0.toByte(), 0xA8.toByte(), 0x0A.toByte(),
            clampedId.toByte(), // Index 12 (13th byte)
            0xC0.toByte(), 0xA8.toByte(), 0x0A.toByte(), 0x01.toByte(),
            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x00.toByte(),
            0xAA.toByte(), 0xAA.toByte(), 0xAA.toByte(), 0xAA.toByte(),
            0xAA.toByte(), 0xAA.toByte(),
            0x00.toByte(),       // Placeholder for Index 27 (28th byte)
            0x03.toByte(), 0x03.toByte(), 0xFC.toByte(), 0xFC.toByte()
        )

        // Calculate XOR Checksum from index 1 to 26
        packet[27] = computeXorChecksum(packet, startIndex = 1, endIndex = 26)
        return packet
    }

    /**
     * Parse SMCU ID from received packet (13th byte, Index 12)
     */
    fun parseSmcuId(packet: ByteArray): Int? {
        if (packet.size >= 13) {
            return packet[12].toInt() and 0xFF
        }
        return null
    }

    /**
     * Build 16-byte SmartRelay Start (스트림 시작) Packet
     * Format: 02 63 FB DC 00 05 01 [8번째: ID_H] [9번째: ID_L] [10번째: 00] [11번째: FF] [Checksum] 03 03 FC FC
     * 11번째 바이트: 0xFF
     * Checksum: XOR from 2nd byte (index 1, 0x63) to 11th byte (index 10, 0xFF)
     * Tail: 03 03 FC FC
     */
    fun buildSmartRelayStartPacket(id: Int): ByteArray {
        val clampedId = id.coerceIn(0, 65535)
        val packet = ByteArray(16)
        packet[0] = 0x02.toByte()
        packet[1] = 0x63.toByte()
        packet[2] = 0xFB.toByte()
        packet[3] = 0xDC.toByte()
        packet[4] = 0x00.toByte()
        packet[5] = 0x05.toByte()
        packet[6] = 0x01.toByte()
        // 8번째 (Index 7) & 9번째 (Index 8) 바이트에 ID 데이터(숫자) 배치
        packet[7] = ((clampedId shr 8) and 0xFF).toByte()
        packet[8] = (clampedId and 0xFF).toByte()
        // 10번째 (Index 9)
        packet[9] = 0x00.toByte()
        // 11번째 (Index 10) -> Start 패킷: 0xFF
        packet[10] = 0xFF.toByte()
        // 12번째 (Index 11): 2번째(index 1)부터 11번째(index 10)까지 XOR Checksum
        packet[11] = computeXorChecksum(packet, startIndex = 1, endIndex = 10)
        // Tail
        packet[12] = 0x03.toByte()
        packet[13] = 0x03.toByte()
        packet[14] = 0xFC.toByte()
        packet[15] = 0xFC.toByte()
        return packet
    }

    /**
     * Build 16-byte SmartRelay Stop (스트림 중지) Packet
     * Format: 02 63 FB DC 00 05 01 [8번째: ID_H] [9번째: ID_L] [10번째: 00] [11번째: 00] [Checksum] 03 03 FC FC
     * 11번째 바이트: 0x00
     * Checksum: XOR from 2nd byte (index 1, 0x63) to 11th byte (index 10, 0x00)
     * Tail: 03 03 FC FC
     */
    fun buildSmartRelayStopPacket(id: Int = 0): ByteArray {
        val clampedId = id.coerceIn(0, 65535)
        val packet = ByteArray(16)
        packet[0] = 0x02.toByte()
        packet[1] = 0x63.toByte()
        packet[2] = 0xFB.toByte()
        packet[3] = 0xDC.toByte()
        packet[4] = 0x00.toByte()
        packet[5] = 0x05.toByte()
        packet[6] = 0x01.toByte()
        // 8번째 (Index 7) & 9번째 (Index 8) 바이트에 ID 데이터(숫자) 배치
        packet[7] = ((clampedId shr 8) and 0xFF).toByte()
        packet[8] = (clampedId and 0xFF).toByte()
        // 10번째 (Index 9)
        packet[9] = 0x00.toByte()
        // 11번째 (Index 10) -> Stop 패킷: 0x00
        packet[10] = 0x00.toByte()
        // 12번째 (Index 11): 2번째(index 1)부터 11번째(index 10)까지 XOR Checksum
        packet[11] = computeXorChecksum(packet, startIndex = 1, endIndex = 10)
        // Tail
        packet[12] = 0x03.toByte()
        packet[13] = 0x03.toByte()
        packet[14] = 0xFC.toByte()
        packet[15] = 0xFC.toByte()
        return packet
    }

    /**
     * Backward-compatible alias for buildSmartRelayStartPacket
     */
    fun buildSmartRelayStreamPacket(id: Int): ByteArray {
        return buildSmartRelayStartPacket(id)
    }

    /**
     * Backward-compatible alias for buildSmartRelayStartPacket
     */
    fun buildSmartRelaySetIdPacket(id: Int): ByteArray {
        return buildSmartRelayStartPacket(id)
    }

    /**
     * SmartRelay Start Packet (16 bytes, ID 0)
     * 02 63 FB DC 00 05 01 00 00 00 FF BF 03 03 FC FC
     * 11번째 바이트 = 0xFF, Checksum = 0xBF
     */
    val SMART_RELAY_START_PACKET = byteArrayOf(
        0x02.toByte(), 0x63.toByte(), 0xFB.toByte(), 0xDC.toByte(),
        0x00.toByte(), 0x05.toByte(), 0x01.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), 0xBF.toByte(),
        0x03.toByte(), 0x03.toByte(), 0xFC.toByte(), 0xFC.toByte()
    )

    /**
     * SmartRelay Stop Packet (16 bytes, ID 0)
     * 02 63 FB DC 00 05 01 00 00 00 00 40 03 03 FC FC
     * 11번째 바이트 = 0x00, Checksum = 0x40
     */
    val SMART_RELAY_STOP_PACKET = byteArrayOf(
        0x02.toByte(), 0x63.toByte(), 0xFB.toByte(), 0xDC.toByte(),
        0x00.toByte(), 0x05.toByte(), 0x01.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x40.toByte(),
        0x03.toByte(), 0x03.toByte(), 0xFC.toByte(), 0xFC.toByte()
    )

    /**
     * SmartRelay 1-second Periodic Transmission Packet (14 bytes)
     * 02 63 FB DD 00 03 01 00 00 47 03 03 FC FC
     * XOR Checksum: 0x63 ^ 0xFB ^ 0xDD ^ 0x00 ^ 0x03 ^ 0x01 ^ 0x00 ^ 0x00 = 0x47
     * Tail: 03 03 FC FC
     */
    val SMART_RELAY_PERIODIC_STREAM_PACKET = byteArrayOf(
        0x02.toByte(), 0x63.toByte(), 0xFB.toByte(), 0xDD.toByte(),
        0x00.toByte(), 0x03.toByte(), 0x01.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x47.toByte(), 0x03.toByte(), 0x03.toByte(),
        0xFC.toByte(), 0xFC.toByte()
    )

    /**
     * SmartRelay Status Request Packet (14 bytes)
     * 02 63 FB 21 00 03 80 00 00 3A 03 03 FC FC
     * 7번째 데이터 01 -> 80 (128개 릴레이 상태 요청)
     * XOR Checksum: 0x63 ^ 0xFB ^ 0x21 ^ 0x00 ^ 0x03 ^ 0x80 ^ 0x00 ^ 0x00 = 0x3A
     * Tail: 03 03 FC FC
     */
    val SMART_RELAY_STATUS_REQUEST_PACKET = byteArrayOf(
        0x02.toByte(), 0x63.toByte(), 0xFB.toByte(), 0x21.toByte(),
        0x00.toByte(), 0x03.toByte(), 0x80.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x3A.toByte(), 0x03.toByte(), 0x03.toByte(),
        0xFC.toByte(), 0xFC.toByte()
    )

    data class SmartRelayStreamResult(
        val value: Int,
        val byte10: Int,
        val byte11: Int,
        val hexString: String
    )

    /**
     * Parse SmartRelay real-time streaming data (10th, 11th bytes, Index 9 and 10)
     * Example: 02 FB 63 DC 00 04 01 00 00 [Byte10] [Byte11] 03 03 FC FC
     */
    fun parseSmartRelayStreamData(packet: ByteArray): SmartRelayStreamResult? {
        if (packet.size >= 11) {
            val byte10 = packet[9].toInt() and 0xFF
            val byte11 = packet[10].toInt() and 0xFF
            val value = (byte10 shl 8) or byte11
            val hexString = "%02X %02X".format(byte10, byte11)
            return SmartRelayStreamResult(value, byte10, byte11, hexString)
        }
        return null
    }

    /**
     * Parse SmartRelay real-time streaming ID (10, 11번째 바이트 파싱값)
     */
    fun parseSmartRelayStreamId(packet: ByteArray): Int? {
        return parseSmartRelayStreamData(packet)?.value
    }

    /**
     * Utility to format ByteArray to uppercase spaced Hex string
     */
    fun toHexString(bytes: ByteArray): String {
        return bytes.joinToString(" ") { "%02X".format(it) }
    }
}
