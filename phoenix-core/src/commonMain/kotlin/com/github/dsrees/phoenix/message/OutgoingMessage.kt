package com.github.dsrees.phoenix.message

/**
 * Defines a message that originates from the client and is being sent to the Server.
 *
 * The message will be encoded into following shape before being sent
 *
 *     [join_ref,ref,topic,event,payload]
 *
 */
data class OutgoingMessage(
    val joinRef: String?,
    val ref: String?,
    val topic: String,
    val event: String,
    val payload: PayloadType,
)
