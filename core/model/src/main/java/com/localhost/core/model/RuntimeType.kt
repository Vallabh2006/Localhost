package com.localhost.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class RuntimeType(val displayName: String, val defaultPort: Int, val defaultEntryFile: String) {
    STATIC("Static HTML/JS", 8081, "index.html"),
    PHP("PHP Built-in", 8000, "index.php"),
    NODEJS("Node.js", 3000, "server.js"),
    JAVA("Java (Embedded)", 8088, "app.jar"),
    PYTHON("Python", 5000, "app.py")
}
