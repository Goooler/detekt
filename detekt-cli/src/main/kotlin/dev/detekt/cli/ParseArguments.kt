package dev.detekt.cli

import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.core.UsageError
import kotlin.io.path.isRegularFile
import kotlin.io.path.notExists

fun parseArguments(args: Array<out String>): CliArgs {
    val cli = CliArgs()

    try {
        cli.parse(args.toList())
    } catch (e: PrintHelpMessage) {
        throw HelpRequest(cli.getFormattedHelp())
    } catch (e: UsageError) {
        throw HandledArgumentViolation(e.message ?: "", cli.getFormattedHelp())
    }

    cli.validate()
    return cli
}

private fun CliArgs.validate() {
    var violation: String? = null
    val baseline = baseline

    if (createBaseline && baseline == null) {
        violation = "Creating a baseline.xml requires the --baseline parameter to specify a path."
    }

    if (!createBaseline && baseline != null) {
        if (baseline.notExists()) {
            violation = "The file specified by --baseline should exist '$baseline'."
        } else if (!baseline.isRegularFile()) {
            violation = "The path specified by --baseline should be a file '$baseline'."
        }
    }

    if (violation != null) {
        throw HandledArgumentViolation(violation, getFormattedHelp())
    }
}
