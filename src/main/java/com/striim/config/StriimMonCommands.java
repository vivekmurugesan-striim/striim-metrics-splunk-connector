package com.striim.config;

import java.util.Arrays;
import java.util.List;

public class StriimMonCommands {

    public static final String MON_APPLICATIONS = "mon;";
    public static final String MON_STATUS = "status;";
    public static final String MON_SOURCES = "mon sources;";
    public static final String MON_TARGETS = "mon targets;";

    public static final List<String> DEFAULT_COMMANDS = Arrays.asList(MON_APPLICATIONS);

    public static final List<String> AVAILABLE_COMMANDS = Arrays.asList(
        MON_APPLICATIONS,
        MON_STATUS,
        MON_SOURCES,
        MON_TARGETS
    );

    public static class CommandDescription {
        public final String command;
        public final String description;

        public CommandDescription(String command, String description) {
            this.command = command;
            this.description = description;
        }

        public String getCommand() {
            return command;
        }

        public String getDescription() {
            return description;
        }
    }

    public static final List<CommandDescription> AVAILABLE_COMMANDS_WITH_DESC = Arrays.asList(
        new CommandDescription(MON_APPLICATIONS, "List all applications and their status"),
        new CommandDescription(MON_STATUS, "Show overall system status"),
        new CommandDescription(MON_SOURCES, "List all data sources"),
        new CommandDescription(MON_TARGETS, "List all data targets")
    );
}
