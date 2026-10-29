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

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;

/**
 * Test double of the {@link PicoCLIOptions} a Besu that bundles this plugin hands it: Besu's {@code
 * PicoCLIOptionsImpl}, which only accepts option names starting with {@code --plugin-<namespace>-}
 * or {@code --Xplugin-<namespace>-}, extended to also accept the given prefixes (for the GraphQL
 * plugin, {@code --graphql-}). The checks, the error log and the exception are the same as in
 * {@code PicoCLIOptionsImpl}: every name is checked before any option is added to the command. A
 * bundling Besu accepts the additional prefixes only while a bundled plugin registers; this double
 * stands for that case (see MAINTAINERS.md, "Contracts with Besu").
 */
final class LegacyPrefixPicoCLIOptions implements PicoCLIOptions {

  private static final Logger LOG = LoggerFactory.getLogger(LegacyPrefixPicoCLIOptions.class);

  private final CommandLine commandLine;
  private final Set<String> additionalAllowedPrefixes;

  LegacyPrefixPicoCLIOptions(
      final CommandLine commandLine, final Set<String> additionalAllowedPrefixes) {
    this.commandLine = commandLine;
    this.additionalAllowedPrefixes = Set.copyOf(additionalAllowedPrefixes);
  }

  @Override
  public void addPicoCLIOptions(final String namespace, final Object optionObject) {
    final String pluginPrefix = "--plugin-" + namespace + "-";
    final String unstablePrefix = "--Xplugin-" + namespace + "-";
    final CommandSpec mixin = CommandSpec.forAnnotatedObject(optionObject);
    boolean badOptionName = false;

    for (final OptionSpec optionSpec : mixin.options()) {
      for (final String optionName : optionSpec.names()) {
        if (!optionName.startsWith(pluginPrefix)
            && !optionName.startsWith(unstablePrefix)
            && additionalAllowedPrefixes.stream().noneMatch(optionName::startsWith)) {
          badOptionName = true;
          LOG.error(
              "Plugin option {} did not have the expected prefix of {}", optionName, pluginPrefix);
        }
      }
    }
    if (badOptionName) {
      throw new RuntimeException("Error loading CLI options");
    } else {
      commandLine.getCommandSpec().addMixin("Plugin " + namespace, mixin);
    }
  }
}
