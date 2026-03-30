package dev.detekt.cli

import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.enum
import dev.detekt.tooling.api.AnalysisMode
import dev.detekt.tooling.api.spec.RulesSpec
import dev.detekt.tooling.api.spec.RulesSpec.FailurePolicy.FailOnSeverity
import dev.detekt.tooling.api.spec.RulesSpec.FailurePolicy.NeverFail
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import java.net.URL
import java.nio.file.InvalidPathException
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

class CliArgs : CliktCommand(name = "detekt") {
    override val treatUnknownOptionsAsArgs: Boolean = true

    val inputPaths: List<Path> by option(
        "--input", "-i",
        help = "Input paths to analyze. Multiple paths are separated by comma. If not specified the " +
            "current working directory is used."
    ).convert { value ->
        val segments = value.split(',', ';')
        val paths = ArrayList<Path>(segments.size)
        for (segment in segments) {
            try {
                paths.add(Path(segment))
            } catch (_: InvalidPathException) {
                fail("couldn't convert \"$segment\" to a path")
            }
        }
        paths
    }.default(listOf(Path(System.getProperty("user.dir"))))
        .validate { paths ->
            for (path in paths) {
                if (!path.exists()) fail("Input path does not exist: '$path'")
            }
        }

    val analysisMode: AnalysisMode by option(
        "--analysis-mode",
        help = "Analysis mode used by detekt. " +
            "'full' analysis mode is comprehensive but requires the correct compiler options to be provided. " +
            "'light' analysis cannot utilise compiler information and some rules cannot be run in this mode."
    ).enum<AnalysisMode>().default(AnalysisMode.light)

    val includes: String? by option(
        "--includes", "-in",
        help = "Globbing patterns describing paths to include in the analysis. " +
            "Useful in combination with 'excludes' patterns."
    )

    val excludes: String? by option(
        "--excludes", "-ex",
        help = "Globbing patterns describing paths to exclude from the analysis."
    )

    val config: List<Path> by option(
        "--config", "-c",
        help = "Path to the config file (path/to/config.yml). " +
            "Multiple configuration files can be specified with ',' or ';' as separator."
    ).convert { value ->
        val segments = value.split(',', ';')
        val paths = ArrayList<Path>(segments.size)
        for (segment in segments) {
            try {
                paths.add(Path(segment))
            } catch (_: InvalidPathException) {
                fail("couldn't convert \"$segment\" to a path")
            }
        }
        paths
    }.default(emptyList())
        .validate { paths ->
            for (path in paths) {
                if (!path.exists()) fail("Input path does not exist: '$path'")
            }
        }

    val configResource: List<URL> by option(
        "--config-resource", "-cr",
        help = "Path to the config resource on detekt's classpath (path/to/config.yml)."
    ).convert { value ->
        val segments = value.split(',', ';')
        val urls = ArrayList<URL>(segments.size)
        for (segment in segments) {
            val relativeResource = if (segment.startsWith("/")) segment else "/$segment"
            val url = CliArgs::class.java.getResource(relativeResource)
                ?: throw BadParameterValue("Classpath resource '$segment' does not exist!")
            urls.add(url)
        }
        urls
    }.default(emptyList())

    val generateConfig: Path? by option(
        "--generate-config", "-gc",
        help = "Export default config to the provided path."
    ).convert { Path(it) }

    val plugins: List<Path> by option(
        "--plugins", "-p",
        help = "Extra paths to plugin jars separated by ',' or ';'."
    ).convert { value ->
        val segments = value.split(',', ';')
        val paths = ArrayList<Path>(segments.size)
        for (segment in segments) {
            try {
                paths.add(Path(segment))
            } catch (_: InvalidPathException) {
                fail("couldn't convert \"$segment\" to a path")
            }
        }
        paths
    }.default(emptyList())
        .validate { paths ->
            for (path in paths) {
                if (!path.exists()) fail("Input path does not exist: '$path'")
            }
        }

    val parallel: Boolean by option(
        "--parallel",
        help = "Enables parallel compilation and analysis of source files." +
            " Do some benchmarks first before enabling this flag." +
            " Heuristics show performance benefits starting from 2000 lines of Kotlin code."
    ).flag()

    val baseline: Path? by option(
        "--baseline", "-b",
        help = "If a baseline xml file is passed in," +
            " only new findings not in the baseline are printed in the console."
    ).convert { Path(it) }

    val createBaseline: Boolean by option(
        "--create-baseline", "-cb",
        help = "Treats current analysis findings as a smell baseline for future detekt runs."
    ).flag()

    val reportPaths: List<ReportPath> by option(
        "--report", "-r",
        help = "Generates a report for given 'report-id' and stores it on given 'path'. " +
            "Entry should consist of: [report-id:path]. " +
            "Available 'report-id' values: 'checkstyle', 'html', 'md', 'sarif'. " +
            "These can also be used in combination with each other " +
            "e.g. '-r html:reports/detekt.html -r checkstyle:reports/detekt.xml'"
    ).convert { ReportPath.from(it) }
        .multiple()

