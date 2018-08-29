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

package org.jboss.openshift.txrecovery;

import org.jboss.openshift.txrecovery.cliargs.ParsedArguments;
import org.jboss.openshift.txrecovery.types.CommandType;
import org.jboss.openshift.txrecovery.types.DatabaseType;
import org.jboss.openshift.txrecovery.types.OutputFormatType;
import org.jboss.openshift.txrecovery.cliargs.ArgumentParserException;
import org.junit.Assert;
import org.junit.Test;


/**
 * Checking to run the command line parser.
 */
public class ArgumentParserTest {
    public static final String[] H2_CONNECTION_ARGS = new String[] {
        "-y", "h2",
        "-l", DBH2Connector.DB_H2_CONNECTION,
        "-u", "",
        "-s", "",
        "-t", DBH2Connector.DB_TABLE_NAME};

    @Test
    public void allShortOptions() throws Exception {
        String[] args = new String[] {
            "-y", "h2",
            "-i", "org.hibernate.dialect.H2Dialect",
            "-j", "org.h2.Driver",
            "-l", "jdbc:h2:mem:test",
            "-o", "localhost",
            "-p", "1111",
            "-d", "test_db",
            "-u", "test_user",
            "-s", "test_pass",
            "-t", "test_table",
            "-c", "create",
            "-a", "name_of_app_pod",
            "-r", "name_of_recovery_pod",
            "-f", "raw",
            "-v"};
        ParsedArguments ap = ParsedArguments.parse(args);

        Assert.assertEquals(DatabaseType.H2, ap.getTypeDb());
        Assert.assertEquals("org.hibernate.dialect.H2Dialect", ap.getHibernateDialect());
        Assert.assertEquals("org.h2.Driver", ap.getJdbcDriverClass());
        Assert.assertEquals("jdbc:h2:mem:test", ap.getJdbcUrl());
        Assert.assertEquals("localhost", ap.getHost());
        Assert.assertEquals((Integer) 1111, ap.getPort());
        Assert.assertEquals("test_db", ap.getDatabase());
        Assert.assertEquals("test_user", ap.getUser());
        Assert.assertEquals("test_pass", ap.getPassword());
        Assert.assertEquals("test_table", ap.getTableName());
        Assert.assertEquals(CommandType.CREATE, ap.getCommand());
        Assert.assertEquals("name_of_app_pod", ap.getApplicationPodName());
        Assert.assertEquals("name_of_recovery_pod", ap.getRecoveryPodName());
        Assert.assertEquals(OutputFormatType.RAW, ap.getFormat());
    }

    @Test
    public void allLongOptions() throws Exception {
        String[] args = new String[] {
            "--type_db", "h2",
            "--hibernate_dialect", "org.hibernate.dialect.H2Dialect",
            "--jdbc_driver_class", "org.h2.Driver",
            "--url", "jdbc:h2:mem:test",
            "--host", "localhost",
            "--port", "1111",
            "--database", "test_db",
            "--user", "test_user",
            "--password", "test_pass",
            "--table_name", "test_table",
            "--command", "create",
            "--application_pod_name", "name_of_app_pod",
            "--recovery_pod_name", "name_of_recovery_pod",
            "--format", "raw",
            "--verbose"};
        ParsedArguments ap = ParsedArguments.parse(args);

        Assert.assertEquals(DatabaseType.H2, ap.getTypeDb());
        Assert.assertEquals("org.hibernate.dialect.H2Dialect", ap.getHibernateDialect());
        Assert.assertEquals("org.h2.Driver", ap.getJdbcDriverClass());
        Assert.assertEquals("jdbc:h2:mem:test", ap.getJdbcUrl());
        Assert.assertEquals("localhost", ap.getHost());
        Assert.assertEquals((Integer) 1111, ap.getPort());
        Assert.assertEquals("test_db", ap.getDatabase());
        Assert.assertEquals("test_user", ap.getUser());
        Assert.assertEquals("test_pass", ap.getPassword());
        Assert.assertEquals("test_table", ap.getTableName());
        Assert.assertEquals(CommandType.CREATE, ap.getCommand());
        Assert.assertEquals("name_of_app_pod", ap.getApplicationPodName());
        Assert.assertEquals("name_of_recovery_pod", ap.getRecoveryPodName());
        Assert.assertEquals(OutputFormatType.RAW, ap.getFormat());
    }

    @Test
    public void databaseNotDefined() throws Exception {
        String[] args = new String[] {
            "-y", "h2",
            "-o", "localhost",
            "-p", "1111",
            "-u", "test_user",
            "-s", "test_password"};
        try {
            ParsedArguments.parse(args);
        } catch (ArgumentParserException ape) {
            if(!(ape.getCause() instanceof IllegalArgumentException)) {
                throw ape;
            }
        }
    }

    @Test
    public void databaseDuplicated() throws Exception {
        String[] args = new String[] {
            "-y", "h2",
            "-d", "my_db",
            "-u", "test_user",
            "-s", "test_password",
            "-d", "test_dbname"};
        ParsedArguments ap = ParsedArguments.parse(args);

        Assert.assertEquals("test_dbname", ap.getDatabase());
    }

    @Test
    public void h2Settings() throws Exception {
        ParsedArguments ap = ParsedArguments.parse(H2_CONNECTION_ARGS);

        Assert.assertEquals(DatabaseType.H2, ap.getTypeDb());
        Assert.assertEquals("org.hibernate.dialect.H2Dialect", ap.getHibernateDialect());
        Assert.assertEquals("org.h2.Driver", ap.getJdbcDriverClass());
        Assert.assertEquals(DBH2Connector.DB_H2_CONNECTION, ap.getJdbcUrl());
        Assert.assertEquals("localhost", ap.getHost());
        Assert.assertEquals((Integer) 5432, ap.getPort());
        Assert.assertEquals("", ap.getUser());
        Assert.assertEquals("", ap.getPassword());
        Assert.assertEquals(DBH2Connector.DB_TABLE_NAME, ap.getTableName());
    }
}
