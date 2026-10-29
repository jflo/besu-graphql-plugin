/*
 * Copyright contributors to Besu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.plugins.graphql;

import static org.hyperledger.besu.plugins.graphql.GraphQlOptions.LEGACY_OPTION_PREFIX;
import static org.hyperledger.besu.plugins.graphql.GraphQlOptions.OPTION_PREFIX;
import static org.hyperledger.besu.plugins.graphql.GraphQlOptions.canonicalName;
import static org.hyperledger.besu.plugins.graphql.GraphQlOptions.legacyName;

import org.hyperledger.besu.plugin.services.PicoCLIOptions;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.IDefaultValueProvider;
import picocli.CommandLine.Model.ArgGroupSpec;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.ParseResult;

/**
 * The command line options of the GraphQL plugin, registered as a mixin of Besu's command. The
 * options are in an argument group so that the help shows them in their own "GraphQL Options"
 * section, as before the GraphQL service became a plugin.
 *
 * <p>The options are named {@code --plugin-graphql-*}, which every Besu accepts for a plugin whose
 * namespace is {@code graphql}. When the GraphQL service was part of Besu's core, they were named
 * {@code --graphql-*}. A Besu that bundles this plugin accepts those names too, and then the
 * options are registered under both names, so that existing command lines, configuration file keys
 * and {@code BESU_GRAPHQL_*} environment variables keep working. The legacy names are deprecated:
 * using one logs a warning that names its replacement.
 *
 * <p>The legacy names come with the bundling: the plugin offers them only when Besu loaded it from
 * the directory of the plugins bundled with its distribution ({@value #BUNDLED_PLUGINS_DIRECTORY}).
 * Installed as an external plugin, it only has its {@code --plugin-graphql-*} names. The {@value
 * #LEGACY_OPTION_NAMES_PROPERTY} system property overrides this: {@code true} offers the legacy
 * names wherever the plugin is installed, {@code false} never offers them.
 *
 * <p>The plugin API has no way to ask Besu which option names it accepts, so when it offers them,
 * {@link #register(String, PicoCLIOptions)} first offers the options with both names; if Besu
 * rejects them, it registers them with the {@code --plugin-graphql-*} names only. Besu checks every
 * name before it adds any option, so a rejected attempt leaves nothing behind (Besu logs an error
 * for each name it rejects).
 */
public class GraphQLCliOptions {

  /**
   * The system property that, set to {@code false}, keeps the plugin from offering the deprecated
   * {@code --graphql-*} names to Besu, and set to {@code true}, makes it offer them even when it is
   * not bundled with Besu. When it is not set, the plugin offers them only when it is bundled.
   */
  public static final String LEGACY_OPTION_NAMES_PROPERTY =
      "besu.plugin.graphql.legacy-option-names";

  /**
   * The directory, under its home directory, that a Besu bundling this plugin loads it from. The
   * plugin offers its deprecated {@code --graphql-*} names only when it is loaded from there.
   */
  public static final String BUNDLED_PLUGINS_DIRECTORY = "bundled-plugins";

  private static final Logger LOG = LoggerFactory.getLogger(GraphQLCliOptions.class);

  private static final String ENV_VAR_PREFIX = "BESU_";

  @SuppressWarnings({"FieldCanBeFinal", "FieldMayBeFinal"}) // PicoCLI may replace the group object
  @CommandLine.ArgGroup(validate = false, heading = "@|bold GraphQL Options|@%n")
  private GraphQlOptions graphQlOptions = new GraphQlOptions();

  private final Supplier<Map<String, String>> environment;
  private final Supplier<String> legacyOptionNamesProperty;
  private final BooleanSupplier bundled;

  // The spec of the options as they were added to Besu's command.
  private CommandSpec registeredSpec;
  private boolean legacyNamesRegistered;

  /** Default constructor */
  public GraphQLCliOptions() {
    this(
        System::getenv,
        () -> System.getProperty(LEGACY_OPTION_NAMES_PROPERTY),
        () -> isInBundledPluginsDirectory(GraphQLCliOptions.class));
  }

