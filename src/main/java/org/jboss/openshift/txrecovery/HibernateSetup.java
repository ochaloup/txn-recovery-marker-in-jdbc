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

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.service.ServiceRegistry;

/**
 * Utility methods to setup hibernate standalone app.
 */
public final class HibernateSetup {
    private static final Logger log = Logger.getLogger(HibernateSetup.class.getName());

    private HibernateSetup() {
        // utility class
    }

    public static final String PROPERTIES_FILE_PARAM = "properties.file";
    public static final String DB_TABLE_NAME_PARAM = "db.table.name";
    public static final String HIBERNATE_DIALECT_PARAM = "hibernate.dialect";
    public static final String HIBERNATE_CONNECTION_DRIVER_CLASS_PARAM = "hibernate.connection.driver_class";
    public static final String HIBERNATE_CONNECTION_URL_PARAM = "hibernate.connection.url";
    public static final String HIBERNATE_CONNECTION_USERNAME_PARAM = "hibernate.connection.username";
    public static final String HIBERNATE_CONNECTION_PASSWORD_PARAM = "hibernate.connection.password";

    /**
     * Boot-up the app by gathering properties needed for Hibernate start-up.
     *
     * @return  properties for hibernate being able to connect to db
     */
    public static Properties getConfigurationProperties() {
        Properties outputProperties = new Properties();

        // loading properties from properties file if specified
        if(getProperty(PROPERTIES_FILE_PARAM) != null) {
            File propFile = new File(getProperty(PROPERTIES_FILE_PARAM));
            if(!propFile.exists()) throw new IllegalArgumentException("Argument " + PROPERTIES_FILE_PARAM
                    + " does not point to and existing file but defines it as " + getProperty(PROPERTIES_FILE_PARAM));

            FileInputStream inputStreamProperties = null;
            try {
                inputStreamProperties = new FileInputStream(propFile);
                outputProperties.load(inputStreamProperties);
            } catch (Exception e) {
                throw new IllegalStateException("Cannot load properties from file " + propFile, e);
            } finally {
                try {
                    if(inputStreamProperties != null) inputStreamProperties.close();
                } catch (Exception e) {
                    log.log(Level.FINE, "Cannot close input stream of file " + propFile, e);
                }
            }
        }

        // taking properties from env and system properties
        setProperty(outputProperties, HIBERNATE_DIALECT_PARAM);
        setProperty(outputProperties, HIBERNATE_CONNECTION_DRIVER_CLASS_PARAM);
        setProperty(outputProperties, HIBERNATE_CONNECTION_URL_PARAM);
        setProperty(outputProperties, HIBERNATE_CONNECTION_USERNAME_PARAM);
        setProperty(outputProperties, HIBERNATE_CONNECTION_PASSWORD_PARAM);
        setProperty(outputProperties, DB_TABLE_NAME_PARAM);

        return outputProperties;
    }

    /**
     * Setting up the Hibernate as standalone app. It uses  the {@link Metadata} filled from provided properties.
     *
     * @param setupProperties properties, probably taken from {@link #getConfigurationProperties()}
     * @return hibernate metadata to be used for {@link Session} creation
     */
    @SuppressWarnings("rawtypes")
    public static Metadata getHibernateStartupMetadata(Properties setupProperties) {
        StandardServiceRegistryBuilder standardRegistryBuilder = new StandardServiceRegistryBuilder();
        standardRegistryBuilder.applySettings((Map) setupProperties);

        // loading name of table that will be used for saving data, in null then value is not used
        final String tableName = setupProperties.getProperty(DB_TABLE_NAME_PARAM);

        final ServiceRegistry standardRegistry = standardRegistryBuilder.build();
        MetadataSources sources = new MetadataSources(standardRegistry)
                .addAnnotatedClass(ApplicationRecoveryPodDto.class);
        MetadataBuilder metadataBuilder = sources.getMetadataBuilder();
        metadataBuilder.applyPhysicalNamingStrategy(new PhysicalNamingStrategyStandardImpl() {
            private static final long serialVersionUID = 1L;
            @Override
            public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment jdbcEnvironment) {
                if(name.getCanonicalName().equalsIgnoreCase(ApplicationRecoveryPodDto.TABLE_NAME) && tableName != null)
                    return Identifier.toIdentifier(tableName);
                return name;
            }
        });

        return metadataBuilder.build();
    }

    /**
     * Returning current table name being used in the app for saving the recovery markers.
     * 
     * @param setupProperties  properties to search for the db table name
     * @return name of table used in app
     */
    public static String getTableName(Properties setupProperties) {
        String appRecoveryPodTableName = setupProperties.getProperty(DB_TABLE_NAME_PARAM);
        if(appRecoveryPodTableName == null) appRecoveryPodTableName = ApplicationRecoveryPodDto.TABLE_NAME;
        return appRecoveryPodTableName;
    }

    private static String getProperty(String key) {
        if(key == null) throw new NullPointerException("key");
        String property = System.getProperty(key);
        if(property == null) property = System.getenv(key);
        if(property == null) property = System.getProperty(key.toLowerCase().replaceAll("_", "."));
        if(property == null) property = System.getenv(key.toUpperCase().replaceAll("[.]", "_"));
        return property;
    }
    
    private static Properties setProperty(Properties props, String key) {
        String value = getProperty(key);
        if(value != null) props.setProperty(key, value);
        return props;
    }
}
