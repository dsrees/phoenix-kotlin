package com.github.dsrees.phoenix

/**
 * Defines a message dispatched from the server and received by the client.
 *
 * The serialized format from the server will be in the shape of:
 *
 *     [join_ref, ref, topic, event, payload]
 *
 * If the message was a reply from the server in response to a Push from the client,
 * then the serialized message from the server will be in the shape of:
 *
 *     [join_ref, ref, topic, null, {"status": status, "response": payload}]
 *
 * In addition to the parsed values from the server, the original messages can also be
 * accessed for additional information.
 */

data class IncomingMessage(
    /** The unique string ref when joining */
    val joinRef: String?,
    /** The unique string ref */
    val ref: String?,
    /**
     * The string topic or topic:subtopic pair namespace, for example
     * "messages", "messages:123"
     */
    val topic: String,
    /** The string event name, for example "phx_join" */
    val event: String,
    /** The reply status as a string */
    val status: String?,
    /**
     * If determined, then the data is the payload from the serializer.
     * If undetermined, then the payload must be parsed from the data
     */
    val payload: ByteArray,
    /** The raw text from the server if the message was sent as a String */
    val rawText: String?,
    /** The raw binary value from the server if the message was sent as Data */
    val rawBinary: ByteArray?,
)
