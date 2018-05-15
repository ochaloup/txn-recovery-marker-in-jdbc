package org.jboss.openshift;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.NoResultException;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.criterion.Restrictions;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

/**
 * Generate schema, persist and query with Hibernate API.
 */
public class Main {
    private static final Logger log = Logger.getLogger(Main.class.getName());

    public static final String PROPERTIES_FILE_PARAM = "properties.file";
    public static final String DB_TABLE_NAME_PARAM = "db.table.name";
    public static final String HIBERNATE_DIALECT_PARAM = "hibernate.dialect";
    public static final String HIBERNATE_CONNECTION_DRIVER_CLASS_PARAM = "hibernate.connection.driver_class";
    public static final String HIBERNATE_CONNECTION_URL_PARAM = "hibernate.connection.url";
    public static final String HIBERNATE_CONNECTION_USERNAME_PARAM = "hibernate.connection.username";
    public static final String HIBERNATE_CONNECTION_PASSWORD_PARAM = "hibernate.connection.password";

    public static void main( String[] args ) {
        if(args.length < 1) throw new IllegalArgumentException("No argument specified. "
            + "Expecting at least action to do from list: " + Arrays.asList(ArgsOptions.values()));

        Properties setupProperties = getConfigurationProperties();
        Metadata metadata = getHibernateStartupMetadata(setupProperties);
        SessionFactory sessionFactory = metadata.buildSessionFactory();
        Session session = sessionFactory.openSession();

        try {

            // gathering table name of dto we use for saving the recovery marker 
            String appRecoveryPodTableName = setupProperties.getProperty(DB_TABLE_NAME_PARAM);
            if(appRecoveryPodTableName == null) appRecoveryPodTableName = ApplicationRecoveryPodDTO.TABLE_NAME;
    
            String action = args[0];
            // create
            if(action.equals(ArgsOptions.CREATE.getOption())) {
                checkArg(ArgsOptions.CREATE, args, 3, "application and recovery pod names");
                if(!tableExists(session, appRecoveryPodTableName)) createTable(metadata);
                saveRecord(session, args[1], args[2]);
            // delete_by_application
            } else if(action.equals(ArgsOptions.DELETE_BY_APPLICATION_POD.getOption())) {
                checkArg(ArgsOptions.DELETE_BY_APPLICATION_POD, args, 2, "application pod name to delete the marker for");
                deleteRecordByApplicationPodname(session, args[1]);
            // delete_by_recovery
            } else if(action.equals(ArgsOptions.DELETE_BY_RECOVERY_POD.getOption())) {
                checkArg(ArgsOptions.DELETE_BY_RECOVERY_POD, args, 2, "recovery pod name to delete the marker for");
                deleteRecordByRecoveryPodname(session, args[1]);
            // get
            } else if(action.equals(ArgsOptions.GET.getOption())) {
                checkArg(ArgsOptions.GET, args, 2, "application pod name as key to find");
                System.out.println(getRecordByAppPod(session, args[1]));
            }
        } finally {
            close(sessionFactory, session);
        }
    }

    private static void close(SessionFactory sf, Session s) {
        if(s.isOpen()) s.close();
        if(!sf.isClosed()) sf.close();
    }

    private static void checkArg(ArgsOptions artOpt, String[] args, int expectedNumberOfArguments, String additionalMsg) {
        if(args.length != expectedNumberOfArguments) {
            throw new IllegalArgumentException("Action '" + artOpt.getOption() +
                "' expects " + (expectedNumberOfArguments - 1) + " more argument, " + additionalMsg);
        }
    }

    public static boolean saveRecord(Session session, String applicationPodName, String recoveryPodName) {
        session.getTransaction().begin();
        ApplicationRecoveryPodDTO record = new ApplicationRecoveryPodDTO(
                applicationPodName, recoveryPodName);
        try {
            session.persist(record);
            session.getTransaction().commit();
        } catch (Exception e) {
            if(session.getTransaction() != null && session.getTransaction().getStatus() == TransactionStatus.ACTIVE)
                session.getTransaction().rollback();
            log.log(Level.SEVERE, "Cannot persist record: " + record, e);
            return false;
        }
        return true;
    }
    
