package com.github.dsrees.phoenix

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform