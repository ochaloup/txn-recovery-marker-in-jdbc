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
import java.util.Collection;
import java.util.EnumSet;
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
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.jboss.openshift.txrecovery.cliargs.ArgumentParser;
import org.jboss.openshift.txrecovery.cliargs.CommandType;

/**
 * Class processing the arguments and calling service to save, delete data in database.
 */
public class Main {
    private static final Logger log = Logger.getLogger(Main.class.getName());


    public static void main( String[] args ) {
        ArgumentParser parsedArguments = ArgumentParser.parse(args);

        // Hibernate setup
        Properties setupProperties = HibernateSetup.getConfigurationProperties(parsedArguments);
        final ServiceRegistry standardRegistry = HibernateSetup.getStandardRegistry(setupProperties);
        Metadata metadata = HibernateSetup.getHibernateStartupMetadata(setupProperties, standardRegistry);
        SessionFactory sessionFactory = metadata.buildSessionFactory();
        Session session = sessionFactory.openSession();
        ApplicationRecoveryPodDao dtoService = new ApplicationRecoveryPodDao(session);

        // Gathering table name of dto we use for saving the recovery marker 
        String appRecoveryPodTableName = HibernateSetup.getTableName(setupProperties);

        List<String> outputListing = new ArrayList<String>();
        try {
            switch(parsedArguments.getCommand()) {
                case CREATE:
                    if(!dtoService.tableExists(appRecoveryPodTableName)) createTable(metadata);
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

                    if(!dtoService.tableExists(appRecoveryPodTableName)) createTable(metadata);
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
                    Collection<ApplicationRecoveryPodDto> dtos = dtoService.getRecords(appPod, recPod);
                    for(ApplicationRecoveryPodDto dto: dtos) {
                        if(parsedArguments.getCommand() == CommandType.SELECT_APPLICATION)
                            outputListing.add(dto.getApplicationPodName());
                        if(parsedArguments.getCommand() == CommandType.SELECT_RECOVERY)
                            outputListing.add(dto.getRecoveryPodName());
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unknown handler for command '" + parsedArguments.getCommand() + "'");
            }
        } finally {
            close(sessionFactory, session);
            // https://stackoverflow.com/a/22278250/187035
            if(standardRegistry!= null) {
                StandardServiceRegistryBuilder.destroy(standardRegistry);
            }
        }

        switch(parsedArguments.getFormat()) {
            case LIST_COMMA:
                System.out.println(
                    outputListing.stream().collect(Collectors.joining(", ")));
                break;
            case RAW:
                System.out.println(outputListing);
                break;
            case LIST_SPACE:
            default:
                System.out.println(
                        outputListing.stream().collect(Collectors.joining(" ")));
        }
    }

    private static boolean createTable(Metadata metadata) {
        SchemaExport schemaExport = new SchemaExport();
        schemaExport.createOnly( EnumSet.of( TargetType.DATABASE ), metadata);
        log.log(Level.SEVERE, "exception: " + schemaExport.getExceptions());
        return schemaExport.getExceptions() == null || schemaExport.getExceptions().isEmpty();
    }

    private static void close(SessionFactory sf, Session s) {
        if(s.isOpen()) s.close();
        if(!sf.isClosed()) sf.close();
    }

}
