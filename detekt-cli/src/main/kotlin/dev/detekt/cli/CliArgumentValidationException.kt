package dev.detekt.cli

class CliArgumentValidationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
