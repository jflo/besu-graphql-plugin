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

import static java.util.Arrays.asList;
import static org.hyperledger.besu.plugins.graphql.GraphQLConfiguration.DEFAULT_GRAPHQL_HTTP_PORT;

import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import picocli.CommandLine;

/**
 * Handles configuration options for the GraphQL HTTP service in Besu. The options are named {@code
 * --plugin-graphql-*}, as Besu requires for the options of a plugin, and have the same defaults,
 * descriptions and validation as the {@code --graphql-*} options of the GraphQL service that used
 * to be part of Besu's core. When Besu allows it, the options are also registered under those
 * legacy names, see {@link GraphQLCliOptions}.
 *
 * <p>The methods that report option names take a function that maps the name of an option to the
 * name the user gave it, so that messages name the option the way the user wrote it.
 */
public class GraphQlOptions {

  /** The prefix of the option names. */
  public static final String OPTION_PREFIX = "--plugin-graphql-";

  /**
   * The prefix of the deprecated option names, which the options had when the GraphQL service was
   * part of Besu's core.
   */
  public static final String LEGACY_OPTION_PREFIX = "--graphql-";

  /** The name of the option that enables the GraphQL HTTP service. */
  public static final String HTTP_ENABLED = OPTION_PREFIX + "http-enabled";

  static final String HTTP_HOST = OPTION_PREFIX + "http-host";
  static final String HTTP_PORT = OPTION_PREFIX + "http-port";
  static final String HTTP_CORS_ORIGINS = OPTION_PREFIX + "http-cors-origins";
  static final String TLS_ENABLED = OPTION_PREFIX + "tls-enabled";
  static final String TLS_KEYSTORE_FILE = OPTION_PREFIX + "tls-keystore-file";
  static final String TLS_KEYSTORE_PASSWORD_FILE = OPTION_PREFIX + "tls-keystore-password-file";
  static final String MTLS_ENABLED = OPTION_PREFIX + "mtls-enabled";
  static final String TLS_TRUSTSTORE_FILE = OPTION_PREFIX + "tls-truststore-file";
  static final String TLS_TRUSTSTORE_PASSWORD_FILE = OPTION_PREFIX + "tls-truststore-password-file";
  static final String MAX_BLOCKS_RANGE = OPTION_PREFIX + "max-blocks-range";

  private static final String MANDATORY_HOST_FORMAT_HELP = "<HOST>";
  private static final String MANDATORY_PORT_FORMAT_HELP = "<PORT>";

  private static final String DEPENDENCY_WARNING_MSG =
      "{} has been ignored because {} was not defined on the command line.";

  @CommandLine.Option(
      names = {HTTP_ENABLED},
      description = "Set to start the GraphQL HTTP service (default: ${DEFAULT-VALUE})")
  private final Boolean isGraphQLHttpEnabled = false;

  @SuppressWarnings({"FieldCanBeFinal", "FieldMayBeFinal"}) // PicoCLI requires non-final Strings.
  @CommandLine.Option(
      names = {HTTP_HOST},
      paramLabel = MANDATORY_HOST_FORMAT_HELP,
      description = "Host for GraphQL HTTP to listen on (default: ${DEFAULT-VALUE})")
  private String graphQLHttpHost;

  @CommandLine.Option(
      names = {HTTP_PORT},
      paramLabel = MANDATORY_PORT_FORMAT_HELP,
      description = "Port for GraphQL HTTP to listen on (default: ${DEFAULT-VALUE})")
  private final Integer graphQLHttpPort = DEFAULT_GRAPHQL_HTTP_PORT;

  @CommandLine.Option(
      names = {HTTP_CORS_ORIGINS},
      description = "Comma separated origin domain URLs for CORS validation (default: none)")
  private final CorsAllowedOriginsProperty graphQLHttpCorsAllowedOrigins =
      new CorsAllowedOriginsProperty();

  @CommandLine.Option(
      names = {TLS_ENABLED},
      description = "Enable TLS for GraphQL HTTP service")
  private Boolean graphqlTlsEnabled = false;

  @CommandLine.Option(
      names = {TLS_KEYSTORE_FILE},
      description = "Path to the TLS keystore file for GraphQL HTTP service")
  private String graphqlTlsKeystoreFile;

  @CommandLine.Option(
      names = {TLS_KEYSTORE_PASSWORD_FILE},
      description = "Path to the file containing the password for the TLS keystore")
  private String graphqlTlsKeystorePasswordFile;

  @CommandLine.Option(
      names = {MTLS_ENABLED},
      description = "Enable mTLS for GraphQL HTTP service")
  private Boolean graphqlMtlsEnabled = false;

  @CommandLine.Option(
      names = {TLS_TRUSTSTORE_FILE},
      description = "Path to the TLS truststore file for GraphQL HTTP service")
  private String graphqlTlsTruststoreFile;

