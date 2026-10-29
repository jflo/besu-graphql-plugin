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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.hyperledger.besu.cli.util.ConfigDefaultValueProviderStrategy;
import org.hyperledger.besu.plugin.ServiceManager;
import org.hyperledger.besu.plugin.services.PicoCLIOptions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;

/**
 * The options are named {@code --plugin-graphql-*} on every Besu, and also accept their deprecated
 * {@code --graphql-*} names on a Besu that bundles the plugin. Each name is checked in the three
 * places Besu reads options from (the command line, the TOML configuration file and {@code BESU_*}
 * environment variables) with Besu's own default value provider, on both kinds of Besu: a bundling
 * Besu (see {@link LegacyPrefixPicoCLIOptions}) and Besu's own {@code PicoCLIOptionsImpl}, which is
 * what an externally installed plugin gets.
 */
class GraphQLOptionNamesTest {

  private static final String DEPRECATION_WARNING =
      "{} {} is deprecated and will be removed in a future release, use {} instead";

  private static final String IGNORED_WARNING =
      "{} {} is deprecated and ignored, because {} is also set";

  @CommandLine.Command(name = "besu")
  static class TestCommand implements Runnable {
    @Override
    public void run() {}
  }

  /** Where a value is given. */
  enum Source {
    COMMAND_LINE,
    CONFIG_FILE,
    ENVIRONMENT
  }

  /**
   * An option, a value for it on the command line or in the environment, the same value in TOML,
   * and how to read it back from the configuration.
   */
  record OptionCase(
      String suffix,
      String value,
      String tomlValue,
      Function<GraphQLConfiguration, Object> read,
      Object expected) {

    String name() {
      return GraphQlOptions.OPTION_PREFIX + suffix;
    }

    String legacyName() {
      return GraphQlOptions.LEGACY_OPTION_PREFIX + suffix;
    }

    @Override
    public String toString() {
      return suffix;
    }
  }

  @TempDir static Path tempDir;

  private final Map<String, String> environment = new HashMap<>();

  @SuppressWarnings("PrivateStaticFinalLoggers") // a fresh mock for each test
  private final Logger logger = mock(Logger.class);

  private CommandLine commandLine;
  private GraphQLPlugin plugin;
  private Path configFile;

  @BeforeEach
  void setUp() {
    commandLine = new CommandLine(new TestCommand());
  }

  static Stream<OptionCase> options() throws IOException {
    final Path keystorePassword = Files.writeString(tempDir.resolve("ks-pw"), "ks-secret");
    final Path truststorePassword = Files.writeString(tempDir.resolve("ts-pw"), "ts-secret");
    return Stream.of(
        new OptionCase("http-enabled", "true", "true", GraphQLConfiguration::isEnabled, true),
        new OptionCase(
            "http-host", "1.2.3.4", "\"1.2.3.4\"", GraphQLConfiguration::getHost, "1.2.3.4"),
        new OptionCase("http-port", "1234", "1234", GraphQLConfiguration::getPort, 1234),
        new OptionCase(
            "http-cors-origins",
            "http://a.com,http://b.com",
            "[\"http://a.com\", \"http://b.com\"]",
            config -> List.copyOf(config.getCorsAllowedDomains()),
            List.of("http://a.com", "http://b.com")),
        new OptionCase("tls-enabled", "true", "true", GraphQLConfiguration::isTlsEnabled, true),
        new OptionCase(
            "tls-keystore-file",
            "/keystore",
            "\"/keystore\"",
            GraphQLConfiguration::getTlsKeyStorePath,
            "/keystore"),
        new OptionCase(
            "tls-keystore-password-file",
            keystorePassword.toString(),
            toml(keystorePassword),
            config -> password(config::getTlsKeyStorePassword),
            "ks-secret"),
        new OptionCase("mtls-enabled", "true", "true", GraphQLConfiguration::isMtlsEnabled, true),
        new OptionCase(
            "tls-truststore-file",
            "/truststore",
            "\"/truststore\"",
            GraphQLConfiguration::getTlsTrustStorePath,
            "/truststore"),
        new OptionCase(
            "tls-truststore-password-file",
            truststorePassword.toString(),
            toml(truststorePassword),
            config -> password(config::getTlsTrustStorePassword),
            "ts-secret"),
        new OptionCase(
            "max-blocks-range", "42", "42", GraphQLConfiguration::getMaxBlockRange, 42L));
  }

