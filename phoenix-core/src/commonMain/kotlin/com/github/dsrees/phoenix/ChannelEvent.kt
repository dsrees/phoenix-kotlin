package com.github.dsrees.phoenix

import com.github.dsrees.phoenix.ChannelEvent.CLOSE
import com.github.dsrees.phoenix.ChannelEvent.ERROR
import com.github.dsrees.phoenix.ChannelEvent.HEARTBEAT
import com.github.dsrees.phoenix.ChannelEvent.JOIN
import com.github.dsrees.phoenix.ChannelEvent.LEAVE
import com.github.dsrees.phoenix.ChannelEvent.REPLY

object ChannelEvent {
    const val HEARTBEAT = "heartbeat"
    const val JOIN = "phx_join"
    const val LEAVE = "phx_leave"
    const val REPLY = "phx_reply"
    const val ERROR = "phx_error"
    const val CLOSE = "phx_close"
}

val String.isChannelLifecycleEvent
    get() =
        when (this) {
            HEARTBEAT, JOIN, LEAVE, REPLY, ERROR, CLOSE -> true
            else -> false
        }