  @CommandLine.Option(
      names = {TLS_TRUSTSTORE_PASSWORD_FILE},
      description = "Path to the file containing the password for the TLS truststore")
  private String graphqlTlsTruststorePasswordFile;

  @CommandLine.Option(
      names = {MAX_BLOCKS_RANGE},
      description =
          "Specifies the maximum number of blocks that can be retrieved by a single GraphQL "
              + "blocks(from,to) query. Must be >=0. 0 specifies no limit (default: ${DEFAULT-VALUE})")
  private final Long graphQLMaxBlockRange = GraphQLConfiguration.DEFAULT_MAX_BLOCK_RANGE;

  /** Default constructor */
  public GraphQlOptions() {}

  /**
   * Validates the GraphQL HTTP options, the same way as Besu did before the GraphQL service became
   * a plugin: a negative maximum block range is rejected, and the options that have no effect
   * because the option they depend on is not set are logged.
   *
   * @param logger Logger instance
   * @param commandLine CommandLine instance
   * @param nameInUse maps the name of an option to the name the user gave it
   * @throws CommandLine.ParameterException if the maximum block range is negative
   */
  public void validate(
      final Logger logger, final CommandLine commandLine, final UnaryOperator<String> nameInUse) {
    if (isMaxBlockRangeInvalid()) {
      throw new CommandLine.ParameterException(
          commandLine, maxBlocksRangeError(nameInUse.apply(MAX_BLOCKS_RANGE)));
    }
    checkOptionDependencies(logger, commandLine, nameInUse);
  }

  /**
   * Validates the option values when the command line they belong to is not known: a negative
   * maximum block range is rejected with the same message as {@link #validate(Logger, CommandLine,
   * UnaryOperator)}. The option dependencies cannot be checked without the command line.
   *
   * @throws IllegalArgumentException if the maximum block range is negative
   */
  public void validateWithoutCommandLine() {
    if (isMaxBlockRangeInvalid()) {
      throw new IllegalArgumentException(maxBlocksRangeError(MAX_BLOCKS_RANGE));
    }
  }

  /**
   * Returns the error for a negative maximum block range.
   *
   * @param optionName the name of the maximum block range option, as the user gave it
   * @return the error message
   */
  static String maxBlocksRangeError(final String optionName) {
    return optionName + " must be >= 0 (0 specifies no limit)";
  }

  private boolean isMaxBlockRangeInvalid() {
    return graphQLMaxBlockRange < 0;
  }

  /**
   * Logs a warning for the options that have no effect because the option they depend on is not
   * set, with the same messages as Besu's own option checks. The options are named by their {@code
   * --plugin-graphql-*} names.
   *
   * @param logger Logger instance
   * @param commandLine CommandLine instance
   */
  public void checkOptionDependencies(final Logger logger, final CommandLine commandLine) {
    checkOptionDependencies(logger, commandLine, UnaryOperator.identity());
  }

  /**
   * Logs a warning for the options that have no effect because the option they depend on is not
   * set, with the same messages as Besu's own option checks.
   *
   * @param logger Logger instance
   * @param commandLine CommandLine instance
   * @param nameInUse maps the name of an option to the name the user gave it
   */
  public void checkOptionDependencies(
      final Logger logger, final CommandLine commandLine, final UnaryOperator<String> nameInUse) {
    checkOptionDependencies(
        logger,
        commandLine,
        nameInUse,
        HTTP_ENABLED,
        !isGraphQLHttpEnabled,
        asList(HTTP_CORS_ORIGINS, HTTP_HOST, HTTP_PORT, TLS_ENABLED));

    checkOptionDependencies(
        logger,
        commandLine,
        nameInUse,
        TLS_ENABLED,
        !graphqlTlsEnabled,
        asList(TLS_KEYSTORE_FILE, TLS_KEYSTORE_PASSWORD_FILE, MTLS_ENABLED));

    checkOptionDependencies(
        logger,
        commandLine,
        nameInUse,
        MTLS_ENABLED,
        !graphqlMtlsEnabled,
        asList(TLS_TRUSTSTORE_FILE, TLS_TRUSTSTORE_PASSWORD_FILE));
  }

  // Same as Besu's CommandLineUtils.checkOptionDependencies. The main option is named the way the
  // affected options are: by its legacy name if the user gave any of them a legacy name.
  private static void checkOptionDependencies(
      final Logger logger,
      final CommandLine commandLine,
      final UnaryOperator<String> nameInUse,
      final String mainOptionName,
      final boolean isMainOptionCondition,
      final List<String> dependentOptionsNames) {
    if (isMainOptionCondition) {
      final List<String> affectedOptions =
          getAffectedOptions(commandLine, dependentOptionsNames).stream().map(nameInUse).toList();

      if (!affectedOptions.isEmpty()) {
        final boolean legacyNamesUsed =
            affectedOptions.stream().anyMatch(name -> name.startsWith(LEGACY_OPTION_PREFIX));
        logger.warn(
            DEPENDENCY_WARNING_MSG,
            joiningWithLastDelimiter(affectedOptions),
            legacyNamesUsed ? legacyName(mainOptionName) : mainOptionName);
      }
    }
  }

