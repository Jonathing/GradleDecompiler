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

@CompileStatic
final class DecompilerExtension {
    public static final String NAME = 'decompiler'

    private final DependencyHandler dependencies
    String decompiler = Constants.VINEFLOWER_ARTIFACT

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
                spec.parameters.decompiler.set objects.fileProperty().fileProvider(providers.provider {
                    LOGGER.debug 'Using decompiler: {}', this.decompiler
                    p.configurations.detachedConfiguration(
                        p.dependencies.create(this.decompiler)
                    ).singleFile
                })

                spec.from.attribute Constants.ATTRIBUTE_TRANSFORMED, false
                spec.to.attribute Constants.ATTRIBUTE_TRANSFORMED, true
            }
        }
    }

    Dependency dep(
        def value,
        @DelegatesTo(Dependency)
        @ClosureParams(value = SimpleType, options = 'org.gradle.api.artifacts.Dependency')
            Closure<Void> closure = {}
    ) {
        this.dependencies.create(value) { Dependency dependency ->
            closure.call dependency

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
