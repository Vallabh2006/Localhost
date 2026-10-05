package com.localhost.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class RestartPolicy {
    NEVER,
    ON_CRASH,
    ALWAYS
}
