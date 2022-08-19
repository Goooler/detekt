package io.gitlab.arturbosch.detekt.internal

import io.gitlab.arturbosch.detekt.DetektPlugin
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import java.io.File

internal class DetektJvm(private val project: Project) {
    fun registerTasks(extension: DetektExtension) {
        (project.extensions.getByName("kotlin") as KotlinJvmProjectExtension).sourceSets.all { sourceSet ->
            project.registerJvmDetektTask(extension, sourceSet.kotlin, sourceSet.name)
            project.registerJvmCreateBaselineTask(extension, sourceSet.kotlin, sourceSet.name)
        }
    }

    private fun Project.registerJvmDetektTask(extension: DetektExtension, sourceSet: SourceDirectorySet, name: String) {
        registerDetektTask(DetektPlugin.DETEKT_TASK_NAME + name.capitalize(), extension) {
            source = sourceSet
            classpath.setFrom(sourceSet.srcDirs.existingFiles())
            // If a baseline file is configured as input file, it must exist to be configured, otherwise the task fails.
            // We try to find the configured baseline or alternatively a specific variant matching this task.
            extension.baseline?.existingVariantOrBaseFile(name)?.let { baselineFile ->
                baseline.set(layout.file(project.provider { baselineFile }))
            }
            setReportOutputConventions(reports, extension, name)
            description = "EXPERIMENTAL: Run detekt analysis for $name classes with type resolution"
        }
    }

    private fun Project.registerJvmCreateBaselineTask(
        extension: DetektExtension,
        sourceSet: SourceDirectorySet,
        name: String
    ) {
        registerCreateBaselineTask(DetektPlugin.BASELINE_TASK_NAME + name.capitalize(), extension) {
            source = sourceSet
            classpath.setFrom(sourceSet.srcDirs.existingFiles())
            val variantBaselineFile = extension.baseline?.addVariantName(name)
            baseline.set(project.layout.file(project.provider { variantBaselineFile }))
            description = "EXPERIMENTAL: Creates detekt baseline for $name classes with type resolution"
        }
    }

    private fun Set<File>.existingFiles() = filter { it.exists() }
}
