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

import org.hyperledger.besu.plugin.services.PicoCLIOptions;
import org.hyperledger.besu.services.PicoCLIOptionsImpl;

import java.util.Set;

import picocli.CommandLine;

/** The kinds of Besu the plugin is registered with, as seen through its command line service. */
enum BesuHost {
  /**
   * A Besu that bundles the plugin and accepts the deprecated {@code --graphql-} option names for
   * it, as well as {@code --plugin-graphql-}.
   */
  BUNDLED {
    @Override
    PicoCLIOptions picoCLIOptions(final CommandLine commandLine) {
      return new LegacyPrefixPicoCLIOptions(
          commandLine, Set.of(GraphQlOptions.LEGACY_OPTION_PREFIX));
    }
  },

  /**
   * A Besu the plugin is installed in as an external plugin: Besu's own {@link PicoCLIOptionsImpl},
   * which only accepts {@code --plugin-graphql-} names.
   */
  EXTERNAL {
    @Override
    PicoCLIOptions picoCLIOptions(final CommandLine commandLine) {
      return new PicoCLIOptionsImpl(commandLine);
    }
  };

  /**
   * Whether this kind of Besu bundles the plugin, i.e. loads it from its bundled plugins directory.
   *
   * @return true for a Besu that bundles the plugin
   */
  boolean bundlesThePlugin() {
    return this == BUNDLED;
  }

  /**
   * Creates the plugin as this kind of Besu loads it: bundled or as an external plugin.
   *
   * @return the plugin
   */
  GraphQLPlugin newPlugin() {
    return new GraphQLPlugin(
        new GraphQLCliOptions(
            System::getenv,
            () -> System.getProperty(GraphQLCliOptions.LEGACY_OPTION_NAMES_PROPERTY),
            this::bundlesThePlugin));
  }

  /**
   * Returns the command line service this kind of Besu hands the plugin.
   *
   * @param commandLine Besu's command line
   * @return the command line service
   */
  abstract PicoCLIOptions picoCLIOptions(CommandLine commandLine);
}
