/*
 * Copyright (c) Jonathan Colmenares
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package me.jonathing.gradle.decompiler

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.HasConfigurableAttributes
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory

import static me.jonathing.gradle.decompiler.DecompilerPlugin.LOGGER

/**
 * The extension for the decompiler plugin. This is used to configure the decompiler and add dependencies to be
 * decompiled by the JAR generator.
 *
 * @see DecompiledJarGenerator
 */
@CompileStatic
final class DecompilerExtension {
    /** The name of the extension. */
    public static final String NAME = 'decompiler'

    private final DependencyHandler dependencies

    /** The maven coordinate of the decompiler to be used. */
    String decompiler = Constants.VINEFLOWER_ARTIFACT
    /** Whether the decompiler should add a comment banner to the decompiled sources. */
    public boolean useCommentBanner = true
    /** Whether the decompiler should strip non-class files from the decompiled sources. */
    public boolean strip = true

    @PackageScope DecompilerExtension(Project project, ObjectFactory objects, ProviderFactory providers) {
        this.dependencies = project.dependencies.tap {
            LOGGER.debug 'Adding the decompiled attribute to project: {}', project
            attributesSchema.attribute Constants.ATTRIBUTE_TRANSFORMED
            artifactTypes.named(ArtifactTypeDefinition.JAR_TYPE) { jar ->
                jar.attributes.attribute Constants.ATTRIBUTE_TRANSFORMED, false
            }
        }

        project.afterEvaluate { p ->
            LOGGER.debug 'Registering the decompiler artifact transform for project: {}', p
            p.dependencies.registerTransform(DecompiledJarGenerator) { spec ->
                spec.parameters { parameters ->
                    LOGGER.debug 'Using decompiler: {}', this.decompiler
                    parameters.decompilerName.set this.decompiler
                    parameters.decompilerArtifact.set objects.fileProperty().fileProvider(providers.provider {
                        p.configurations.detachedConfiguration(
                            p.dependencies.create(this.decompiler)
                        ).singleFile
                    })
                    parameters.useCommentBanner.set this.useCommentBanner
                    parameters.strip.set this.strip
                }

                // This is how we tell Gradle that this is a JAR to be decompiled, so it can use our transformer.
                spec.from.attribute Constants.ATTRIBUTE_TRANSFORMED, false
                spec.to.attribute Constants.ATTRIBUTE_TRANSFORMED, true
            }
        }
    }

    /**
     * Adds a dependency to be decompiled.
     *
     * @param value   The dependency (notation)
     * @param closure The configuring closure for the dependency
     * @return The dependency that was added
     */
    Dependency dep(
        def value,
        @DelegatesTo(Dependency)
        @ClosureParams(value = SimpleType, options = 'org.gradle.api.artifacts.Dependency')
            Closure<Void> closure = {}
    ) {
        this.dependencies.create(value, closure).tap { dependency ->
            if (dependency instanceof HasConfigurableAttributes<?>) {
                dependency.attributes {
                    it.attribute Constants.ATTRIBUTE_TRANSFORMED, true
                }
            } else {
                throw new IllegalArgumentException('Decompilable dependency must be a module or have configurable attributes')
            }
        }
    }
}
