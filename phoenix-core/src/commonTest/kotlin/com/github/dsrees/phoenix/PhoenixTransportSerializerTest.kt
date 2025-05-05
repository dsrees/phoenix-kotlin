package com.github.dsrees.phoenix

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PhoenixTransportSerializerTest {
    val serializer: TransportSerializer = PhoenixTransportSerializer()

    @Test
    fun `binaryDecode - decodePush`() {
        // "\0\x03\x03\n123topsome-event\x01\x01"
        val bin =
            byteArrayOf(0, 3, 3, 10)
                .plus("123topsome-event".encodeToByteArray())
                .plus(byteArrayOf(1, 1))

        serializer
            .binaryDecode(bin)
            .apply {
                assertEquals("123", joinRef)
                assertNull(ref)
                assertEquals("top", topic)
                assertEquals("some-event", event)
                assertNull(status)
                assertContentEquals(byteArrayOf(1, 1), payload)
                assertNull(rawText)
                assertEquals(bin, rawBinary)
            }
    }

    @Test
    fun `binaryDecode - decodeReply`() {
        // "\x01\x03\x02\x03\x0210012topok\x01\x01"
        val bin =
            byteArrayOf(1, 3, 2, 3, 2)
                .plus("10012topok".encodeToByteArray())
                .plus(byteArrayOf(1, 1))

        serializer
            .binaryDecode(bin)
            .apply {
                assertEquals("100", joinRef)
                assertEquals("12", ref)
                assertEquals("top", topic)
                assertEquals("phx_reply", event)
                assertEquals("ok", status)
                assertContentEquals(byteArrayOf(1, 1), payload)
                assertNull(rawText)
                assertEquals(bin, rawBinary)
            }
    }

    @Test
    fun `binaryDecode - decodeBroadcast`() {
        // "\x02\x03\ntopsome-event\x01\x01"
        val bin =
            byteArrayOf(2, 3, 10)
                .plus("topsome-event".encodeToByteArray())
                .plus(byteArrayOf(1, 1))

        serializer
            .binaryDecode(bin)
            .apply {
                assertNull(joinRef)
                assertNull(ref)
                assertEquals("top", topic)
                assertEquals("some-event", event)
                assertNull(status)
                assertContentEquals(byteArrayOf(1, 1), payload)
                assertNull(rawText)
                assertEquals(bin, rawBinary)
            }
    }
}