  /**
   * Creates the options with the given sources for the environment, the {@value
   * #LEGACY_OPTION_NAMES_PROPERTY} property and whether the plugin is bundled with Besu.
   *
   * @param environment the environment variables Besu reads options from
   * @param legacyOptionNamesProperty the value of the {@value #LEGACY_OPTION_NAMES_PROPERTY}
   *     property, null if it is not set
   * @param bundled whether Besu loaded the plugin from the plugins bundled with its distribution
   */
  GraphQLCliOptions(
      final Supplier<Map<String, String>> environment,
      final Supplier<String> legacyOptionNamesProperty,
      final BooleanSupplier bundled) {
    this.environment = environment;
    this.legacyOptionNamesProperty = legacyOptionNamesProperty;
    this.bundled = bundled;
  }

  /**
   * Whether the given class was loaded from a jar in a directory named {@value
   * #BUNDLED_PLUGINS_DIRECTORY}, the directory of the plugins bundled with Besu's distribution.
   *
   * @param type the class
   * @return true if the class comes from the bundled plugins directory
   */
  static boolean isInBundledPluginsDirectory(final Class<?> type) {
    try {
      final CodeSource codeSource = type.getProtectionDomain().getCodeSource();
      if (codeSource == null || codeSource.getLocation() == null) {
        return false;
      }
      return isInBundledPluginsDirectory(Path.of(codeSource.getLocation().toURI()));
    } catch (final URISyntaxException | RuntimeException e) {
      LOG.debug("Cannot tell where the GraphQL plugin was loaded from", e);
      return false;
    }
  }

  /**
   * Whether the given jar is in a directory named {@value #BUNDLED_PLUGINS_DIRECTORY}.
   *
   * @param jar the path of the jar
   * @return true if the jar is in the bundled plugins directory
   */
  static boolean isInBundledPluginsDirectory(final Path jar) {
    final Path directory = jar.toAbsolutePath().getParent();
    return directory != null
        && directory.getFileName() != null
        && BUNDLED_PLUGINS_DIRECTORY.equals(directory.getFileName().toString());
  }

  private boolean offersLegacyNames() {
    final String property = legacyOptionNamesProperty.get();
    if ("false".equalsIgnoreCase(property)) {
      return false;
    }
    return "true".equalsIgnoreCase(property) || bundled.getAsBoolean();
  }

  /**
   * Returns the GraphQL options. Picocli replaces the options object when it parses a GraphQL
   * option, so callers must not keep the returned object.
   *
   * @return the GraphQL options
   */
  public GraphQlOptions getGraphQlOptions() {
    return graphQlOptions;
  }

  /**
   * Adds the options to Besu's command, with their deprecated {@code --graphql-*} names as well if
   * the plugin is bundled with Besu (or the {@value #LEGACY_OPTION_NAMES_PROPERTY} property asks
   * for them) and Besu accepts them.
   *
   * @param namespace the namespace of the plugin's options
   * @param picoCLIOptions Besu's command line options service
   * @return true if the options were also registered with their deprecated names
   * @throws IllegalStateException if the options are already registered, or if Besu added some of
   *     the options before it failed
   * @throws RuntimeException if Besu rejects the options with their {@code --plugin-graphql-*}
   *     names
   */
  public synchronized boolean register(
      final String namespace, final PicoCLIOptions picoCLIOptions) {
    if (registeredSpec != null) {
      throw new IllegalStateException("The GraphQL options are already registered");
    }
    if (offersLegacyNames()) {
      final CommandSpec withLegacyNames = withLegacyNames(CommandSpec.forAnnotatedObject(this));
      try {
        picoCLIOptions.addPicoCLIOptions(namespace, withLegacyNames);
        registeredSpec = withLegacyNames;
        legacyNamesRegistered = true;
        return true;
      } catch (final RuntimeException e) {
        if (isAddedToAnotherCommand(withLegacyNames)) {
          // Besu accepted the names but failed while adding the options, for example because it
          // has options with the same names: the rejected attempt is not clean, so it is fatal.
          throw new IllegalStateException(
              "The GraphQL options could not be added to Besu's command line: " + e.getMessage(),
              e);
        }
        LOG.info(
            "This Besu does not accept the deprecated {}* option names for the GraphQL plugin ({}),"
                + " so only the {}* names are available; errors logged about the {}* names can be"
                + " ignored. Start Besu with -D{}=false to skip this check.",
            LEGACY_OPTION_PREFIX,
            e.getMessage(),
            OPTION_PREFIX,
            LEGACY_OPTION_PREFIX,
            LEGACY_OPTION_NAMES_PROPERTY);
      }
    }
    final CommandSpec spec = CommandSpec.forAnnotatedObject(this);
    picoCLIOptions.addPicoCLIOptions(namespace, spec);
    registeredSpec = spec;
    legacyNamesRegistered = false;
    return false;
  }

