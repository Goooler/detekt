package dev.detekt.generator

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import kotlin.io.path.Path
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GeneratorCliSpec {
    @Nested
    inner class TextReplacements {

        private fun parse(vararg args: String): ParsedOptions {
            val parser = TestParser()
            parser.parse(listOf("-i", ".", *args))
            return parser.toOptions()
        }

        @Test
        fun noReplacements() {
            val options = parse()

            assertThat(options.textReplacements).isEmpty()
        }

        @Test
        fun simpleReplacement() {
            val options = parse("--replace", "foo=bar")

            val expected = mapOf("foo" to "bar")
            assertThat(options.textReplacements).containsExactlyEntriesOf(expected)
        }

        @Test
        fun simpleReplacementShortcut() {
            val options = parse("-r", "foo=bar")

            val expected = mapOf("foo" to "bar")
            assertThat(options.textReplacements).containsExactlyEntriesOf(expected)
        }

        @Test
        fun emptyReplacementValue() {
            val options = parse("--replace", "foo=")

            val expected = mapOf("foo" to "")
            assertThat(options.textReplacements).containsExactlyEntriesOf(expected)
        }

        @Test
        fun multipleReplacements() {
            val options = parse("--replace", "foo=bar", "--replace", "faz=baz")

            val expected = mapOf(
                "foo" to "bar",
                "faz" to "baz"
            )
            assertThat(options.textReplacements).containsExactlyEntriesOf(expected)
        }
    }
}

private class TestParser : CliktCommand(name = "test") {
    override val printHelpOnEmptyArgs: Boolean = false
    private val input by option("--input", "-i").multiple(required = true)
    private val replacements by option("--replace", "-r").associate()

    override fun run() = Unit

    fun toOptions(): ParsedOptions = ParsedOptions(
        inputPaths = input.flatMap { it.split(',', ';') }.filter { it.isNotBlank() }.map(::Path),
        textReplacements = replacements,
    )
}

private data class ParsedOptions(
    val inputPaths: List<java.nio.file.Path>,
    val textReplacements: Map<String, String>,
)
