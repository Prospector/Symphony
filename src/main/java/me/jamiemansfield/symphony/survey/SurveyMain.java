//******************************************************************************
// Copyright (c) Jamie Mansfield <https://jamiemansfield.me/>
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.
//******************************************************************************

package me.jamiemansfield.symphony.survey;

import static java.util.Arrays.asList;

import com.google.common.io.ByteStreams;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import me.jamiemansfield.lorenz.MappingSet;
import me.jamiemansfield.lorenz.io.parser.SrgReader;
import me.jamiemansfield.lorenz.model.Mapping;
import me.jamiemansfield.symphony.analysis.InheritanceMap;
import me.jamiemansfield.symphony.analysis.SourceSet;
import me.jamiemansfield.symphony.util.PathValueConverter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.tree.ClassNode;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

/**
 * The Main-Class behind Survey, a simple remapping tool.
 */
public final class SurveyMain {

    public static void main(final String[] args) {
        final OptionParser parser = new OptionParser();

        final OptionSpec<Void> helpSpec = parser.acceptsAll(asList("?", "help"), "Show the help")
                .forHelp();

        final OptionSpec<Path> jarInPathSpec = parser.accepts("jarIn", "The location of the jar to map")
                .withRequiredArg()
                .withValuesConvertedBy(PathValueConverter.INSTANCE);
        final OptionSpec<Path> mappingsPathSpec = parser.accepts("mappings", "The location of the mappings")
                .withRequiredArg()
                .withValuesConvertedBy(PathValueConverter.INSTANCE);
        final OptionSpec<Path> jarOutPathSpec = parser.accepts("jarOut", "Where to save the mapped jar")
                .withRequiredArg()
                .withValuesConvertedBy(PathValueConverter.INSTANCE);

        final OptionSet options;
        try {
            options = parser.parse(args);
        } catch (final OptionException ex) {
            System.err.println("Failed to parse OptionSet! Exiting...");
            ex.printStackTrace(System.err);
            System.exit(-1);
            return;
        }

        if (options == null || options.has(helpSpec)) {
            try {
                parser.printHelpOn(System.err);
            } catch (final IOException ex) {
                System.err.println("Failed to print help information!");
                ex.printStackTrace(System.err);
            }
            System.exit(-1);
            return;
        }

        final Path jarInPath = options.valueOf(jarInPathSpec);
        final Path mappingsPath = options.valueOf(mappingsPathSpec);
        final Path jarOutPath = options.valueOf(jarOutPathSpec);

        if (!(Files.exists(jarInPath) && Files.exists(mappingsPath))) {
            throw new RuntimeException("Jar in, mappings, or both do not exist!");
        }

        final MappingSet mappings = new MappingSet();

        try (final SrgReader reader = new SrgReader(new BufferedReader(new InputStreamReader(Files.newInputStream(mappingsPath))))) {
            reader.parse(mappings);
        }
        catch (final IOException ex) {
            ex.printStackTrace();
        }

        try (final JarFile jarFile = new JarFile(jarInPath.toFile())) {
            final SourceSet sources = new SourceSet();
            final InheritanceMap inheritanceMap = new InheritanceMap(sources);

            jarFile.stream()
                    // Filter out directories
                    .filter(entry -> !entry.isDirectory())
                    // I only want to get classes
                    .filter(entry -> entry.getName().endsWith(".class"))
                    // Now to read the class
                    .forEach(entry -> {
                        try (final InputStream in = jarFile.getInputStream(entry)) {
                            final ClassReader reader = new ClassReader(ByteStreams.toByteArray(in));
                            final ClassNode node = new ClassNode();
                            reader.accept(node, 0);
                            sources.add(node);
                        } catch (final IOException ex) {
                            System.err.println("Failed to get an input stream for " + entry.getName() + "!");
                            ex.printStackTrace(System.err);
                        }
                    });

            try (final JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarOutPath))) {
                for (final JarEntry entry : jarFile.stream().collect(Collectors.toSet())) {
                    try (final InputStream is = jarFile.getInputStream(entry)) {
                        if (!entry.getName().endsWith(".class")) {
                            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            ByteStreams.copy(is, baos);

                            jos.putNextEntry(new JarEntry(entry.getName()));
                            jos.write(baos.toByteArray());
                        }
                    }
                }

                for (final ClassNode node : sources.getClasses()) {
                    final ClassNode newNode = new ClassNode();
                    node.accept(new ClassRemapper(newNode, new SurveyRemapper(mappings, inheritanceMap)));

                    final ClassWriter writer = new ClassWriter(0);
                    newNode.accept(writer);

                    final String name = mappings.getClassMapping(newNode.name)
                            .map(Mapping::getFullDeobfuscatedName)
                            .orElse(newNode.name);

                    jos.putNextEntry(new JarEntry(name + ".class"));
                    jos.write(writer.toByteArray());
                }
            }
            catch (final IOException ex) {
                ex.printStackTrace();
            }
        } catch (final IOException ex) {
            System.err.println("Failed to read the jar file!");
            ex.printStackTrace(System.err);
        }
    }

    private SurveyMain() {
    }

}
