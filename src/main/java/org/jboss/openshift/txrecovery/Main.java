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
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;
import org.jboss.openshift.txrecovery.cliargs.ArgumentParser;
import org.jboss.openshift.txrecovery.cliargs.ArgumentParserException;
import org.jboss.openshift.txrecovery.cliargs.CommandType;
import org.jboss.openshift.txrecovery.cliargs.OutputFormatType;

/**
 * Class processing the arguments and calling service to save, delete data in database.
 */
public class Main {
    private static final Logger log = Logger.getLogger(Main.class.getName());


    public static void main(String[] args) {
        ArgumentParser parsedArguments = null;
        try {
            parsedArguments = ArgumentParser.parse(args);
        } catch (ArgumentParserException ape) {
            log.log(Level.FINE, "Error on parsing arguments: " + Arrays.asList(args), ape);
            System.exit(1);
        }

        // Hibernate setup
        Properties setupProperties = HibernateSetup.getConfigurationProperties(parsedArguments);
        final ServiceRegistry standardRegistry = HibernateSetup.getStandardRegistry(setupProperties);
        Metadata metadata = HibernateSetup.getHibernateStartupMetadata(setupProperties, standardRegistry);
        SessionFactory sessionFactory = metadata.buildSessionFactory();
        Session session = sessionFactory.openSession();

        // Gathering table name we use for saving the recovery marker
        String podTableName = HibernateSetup.getTableName(setupProperties);

        List<String> outputListing = null;
        try {
            outputListing = processDatabaseUpdate(parsedArguments, podTableName, session, metadata);
        } finally {
            HibernateSetup.close(sessionFactory, session);
            // https://stackoverflow.com/a/22278250/187035
            if(standardRegistry!= null) {
                StandardServiceRegistryBuilder.destroy(standardRegistry);
            }
        }

        printToStandardOutput(outputListing, parsedArguments.getFormat());
    }

    private static List<String> processDatabaseUpdate(ArgumentParser parsedArguments, String tableName, Session session, Metadata metadata) {
        List<String> outputListing = new ArrayList<String>();
        ApplicationRecoveryPodDAO dtoService = new ApplicationRecoveryPodDAO(session);

        switch(parsedArguments.getCommand()) {
            case CREATE:
                if(!dtoService.tableExists(tableName)) HibernateSetup.createTable(metadata);
                break;
            case INSERT:
                String appPod = parsedArguments.getApplicationPodName();
                String recPod = parsedArguments.getRecoveryPodName();
                if(appPod == null || appPod.isEmpty())
                    throw new IllegalArgumentException("For command '" + parsedArguments.getCommand().name()
                        + "' application pod name has to be specified. Use cli argument '-a/--application_pod_name'");
                if(recPod == null || recPod.isEmpty())
                    throw new IllegalArgumentException("For command '" + parsedArguments.getCommand().name()
                            + "' recovery pod name has to be specified. Use cli argument '-r/--recovery_pod_name'");

                if(!dtoService.tableExists(tableName)) HibernateSetup.createTable(metadata);
                if(!dtoService.saveRecord(appPod, recPod)) {
                    throw new IllegalStateException("Error on saving data [" + appPod +"," + recPod + "] to db "
                        + parsedArguments.getJdbcUrl() + " and table " + parsedArguments.getTableName());
                }
                break;
            case DELETE:
                appPod = parsedArguments.getApplicationPodName();
                recPod = parsedArguments.getRecoveryPodName();
                int numberDeleted = dtoService.delete(appPod, recPod);
                log.info("Number ["  + numberDeleted + "] of records deleted while filtered at [application pod: "
                    + appPod + ", recovery pod: " + recPod + "]");
                break;
            case SELECT_APPLICATION:
            case SELECT_RECOVERY:
                appPod = parsedArguments.getApplicationPodName();
                recPod = parsedArguments.getRecoveryPodName();
                Collection<ApplicationRecoveryPod> dtos = dtoService.getRecords(appPod, recPod);
                for(ApplicationRecoveryPod dto: dtos) {
                    if(parsedArguments.getCommand() == CommandType.SELECT_APPLICATION)
                        outputListing.add(dto.getApplicationPodName());
                    if(parsedArguments.getCommand() == CommandType.SELECT_RECOVERY)
                        outputListing.add(dto.getRecoveryPodName());
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown handler for command '" + parsedArguments.getCommand() + "'");
        }

        return outputListing;
    }

    private static void printToStandardOutput(List<String> dataToPrint, OutputFormatType printingFormat) {
        switch(printingFormat) {
            case LIST_COMMA:
                System.out.println(
                    dataToPrint.stream().collect(Collectors.joining(", ")));
                break;
            case RAW:
                System.out.println(dataToPrint);
                break;
            case LIST_SPACE:
            default:
                System.out.println(
                    dataToPrint.stream().collect(Collectors.joining(" ")));
        }
    }

}
