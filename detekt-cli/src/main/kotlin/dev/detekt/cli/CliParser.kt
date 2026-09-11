package dev.detekt.cli

import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.core.parse

fun parseArguments(args: Array<out String>): CliArgs {
    val cli = CliArgs()

    try {
        cli.parse(args.toList())
    } catch (@Suppress("SwallowedException") ex: PrintHelpMessage) {
        throw HelpRequest(cli.getFormattedHelp(ex).orEmpty())
    } catch (@Suppress("SwallowedException") ex: CliktError) {
        throw HandledArgumentViolation(ex.message, cli.getFormattedHelp(ex).orEmpty())
    }

    return cli
}
