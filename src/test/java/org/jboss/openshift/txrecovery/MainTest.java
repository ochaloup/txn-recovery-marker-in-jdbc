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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Checks for database connections and operations.
 */
public class MainTest {
    private static final Logger log = Logger.getLogger(MainTest.class.getName());

    private DBH2Connector h2Connector = new DBH2Connector();

    @Before
    public void setUp() {
        try {
            h2Connector.dropTable();
        } catch (Exception ignore) {
            log.log(Level.FINE, "Error on dropping h2 testing table", ignore);
        }
    }

    @Test
    public void createTable() {
        String[] args = enrichArray(ArgumentParserTest.H2_CONNECTION_ARGS, "-c", "create");

        Main.main(args);

        // we are fine that call passes without exception
        String out = h2Connector.selectAll();
        Assert.assertTrue("Expecting no data was inserted", out.isEmpty());
    }

    @Test
    public void insertAppPod() {
        String[] args = enrichArray(ArgumentParserTest.H2_CONNECTION_ARGS,
            "-c", "insert", "-a", "goodone", "-r", "badone");

        Main.main(args);

        String out = h2Connector.selectAll();
        log.info("Selected data is: '" + out + "'");
        Assert.assertTrue("Expecting the value of parameter '-a' which was 'good' was added to database", out.contains("good"));
        Assert.assertTrue("Expecting the value of parameter '-r' whic was 'bad' was added to database", out.contains("bad"));
        Assert.assertEquals("Expecting only one row was added into the database", 1, out.split(";").length);
    }

    @Test
    public void deleteApplicationPod() {
        String[] argsInsert = enrichArray(ArgumentParserTest.H2_CONNECTION_ARGS,
                "-c", "insert", "-a", "goodone", "-r", "badone");
        Main.main(argsInsert);

        String out = h2Connector.selectAll();
        Assert.assertEquals("Expecting one row was added into the database", 1, out.split(";").length);

        String[] argsDelete = enrichArray(ArgumentParserTest.H2_CONNECTION_ARGS,
                "-c", "delete", "-a", "goodone", "-r", "badone");
        Main.main(argsDelete);

        out = h2Connector.selectAll();
        Assert.assertTrue("Expecting data whic were inserted were removed", out.isEmpty());
    }

    @Test
    public void deleteRecoveryPod() {
        String[] argsInsert = enrichArray(ArgumentParserTest.H2_CONNECTION_ARGS,
                "-c", "insert", "-a", "goodone", "-r", "badone");
        Main.main(argsInsert);

        String out = h2Connector.selectAll();
        Assert.assertEquals("Expecting one row was added into the database", 1, out.split(";").length);

        String[] argsDelete = enrichArray(ArgumentParserTest.H2_CONNECTION_ARGS,
                "-c", "delete", "-r", "badone");
        Main.main(argsDelete);

        out = h2Connector.selectAll();
        Assert.assertTrue("Expecting data whic were inserted were removed", out.isEmpty());
    }

    @Test
    public void selectRecovery() throws Exception {
        String[] argsInsert = enrichArray(ArgumentParserTest.H2_CONNECTION_ARGS,
                "-c", "insert", "-a", "goodone", "-r", "badone");
        Main.main(argsInsert);

        // a little bit hacking for reading system out
        java.io.ByteArrayOutputStream systemOut = new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(systemOut));
        String[] argsSelectByApplicationPod = enrichArray(ArgumentParserTest.H2_CONNECTION_ARGS,
                "-c", "select_recovery", "-a", "goodone");
        Main.main(argsSelectByApplicationPod);
        Assert.assertFalse("Select should not print name of app pod", systemOut.toString().contains("goodone"));
        Assert.assertTrue("Select should print name of rec pod",systemOut.toString().contains("badone"));

        systemOut = new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(systemOut));
        String[] argsSelectByRecoveryPod = enrichArray(ArgumentParserTest.H2_CONNECTION_ARGS,
                "-c", "select_recovery", "-r", "badone");
        Main.main(argsSelectByRecoveryPod);
        Assert.assertFalse("Select should not print name of app pod", systemOut.toString().contains("goodone"));
        Assert.assertTrue("Select should print name of rec pod", systemOut.toString().contains("badone"));
    }

    @Test
    public void selectApplication() throws Exception {
        String[] argsInsert = enrichArray(ArgumentParserTest.H2_CONNECTION_ARGS,
                "-c", "insert", "-a", "goodone", "-r", "badone");
        Main.main(argsInsert);

        // a little bit hacking for reading system out
        java.io.ByteArrayOutputStream systemOut = new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(systemOut));
        String[] argsSelectByApplicationPod = enrichArray(ArgumentParserTest.H2_CONNECTION_ARGS,
                "-c", "select_application", "-a", "goodone");
        Main.main(argsSelectByApplicationPod);
        Assert.assertTrue("Select should print name of app pod", systemOut.toString().contains("goodone"));
        Assert.assertFalse("Select should not print name of rec pod", systemOut.toString().contains("badone"));

        systemOut = new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(systemOut));
        String[] argsSelectByRecoveryPod = enrichArray(ArgumentParserTest.H2_CONNECTION_ARGS,
                "-c", "select_application", "-r", "badone");
        Main.main(argsSelectByRecoveryPod);
        Assert.assertTrue("Select should print name of app pod", systemOut.toString().contains("goodone"));
        Assert.assertFalse("Select should not print name of rec pod", systemOut.toString().contains("badone"));
    }

    private String[] enrichArray(String[] baseArray, String... argumentsToAdd) {
        List<String> args = new ArrayList<String>(Arrays.asList(baseArray));
        for(String str: argumentsToAdd) {
            args.add(str);
        }
        return args.toArray(new String[] {});
    }
}