    val failOnSeverity: FailureSeverity by option(
        "--fail-on-severity",
        help = "Specifies the minimum severity that causes the build to fail. " +
            "When the value is set to 'Never' detekt will not fail regardless of the number " +
            "of issues and their severities."
    ).convert { FailureSeverity.fromString(it) }
        .default(FailureSeverity.Error)

    val basePath: Path by option(
        "--base-path", "-bp",
        help = "Specifies a directory as the base path." +
            "Currently it impacts all file paths in the formatted reports. " +
            "File paths in console output are not affected and remain as absolute paths."
    ).convert { Path(it) }
        .default(Path(System.getProperty("user.dir")))
        .validate { path ->
            if (!path.isDirectory()) fail("Value passed to --base-path must be a directory.")
        }

    val disableDefaultRuleSets: Boolean by option(
        "--disable-default-rulesets", "-dd",
        help = "Disables default rule sets."
    ).flag()

    val buildUponDefaultConfig: Boolean by option(
        "--build-upon-default-config",
        help = "Preconfigures detekt with a bunch of rules and some opinionated defaults for you. " +
            "Allows additional provided configurations to override the defaults."
    ).flag()

    val allRules: Boolean by option(
        "--all-rules",
        help = "Activates all available (even unstable) rules."
    ).flag()

    val autoCorrect: Boolean by option(
        "--auto-correct", "-ac",
        help = "Allow rules to auto correct code if they support it. " +
            "The default rule sets do NOT support auto correcting and won't change any line in the users code base. " +
            "However custom rules can be written to support auto correcting. " +
            "The additional 'ktlint' rule set, added with '--plugins', does support it and needs this flag."
    ).flag()

    val debug: Boolean by option(
        "--debug",
        help = "Prints extra information about configurations and extensions."
    ).flag()

    val runRule: String? by option(
        "--run-rule",
        help = "Specify a rule by [RuleSet:RuleId] pattern and run it on input.",
        hidden = true
    )

    /*
        The following options are used for type resolution. When additional parameters are required the
        names should mirror the names found in this file (e.g. "classpath", "language-version", "jvm-target"):
        https://github.com/JetBrains/kotlin/blob/master/compiler/cli/cli-common/src/org/jetbrains/kotlin/cli/common/arguments/K2JVMCompilerArguments.kt
     */
    val classpath: String? by option(
        "--classpath", "-cp",
        help = "Paths where to find user class files and depending jar files. " +
            "Used for type resolution."
    )

    val apiVersion: ApiVersion? by option(
        "--api-version",
        help = "Kotlin API version used by the code under analysis. Some rules use this " +
            "information to provide more specific rule violation messages."
    ).convert { value ->
        val languageVersion = LanguageVersion.fromFullVersionString(value)
        requireNotNull(languageVersion) {
            val validValues = LanguageVersion.entries.joinToString { it.toString() }
            "\"$value\" passed to --api-version, expected one of [$validValues]"
        }
        ApiVersion.createByLanguageVersion(languageVersion)
    }

    val languageVersion: LanguageVersion? by option(
        "--language-version",
        help = "Compatibility mode for Kotlin language version X.Y, reports errors for all " +
            "language features that came out later"
    ).convert { value ->
        requireNotNull(LanguageVersion.fromFullVersionString(value)) {
            val validValues = LanguageVersion.entries.joinToString { it.toString() }
            "\"$value\" passed to --language-version, expected one of [$validValues]"
        }
    }

    val jvmTarget: JvmTarget by option(
        "--jvm-target",
        help = "Target version of the generated JVM bytecode that was generated during " +
            "compilation and is now being used for type resolution"
    ).convert { value ->
        checkNotNull(JvmTarget.fromString(value)) {
            val validValues = JvmTarget.entries.joinToString { it.toString() }
            "Invalid value passed to --jvm-target, expected one of [$validValues]"
        }
    }.default(JvmTarget.DEFAULT)

    val jdkHome: Path? by option(
        "--jdk-home",
        help = "Use a custom JDK home directory to include into the classpath"
    ).convert { Path(it) }
        .validate { path ->
            if (!path.isDirectory()) fail("Value passed to --jdk-home must be a directory.")
        }

    val showVersion: Boolean by option(
        "--version",
        help = "Prints the detekt CLI version."
    ).flag()

    val freeCompilerArgs: List<String> by argument(
        help = "Options to pass to the Kotlin compiler."
    ).multiple()

    val failurePolicy: RulesSpec.FailurePolicy
        get() {
            return when (val minSeverity = failOnSeverity) {
                FailureSeverity.Never -> NeverFail

                FailureSeverity.Error,
                FailureSeverity.Warning,
                FailureSeverity.Info,
                -> FailOnSeverity(minSeverity.toSeverity())
            }
        }

    override fun run() = Unit
}
