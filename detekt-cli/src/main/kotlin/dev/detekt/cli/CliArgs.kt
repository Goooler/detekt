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
import kotlin.io.path.Path

class CliArgs {
    var inputPaths: List<Path> = listOf(Path(System.getProperty("user.dir")))
    var analysisMode: AnalysisMode = AnalysisMode.light
    var includes: String? = null
    var excludes: String? = null
    var config: List<Path> = emptyList()
    var configResource: List<URL> = emptyList()
    var generateConfig: Path? = null
    var plugins: List<Path> = emptyList()
    var parallel: Boolean = false
    var baseline: Path? = null
    var createBaseline: Boolean = false
    var reportPaths: List<ReportPath> = emptyList()
    var failOnSeverity: FailureSeverity = FailureSeverity.Error
    var basePath: Path = Path(System.getProperty("user.dir"))
    var disableDefaultRuleSets: Boolean = false
    var buildUponDefaultConfig: Boolean = false
    var allRules: Boolean = false
    var autoCorrect: Boolean = false
    var debug: Boolean = false
    var runRule: String? = null
    var classpath: List<Path> = emptyList()
    var apiVersion: ApiVersion? = null
    var languageVersion: LanguageVersion? = null
    var jvmTarget: JvmTarget = JvmTarget.DEFAULT
    var jdkHome: Path? = null
    var showVersion: Boolean = false
    var freeCompilerArgs: List<String> = mutableListOf()

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
