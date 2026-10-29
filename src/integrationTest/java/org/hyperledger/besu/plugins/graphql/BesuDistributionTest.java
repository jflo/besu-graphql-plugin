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

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Installs the plugin distribution zip into a real Besu installation, the way the README tells
 * users to, starts a node on a single-block post-merge test network with the plugin's options and
 * queries it over GraphQL and JSON-RPC.
 *
 * <p>Needs a Besu installation (an unpacked Besu distribution) whose version contains the plugin
 * API this plugin uses: run with {@code ./gradlew integrationTest -PbesuHome=<path>}. Without it,
 * the tests are skipped.
 */
class BesuDistributionTest {

  private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(3);

  @TempDir Path tempDir;

  private final HttpClient client = HttpClient.newHttpClient();
  private Path besuHome;
  private Path pluginsDir;
  private Path dataDir;
  private Path genesisFile;
  private Path logFile;
  private Process besu;

  @BeforeEach
  void installPlugin() throws IOException {
    final String home = System.getProperty("besu.home.under.test");
    Assumptions.assumeTrue(
        home != null && !home.isBlank(),
        "No Besu installation to test against: run with -PbesuHome=<path to Besu>");
    besuHome = Path.of(home);
    pluginsDir = Files.createDirectories(tempDir.resolve("plugins"));
    dataDir = Files.createDirectories(tempDir.resolve("data"));
    genesisFile = tempDir.resolve("genesis.json");
    try (InputStream genesis = BesuDistributionTest.class.getResourceAsStream("/genesis.json")) {
      Files.copy(genesis, genesisFile);
    }
    logFile = tempDir.resolve("besu.log");
    // Same as the documented `unzip -j <zip> '*.jar' -d <besu>/plugins`.
    unzipFlat(Path.of(System.getProperty("plugin.distribution")), pluginsDir);
  }

  @AfterEach
  void stopBesu() throws InterruptedException, IOException {
    if (besu != null) {
      besu.destroy();
      if (!besu.waitFor(30, TimeUnit.SECONDS)) {
        besu.destroyForcibly().waitFor(30, TimeUnit.SECONDS);
      }
    }
    if (logFile != null && Files.exists(logFile) && System.getenv("RUNNER_DEBUG") != null) {
      System.out.println(Files.readString(logFile));
    }
  }

  @Test
  void servesGraphQLWithThePluginOptions() throws Exception {
    final int configuredPort = freePort();
    final Properties ports =
        startBesu("--plugin-graphql-http-enabled", "--plugin-graphql-http-port=" + configuredPort);

    // net_services and the ports file report the GraphQL service as they did for Besu's own
    final int graphQLPort = Integer.parseInt(ports.getProperty("graphql-http"));
    assertThat(graphQLPort).isEqualTo(configuredPort);
    final JsonObject response =
        post(graphQLPort, "/graphql", "application/json", "{\"query\":\"{block{number}}\"}");
    assertThat(response.containsKey("errors")).isFalse();
    assertThat(response.getJsonObject("data").getJsonObject("block").getString("number"))
        .isEqualTo("0x0");

    final JsonObject services =
        post(
            Integer.parseInt(ports.getProperty("json-rpc")),
            "/",
            "application/json",
            "{\"jsonrpc\":\"2.0\",\"method\":\"net_services\",\"params\":[],\"id\":1}");
    assertThat(services.getJsonObject("result").getJsonObject("graphQL").getString("port"))
        .isEqualTo(String.valueOf(graphQLPort));

    final JsonObject balance =
        post(
            graphQLPort,
            "/graphql",
            "application/graphql",
            "{block {account(address: \"0xfe3b557e8fb62b89f4916b721be55ceb828dbd73\") {balance}}}");
    assertThat(
            balance
                .getJsonObject("data")
                .getJsonObject("block")
                .getJsonObject("account")
                .getString("balance"))
        .isEqualTo("0xad78ebc5ac6200000");

    assertThat(Files.readString(logFile))
        .contains("Registered plugin of type " + GraphQLPlugin.class.getName());
  }

  @Test
  void anExternalInstallDoesNotOfferTheLegacyNames() throws Exception {
    final Properties ports =
        startBesu("--plugin-graphql-http-enabled", "--plugin-graphql-http-port=0");

    assertThat(ports.getProperty("graphql-http")).isNotNull();
    // installed in the plugins directory, not bundled: no attempt at the --graphql-* names
    assertThat(Files.readString(logFile))
        .doesNotContain("did not have the expected prefix of --plugin-graphql-")
        .doesNotContain("does not accept the deprecated");
  }

  @Test
  void theLegacyNamesCanBeOfferedWithTheSystemProperty() throws Exception {
    // whether this Besu accepts them or not, the plugin starts with its own names
    final Properties ports =
        startBesuWithOpts(
            "-Dbesu.plugin.graphql.legacy-option-names=true",
            "--plugin-graphql-http-enabled",
            "--plugin-graphql-http-port=0");

    assertThat(ports.getProperty("graphql-http")).isNotNull();
  }

