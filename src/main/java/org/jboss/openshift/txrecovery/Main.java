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

import java.util.EnumSet;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.jboss.openshift.txrecovery.cliargs.ArgumentParser;

/**
 * Class processing the arguments and calling service to save, delete data in database.
 */
public class Main {
    private static final Logger log = Logger.getLogger(Main.class.getName());


    public static void main( String[] args ) {
        ArgumentParser parsedArguments = ArgumentParser.parse(args);

        // Hibernate setup
        Properties setupProperties = HibernateSetup.getConfigurationProperties();
        Metadata metadata = HibernateSetup.getHibernateStartupMetadata(setupProperties);
        SessionFactory sessionFactory = metadata.buildSessionFactory();
        Session session = sessionFactory.openSession();
        ApplicationRecoveryPodDao dtoService = new ApplicationRecoveryPodDao(session);

        // Gathering table name of dto we use for saving the recovery marker 
        String appRecoveryPodTableName = HibernateSetup.getTableName(setupProperties);

        switch(parsedArguments.getCommand()) {
            case CREATE:
                System.out.println("Ahoj");
                break;
            default: 
                System.out.println("heyhou");
        }
/*
        try {

            // create
            if(action.equals(ArgumentParser.CREATE.getOption())) {
                checkArg(ArgumentParser.CREATE, args, 3, "application and recovery pod names");
                if(!dtoService.tableExists(appRecoveryPodTableName)) createTable(metadata);
                if(!dtoService.saveRecord(args[1], args[2])) {
                    System.exit(1);
                }

            // delete
            } else if(action.equals(ArgumentParser.DELETE.getOption())) {
                checkArg(ArgumentParser.DELETE, args, 3, "application and recovery pod names to delete the marker for");
                ApplicationRecoveryPodDto dto = dtoService.getRecord(args[1], args[2]);
                if(!dtoService.deleteRecord(dto)) {
                    log.info("Cannot delete record based on application '" + args[1] + "' and recovery '" + args[2] + "'. Possibly any such record found.");
                    System.exit(1);
                }

            // delete_by_application
            } else if(action.equals(ArgumentParser.DELETE_BY_APPLICATION_POD.getOption())) {
                checkArg(ArgumentParser.DELETE_BY_APPLICATION_POD, args, 2, "application pod name to delete the marker for");
                if(!dtoService.deleteRecordsByApplicationPodname(args[1])) {
                    log.info("Cannot delete all records based on application pod name '" + args[1] + "'. Possibly any such record found.");
                    System.exit(1);
                }

            // delete_by_recovery
            } else if(action.equals(ArgumentParser.DELETE_BY_RECOVERY_POD.getOption())) {
                checkArg(ArgumentParser.DELETE_BY_RECOVERY_POD, args, 2, "recovery pod name to delete the marker for");
                if(!dtoService.deleteRecordsByRecoveryPodname(args[1])) {
                    log.info("Cannot delete all records based on recovery pod name '" + args[1] + "'. Possibly any such record found.");
                    System.exit(1);
                }

            // get
            } else if(action.equals(ArgumentParser.GET.getOption())) {
                try {
                    checkArg(ArgumentParser.GET, args, 3, "application and recovery pod name as keys to find");
                    ApplicationRecoveryPodDto dto = dtoService.getRecord(args[1], args[2]);
                    if(dto == null) System.exit(1); // no record found
                    System.out.println(dto.getApplicationPodName() + " " + dto.getRecoveryPodName());
                } catch (Exception e) {
                    // the record on the get does not exist probably
                    log.log(Level.WARNING, "A record bound to the app pod name '" + args[1] + "' and recovery pod name '" + args[2] + "' was not found", e);
                    System.exit(1);
                }

            // get_by_application
            } else if(action.equals(ArgumentParser.GET_BY_APPLICATION_POD.getOption())) {
                try {
                    checkArg(ArgumentParser.GET_BY_APPLICATION_POD, args, 2, "application pod name as keys to find");
                    Collection<ApplicationRecoveryPodDto> dtos = dtoService.getRecordsByAppPod(args[1]);
                    if(dtos == null || dtos.isEmpty()) System.exit(1); // no record found
                    StringBuffer recoveryPodsListing = new StringBuffer();
                    for(ApplicationRecoveryPodDto dto: dtos) {
                        recoveryPodsListing.append(dto.getRecoveryPodName()).append(" ");
                    }
                    System.out.println(recoveryPodsListing.toString());
                } catch (Exception e) {
                    // the record on the get does not exist probably
                    log.log(Level.WARNING, "A record bound to the app pod name '" + args[1] + "' was not found", e);
                    System.exit(1);
                }

            // get_all_recovery
            } else if(action.equals(ArgumentParser.GET_ALL_RECOVERY_PODS.getOption())) {
                try {
                    Collection<ApplicationRecoveryPodDto> dtos = dtoService.getAllRecords();
                    StringBuffer recoveryPodsListing = new StringBuffer();
                    for(ApplicationRecoveryPodDto dto: dtos) {
                        recoveryPodsListing.append(dto.getRecoveryPodName()).append(" ");
                    }
                    System.out.println(recoveryPodsListing.toString());
                } catch (Exception e) {
                    // the record on the get does not exist probably
                    log.log(Level.WARNING, "Table '" + appRecoveryPodTableName + "' probably does not exist, no records to list", e);
                    System.exit(1);
                }
            } else {
                throw new IllegalArgumentException("Action '" + args[0] + "' is not expected for the tool which expects" +
                    " an action name from list: " + Arrays.asList(ArgumentParser.values()));
            }
        } finally {
            close(sessionFactory, session);
        }
        */
    }

    private static boolean createTable(Metadata metadata) {
        SchemaExport schemaExport = new SchemaExport();
        schemaExport.createOnly( EnumSet.of( TargetType.DATABASE ), metadata);
        log.log(Level.SEVERE, "exception: " + schemaExport.getExceptions());
        /*
        // https://stackoverflow.com/a/22278250/187035
        if(standardRegistry!= null) {
            StandardServiceRegistryBuilder.destroy(standardRegistry);
        }
        */
        return schemaExport.getExceptions() == null || schemaExport.getExceptions().isEmpty();
    }

    private static void close(SessionFactory sf, Session s) {
        if(s.isOpen()) s.close();
        if(!sf.isClosed()) sf.close();
    }

}
