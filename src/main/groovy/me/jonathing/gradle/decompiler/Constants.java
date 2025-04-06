/*
 * Copyright (c) Jonathan Colmenares
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package me.jonathing.gradle.decompiler;

import org.gradle.api.attributes.Attribute;

interface Constants {
    Attribute<Boolean> ATTRIBUTE_TRANSFORMED = Attribute.of("me.jonathing.gradle.decompiled", Boolean.class);

    String VINEFLOWER_ARTIFACT = "org.vineflower:vineflower:1.11.1";
}
