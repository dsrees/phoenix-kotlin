package com.github.dsrees.phoenix

import com.github.dsrees.phoenix.message.OutgoingMessage
import com.github.dsrees.phoenix.message.PayloadType
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PhoenixTransportSerializerTest {
    private val serializer: TransportSerializer = PhoenixTransportSerializer()

    @Test
    fun `encode - json`() {
        val outgoingMessage =
            OutgoingMessage(
                joinRef = "0",
                ref = "1",
                topic = "t",
                event = "e",
                payload =
                    PayloadType.Json(
                        buildJsonObject {
                            put("foo", JsonPrimitive(1))
                        },
                    ),
            )

        val actual = serializer.encode(outgoingMessage)
        val expected =
            """
            ["0","1","t","e",{"foo":1}]
            """.trimIndent()
        assertEquals(expected, actual)
    }

    @Test
    fun `encode - binary throws exception`() {
        val outgoingMessage =
            OutgoingMessage(
                joinRef = "0",
                ref = "1",
                topic = "t",
                event = "e",
                payload = PayloadType.Binary(byteArrayOf()),
            )
        assertFailsWith(
            IllegalArgumentException::class,
            "Expected JSON. Got ByteArray. use `binaryEncode`",
        ) {
            serializer.encode(outgoingMessage)
        }
    }

    @Test
    fun `binaryEncode - encodes data payload`() {
        // "\0\x01\x01\x01\x0101te\x01"
        val payload = byteArrayOf(0x01)
        val expectedBuffer =
            byteArrayOf(0x00, 0x01, 0x01, 0x01, 0x01)
                .plus("01te".encodeToByteArray())
                .plus(payload)

        val outgoingMessage =
            OutgoingMessage(
                joinRef = "0",
                ref = "1",
                topic = "t",
                event = "e",
                payload = PayloadType.Binary(payload),
            )

        serializer
            .binaryEncode(outgoingMessage)
            .apply {
                assertContentEquals(expectedBuffer, this)
            }
    }

    @Test
    fun `binaryEncode - encodes json payload`() {
        // "\0\x01\x01\x01\x0101te\x01"
        val data = byteArrayOf(0x7B, 0x22, 0x66, 0x6f, 0x6f, 0x22, 0x3A, 0x31, 0x7D)
        val expectedBuffer =
            byteArrayOf(0x00, 0x01, 0x01, 0x01, 0x01)
                .plus("01te".encodeToByteArray())
                // The below array is the value of {foo: 1}
                .plus(data)

        val outgoingMessage =
            OutgoingMessage(
                joinRef = "0",
                ref = "1",
                topic = "t",
                event = "e",
                payload =
                    PayloadType.Json(
                        buildJsonObject {
                            put("foo", JsonPrimitive(1))
                        },
                    ),
            )

        serializer
            .binaryEncode(outgoingMessage)
            .apply {
                assertContentEquals(expectedBuffer, this)
            }
    }

    @Test
    fun `binaryEncode - encodes variable length segments`() {
        // "\0\x02\x01\x03\x02101topev\x01"
        val data = byteArrayOf(0x01)
        val expectedBuffer =
            byteArrayOf(0x00, 0x02, 0x01, 0x03, 0x02)
                .plus("101topev".encodeToByteArray())
                .plus(data)

        val outgoingMessage =
            OutgoingMessage(
                joinRef = "10",
                ref = "1",
                topic = "top",
                event = "ev",
                payload = PayloadType.Binary(data),
            )

        serializer
            .binaryEncode(outgoingMessage)
            .apply {
                assertContentEquals(expectedBuffer, this)
            }
    }

    @Test
    fun `decode - text`() {
        val text =
            """
            ["1","2","topic","event",{"foo":"bar"}]
            """.trimIndent()

        serializer
            .decode(text)
            .apply {
                assertEquals("1", joinRef)
                assertEquals("2", ref)
                assertEquals("topic", topic)
                assertEquals("event", event)
                (payload as PayloadType.Json)
                    .element
                    .jsonObject["foo"]
                    ?.jsonPrimitive
                    ?.content
                    .apply { assertEquals("bar", this) }
                assertEquals(text, rawText)
            }
    }

    @Test
    fun `decode - reply`() {
        val text =
            """
            [null,"2", "topic","phx_reply",{"response":"foo","status":"ok"}]
            """.trimIndent()

        serializer
            .decode(text)
            .apply {
                assertNull(joinRef)
                assertEquals("2", ref)
                assertEquals("topic", topic)
                assertEquals("phx_reply", event)
                (payload as PayloadType.Json)
                    .element
                    .jsonPrimitive
                    .content
                    .apply { assertEquals("foo", this) }
                assertEquals(text, rawText)
            }
    }

    @Test
    fun `decode - broadcast`() {
        val text =
            """
            [null, null, "topic","event",{"foo": 1}]
            """.trimIndent()

        serializer
            .decode(text)
            .apply {
                assertNull(joinRef)
                assertNull(ref)
                assertEquals("topic", topic)
                assertEquals("event", event)

                (payload as PayloadType.Json)
                    .element
                    .jsonObject
                    .apply {
                        val expected =
                            buildJsonObject {
                                put("foo", JsonPrimitive(1))
                            }
                        assertEquals(expected, this)
                    }
                assertEquals(text, rawText)
            }
    }

    @Test
    fun `binaryDecode - decodePush`() {
        // "\0\x03\x03\n123topsome-event\x01\x01"
        val bin =
            byteArrayOf(0x00, 0x03, 0x03, 0x0A)
                .plus("123topsome-event".encodeToByteArray())
                .plus(byteArrayOf(0x01, 0x01))

        serializer
            .binaryDecode(bin)
            .apply {
                assertEquals("123", joinRef)
                assertNull(ref)
                assertEquals("top", topic)
                assertEquals("some-event", event)
                assertNull(status)
                assertContentEquals(byteArrayOf(0x01, 0x01), (payload as PayloadType.Binary).data)
                assertNull(rawText)
                assertEquals(bin, rawBinary)
            }
    }

    @Test
    fun `binaryDecode - decodeReply`() {
        // "\x01\x03\x02\x03\x0210012topok\x01\x01"
        val bin =
            byteArrayOf(0x01, 0x03, 0x02, 0x03, 0x02)
                .plus("10012topok".encodeToByteArray())
                .plus(byteArrayOf(0x01, 0x01))

        serializer
            .binaryDecode(bin)
            .apply {
                assertEquals("100", joinRef)
                assertEquals("12", ref)
                assertEquals("top", topic)
                assertEquals("phx_reply", event)
                assertEquals("ok", status)
                assertContentEquals(byteArrayOf(0x01, 0x01), (payload as PayloadType.Binary).data)
                assertNull(rawText)
                assertEquals(bin, rawBinary)
            }
    }

    @Test
    fun `binaryDecode - decodeBroadcast`() {
        // "\x02\x03\ntopsome-event\x01\x01"
        val bin =
            byteArrayOf(0x02, 0x03, 0x0A)
                .plus("topsome-event".encodeToByteArray())
                .plus(byteArrayOf(0x01, 0x01))

        serializer
            .binaryDecode(bin)
            .apply {
                assertNull(joinRef)
                assertNull(ref)
                assertEquals("top", topic)
                assertEquals("some-event", event)
                assertNull(status)
                assertContentEquals(byteArrayOf(1, 1), (payload as PayloadType.Binary).data)
                assertNull(rawText)
                assertEquals(bin, rawBinary)
            }
    }
}
