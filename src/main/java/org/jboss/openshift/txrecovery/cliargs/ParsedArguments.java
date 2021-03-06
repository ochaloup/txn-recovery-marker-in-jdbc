/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.openshift.txrecovery.cliargs;

import java.text.MessageFormat;

import org.jboss.openshift.txrecovery.types.CommandType;
import org.jboss.openshift.txrecovery.types.DatabaseType;
import org.jboss.openshift.txrecovery.types.OutputFormatType;

public final class ParsedArguments {
    public static final String DEFAULT_TABLE_NAME = "JDBC_RECOVERY";
    public static final String DEFAULT_DB_TYPE = DatabaseType.POSTGRESQL.name();
    public static final String DEFAULT_HOST = "localhost";
    public static final String DEFAULT_PORT = "5432"; // PostgreSQL
    public static final String DEFAULT_COMMAND = CommandType.SELECT_RECOVERY.name();

    private static Options ARGS_OPTIONS = new Options()
        .addOption("y", "type_db", true, "Database type the script will be working with")
        .addOption("i", "hibernate_dialect", true, "Hibernate dialect to be used")
        .addOption("j", "jdbc_driver_class", true, "fully classified JDBC Driver class")
        .addOption("l", "url", true,"JDBC url which has precedence over configured host/port/database information")
        .addOption("o","host", true, "Hostname where the database runs")
        .addOption("p","port", true, "Port where the database runs")
        .addOption("d","database", true, "Database name to connect to at the host and port")
        .addRequiredOption("u","user", true, "Username at the database to connect to")
        .addRequiredOption("s","password", true, "Password for the username at the database to connect to")
        .addOption("t","table_name", true, "Table name to be working with")
        .addOption("c","command", true, "Command to run in database available options are to create db schema"
            + "to insert a record to delete the record and list recovery pod names")
        .addOption("a","application_pod_name", true, "Application pod name which will be either"
            + " inserted/deleted onto database or by which query will be filtered")
        .addOption("r","recovery_pod_name", true, "Recovery pod name which"
            +  " will be either inserted/deleted onto database or by which query will be filtered")
        .addOption("f", "format", true, "Output format")
        .addOption("v", "verbose", false, "Enable verbose logging")
        .addOption("h", "help", false, "Printing this help");

    /**
     * Use the static method for getting instance of parsed arguments.
     *
     * @param args  cli arguments
     * @return parser with getters containing the parsed values
     * @throws ArgumentParserException  error happens during error parsing
     */
    public static ParsedArguments parse(String... args) throws ArgumentParserException {
        return new ParsedArguments(args);
    }

    private DatabaseType typeDb;
    private String hibernateDialect, jdbcDriverClass;
    private String jdbcUrl;
    private String host, database, user, password, tableName;
    private Integer port;
    private CommandType command;
    private String applicationPodName, recoveryPodName;
    private OutputFormatType format;
    private boolean isVerbose;

    private ParsedArguments(String... args) throws ArgumentParserException {
        ArgumentParser parser = new ArgumentParser();

        try {
            parser.parse(ARGS_OPTIONS, args);

            if(parser.hasOption("help")) {
                printHelpStdErr();
                System.exit(2);
            }

            String value = parser.getOptionValue("type_db", DEFAULT_DB_TYPE);
            this.typeDb = DatabaseType.valueOf(value.toUpperCase());
            this.hibernateDialect = parser.getOptionValue("hibernate_dialect", typeDb.dialect());
            this.jdbcDriverClass = parser.getOptionValue("jdbc_driver_class", typeDb.jdbcDriverClasss());

            this.jdbcUrl = parser.getOptionValue("url");
            this.host = parser.getOptionValue("host", DEFAULT_HOST);
            value = parser.getOptionValue("port", DEFAULT_PORT);
            this.port = Integer.valueOf(value);
            this.database = parser.getOptionValue("database");

            if((jdbcUrl == null) && (host.isEmpty() || database == null)) {
                throw new IllegalArgumentException("Argument '-l/--url' is empty and there is not enough"
                   + " data for construction jdbc url. Please add --host, --port and --database.");
            }

            this.user = parser.getOptionValue("user");
            this.password = parser.getOptionValue("password");
            this.tableName = parser.getOptionValue("table_name", DEFAULT_TABLE_NAME);

            value = parser.getOptionValue("command", DEFAULT_COMMAND);
            this.command = CommandType.valueOf(value.toUpperCase());

            this.applicationPodName = parser.getOptionValue("application_pod_name");
            this.recoveryPodName = parser.getOptionValue("recovery_pod_name");

            value = parser.getOptionValue("format", OutputFormatType.LIST_SPACE.name());
            this.format = OutputFormatType.valueOf(value.toUpperCase());

            this.isVerbose = parser.hasOption("verbose");
        } catch(Exception pe) {
            System.err.println(pe.getMessage());
            printHelpStdErr();
            throw new ArgumentParserException(pe);
        }
    }

    void printHelpStdErr() {
        System.err.println("txn-recovery-marker-jdbc: creating and storing transaction recovery markers in database. Available command line arguments are:");
        ARGS_OPTIONS.printHelpToStdErr();
    }

    public static Options getARGS_OPTIONS() {
        return ARGS_OPTIONS;
    }

    public DatabaseType getTypeDb() {
        return typeDb;
    }

    public String getHibernateDialect() {
        return hibernateDialect;
    }

    public String getHost() {
        return host;
    }

    public String getDatabase() {
        return database;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getTableName() {
        return tableName;
    }

    public Integer getPort() {
        return port;
    }

    public CommandType getCommand() {
        return command;
    }

    public String getApplicationPodName() {
        return applicationPodName;
    }

    public String getRecoveryPodName() {
        return recoveryPodName;
    }

    public OutputFormatType getFormat() {
        return format;
    }

    public boolean isVerbose() {
        return isVerbose;
    }

    public String getJdbcDriverClass() {
        return jdbcDriverClass;
    }

    public String getJdbcUrl() {
        if(jdbcUrl != null) return jdbcUrl;
        return MessageFormat.format(typeDb.jdbcUrlPattern(), host, port.intValue(), database);
    }
}
