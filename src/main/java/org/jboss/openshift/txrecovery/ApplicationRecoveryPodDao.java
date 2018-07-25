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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.resource.transaction.spi.TransactionStatus;

/**
 * Data manipulation service working with the {@link ApplicationRecoveryPodDto}.
 */
public class ApplicationRecoveryPodDao {
    private static final Logger log = Logger.getLogger(ApplicationRecoveryPodDao.class.getName());

    private Session session;

    public ApplicationRecoveryPodDao(Session session) {
        this.session = session;
    }

    /**
     * Save a single record with pod name content.
     *
     * @param applicationPodName  app pod name to be saved
     * @param recoveryPodName  recovery pod name to be saved
     * @return true if saved successfully, false otherwise
     */
    public boolean saveRecord(String applicationPodName, String recoveryPodName) {
        session.getTransaction().begin();
        ApplicationRecoveryPodDto record = new ApplicationRecoveryPodDto(
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

    /**
     * To delete all records which links the app pod name.
     *
     * @param applicationPodName  app pod name for records to be deleted
     * @return true if deleted, false otherwise
     */
    public boolean deleteRecordsByApplicationPodname(String applicationPodName) {
        Collection<ApplicationRecoveryPodDto> recordDtos = getRecordsByAppPod(applicationPodName);
        boolean result = true;
        for(ApplicationRecoveryPodDto recordDto: recordDtos) {
            if(!deleteRecord(recordDto)) result = false;
        }
        return result;
    }

    /**
     * To delete all records which links the recovery pod name.
     *
     * @param recoveryPodName  recovery pod name for records to be deleted
     * @return true if deleted, false otherwise
     */
    public boolean deleteRecordsByRecoveryPodname(String recoveryPodName) {
        Collection<ApplicationRecoveryPodDto> recordDtos = getRecordsByRecoveryPod(recoveryPodName);
        boolean result = true;
        for(ApplicationRecoveryPodDto recordDto: recordDtos) {
            if(!deleteRecord(recordDto)) result = false;
        }
        return result;
    }

    /**
     * To delete a record.
     *
     * @param recordDto  dto to be deleted
     * @return true if deleted, false otherwise
     */
    public boolean deleteRecord(ApplicationRecoveryPodDto recordDto) {
        if(recordDto == null) return false;

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

    /**
     * Verifies if table name exists in the database.
     *
     * @param tableName  table name to be found
     * @return true if found, false otherwise
     */
    public boolean tableExists(final String tableName) {
        try {
            return session.doReturningWork(
                new ReturningWork<Boolean>() {
                    public Boolean execute(Connection connection) throws SQLException {
                        ResultSet tables = connection.getMetaData().getTables(null,null,tableName,null);
                        boolean isCaseSensitive = connection.getMetaData().supportsMixedCaseIdentifiers();
                        try {
                            while(tables.next()) {
                                String currentTableName = tables.getString("TABLE_NAME");
                                if(isCaseSensitive) {
                                    if(currentTableName.equals(tableName)) return true;
                                } else {
                                    if(currentTableName.equalsIgnoreCase(tableName)) return true;
                                }
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

    /**
     * To get a record.
     *
     * @param applicationPodName  record will be searched with app pod name of this parameter
     * @param recoveryPodName  record will be searched with recovery pod name of this parameter
     * @return the record or null
     */
    public ApplicationRecoveryPodDto getRecord(String applicationPodName, String recoveryPodName) {
        // the Criteria is deprecated in Hibernate 5.2 (see https://github.com/treehouse/giflib-hibernate/commit/f97a2828a466e849d8ae84884b5dce60a66cf412)
        return (ApplicationRecoveryPodDto) session.createCriteria(ApplicationRecoveryPodDto.class)
                .add(Restrictions.eq("id.applicationPodName", applicationPodName))
                .add(Restrictions.eq("id.recoveryPodName", recoveryPodName))
                .uniqueResult();
    }

    /**
     * To get all recovery marker records.
     *
     * @return the records or null
     */
    @SuppressWarnings("unchecked")
    public Collection<ApplicationRecoveryPodDto> getAllRecords() {
        return session.createCriteria(ApplicationRecoveryPodDto.class).list();
    }

    /**
     * To get records that contains specified app pod name.
     *
     * @param applicationPodName  app pod name to filter recovery markers by
     * @return the records or null
     */
    @SuppressWarnings("unchecked")
    public Collection<ApplicationRecoveryPodDto> getRecordsByAppPod(String applicationPodName) {
        return session.createCriteria(ApplicationRecoveryPodDto.class)
          .add(Restrictions.eq("id.applicationPodName", applicationPodName))
          .list();
    }
    
    /**
     * To get records that contains specified recovery pod name.
     *
     * @param recoveryPodName  recovery pod name to filter recovery markers by
     * @return the records or null
     */
    @SuppressWarnings("unchecked")
    public Collection<ApplicationRecoveryPodDto> getRecordsByRecoveryPod(String recoveryPodName) {
        return session.createCriteria(ApplicationRecoveryPodDto.class)
            .add(Restrictions.eq("id.recoveryPodName", recoveryPodName))
            .list();
    }

}
