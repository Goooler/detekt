package dev.detekt.cli

import dev.detekt.tooling.api.AnalysisMode
import dev.detekt.tooling.api.spec.RulesSpec
import dev.detekt.tooling.api.spec.RulesSpec.FailurePolicy.FailOnSeverity
import dev.detekt.tooling.api.spec.RulesSpec.FailurePolicy.NeverFail
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import java.net.URL
import java.nio.file.Path

data class CliArgs(
    val inputPaths: List<Path>,
    val analysisMode: AnalysisMode,
    val includes: String?,
    val excludes: String?,
    val config: List<Path>,
    val configResource: List<URL>,
    val generateConfig: Path?,
    val plugins: List<Path>,
    val parallel: Boolean,
    val baseline: Path?,
    val createBaseline: Boolean,
    val reportPaths: List<ReportPath>,
    val failOnSeverity: FailureSeverity,
    val basePath: Path,
    val disableDefaultRuleSets: Boolean,
    val buildUponDefaultConfig: Boolean,
    val allRules: Boolean,
    val autoCorrect: Boolean,
    val debug: Boolean,
    val runRule: String?,
    val classpath: List<Path>,
    val apiVersion: ApiVersion?,
    val languageVersion: LanguageVersion?,
    val jvmTarget: JvmTarget,
    val jdkHome: Path?,
    val showVersion: Boolean,
    val freeCompilerArgs: List<String>,
) {
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
}
