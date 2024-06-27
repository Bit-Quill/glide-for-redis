/** Copyright GLIDE-for-Redis Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.models.commands.scan;

import glide.api.commands.SetBaseCommands;
import lombok.experimental.SuperBuilder;

/**
 * Optional arguments for {@link SetBaseCommands#sscan(String, long)}, {@link
 * SetBaseCommands#sscan(String, long, SScanOptions)}.
 *
 * @see <a href="https://redis.io/commands/sscan/">redis.io</a>
 */
@SuperBuilder
public class SScanOptions extends ScanOptions {}