  @Test
  void asBundledWithBesuOffersTheDeprecatedNames() throws Exception {
    // Only a Besu that bundles plugins has a bundled-plugins directory. The distribution under
    // test replaces the plugin bundled with it: Besu reads bundled-plugins/ under besu.home.
    Assumptions.assumeTrue(
        Files.isDirectory(besuHome.resolve(GraphQLCliOptions.BUNDLED_PLUGINS_DIRECTORY)),
        "This Besu does not bundle plugins");
    final Path home = Files.createDirectories(tempDir.resolve("besu-home"));
    final Path bundledDir =
        Files.createDirectories(home.resolve(GraphQLCliOptions.BUNDLED_PLUGINS_DIRECTORY));
    unzipFlat(Path.of(System.getProperty("plugin.distribution")), bundledDir);
    final Path emptyPluginsDir = Files.createDirectories(tempDir.resolve("no-plugins"));
    final int configuredPort = freePort();

    // the deprecated names on the command line and in the environment
    final Properties ports =
        startBesu(
            Map.of("BESU_GRAPHQL_HTTP_PORT", String.valueOf(configuredPort)),
            "-Dbesu.plugins.dir=" + emptyPluginsDir + " -Dbesu.home=" + home,
            "--graphql-http-enabled");

    assertThat(Integer.parseInt(ports.getProperty("graphql-http"))).isEqualTo(configuredPort);
    final JsonObject response =
        post(configuredPort, "/graphql", "application/json", "{\"query\":\"{block{number}}\"}");
    assertThat(response.getJsonObject("data").getJsonObject("block").getString("number"))
        .isEqualTo("0x0");
    assertThat(Files.readString(logFile))
        .contains(
            "Option --graphql-http-enabled is deprecated and will be removed in a future release,"
                + " use --plugin-graphql-http-enabled instead")
        .contains(
            "Environment variable BESU_GRAPHQL_HTTP_PORT is deprecated and will be removed in a"
                + " future release, use BESU_PLUGIN_GRAPHQL_HTTP_PORT instead")
        // Besu's own notice about the bundled plugin
        .contains("GraphQL is served by the besu-graphql-plugin bundled with Besu")
        .doesNotContain("did not have the expected prefix of --plugin-graphql-");
  }

  @Test
  void isDisabledByDefault() throws Exception {
    final Properties ports = startBesu();

    assertThat(ports.getProperty("graphql-http")).isNull();
    assertThat(ports.getProperty("json-rpc")).isNotNull();
  }

  private Properties startBesu(final String... args) throws Exception {
    return startBesuWithOpts("", args);
  }

  private Properties startBesuWithOpts(final String javaOpts, final String... args)
      throws Exception {
    return startBesu(Map.of(), ("-Dbesu.plugins.dir=" + pluginsDir + " " + javaOpts).trim(), args);
  }

  private Properties startBesu(
      final Map<String, String> environment, final String besuOpts, final String... args)
      throws Exception {
    final List<String> command = new ArrayList<>();
    command.add(besuHome.resolve("bin").resolve(isWindows() ? "besu.bat" : "besu").toString());
    command.addAll(
        List.of(
            "--genesis-file=" + genesisFile,
            "--data-path=" + dataDir,
            "--p2p-enabled=false",
            "--engine-rpc-port=0",
            "--engine-jwt-disabled",
            "--rpc-http-enabled",
            "--rpc-http-port=0",
            "--rpc-http-api=NET,ETH"));
    command.addAll(List.of(args));
    final ProcessBuilder builder =
        new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(logFile.toFile());
    builder.environment().putAll(environment);
    builder.environment().put("BESU_OPTS", besuOpts);
    besu = builder.start();

    final Path portsFile = dataDir.resolve("besu.ports");
    final Instant deadline = Instant.now().plus(STARTUP_TIMEOUT);
    while (!Files.exists(portsFile)) {
      if (!besu.isAlive()) {
        throw new AssertionError(
            "Besu exited with " + besu.exitValue() + ":\n" + Files.readString(logFile));
      }
      if (Instant.now().isAfter(deadline)) {
        throw new AssertionError("Besu did not start in time:\n" + Files.readString(logFile));
      }
      Thread.sleep(250);
    }
    // the file is written in one go once every service is up, give the write a moment
    Thread.sleep(250);
    final Properties ports = new Properties();
    try (InputStream in = Files.newInputStream(portsFile)) {
      ports.load(in);
    }
    return ports;
  }

  private JsonObject post(
      final int port, final String path, final String contentType, final String body)
      throws IOException, InterruptedException {
    final HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
            .header("Content-Type", contentType)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    final HttpResponse<String> response =
        client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
    return new JsonObject(response.body());
  }

  private static int freePort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
      return socket.getLocalPort();
    }
  }

  private static void unzipFlat(final Path zip, final Path target) throws IOException {
    try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip))) {
      ZipEntry entry;
      while ((entry = in.getNextEntry()) != null) {
        if (!entry.isDirectory() && entry.getName().endsWith(".jar")) {
          final Path name = Path.of(entry.getName()).getFileName();
          Files.copy(in, target.resolve(name.toString()));
        }
      }
    }
  }

  private static boolean isWindows() {
    return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win");
  }
}
