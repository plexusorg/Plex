package dev.plex.command;

import dev.plex.command.source.RequiredCommandSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Explicit metadata for a Plex command.
 *
 * @param name primary command name
 * @param description short command description
 * @param usage command usage text; {@code <command>} is replaced with the command name
 * @param aliases alternate root labels for the command
 * @param permission permission node required to use the command
 * @param requiredSource source restriction for command execution
 */
public record CommandSpec(
        String name,
        String description,
        String usage,
        List<String> aliases,
        String permission,
        RequiredCommandSource requiredSource)
{
    /**
     * Creates a command spec builder for the given primary name.
     *
     * @param name primary command name
     * @return command spec builder
     */
    public static Builder builder(String name)
    {
        return new Builder(name);
    }

    /**
     * Returns usage text with the command placeholder expanded.
     *
     * @return command usage text for this command
     */
    public String resolvedUsage()
    {
        return usage.replace("<command>", name);
    }

    /**
     * Builder for command specs.
     */
    public static final class Builder
    {
        private final String name;
        private String description = "";
        private String usage = "/<command>";
        private List<String> aliases = List.of();
        private String permission = "";
        private RequiredCommandSource requiredSource = RequiredCommandSource.ANY;

        private Builder(String name)
        {
            this.name = name;
        }

        /**
         * Sets the command description.
         *
         * @param description command description
         * @return this builder
         */
        public Builder description(String description)
        {
            this.description = description;
            return this;
        }

        /**
         * Sets the command usage text.
         *
         * @param usage command usage text
         * @return this builder
         */
        public Builder usage(String usage)
        {
            this.usage = usage;
            return this;
        }

        /**
         * Sets comma-separated command aliases.
         *
         * @param aliases comma-separated command aliases
         * @return this builder
         */
        public Builder aliases(String aliases)
        {
            if (aliases == null || aliases.isBlank())
            {
                this.aliases = List.of();
                return this;
            }
            this.aliases = Arrays.stream(aliases.split(","))
                    .map(String::trim)
                    .filter(alias -> !alias.isBlank())
                    .toList();
            return this;
        }

        /**
         * Sets command aliases.
         *
         * @param aliases command aliases
         * @return this builder
         */
        public Builder aliases(List<String> aliases)
        {
            this.aliases = aliases == null ? List.of() : new ArrayList<>(aliases);
            return this;
        }

        /**
         * Sets the required permission node.
         *
         * @param permission permission node
         * @return this builder
         */
        public Builder permission(String permission)
        {
            this.permission = permission == null ? "" : permission;
            return this;
        }

        /**
         * Sets the required command source.
         *
         * @param requiredSource required command source
         * @return this builder
         */
        public Builder source(RequiredCommandSource requiredSource)
        {
            this.requiredSource = requiredSource == null ? RequiredCommandSource.ANY : requiredSource;
            return this;
        }

        /**
         * Builds the command spec.
         *
         * @return command spec
         */
        public CommandSpec build()
        {
            return new CommandSpec(name, description, usage, List.copyOf(aliases), permission, requiredSource);
        }
    }
}
