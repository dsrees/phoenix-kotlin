package com.github.dsrees.phoenix

import com.github.dsrees.phoenix.message.IncomingMessage
import com.github.dsrees.phoenix.message.OutgoingMessage
import com.github.dsrees.phoenix.message.PayloadType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

interface TransportSerializer {
    fun encode(message: OutgoingMessage): String

    fun binaryEncode(message: OutgoingMessage): ByteArray

    fun decode(text: String): IncomingMessage

    fun binaryDecode(data: ByteArray): IncomingMessage
}

class PhoenixTransportSerializer(
    private val json: Json = Json,
) : TransportSerializer {
    companion object {
        private const val HEADER_LENGTH: Int = 1
        private const val META_LENGTH: Int = 4

        private const val KIND_PUSH: UInt = 0u
        private const val KIND_REPLY: UInt = 1u
        private const val KIND_BROADCAST: UInt = 2u
    }

    override fun encode(message: OutgoingMessage): String =
        when (message.payload) {
            is PayloadType.Binary -> throw IllegalArgumentException("Expected JSON. Got ByteArray. use `binaryEncode`")
            is PayloadType.Json -> {
                buildJsonArray {
                    add(JsonPrimitive(message.joinRef))
                    add(JsonPrimitive(message.ref))
                    add(JsonPrimitive(message.topic))
                    add(JsonPrimitive(message.event))
                    add(message.payload.element)
                }.toString()
            }
        }

    override fun binaryEncode(message: OutgoingMessage): ByteArray {
        var byteArray = byteArrayOf()

        // Add the KIND, which is always a PUSH from the client to the server
        byteArray += KIND_PUSH.toByte()

        val joinRefByteArray = message.joinRef?.encodeToByteArray()
        val refByteArray = message.ref?.encodeToByteArray()
        val topicByteArray = message.topic.encodeToByteArray()
        val eventByteArray = message.event.encodeToByteArray()

        // Add the lengths of each piece of the message
        byteArray += (joinRefByteArray?.size ?: 0).toByte()
        byteArray += (refByteArray?.size ?: 0).toByte()
        byteArray += topicByteArray.size.toByte()
        byteArray += eventByteArray.size.toByte()

        joinRefByteArray?.let { byteArray += it }
        refByteArray?.let { byteArray += it }
        byteArray += (topicByteArray)
        byteArray += (eventByteArray)

        when (message.payload) {
            is PayloadType.Binary -> byteArray += message.payload.data
            is PayloadType.Json -> {
                message.payload.element
                    .toString()
                    .encodeToByteArray()
                    .also { byteArray += it }
            }
        }

        return byteArray
    }

    override fun decode(text: String): IncomingMessage {
        val unKeyedJsonArray = json.parseToJsonElement(text).jsonArray
        val (joinRef, ref, topic, event, payload) = unKeyedJsonArray

        return when {
            event == JsonPrimitive(ChannelEvent.REPLY) -> {
                val status = payload.jsonObject["status"]
                val response =
                    payload.jsonObject["response"]
                        ?: throw IllegalArgumentException("Invalid reply structure. Expected response key. $text")

                buildIncomingMessage(
                    joinRef = joinRef.jsonPrimitive.contentOrNull,
                    ref = ref.jsonPrimitive.contentOrNull,
                    topic = topic.jsonPrimitive.content,
                    event = ChannelEvent.REPLY,
                    status = status?.toString(),
                    payload = PayloadType.Json(response),
                    rawText = text,
                )
            }

            joinRef != JsonNull || ref != JsonNull -> {
                buildIncomingMessage(
                    joinRef = joinRef.jsonPrimitive.contentOrNull,
                    ref = ref.jsonPrimitive.contentOrNull,
                    topic = topic.jsonPrimitive.content,
                    event = event.jsonPrimitive.content,
                    payload = PayloadType.Json(payload),
                    rawText = text,
                )
            }

            else ->
                buildIncomingMessage(
                    topic = topic.jsonPrimitive.content,
                    event = event.jsonPrimitive.content,
                    payload = PayloadType.Json(payload),
                    rawText = text,
                )
        }
    }

    override fun binaryDecode(data: ByteArray): IncomingMessage =
        when (val byte = data[0].toUByte().toUInt()) {
            KIND_PUSH -> decodePush(data)
            KIND_REPLY -> decodeReply(data)
            KIND_BROADCAST -> decodeBroadcast(data)
            else -> throw IllegalArgumentException("Expected binary data to include a KIND of push, reply, or broadcast. Got $byte")
        }

    private fun decodePush(buffer: ByteArray): IncomingMessage {
        val joinRefSize = buffer[1].toInt()
        val topicSize = buffer[2].toInt()
        val eventSize = buffer[3].toInt()
        var offset = HEADER_LENGTH + META_LENGTH - 1 // pushes have no ref

        val joinRef =
            buffer
                .sliceArray(offset..<offset + joinRefSize)
                .decodeToString()
        offset += joinRefSize

        val topic =
            buffer
                .sliceArray(offset..<offset + topicSize)
                .decodeToString()
        offset += topicSize

        val event =
            buffer
                .sliceArray(offset..<offset + eventSize)
                .decodeToString()
        offset += eventSize

        val data = buffer.sliceArray(offset..<buffer.count())
        return buildIncomingMessage(
            joinRef = joinRef,
            topic = topic,
            event = event,
            payload = PayloadType.Binary(data),
            rawBinary = buffer,
        )
    }

    private fun decodeReply(buffer: ByteArray): IncomingMessage {
        val joinRefSize = buffer[1].toInt()
        val refSize = buffer[2].toInt()
        val topicSize = buffer[3].toInt()
        val eventSize = buffer[4].toInt()
        var offset = HEADER_LENGTH + META_LENGTH

        val joinRef =
            buffer
                .sliceArray(offset..<offset + joinRefSize)
                .decodeToString()
        offset += joinRefSize

        val ref =
            buffer
                .sliceArray(offset..<offset + refSize)
                .decodeToString()
        offset += refSize

        val topic =
            buffer
                .sliceArray(offset..<offset + topicSize)
                .decodeToString()
        offset += topicSize

        val event =
            buffer
                .sliceArray(offset..<offset + eventSize)
                .decodeToString()
        offset += eventSize

        val data = buffer.sliceArray(offset..<buffer.count())
        return buildIncomingMessage(
            joinRef = joinRef,
            ref = ref,
            topic = topic,
            event = ChannelEvent.REPLY,
            status = event,
            payload = PayloadType.Binary(data),
            rawBinary = buffer,
        )
    }

    private fun decodeBroadcast(buffer: ByteArray): IncomingMessage {
        val topicSize = buffer[1].toInt()
        val eventSize = buffer[2].toInt()
        var offset = HEADER_LENGTH + 2

        val topic =
            buffer
                .sliceArray(offset..<offset + topicSize)
                .decodeToString()
        offset += topicSize

        val event =
            buffer
                .sliceArray(offset..<offset + eventSize)
                .decodeToString()
        offset += eventSize

        val data = buffer.sliceArray(offset..<buffer.count())
        return buildIncomingMessage(
            topic = topic,
            event = event,
            payload = PayloadType.Binary(data),
            rawBinary = buffer,
        )
    }

    private fun buildIncomingMessage(
        joinRef: String? = null,
        ref: String? = null,
        topic: String,
        event: String,
        status: String? = null,
        payload: PayloadType,
        rawText: String? = null,
        rawBinary: ByteArray? = null,
    ): IncomingMessage =
        IncomingMessage(
            joinRef = joinRef,
            ref = ref,
            topic = topic,
            event = event,
            status = status,
            payload = payload,
            rawText = rawText,
            rawBinary = rawBinary,
        )
}
