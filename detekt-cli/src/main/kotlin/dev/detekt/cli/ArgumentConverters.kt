package dev.detekt.cli

import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import java.io.File
import java.net.URL
import java.nio.file.InvalidPathException
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

internal fun parseApiVersion(value: String): ApiVersion {
    val languageVersion = LanguageVersion.fromFullVersionString(value)
    requireNotNull(languageVersion) {
        val validValues = LanguageVersion.entries.joinToString { it.toString() }
        "\"$value\" passed to --api-version, expected one of [$validValues]"
    }
    return ApiVersion.createByLanguageVersion(languageVersion)
}

internal fun parseLanguageVersion(value: String): LanguageVersion =
    requireNotNull(LanguageVersion.fromFullVersionString(value)) {
        val validValues = LanguageVersion.entries.joinToString { it.toString() }
        "\"$value\" passed to --language-version, expected one of [$validValues]"
    }

internal fun parseJvmTarget(value: String): JvmTarget =
    checkNotNull(JvmTarget.fromString(value)) {
        val validValues = JvmTarget.entries.joinToString { it.toString() }
        "Invalid value passed to --jvm-target, expected one of [$validValues]"
    }

internal fun parseClasspathResource(resource: String): URL {
    val relativeResource = if (resource.startsWith("/")) resource else "/$resource"
    return object {}.javaClass.getResource(relativeResource)
        ?: throw CliArgumentValidationException("Classpath resource '$resource' does not exist!")
}

internal fun parseFailureSeverity(value: String): FailureSeverity = FailureSeverity.fromString(value)

internal fun parseReportPath(value: String): ReportPath =
    try {
        ReportPath.from(value)
    } catch (e: IllegalArgumentException) {
        throw CliArgumentValidationException(e.message.orEmpty(), e)
    }

internal fun splitOnCommaOrSemicolon(raw: String): List<String> = raw.split(',', ';').filter { it.isNotBlank() }

internal fun splitOnSystemPathSeparator(raw: String): List<String> =
    raw.split(File.pathSeparatorChar).filter { it.isNotBlank() }

internal fun parsePath(value: String): Path =
    try {
        Path(value)
    } catch (e: InvalidPathException) {
        throw CliArgumentValidationException(e.message.orEmpty(), e)
    }

internal fun validateExistingPaths(name: String, value: List<Path>) {
    value.forEach {
        if (!it.exists()) throw CliArgumentValidationException("Path '$it' passed to $name does not exist.")
    }
}

internal fun validateDirectory(name: String, value: Path) {
    if (!value.isDirectory()) throw CliArgumentValidationException("Value passed to $name must be a directory.")
}
