package com.example

import com.example.ble.BleProtocol
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BleProtocolUnitTest {

    @Test
    fun testDefaultTcpIpPacketMatchesSpecification() {
        val packet = BleProtocol.buildTcpIpDownloadPacket(
            ip = "192.168.10.1",
            gw = "192.168.10.1",
            sub = "255.255.255.0",
            mac = "AA. AA. AA. AA. AA. AA"
        )

        val hexString = BleProtocol.toHexString(packet)
        val expectedHexString = "02 63 FB DE 00 15 12 00 00 C0 A8 0A 01 C0 A8 0A 01 FF FF FF 00 AA AA AA AA AA AA BE 03 03 FC FC"
        
        assertEquals(expectedHexString, hexString)
        assertEquals(32, packet.size)

        // Verify Checksum byte at index 27 (28th byte)
        val checksum = packet[27].toInt() and 0xFF
        assertEquals(0xBE, checksum)

        // Verify 4-byte tail: 03 03 FC FC
        assertEquals(0x03.toByte(), packet[28])
        assertEquals(0x03.toByte(), packet[29])
        assertEquals(0xFC.toByte(), packet[30])
        assertEquals(0xFC.toByte(), packet[31])
    }

    @Test
    fun testXorChecksumRule() {
        // Test manual XOR calculation against computeXorChecksum
        val packet = BleProtocol.buildTcpIpDownloadPacket(
            ip = "192.168.10.20",
            gw = "192.168.10.1",
            sub = "255.255.255.0",
            mac = "12:34:56:78:9A:BC"
        )

        var manualXor = 0
        for (i in 1..26) { // 2nd byte (index 1) to 27th byte (index 26)
            manualXor = manualXor xor (packet[i].toInt() and 0xFF)
        }

        val actualChecksum = packet[27].toInt() and 0xFF
        assertEquals(manualXor, actualChecksum)
    }

    @Test
    fun testIpParsing() {
        val ipBytes = BleProtocol.parseIpAddress("192.168.10.1")
        assertArrayEquals(
            byteArrayOf(0xC0.toByte(), 0xA8.toByte(), 0x0A.toByte(), 0x01.toByte()),
            ipBytes
        )
    }

    @Test
    fun testMacParsingVariousFormats() {
        val expected = byteArrayOf(
            0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte()
        )

        assertArrayEquals(expected, BleProtocol.parseMacAddress("AA.BB.CC.DD.EE.FF"))
        assertArrayEquals(expected, BleProtocol.parseMacAddress("AA. BB. CC. DD. EE. FF"))
        assertArrayEquals(expected, BleProtocol.parseMacAddress("AA:BB:CC:DD:EE:FF"))
        assertArrayEquals(expected, BleProtocol.parseMacAddress("AA-BB-CC-DD-EE-FF"))
        assertArrayEquals(expected, BleProtocol.parseMacAddress("AABBCCDDEEFF"))
    }

    @Test
    fun testSmcuDownloadChecksumWithXor() {
        val packetId1 = BleProtocol.buildSmcuDownloadPacket(1)
        val cs1 = packetId1[27].toInt() and 0xFF
        assertEquals(0xBE, cs1)

        val packetId2 = BleProtocol.buildSmcuDownloadPacket(2)
        val cs2 = packetId2[27].toInt() and 0xFF
        assertEquals(0xBD, cs2)
    }

    @Test
    fun testTcpIpUploadPacketMatchesSpecification() {
        val uploadPacket = BleProtocol.TCP_IP_UPLOAD_PACKET
        val hexString = BleProtocol.toHexString(uploadPacket)
        val expectedHexString = "02 63 FB BE 00 03 01 00 00 24 03 03 FC FC"

        assertEquals(expectedHexString, hexString)
        assertEquals(14, uploadPacket.size)

        // Verify Tail
        assertEquals(0x03.toByte(), uploadPacket[10])
        assertEquals(0x03.toByte(), uploadPacket[11])
        assertEquals(0xFC.toByte(), uploadPacket[12])
        assertEquals(0xFC.toByte(), uploadPacket[13])
    }

    @Test
    fun testTcpIpResponseParsing() {
        val packet = BleProtocol.buildTcpIpDownloadPacket(
            ip = "192.168.1.100",
            gw = "192.168.1.1",
            sub = "255.255.0.0",
            mac = "11. 22. 33. 44. 55. 66"
        )
        val parsed = BleProtocol.parseTcpIpPacket(packet)
        assertNotNull(parsed)
        assertEquals("192.168.1.100", parsed!!.ip)
        assertEquals("192.168.1.1", parsed.gw)
        assertEquals("255.255.0.0", parsed.sub)
        assertEquals("11. 22. 33. 44. 55. 66", parsed.mac)
    }

    @Test
    fun testSmartRelayStatusRequestPacketMatchesSpecification() {
        val packet = BleProtocol.SMART_RELAY_STATUS_REQUEST_PACKET
        val hexString = BleProtocol.toHexString(packet)
        // 7번째 데이터 01 -> 80 변경, Checksum 0x3A
        val expectedHexString = "02 63 FB 21 00 03 80 00 00 3A 03 03 FC FC"

        assertEquals(expectedHexString, hexString)
        assertEquals(14, packet.size)

        // 7번째 데이터 (index 6): 0x80 (128개 릴레이 상태 요청)
        assertEquals(0x80.toByte(), packet[6])

        // Verify Checksum byte at index 9 (0x3A)
        val checksum = packet[9].toInt() and 0xFF
        assertEquals(0x3A, checksum)

        // Verify XOR Checksum from index 1 to 8
        var manualXor = 0
        for (i in 1..8) {
            manualXor = manualXor xor (packet[i].toInt() and 0xFF)
        }
        assertEquals(manualXor, checksum)

        // Verify 4-byte tail: 03 03 FC FC
        assertEquals(0x03.toByte(), packet[10])
        assertEquals(0x03.toByte(), packet[11])
        assertEquals(0xFC.toByte(), packet[12])
        assertEquals(0xFC.toByte(), packet[13])
    }

    @Test
    fun test128RelayStatusParsingByte10AsRelay0WithFfOnAnd00Off() {
        // Construct a 142-byte response packet:
        // Header: 02 FB 63 21 00 83 80 00 00 (9 bytes, indices 0..8)
        // 10th byte (index 9) to index 136: 128 relay bytes (index 9 is Relay 0)
        // Checksum (index 137)
        // Tail: 03 03 FC FC (indices 138..141)
        val packet = ByteArray(142)
        packet[0] = 0x02.toByte()
        packet[1] = 0xFB.toByte()
        packet[2] = 0x63.toByte()
        packet[3] = 0x21.toByte()
        packet[4] = 0x00.toByte()
        packet[5] = 0x83.toByte() // length
        packet[6] = 0x80.toByte() // 128 relays
        packet[7] = 0x00.toByte() // offset H
        packet[8] = 0x00.toByte() // offset L

        // Set Relay 0 (index 9) to 0xFF (ON)
        packet[9] = 0xFF.toByte()
        // Set Relay 1 (index 10) to 0x00 (OFF)
        packet[10] = 0x00.toByte()
        // Set Relay 2 (index 11) to 0xFF (ON)
        packet[11] = 0xFF.toByte()
        // Set Relay 127 (index 136) to 0xFF (ON)
        packet[136] = 0xFF.toByte()

        // Tail
        packet[137] = 0x00.toByte() // Checksum placeholder
        packet[138] = 0x03.toByte()
        packet[139] = 0x03.toByte()
        packet[140] = 0xFC.toByte()
        packet[141] = 0xFC.toByte()

        // Verify Relay 0 is at index 9 (10th byte)
        val relay0 = (packet[9].toInt() and 0xFF) == 0xFF
        val relay1 = (packet[10].toInt() and 0xFF) == 0xFF
        val relay2 = (packet[11].toInt() and 0xFF) == 0xFF
        val relay127 = (packet[136].toInt() and 0xFF) == 0xFF

        assertTrue(relay0)
        assertFalse(relay1)
        assertTrue(relay2)
        assertTrue(relay127)
    }

    @Test
    fun testSmartRelayStartPacketStructureWithBytes8And9And11thByteFF() {
        val testId = 0x0105 // 261 (High byte 0x01, Low byte 0x05)
        val packet = BleProtocol.buildSmartRelayStartPacket(testId)

        assertEquals(16, packet.size)
        // 1st byte (index 0): STX 0x02
        assertEquals(0x02.toByte(), packet[0])
        // 2nd~7th: 63 FB DC 00 05 01
        assertEquals(0x63.toByte(), packet[1])
        assertEquals(0xFB.toByte(), packet[2])
        assertEquals(0xDC.toByte(), packet[3])
        assertEquals(0x00.toByte(), packet[4])
        assertEquals(0x05.toByte(), packet[5])
        assertEquals(0x01.toByte(), packet[6])

        // 8th byte (index 7): ID High byte (0x01)
        assertEquals(0x01.toByte(), packet[7])
        // 9th byte (index 8): ID Low byte (0x05)
        assertEquals(0x05.toByte(), packet[8])

        // 10th byte (index 9): 0x00
        assertEquals(0x00.toByte(), packet[9])
        // 11th byte (index 10): 0xFF (Start 요청)
        assertEquals(0xFF.toByte(), packet[10])

        // 12th byte (index 11): Checksum XOR from index 1 to 10
        var expectedChecksum = 0
        for (i in 1..10) {
            expectedChecksum = expectedChecksum xor (packet[i].toInt() and 0xFF)
        }
        assertEquals(expectedChecksum.toByte(), packet[11])

        // 13th~16th bytes (index 12..15): Tail 03 03 FC FC
        assertEquals(0x03.toByte(), packet[12])
        assertEquals(0x03.toByte(), packet[13])
        assertEquals(0xFC.toByte(), packet[14])
        assertEquals(0xFC.toByte(), packet[15])
    }

    @Test
    fun testSmartRelayStartPacketDefaultConstant() {
        val startPacket = BleProtocol.SMART_RELAY_START_PACKET
        val hexString = BleProtocol.toHexString(startPacket)
        val expectedHexString = "02 63 FB DC 00 05 01 00 00 00 FF BF 03 03 FC FC"

        assertEquals(expectedHexString, hexString)
        assertEquals(16, startPacket.size)

        // 11th byte (index 10) = 0xFF
        assertEquals(0xFF.toByte(), startPacket[10])
        // 12th byte (index 11) = 0xBF
        assertEquals(0xBF.toByte(), startPacket[11])
    }

    @Test
    fun testSmartRelayStopPacketMatchesSpecification() {
        val stopPacket = BleProtocol.SMART_RELAY_STOP_PACKET
        val hexString = BleProtocol.toHexString(stopPacket)
        val expectedHexString = "02 63 FB DC 00 05 01 00 00 00 00 40 03 03 FC FC"

        assertEquals(expectedHexString, hexString)
        assertEquals(16, stopPacket.size)

        // 11th byte (index 10) = 0x00
        assertEquals(0x00.toByte(), stopPacket[10])
        // 12th byte (index 11) = 0x40
        assertEquals(0x40.toByte(), stopPacket[11])
    }

    @Test
    fun testSmartRelayStreamPacketParsingBytes10And11() {
        val mockRxPacket = byteArrayOf(
            0x02.toByte(), 0xFB.toByte(), 0x63.toByte(), 0xDC.toByte(),
            0x00.toByte(), 0x04.toByte(), 0x01.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x02.toByte(), 0x0B.toByte(), 0x40.toByte(),
            0x03.toByte(), 0x03.toByte(), 0xFC.toByte(), 0xFC.toByte()
        )

        val result = BleProtocol.parseSmartRelayStreamData(mockRxPacket)
        assertNotNull(result)
        // 10th byte (index 9) = 0x02
        assertEquals(0x02, result!!.byte10)
        // 11th byte (index 10) = 0x0B
        assertEquals(0x0B, result.byte11)
        // Value = (2 << 8) | 11 = 523
        assertEquals(523, result.value)
        assertEquals("02 0B", result.hexString)
    }

    @Test
    fun testSmartRelayPeriodicStreamPacketMatchesSpecification() {
        val packet = BleProtocol.SMART_RELAY_PERIODIC_STREAM_PACKET
        val hexString = BleProtocol.toHexString(packet)
        val expectedHexString = "02 63 FB DD 00 03 01 00 00 47 03 03 FC FC"

        assertEquals(expectedHexString, hexString)
        assertEquals(14, packet.size)

        // Verify Checksum: 0x63 ^ 0xFB ^ 0xDD ^ 0x00 ^ 0x03 ^ 0x01 ^ 0x00 ^ 0x00 = 0x47
        var checksum = 0
        for (i in 1..8) {
            checksum = checksum xor (packet[i].toInt() and 0xFF)
        }
        assertEquals(0x47, checksum)
        assertEquals(0x47.toByte(), packet[9])
    }
}
