package dev.detekt.generator

import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.core.parse
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class GeneratorArgsSpec {

    @ParameterizedTest
    @ValueSource(strings = ["-h", "--help"])
    fun `throws PrintHelpMessage with full usage text on help flag`(helpFlag: String) {
        val args = GeneratorArgs()
        assertThatThrownBy { args.parse(listOf(helpFlag)) }
            .isInstanceOf(PrintHelpMessage::class.java)
            .extracting { args.getFormattedHelp(it as PrintHelpMessage)?.trim() }
            .isEqualTo(expectedHelp)
    }
}

private val expectedHelp = """
    |Usage: detekt-generator [<options>]
    |
    |Options:
    |  -i, --input=<path>                    Input paths to analyze.
    |  -d, --documentation=<path>            Output path for generated documentation.
    |  -c, --config=<path>                   Output path for generated detekt config.
    |  -gcrc, --generate-custom-rule-config  Generate config for user-defined rules. Path to user rules can be specified with --input option
    |  -h, --help                            Show this message and exit
""".trimMargin()
