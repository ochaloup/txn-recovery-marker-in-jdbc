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

/**
 * Program arguments supported by the main class.
 */
public enum ArgsOptions {
    CREATE("create"),
    GET("get"),
    GET_BY_APPLICATION_POD("get_by_application"),
    GET_ALL_RECOVERY_PODS("get_all_recovery"),
    DELETE("delete"),
    DELETE_BY_APPLICATION_POD("delete_by_application"),
    DELETE_BY_RECOVERY_POD("delete_by_recovery");

    private String option;
    
    private ArgsOptions(String option) {
        this.option = option;
    }

    public String getOption() {
        return option;
    }
}
