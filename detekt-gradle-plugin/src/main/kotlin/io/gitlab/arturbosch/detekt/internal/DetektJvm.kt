package io.gitlab.arturbosch.detekt.internal

import io.gitlab.arturbosch.detekt.DetektPlugin
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

internal class DetektJvm(private val project: Project) {
    fun registerTasks(extension: DetektExtension) {
        (project.extensions.getByName("kotlin") as KotlinProjectExtension).sourceSets.all { sourceSet ->
            project.registerJvmDetektTask(extension, sourceSet.kotlin)
            project.registerJvmCreateBaselineTask(extension, sourceSet.kotlin)
        }
    }

    private fun Project.registerJvmDetektTask(extension: DetektExtension, sourceSet: SourceDirectorySet) {
        registerDetektTask(DetektPlugin.DETEKT_TASK_NAME + sourceSet.name.capitalize(), extension) {
            source = sourceSet
            source(sourceSet.sourceDirectories.existingFiles())
            classpath.setFrom(sourceSet.destinationDirectory.asFileTree.existingFiles())
            // If a baseline file is configured as input file, it must exist to be configured, otherwise the task fails.
            // We try to find the configured baseline or alternatively a specific variant matching this task.
            extension.baseline?.existingVariantOrBaseFile(sourceSet.name)?.let { baselineFile ->
                baseline.set(layout.file(project.provider { baselineFile }))
            }
            setReportOutputConventions(reports, extension, sourceSet.name)
            description = "EXPERIMENTAL: Run detekt analysis for ${sourceSet.name} classes with type resolution"
        }
    }

    private fun Project.registerJvmCreateBaselineTask(extension: DetektExtension, sourceSet: SourceDirectorySet) {
        registerCreateBaselineTask(DetektPlugin.BASELINE_TASK_NAME + sourceSet.name.capitalize(), extension) {
            source = sourceSet
            source(sourceSet.sourceDirectories.existingFiles())
            classpath.setFrom(sourceSet.destinationDirectory.asFileTree.existingFiles())
            val variantBaselineFile = extension.baseline?.addVariantName(sourceSet.name)
            baseline.set(project.layout.file(project.provider { variantBaselineFile }))
            description = "EXPERIMENTAL: Creates detekt baseline for ${sourceSet.name} classes with type resolution"
        }
    }

    private fun FileCollection.existingFiles() = filter { it.exists() }
}
