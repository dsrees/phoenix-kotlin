package com.github.dsrees.phoenix.message

import kotlinx.serialization.json.JsonElement

/**
 * Represents the two types of payloads that can be sent or received
 * by the client, either raw binary data or json.
 */
sealed class PayloadType {
    data class Json(
        val element: JsonElement,
    ) : PayloadType()

    class Binary(
        val data: ByteArray,
    ) : PayloadType()
}
