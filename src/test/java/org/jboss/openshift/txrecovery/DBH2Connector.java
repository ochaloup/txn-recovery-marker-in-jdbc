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

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;

public class DBH2Connector {
    public static final String DB_NAME = "txn-recovery-marker-test";
    public static final String DB_H2_CONNECTION = "jdbc:h2:mem:" + DB_NAME + ";DB_CLOSE_DELAY=-1";

    private DataSource ds;

    public DBH2Connector() {
        JdbcDataSource h2ds = new JdbcDataSource();
        h2ds.setUrl(DB_H2_CONNECTION);
        this.ds = h2ds;
    }

}