  static Stream<Arguments> hostsSourcesAndOptions() throws IOException {
    final List<OptionCase> options = options().toList();
    return Arrays.stream(BesuHost.values())
        .flatMap(
            host ->
                Arrays.stream(Source.values())
                    .flatMap(
                        source ->
                            options.stream().map(option -> Arguments.of(host, source, option))));
  }

  static Stream<Arguments> sourcesAndOptions() throws IOException {
    final List<OptionCase> options = options().toList();
    return Arrays.stream(Source.values())
        .flatMap(source -> options.stream().map(option -> Arguments.of(source, option)));
  }

  @ParameterizedTest(name = "{0} {1} {2}")
  @MethodSource("hostsSourcesAndOptions")
  void pluginOptionNamesWorkOnEveryBesu(
      final BesuHost host, final Source source, final OptionCase option) throws IOException {
    register(host);

    parse(source, option.name(), option.value(), option.tomlValue());

    assertThat(option.read().apply(plugin.getConfiguration())).isEqualTo(option.expected());
    assertThat(plugin.getCliOptions().nameInUse(option.name())).isEqualTo(option.name());
    plugin.getCliOptions().logDeprecatedNames(logger);
    verify(logger, never()).warn(anyString(), any(Object[].class));
  }

  @ParameterizedTest(name = "{0} {1}")
  @MethodSource("sourcesAndOptions")
  void legacyOptionNamesWorkOnABundlingBesuAndAreReportedAsDeprecated(
      final Source source, final OptionCase option) throws IOException {
    register(BesuHost.BUNDLED);

    parse(source, option.legacyName(), option.value(), option.tomlValue());

    assertThat(option.read().apply(plugin.getConfiguration())).isEqualTo(option.expected());
    assertThat(plugin.getCliOptions().nameInUse(option.name())).isEqualTo(option.legacyName());
    plugin.getCliOptions().logDeprecatedNames(logger);
    switch (source) {
      case COMMAND_LINE ->
          verify(logger).warn(DEPRECATION_WARNING, "Option", option.legacyName(), option.name());
      case CONFIG_FILE ->
          verify(logger)
              .warn(
                  DEPRECATION_WARNING,
                  "Configuration file key",
                  option.legacyName().substring(2),
                  option.name().substring(2));
      case ENVIRONMENT ->
          verify(logger)
              .warn(
                  DEPRECATION_WARNING,
                  "Environment variable",
                  envVar(option.legacyName()),
                  envVar(option.name()));
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("options")
  void legacyOptionNamesAreUnknownOnTheCommandLineOfAnotherBesu(final OptionCase option) {
    register(BesuHost.EXTERNAL);

    assertThatThrownBy(() -> parse(Source.COMMAND_LINE, option.legacyName(), option.value(), null))
        .isInstanceOf(CommandLine.UnmatchedArgumentException.class)
        .hasMessageContaining(option.legacyName());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("options")
  void legacyOptionNamesAreUnknownInTheConfigFileOfAnotherBesu(final OptionCase option) {
    register(BesuHost.EXTERNAL);

    assertThatThrownBy(
            () -> parse(Source.CONFIG_FILE, option.legacyName(), null, option.tomlValue()))
        .isInstanceOf(CommandLine.ParameterException.class)
        .hasMessage(
            "Unknown option in TOML configuration file: " + option.legacyName().substring(2));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("options")
  void legacyEnvironmentVariablesAreIgnoredWithAWarningOnAnotherBesu(final OptionCase option)
      throws IOException {
    register(BesuHost.EXTERNAL);

    parse(Source.ENVIRONMENT, option.legacyName(), option.value(), null);

    assertThat(option.read().apply(plugin.getConfiguration()))
        .isEqualTo(option.read().apply(GraphQLConfiguration.createDefault()));
    plugin.getCliOptions().logDeprecatedNames(logger);
    verify(logger)
        .warn(
            "Environment variable {} is ignored, as the deprecated {}* option names are only"
                + " available when the GraphQL plugin is bundled with Besu: use {} instead",
            envVar(option.legacyName()),
            "--graphql-",
            envVar(option.name()));
  }

  @Test
  void thePluginNameWinsOverTheLegacyNameInTheConfigFileAndEnvironment() throws IOException {
    register(BesuHost.BUNDLED);
    environment.put("BESU_PLUGIN_GRAPHQL_HTTP_PORT", "1111");
    environment.put("BESU_GRAPHQL_HTTP_PORT", "2222");
    writeConfig("graphql-http-host=\"2.2.2.2\"\nplugin-graphql-http-host=\"1.1.1.1\"\n");

    parse();

    assertThat(plugin.getConfiguration().getPort()).isEqualTo(1111);
    assertThat(plugin.getConfiguration().getHost()).isEqualTo("1.1.1.1");
    plugin.getCliOptions().logDeprecatedNames(logger);
    // the ignored legacy values are reported, not as values in use
    verify(logger)
        .warn(
            IGNORED_WARNING,
            "Environment variable",
            "BESU_GRAPHQL_HTTP_PORT",
            "BESU_PLUGIN_GRAPHQL_HTTP_PORT");
    verify(logger)
        .warn(
            IGNORED_WARNING,
            "Configuration file key",
            "graphql-http-host",
            "plugin-graphql-http-host");
    verify(logger, never()).warn(eq(DEPRECATION_WARNING), any(Object[].class));
  }

  @Test
  void aLegacyValueEqualToThePluginValueIsNotReportedAsIgnored() throws IOException {
    register(BesuHost.BUNDLED);
    environment.put("BESU_PLUGIN_GRAPHQL_HTTP_HOST", "0.0.0.0");
    environment.put("BESU_GRAPHQL_HTTP_HOST", "0.0.0.0");
    writeConfig("graphql-http-port=1111\nplugin-graphql-http-port=1111\n");

    parse();

    plugin.getCliOptions().logDeprecatedNames(logger);
    verify(logger, never()).warn(anyString(), any(Object[].class));
  }

  @Test
  void aLegacyEnvironmentVariableWinsOverThePluginNameInTheConfigFile() throws IOException {
    // the environment wins over the configuration file, whatever the names
    register(BesuHost.BUNDLED);
    environment.put("BESU_GRAPHQL_HTTP_HOST", "2.2.2.2");
    writeConfig("plugin-graphql-http-host=\"1.1.1.1\"\n");

    parse();

    assertThat(plugin.getConfiguration().getHost()).isEqualTo("2.2.2.2");
    assertThat(plugin.getCliOptions().nameInUse("--plugin-graphql-http-host"))
        .isEqualTo("--graphql-http-host");
    plugin.getCliOptions().logDeprecatedNames(logger);
    verify(logger)
        .warn(
            DEPRECATION_WARNING,
            "Environment variable",
            "BESU_GRAPHQL_HTTP_HOST",
            "BESU_PLUGIN_GRAPHQL_HTTP_HOST");
    verify(logger, never()).warn(eq(IGNORED_WARNING), any(Object[].class));
  }

  @Test
  void thePluginEnvironmentVariableOverridesTheLegacyOneOfTheDockerImage() throws IOException {
    // Besu's Docker image sets BESU_GRAPHQL_HTTP_HOST=0.0.0.0. A container that sets
    // BESU_GRAPHQL_HTTP_HOST replaces that value; one that sets BESU_PLUGIN_GRAPHQL_HTTP_HOST
    // overrides it, with a warning, as the values differ.
    register(BesuHost.BUNDLED);
    environment.put("BESU_GRAPHQL_HTTP_HOST", "0.0.0.0");
    environment.put("BESU_PLUGIN_GRAPHQL_HTTP_HOST", "127.0.0.1");

    parse("--plugin-graphql-http-enabled");

    assertThat(plugin.getConfiguration().getHost()).isEqualTo("127.0.0.1");
    plugin.getCliOptions().logDeprecatedNames(logger, plugin.isEnabled());
    verify(logger)
        .warn(
            IGNORED_WARNING,
            "Environment variable",
            "BESU_GRAPHQL_HTTP_HOST",
            "BESU_PLUGIN_GRAPHQL_HTTP_HOST");
  }

  @Test
  void deprecatedNamesAreNotReportedWhenTheServiceIsDisabled() throws IOException {
    // the Docker image sets BESU_GRAPHQL_HTTP_HOST for every container, GraphQL enabled or not
    register(BesuHost.BUNDLED);
    environment.put("BESU_GRAPHQL_HTTP_HOST", "0.0.0.0");
    environment.put("BESU_PLUGIN_GRAPHQL_HTTP_PORT", "1111");
    environment.put("BESU_GRAPHQL_HTTP_PORT", "2222");

    parse();

    assertThat(plugin.isEnabled()).isFalse();
    plugin.getCliOptions().logDeprecatedNames(logger, false);
    verify(logger, never()).warn(anyString(), any(Object[].class));
    // only the warning about the options without effect, as before GraphQL became a plugin
    plugin.validateOptions(logger);
    verify(logger)
        .warn(
            "{} has been ignored because {} was not defined on the command line.",
            "--graphql-http-host and --plugin-graphql-http-port",
            "--graphql-http-enabled");
  }

  @Test
  void ignoredLegacyEnvironmentVariablesAreReportedWhetherTheServiceIsEnabledOrNot()
      throws IOException {
    register(BesuHost.EXTERNAL);
    environment.put("BESU_GRAPHQL_HTTP_ENABLED", "true");

    parse();

    plugin.getCliOptions().logDeprecatedNames(logger, false);
    verify(logger)
        .warn(
            anyString(),
            eq("BESU_GRAPHQL_HTTP_ENABLED"),
            eq("--graphql-"),
            eq("BESU_PLUGIN_GRAPHQL_HTTP_ENABLED"));
  }

  @Test
  void theCommandLineWinsOverTheConfigFileAndEnvironmentWhateverTheNames() throws IOException {
    register(BesuHost.BUNDLED);
    environment.put("BESU_PLUGIN_GRAPHQL_HTTP_PORT", "1111");
    writeConfig("plugin-graphql-http-host=\"1.1.1.1\"\n");

    parse("--graphql-http-port=3333", "--graphql-http-host=3.3.3.3");

    assertThat(plugin.getConfiguration().getPort()).isEqualTo(3333);
    assertThat(plugin.getConfiguration().getHost()).isEqualTo("3.3.3.3");
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(Source.class)
  void negativeMaxBlocksRangeIsReportedUnderTheLegacyNameWhenThatIsUsed(final Source source)
      throws IOException {
    register(BesuHost.BUNDLED);
    parse(source, "--graphql-max-blocks-range", "-1", "-1");

    assertThatThrownBy(plugin::validateOptions)
        .isInstanceOfSatisfying(
            CommandLine.ParameterException.class,
            e -> assertThat(e.getCommandLine()).isSameAs(commandLine))
        .hasMessage("--graphql-max-blocks-range must be >= 0 (0 specifies no limit)");
  }

  @ParameterizedTest(name = "{0} {1}")
  @MethodSource("hostsAndSources")
  void negativeMaxBlocksRangeIsReportedUnderThePluginName(final BesuHost host, final Source source)
      throws IOException {
    register(host);
    parse(source, "--plugin-graphql-max-blocks-range", "-1", "-1");

    assertThatThrownBy(plugin::validateOptions)
        .isInstanceOf(CommandLine.ParameterException.class)
        .hasMessage("--plugin-graphql-max-blocks-range must be >= 0 (0 specifies no limit)");
  }

  static Stream<Arguments> hostsAndSources() {
    return Arrays.stream(BesuHost.values())
        .flatMap(host -> Arrays.stream(Source.values()).map(source -> Arguments.of(host, source)));
  }

  @Test
  void ignoredOptionsAreReportedUnderTheNamesInUse() throws IOException {
    register(BesuHost.BUNDLED);
    writeConfig("graphql-tls-keystore-file=\"/keystore\"\n");
    parse("--graphql-http-host=1.2.3.4", "--plugin-graphql-http-port=1234");

    plugin.validateOptions(logger);

    verify(logger)
        .warn(
            "{} has been ignored because {} was not defined on the command line.",
            "--graphql-http-host and --plugin-graphql-http-port",
            "--graphql-http-enabled");
    verify(logger)
        .warn(
            "{} has been ignored because {} was not defined on the command line.",
            "--graphql-tls-keystore-file",
            "--graphql-tls-enabled");
  }

  @Test
  void ignoredOptionsAreReportedUnderThePluginNamesOnAnotherBesu() throws IOException {
    register(BesuHost.EXTERNAL);
    parse("--plugin-graphql-http-host=1.2.3.4", "--plugin-graphql-http-port=1234");

    plugin.validateOptions(logger);

    verify(logger)
        .warn(
            "{} has been ignored because {} was not defined on the command line.",
            "--plugin-graphql-http-host and --plugin-graphql-http-port",
            "--plugin-graphql-http-enabled");
  }

  @Test
  void anExternalInstallDoesNotOfferTheLegacyNames() {
    final List<String> offeredNames = new ArrayList<>();
    final PicoCLIOptions bundling = BesuHost.BUNDLED.picoCLIOptions(commandLine);
    // even to a Besu that would accept them
    register(
        (namespace, optionObject) -> {
          CommandSpec.forAnnotatedObject(optionObject).options().stream()
              .flatMap(o -> Arrays.stream(o.names()))
              .forEach(offeredNames::add);
          bundling.addPicoCLIOptions(namespace, optionObject);
        },
        null,
        false);

    assertThat(plugin.getCliOptions().isLegacyNamesRegistered()).isFalse();
    assertThat(offeredNames)
        .containsExactlyInAnyOrderElementsOf(allOptionNames(GraphQlOptions.OPTION_PREFIX));
  }

  @Test
  void theSystemPropertyOffersTheLegacyNamesToAnExternalInstall() {
    register(BesuHost.BUNDLED.picoCLIOptions(commandLine), "true", false);

    assertThat(plugin.getCliOptions().isLegacyNamesRegistered()).isTrue();
  }

  @Test
  void theBundledPluginsDirectoryIsRecognised() {
    assertThat(
            GraphQLCliOptions.isInBundledPluginsDirectory(
                Path.of("/opt/besu/bundled-plugins/besu-graphql-plugin-1.0.0.jar")))
        .isTrue();
    assertThat(
            GraphQLCliOptions.isInBundledPluginsDirectory(
                Path.of("/opt/besu/plugins/besu-graphql-plugin-1.0.0.jar")))
        .isFalse();
    assertThat(GraphQLCliOptions.isInBundledPluginsDirectory(Path.of("/"))).isFalse();
    // the tests run the plugin from its build directory, as an external plugin would be
    assertThat(GraphQLCliOptions.isInBundledPluginsDirectory(GraphQLCliOptions.class)).isFalse();
  }

  @Test
  void aRejectedAttemptLeavesOnlyThePluginOptions() {
    // the system property makes the plugin offer the legacy names to a Besu that rejects them
    register(BesuHost.EXTERNAL.picoCLIOptions(commandLine), "true", false);

    assertThat(plugin.getCliOptions().isLegacyNamesRegistered()).isFalse();
    assertThat(graphQLOptionNames())
        .containsExactlyInAnyOrderElementsOf(allOptionNames(GraphQlOptions.OPTION_PREFIX));
    assertThat(commandLine.getCommandSpec().mixins()).containsOnlyKeys("Plugin graphql");
  }

  @Test
  void aBundlingBesuGetsBothNames() {
    register(BesuHost.BUNDLED);

    assertThat(plugin.getCliOptions().isLegacyNamesRegistered()).isTrue();
    final List<String> expected = new ArrayList<>(allOptionNames(GraphQlOptions.OPTION_PREFIX));
    expected.addAll(allOptionNames(GraphQlOptions.LEGACY_OPTION_PREFIX));
    assertThat(graphQLOptionNames()).containsExactlyInAnyOrderElementsOf(expected);
    // one option per pair of names
    assertThat(
            commandLine.getCommandSpec().options().stream()
                .filter(o -> o.longestName().contains("graphql")))
        .hasSize(11)
        .allSatisfy(o -> assertThat(o.names()).hasSize(2));
  }

  @Test
  void theLegacyNamesAreNotOfferedWhenTheSystemPropertyDisablesThem() {
    final List<String> offeredNames = new ArrayList<>();
    final PicoCLIOptions bundled = BesuHost.BUNDLED.picoCLIOptions(commandLine);
    register(
        (namespace, optionObject) -> {
          CommandSpec.forAnnotatedObject(optionObject).options().stream()
              .flatMap(o -> Arrays.stream(o.names()))
              .forEach(offeredNames::add);
          bundled.addPicoCLIOptions(namespace, optionObject);
        },
        "false",
        true);

    assertThat(plugin.getCliOptions().isLegacyNamesRegistered()).isFalse();
    assertThat(offeredNames)
        .containsExactlyInAnyOrderElementsOf(allOptionNames(GraphQlOptions.OPTION_PREFIX));
  }

  @Test
  void aBesuWithItsOwnGraphQLOptionsKeepsThemAndGetsThePluginOptions() throws IOException {
    // A Besu that still has its core GraphQL service has options named --graphql-* itself.
    final CommandLine.Model.CommandSpec spec = commandLine.getCommandSpec();
    spec.addOption(OptionSpec.builder("--graphql-http-enabled").type(Boolean.class).build());
    register(BesuHost.EXTERNAL);
    environment.put("BESU_GRAPHQL_HTTP_ENABLED", "true");

    parse("--plugin-graphql-http-port=1234");

    assertThat(plugin.getCliOptions().isLegacyNamesRegistered()).isFalse();
    assertThat(plugin.getConfiguration().getPort()).isEqualTo(1234);
    assertThat(plugin.isEnabled()).isFalse();
    plugin.getCliOptions().logDeprecatedNames(logger);
    // that Besu reads BESU_GRAPHQL_HTTP_ENABLED itself
    verify(logger, never()).warn(anyString(), any(Object[].class));
  }

  @Test
  void aFailureAfterBesuStartedAddingTheOptionsIsFatal() {
    // A Besu that accepts the legacy names but already has one of them: picocli fails part way.
    commandLine
        .getCommandSpec()
        .addOption(OptionSpec.builder("--graphql-http-port").type(Integer.class).build());

    assertThatThrownBy(() -> register(BesuHost.BUNDLED))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageStartingWith("The GraphQL options could not be added to Besu's command line");
  }

  @Test
  void helpOfABundlingBesuShowsBothNamesInTheGraphQLSection() {
    register(BesuHost.BUNDLED);

    final String usage = commandLine.getUsageMessage(CommandLine.Help.Ansi.OFF);

    assertThat(usage)
        .contains("GraphQL Options (--graphql-* names are deprecated, use --plugin-graphql-*)");
    assertThat(usage).contains(allOptionNames(GraphQlOptions.OPTION_PREFIX));
    assertThat(usage).contains(allOptionNames(GraphQlOptions.LEGACY_OPTION_PREFIX));
    assertThat(usage).contains("Port for GraphQL HTTP to listen on (default: 8547)");
  }

  @Test
  void helpOfAnotherBesuShowsThePluginNames() {
    register(BesuHost.EXTERNAL);

    final String usage = commandLine.getUsageMessage(CommandLine.Help.Ansi.OFF);

    assertThat(usage).contains("GraphQL Options").doesNotContain("deprecated");
    assertThat(usage).contains(allOptionNames(GraphQlOptions.OPTION_PREFIX));
    assertThat(usage.replace("--plugin-graphql-", "")).doesNotContain("--graphql-");
    assertThat(usage).contains("(default: 5000)");
  }

  private void register(final BesuHost host) {
    register(host.picoCLIOptions(commandLine), null, host.bundlesThePlugin());
  }

  private void register(
      final PicoCLIOptions picoCLIOptions,
      final String legacyNamesProperty,
      final boolean bundled) {
    plugin =
        new GraphQLPlugin(
            new GraphQLCliOptions(() -> environment, () -> legacyNamesProperty, () -> bundled));
    final ServiceManager.SimpleServiceManager serviceManager =
        new ServiceManager.SimpleServiceManager();
    serviceManager.addService(PicoCLIOptions.class, picoCLIOptions);
    plugin.register(serviceManager);
  }

  private void parse(
      final Source source, final String name, final String value, final String tomlValue)
      throws IOException {
    switch (source) {
      case COMMAND_LINE -> parse(name + "=" + value);
      case CONFIG_FILE -> {
        writeConfig(name.substring(2) + "=" + tomlValue + "\n");
        parse();
      }
      case ENVIRONMENT -> {
        environment.put(envVar(name), value);
        parse();
      }
    }
  }

  // As Besu does: the environment and the config file are read by its default value provider.
  private void parse(final String... args) {
    commandLine.setDefaultValueProvider(
        new ConfigDefaultValueProviderStrategy(null, environment)
            .createDefaultValueProvider(
                commandLine, Optional.ofNullable(configFile).map(Path::toFile), Optional.empty()));
    commandLine.parseArgs(args);
  }

  private void writeConfig(final String toml) throws IOException {
    configFile = Files.createTempFile(tempDir, "config", ".toml");
    Files.writeString(configFile, toml, StandardCharsets.UTF_8);
  }

  private List<String> graphQLOptionNames() {
    return commandLine.getCommandSpec().optionsMap().keySet().stream()
        .filter(name -> name.contains("graphql"))
        .toList();
  }

  private static List<String> allOptionNames(final String prefix) {
    return Stream.of(
            "http-enabled",
            "http-host",
            "http-port",
            "http-cors-origins",
            "tls-enabled",
            "tls-keystore-file",
            "tls-keystore-password-file",
            "mtls-enabled",
            "tls-truststore-file",
            "tls-truststore-password-file",
            "max-blocks-range")
        .map(suffix -> prefix + suffix)
        .toList();
  }

  private static String envVar(final String optionName) {
    return "BESU_" + optionName.substring(2).replace('-', '_').toUpperCase(java.util.Locale.US);
  }

  private static String toml(final Path path) {
    return "\"" + path.toString().replace("\\", "\\\\") + "\"";
  }

  private interface ThrowingSupplier<T> {
    T get() throws Exception;
  }

  // The password read from the configured password file, null if no file is configured.
  private static String password(final ThrowingSupplier<String> supplier) {
    try {
      return supplier.get();
    } catch (final NullPointerException e) {
      return null;
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
