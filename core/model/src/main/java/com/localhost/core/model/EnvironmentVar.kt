package com.localhost.core.model

import kotlinx.serialization.Serializable

@Serializable
data class EnvironmentVar(
    val key: String,
    val value: String,
    val isSecret: Boolean = false
)