  /**
   * Returns the deprecated legacy name of an option.
   *
   * @param optionName the {@code --plugin-graphql-*} name of the option
   * @return the corresponding {@code --graphql-*} name
   */
  static String legacyName(final String optionName) {
    return LEGACY_OPTION_PREFIX + optionName.substring(OPTION_PREFIX.length());
  }

  // The names (as declared, so the --plugin-graphql-* name) of the dependent options that are set.
  private static List<String> getAffectedOptions(
      final CommandLine commandLine, final List<String> dependentOptionsNames) {
    return commandLine.getCommandSpec().options().stream()
        .filter(option -> Arrays.stream(option.names()).anyMatch(dependentOptionsNames::contains))
        .filter(GraphQlOptions::isOptionSet)
        .map(GraphQlOptions::canonicalName)
        .collect(Collectors.toList());
  }

  /**
   * Returns the {@code --plugin-graphql-*} name of an option, whichever names it was registered
   * with.
   *
   * @param option the option
   * @return the name of the option
   */
  static String canonicalName(final CommandLine.Model.OptionSpec option) {
    return Arrays.stream(option.names())
        .filter(name -> name.startsWith(OPTION_PREFIX))
        .findFirst()
        .orElse(option.names()[0]);
  }

  private static boolean isOptionSet(final CommandLine.Model.OptionSpec option) {
    final CommandLine commandLine = option.command().commandLine();
    try {
      return !option.stringValues().isEmpty()
          || !Strings.isNullOrEmpty(commandLine.getDefaultValueProvider().defaultValue(option));
    } catch (final Exception e) {
      return false;
    }
  }

  private static String joiningWithLastDelimiter(final List<String> list) {
    final int last = list.size() - 1;
    if (last < 1) return String.join(", ", list);
    return String.join(" and ", String.join(", ", list.subList(0, last)), list.get(last));
  }

  /**
   * Creates a GraphQLConfiguration based on the provided options.
   *
   * @param hostsAllowlist List of hosts allowed
   * @param defaultHostAddress Default host address
   * @param timeoutSec Timeout in seconds
   * @return A GraphQLConfiguration instance
   */
  public GraphQLConfiguration graphQLConfiguration(
      final List<String> hostsAllowlist, final String defaultHostAddress, final Long timeoutSec) {
    final GraphQLConfiguration graphQLConfiguration = GraphQLConfiguration.createDefault();
    graphQLConfiguration.setEnabled(isGraphQLHttpEnabled);
    graphQLConfiguration.setHost(getGraphQLHttpHost(defaultHostAddress));
    graphQLConfiguration.setPort(graphQLHttpPort);
    graphQLConfiguration.setHostsAllowlist(hostsAllowlist);
    graphQLConfiguration.setCorsAllowedDomains(graphQLHttpCorsAllowedOrigins);
    graphQLConfiguration.setHttpTimeoutSec(timeoutSec);
    graphQLConfiguration.setTlsEnabled(graphqlTlsEnabled);
    graphQLConfiguration.setTlsKeyStorePath(graphqlTlsKeystoreFile);
    graphQLConfiguration.setTlsKeyStorePasswordFile(graphqlTlsKeystorePasswordFile);
    graphQLConfiguration.setMtlsEnabled(graphqlMtlsEnabled);
    graphQLConfiguration.setTlsTrustStorePath(graphqlTlsTruststoreFile);
    graphQLConfiguration.setTlsTrustStorePasswordFile(graphqlTlsTruststorePasswordFile);
    graphQLConfiguration.setMaxBlockRange(graphQLMaxBlockRange);

    return graphQLConfiguration;
  }

  /**
   * Checks if GraphQL over HTTP is enabled.
   *
   * @return true if enabled, false otherwise
   */
  public Boolean isGraphQLHttpEnabled() {
    return isGraphQLHttpEnabled;
  }

  /**
   * Returns the host for GraphQL over HTTP.
   *
   * @param defaultHostAddress the host to use when none is configured
   * @return the configured host, or the default host if none is configured
   */
  public String getGraphQLHttpHost(final String defaultHostAddress) {
    return Strings.isNullOrEmpty(graphQLHttpHost) ? defaultHostAddress : graphQLHttpHost;
  }

  /**
   * Returns the port for GraphQL over HTTP.
   *
   * @return The port number
   */
  public Integer getGraphQLHttpPort() {
    return graphQLHttpPort;
  }

  /**
   * Returns the maximum number of blocks a single range query may span.
   *
   * @return the maximum block range, 0 for no limit
   */
  public Long getGraphQLMaxBlockRange() {
    return graphQLMaxBlockRange;
  }
}
