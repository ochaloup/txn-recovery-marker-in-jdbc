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

import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.service.ServiceRegistry;
import org.jboss.openshift.txrecovery.cliargs.ArgumentParser;

/**
 * Utility methods to setup hibernate standalone app.
 */
public final class HibernateSetup {
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
     * <p>
     * Boot-up the app by gathering properties needed for Hibernate start-up.<br>
     * Method searches for values and then create the hibernate setup based on it.
     * <p>
     * It uses {@link ArgumentParser} to get values for the properties
     * and it uses system properties and env properties as another source
     * to setup values.
     *
     * @param arguments  values to  be used as the most important for the setup
     * @return  properties for hibernate being able to connect to db
     */
    public static Properties getConfigurationProperties(ArgumentParser args) {
        Properties outputProperties = getConfigurationProperties();

        setIfNotNull(HIBERNATE_DIALECT_PARAM, args.getHibernateDialect(), outputProperties);
        setIfNotNull(HIBERNATE_DIALECT_PARAM, args.getHibernateDialect(), outputProperties);
        setIfNotNull(HIBERNATE_CONNECTION_DRIVER_CLASS_PARAM, args.getJdbcDriverClass(), outputProperties);
        setIfNotNull(HIBERNATE_CONNECTION_URL_PARAM, args.getJdbcUrl(), outputProperties);
        setIfNotNull(HIBERNATE_CONNECTION_USERNAME_PARAM, args.getUser(), outputProperties);
        setIfNotNull(HIBERNATE_CONNECTION_PASSWORD_PARAM, args.getPassword(), outputProperties);
        setIfNotNull(DB_TABLE_NAME_PARAM, args.getTableName(), outputProperties);
        return outputProperties;
    }

    /**
     * Loading hibernate setup data only from environmental and system properties.
     *
     * See the #getConfigurationProperties(ArgumentParser)
     */
    public static Properties getConfigurationProperties() {
        Properties outputProperties = new Properties();
        getAndWriteProperty(HIBERNATE_DIALECT_PARAM, outputProperties);
        getAndWriteProperty(HIBERNATE_CONNECTION_DRIVER_CLASS_PARAM, outputProperties);
        getAndWriteProperty(HIBERNATE_CONNECTION_URL_PARAM, outputProperties);
        getAndWriteProperty(HIBERNATE_CONNECTION_USERNAME_PARAM, outputProperties);
        getAndWriteProperty(HIBERNATE_CONNECTION_PASSWORD_PARAM, outputProperties);
        getAndWriteProperty(DB_TABLE_NAME_PARAM, outputProperties);
        return outputProperties;
    }

    /**
     * Generate hibernate registry while filling it with properties.
     *
     * @param setupProperties  properties for connection
     * @return hibernate standard registry
     */
    @SuppressWarnings("rawtypes")
    public static StandardServiceRegistry getStandardRegistry(Properties setupProperties) {
        StandardServiceRegistryBuilder standardRegistryBuilder = new StandardServiceRegistryBuilder();
        standardRegistryBuilder.applySettings((Map) setupProperties);
        return standardRegistryBuilder.build();
    }
    /**
     * Setting up the Hibernate as standalone app. It uses  the {@link Metadata} filled from provided properties.
     *
     * @param setupProperties properties, probably taken from {@link #getConfigurationProperties()}
     * @return hibernate metadata to be used for {@link Session} creation
     */
    public static Metadata getHibernateStartupMetadata(Properties setupProperties, final ServiceRegistry standardRegistry) {
        // loading name of table that will be used for saving data, in null then value is not used
        final String tableName = setupProperties.getProperty(DB_TABLE_NAME_PARAM);

        MetadataSources sources = new MetadataSources(standardRegistry)
                .addAnnotatedClass(ApplicationRecoveryPod.class);
        MetadataBuilder metadataBuilder = sources.getMetadataBuilder();
        metadataBuilder.applyPhysicalNamingStrategy(new PhysicalNamingStrategyStandardImpl() {
            private static final long serialVersionUID = 1L;
            @Override
            public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment jdbcEnvironment) {
                if(name.getCanonicalName().equalsIgnoreCase(ApplicationRecoveryPod.TABLE_NAME) && tableName != null && !tableName.isEmpty())
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
        if(appRecoveryPodTableName == null) appRecoveryPodTableName = ApplicationRecoveryPod.TABLE_NAME;
        return appRecoveryPodTableName;
    }

    /**
     * Search of environment properties and system properties for the {@code key}.
     *
     * @param key  name of property which will be search for
     * @return  value belonging to the property
     */
    private static Optional<String> getProperty(String key) {
        if(key == null) throw new NullPointerException("key");
        String property = System.getProperty(key);
        if(property == null) property = System.getenv(key);
        if(property == null) property = System.getProperty(key.toLowerCase().replaceAll("_", "."));
        if(property == null) property = System.getenv(key.toUpperCase().replaceAll("[.]", "_"));
        return Optional.ofNullable(property);
    }

    /**
     * Get property value and if it's found it's written to the outputProperties
     */
    private static Optional<String> getAndWriteProperty(String key, final Properties propertiesToWriteIn) {
        Optional<String> value = getProperty(key);
        if(value.isPresent()) propertiesToWriteIn.setProperty(key, value.get());
        return value;
    }

    private static Properties setIfNotNull(String key, String value, final Properties propertiesToChange) {
        if(key != null && value != null && !key.isEmpty() && !value.isEmpty())
            propertiesToChange.setProperty(key, value);
        return propertiesToChange;
    }
}
