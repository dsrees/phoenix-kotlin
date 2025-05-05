package com.github.dsrees.phoenix

interface TransportSerializer {
    fun encode(): String

    fun binaryEncode(): ByteArray

    fun decode(text: String)

    fun binaryDecode(data: ByteArray): IncomingMessage
}

class PhoenixTransportSerializer : TransportSerializer {
    companion object {
        private const val HEADER_LENGTH: Int = 1
        private const val META_LENGTH: Int = 4

        private const val KIND_PUSH: UInt = 0u
        private const val KIND_REPLY: UInt = 1u
        private const val KIND_BROADCAST: UInt = 2u
    }

    override fun encode(): String {
        TODO("Not yet implemented")
    }

    override fun binaryEncode(): ByteArray {
        TODO("Not yet implemented")
    }

    override fun decode(text: String) {
//        json.fromstring

        TODO("Not yet implemented")
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
            payload = data,
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
            payload = data,
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
            payload = data,
            rawBinary = buffer,
        )
    }

    private fun buildIncomingMessage(
        joinRef: String? = null,
        ref: String? = null,
        topic: String,
        event: String,
        status: String? = null,
        payload: ByteArray,
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