  /**
   * Whether the options were registered with their deprecated {@code --graphql-*} names as well.
   *
   * @return true if the deprecated names are accepted
   */
  public synchronized boolean isLegacyNamesRegistered() {
    return legacyNamesRegistered;
  }

  /**
   * Returns the command line of the command these options were added to.
   *
   * @return the command line, or null if the options were not added to a command
   */
  public synchronized CommandLine getCommandLine() {
    if (registeredSpec == null) {
      return null;
    }
    final CommandSpec ownSpec = registeredSpec;
    return ownSpec.options().stream()
        .map(OptionSpec::command)
        .filter(command -> command != null && command != ownSpec)
        .map(CommandSpec::commandLine)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  /**
   * Returns the name the user gave an option: its deprecated {@code --graphql-*} name if the value
   * was given under that name (on the command line, in the configuration file or in the
   * environment), its {@code --plugin-graphql-*} name otherwise.
   *
   * @param optionName the {@code --plugin-graphql-*} name of the option
   * @return the name of the option as the user gave it
   */
  public synchronized String nameInUse(final String optionName) {
    final CommandLine commandLine = getCommandLine();
    if (!legacyNamesRegistered || commandLine == null) {
      return optionName;
    }
    final OptionSpec option = registeredSpec.findOption(optionName);
    if (option == null) {
      return optionName;
    }
    return legacyUse(option, commandLine).isPresent() ? legacyName(optionName) : optionName;
  }

  /**
   * Logs a warning for each deprecated name the user gave an option, naming its replacement. When
   * the options do not have their deprecated names (the plugin is not bundled with Besu, or Besu
   * does not accept them), logs a warning for each {@code BESU_GRAPHQL_*} environment variable that
   * is ignored because of that (Besu itself rejects the deprecated names on the command line and in
   * the configuration file).
   *
   * @param logger the logger for the warnings
   */
  public synchronized void logDeprecatedNames(final Logger logger) {
    final CommandLine commandLine = getCommandLine();
    if (commandLine == null) {
      return;
    }
    for (final OptionSpec option : registeredSpec.options()) {
      if (legacyNamesRegistered) {
        legacyUse(option, commandLine)
            .ifPresent(
                use ->
                    logger.warn(
                        "{} {} is deprecated and will be removed in a future release, use {}"
                            + " instead",
                        use.kind(),
                        use.legacyName(),
                        use.replacement()));
      } else {
        final String legacyName = legacyName(canonicalName(option));
        final String legacyEnvVar = envVarName(legacyName);
        // A Besu that still has its own GraphQL service reads these variables itself.
        if (environment.get().containsKey(legacyEnvVar)
            && commandLine.getCommandSpec().findOption(legacyName) == null) {
          logger.warn(
              "Environment variable {} is ignored, as the deprecated {}* option names are only"
                  + " available when the GraphQL plugin is bundled with Besu: use {} instead",
              legacyEnvVar,
              LEGACY_OPTION_PREFIX,
              envVarName(canonicalName(option)));
        }
      }
    }
  }

  /** How the user gave an option its value under its deprecated name. */
  private record LegacyUse(String kind, String legacyName, String replacement) {}

  private Optional<LegacyUse> legacyUse(final OptionSpec option, final CommandLine commandLine) {
    final String name = canonicalName(option);
    final String legacyName = legacyName(name);
    final ParseResult parseResult = commandLine.getParseResult();
    if (parseResult != null) {
      if (isOnCommandLine(parseResult.originalArgs(), legacyName)) {
        return Optional.of(new LegacyUse("Option", legacyName, name));
      }
      if (isOnCommandLine(parseResult.originalArgs(), name)) {
        return Optional.empty();
      }
    }
    // The configuration file and the environment are read by Besu's default value provider, which
    // looks the option up by each of its names in turn.
    final IDefaultValueProvider provider = commandLine.getDefaultValueProvider();
    if (provider == null
        || valueUnderName(provider, option, name) != null
        || valueUnderName(provider, option, legacyName) == null) {
      return Optional.empty();
    }
    final String legacyEnvVar = envVarName(legacyName);
    if (environment.get().containsKey(legacyEnvVar)) {
      return Optional.of(new LegacyUse("Environment variable", legacyEnvVar, envVarName(name)));
    }
    return Optional.of(
        new LegacyUse("Configuration file key", legacyName.substring(2), name.substring(2)));
  }

  private static boolean isOnCommandLine(final List<String> args, final String optionName) {
    return args.stream()
        .anyMatch(arg -> arg.equals(optionName) || arg.startsWith(optionName + "="));
  }

  private static String valueUnderName(
      final IDefaultValueProvider provider, final OptionSpec option, final String name) {
    try {
      return provider.defaultValue(OptionSpec.builder(option).names(name).build());
    } catch (final Exception e) {
      return null;
    }
  }

  // Same mapping as Besu's EnvironmentVariableDefaultProvider.
  private static String envVarName(final String optionName) {
    return ENV_VAR_PREFIX
        + optionName.substring("--".length()).replace('-', '_').toUpperCase(Locale.US);
  }

  private static boolean isAddedToAnotherCommand(final CommandSpec spec) {
    return spec.options().stream()
        .anyMatch(option -> option.command() != null && option.command() != spec);
  }

  /**
   * Returns a copy of the given options spec in which every {@code --plugin-graphql-*} option also
   * has its deprecated {@code --graphql-*} name. The copied options and groups set the same fields
   * as the originals.
   *
   * @param spec the options spec
   * @return the copy with the deprecated names
   */
  static CommandSpec withLegacyNames(final CommandSpec spec) {
    final CommandSpec copy = CommandSpec.create();
    for (final ArgGroupSpec group : spec.argGroups()) {
      copy.addArgGroup(withLegacyNames(group));
    }
    for (final OptionSpec option : spec.options()) {
      if (option.group() == null) {
        copy.addOption(withLegacyName(option));
      }
    }
    return copy;
  }

  private static ArgGroupSpec withLegacyNames(final ArgGroupSpec group) {
    final ArgGroupSpec.Builder builder =
        ArgGroupSpec.builder()
            .heading(group.heading())
            .headingKey(group.headingKey())
            .exclusive(group.exclusive())
            .multiplicity(group.multiplicity())
            .validate(group.validate())
            .order(group.order())
            .typeInfo(group.typeInfo())
            .getter(group.getter())
            .setter(group.setter())
            .scope(group.scope());
    for (final ArgSpec arg : group.args()) {
      builder.addArg(arg instanceof OptionSpec option ? withLegacyName(option) : arg);
    }
    for (final ArgGroupSpec subgroup : group.subgroups()) {
      builder.addSubgroup(withLegacyNames(subgroup));
    }
    return builder.build();
  }

  private static OptionSpec withLegacyName(final OptionSpec option) {
    final String name = canonicalName(option);
    if (!name.startsWith(OPTION_PREFIX)) {
      return OptionSpec.builder(option).build();
    }
    return OptionSpec.builder(option).names(name, legacyName(name)).build();
  }
}
