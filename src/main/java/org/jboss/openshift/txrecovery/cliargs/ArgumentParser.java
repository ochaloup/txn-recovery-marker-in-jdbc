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

import java.io.PrintWriter;
import java.text.MessageFormat;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

public final class ArgumentParser {
    public static final String DEFAULT_TABLE_NAME = "JDBC_RECOVERY";

    private static Options ARGS_OPTIONS = new Options()
        .addOption("y", "type_db", true, "Database type the script will be working with")
        .addOption("i", "hibernate_dialect", true, "Hibernate dialect to be used")
        .addOption("j", "jdbc_driver_class", true, "fully classified JDBC Driver class")
        .addOption("o","host", true, "Hostname where the database runs")
        .addOption("p","port", true, "Port where the database runs")
        .addRequiredOption("d","database", true, "Databese name to connect to at the host and port")
        .addRequiredOption("u","user", true, "Username at the database to connect to")
        .addRequiredOption("s","password", true, "Password for the username at the database to connect to")
        .addOption("t","table_name", true, "Table name to be working with")
        .addOption("c","command", true, "Command to run in database available options are to create db schema"
            + "to insert a record to delete the record and list recovery pod names")
        .addOption("a","application_pod_name", true, "Application pod name which will be either"
            + " inserted/deleted onto database or by which query will be filtered")
        .addOption("r","recovery_pod_name", true, "Recovery pod name which"
            +  " will be either inserted/deleted onto database or by which query will be filtered")
        .addOption("f","format", true, "Output format")
        .addOption("v","verbose", false, "Enable verbose logging");

    /**
     * Use the static method for getting instance of parsed arguments.
     *
     * @param args  cli arguments
     * @return parser with getters containing the parsed values
     */
    public static ArgumentParser parse(String args[]) {
        return new ArgumentParser(args);
    }

    private DatabaseType typeDb;
    private String hibernateDialect, jdbcDriverClass;
    private String host, database, user, password, tableName;
    private Integer port;
    private CommandType command;
    private String applicationPodName, recoveryPodName;
    private OutputFormatType format;
    private boolean isVerbose;

    private ArgumentParser(String args[]) {
        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine line = parser.parse(ARGS_OPTIONS, args);

            String value = line.getOptionValue("type_db", DatabaseType.POSTGRESQL.name());
            this.typeDb = DatabaseType.valueOf(value.toUpperCase());
            this.hibernateDialect = line.getOptionValue("hibernate_dialect", typeDb.dialect());
            this.jdbcDriverClass = line.getOptionValue("jdbc_driver_class", typeDb.jdbcDriverClasss());

            this.host = line.getOptionValue("host", "localhost");
            value = line.getOptionValue("port", "5432");
            this.port = Integer.valueOf(value);
            this.database = line.getOptionValue("database");
            this.user = line.getOptionValue("user");
            this.password = line.getOptionValue("password");
            this.tableName = line.getOptionValue("table_name", DEFAULT_TABLE_NAME);

            value = line.getOptionValue("command", CommandType.SELECT_RECOVERY.name());
            this.command = CommandType.valueOf(value.toUpperCase());

            this.applicationPodName = line.getOptionValue("application_pod_name");
            this.recoveryPodName = line.getOptionValue("recovery_pod_name");

            value = line.getOptionValue("format", OutputFormatType.LIST_SPACE.name());
            this.format = OutputFormatType.valueOf(value.toUpperCase());

            this.isVerbose = line.hasOption("verbose");
        } catch(Exception pe) {
            System.err.println(pe.getMessage());

            HelpFormatter formatter = new HelpFormatter();
            PrintWriter writer = new PrintWriter(System.err, true);
            formatter.printHelp(writer, 80, "txn-recovery-marker-jdbc: creating and storing transaction recovery markers in database",
                    null, ARGS_OPTIONS, HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD, null, true);
            System.exit(1);
        }
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
        return MessageFormat.format(typeDb.jdbcUrlPattern(), host, port.intValue(), database);
    }
}