    public static boolean deleteRecordByApplicationPodname(Session session, String applicationPodName) {
        ApplicationRecoveryPodDTO recordDto = getRecordByAppPod(session, applicationPodName);
        return deleteRecord(session, recordDto);
    }
    
    public static boolean deleteRecordByRecoveryPodname(Session session, String recoveryPodName) {
        ApplicationRecoveryPodDTO recordDto = getRecordByRecoveryPod(session, recoveryPodName);
        return deleteRecord(session, recordDto);
    }

    public static boolean deleteRecord(Session session, ApplicationRecoveryPodDTO recordDto) {
        session.getTransaction().begin();
        try {
            session.delete(recordDto);
            session.getTransaction().commit();
        } catch (Exception e) {
            if(session.getTransaction() != null && session.getTransaction().getStatus() == TransactionStatus.ACTIVE)
                session.getTransaction().rollback();
            log.log(Level.SEVERE, "Cannot remove record: " + recordDto, e);
            return false;
        }
        return true;
    }

    public static boolean tableExists(final Session session, final String tableName) {
        try {
            return session.doReturningWork(
                new ReturningWork<Boolean>() {
                    public Boolean execute(Connection connection) throws SQLException {
                        ResultSet tables = connection.getMetaData().getTables(null,null,tableName,null);
                        try {
                            while(tables.next()) {
                                String currentTableName = tables.getString("TABLE_NAME");
                                if(currentTableName.equals(tableName)) return true;
                            }
                        } finally {
                            if(tables != null) tables.close();
                        }
                        return false;
                    }
                }
            );
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error on searching existence of table " + tableName, e);
            return false;
        }
    }

    public static ApplicationRecoveryPodDTO getRecordByAppPod(Session session, String applicationPodName) {
        // the Criteria is deprecated in Hibernate 5.2 (see https://github.com/treehouse/giflib-hibernate/commit/f97a2828a466e849d8ae84884b5dce60a66cf412)
        return (ApplicationRecoveryPodDTO) session.createCriteria(ApplicationRecoveryPodDTO.class)
          .add(Restrictions.eq("id.applicationPodName", applicationPodName))
          .uniqueResult();
    }
    
    public static ApplicationRecoveryPodDTO getRecordByRecoveryPod(Session session, String recoveryPodName) {
        // the Criteria is deprecated in Hibernate 5.2
        return (ApplicationRecoveryPodDTO) session.createCriteria(ApplicationRecoveryPodDTO.class)
                .add(Restrictions.eq("id.recoveryPodName", recoveryPodName))
                .uniqueResult();
    }
    
    public static boolean createTable(Metadata metadata) {
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

    public static Metadata getHibernateStartupMetadata(Properties setupProperties) {
        StandardServiceRegistryBuilder standardRegistryBuilder = new StandardServiceRegistryBuilder();
        standardRegistryBuilder.applySettings((Map) setupProperties);

        // loading name of table that will be used for saving data, in null then value is not used
        final String tableName = setupProperties.getProperty(DB_TABLE_NAME_PARAM);

        final ServiceRegistry standardRegistry = standardRegistryBuilder.build();
        MetadataSources sources = new MetadataSources(standardRegistry)
                .addAnnotatedClass(ApplicationRecoveryPodDTO.class);
        MetadataBuilder metadataBuilder = sources.getMetadataBuilder();
        metadataBuilder.applyPhysicalNamingStrategy(new PhysicalNamingStrategyStandardImpl() {
            private static final long serialVersionUID = 1L;
            @Override
            public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment jdbcEnvironment) {
                if(name.getCanonicalName().equals(ApplicationRecoveryPodDTO.TABLE_NAME) && tableName != null)
                    return Identifier.toIdentifier(tableName);
                return name;
            }
        });

        return metadataBuilder.build();
    }

    private static Properties getConfigurationProperties() {
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
