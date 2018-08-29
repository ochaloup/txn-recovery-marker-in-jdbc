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

package org.jboss.openshift.txrecovery.cliargs;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper of options which are permitted
 * to be get from the command line.
 */
class Options {
    List<OptionsData> optionData = new ArrayList<OptionsData>();

    /**
     * Options data storage
     */
    static class OptionsData {
        String shortName, longName, description;
        boolean withArgument, isRequired;
        String value;

        static void add(List<OptionsData> listToAddTo, String shortName, String longName, boolean withArgument, String description, boolean isRequired) {
            OptionsData od = new OptionsData(shortName, longName, withArgument, description, isRequired);
            listToAddTo.add(od);
        }

        private OptionsData(String shortName, String longName, boolean withArgument, String description, boolean isRequired) {
            this.shortName = shortName;
            this.longName = longName;
            this.withArgument = withArgument;
            this.description = description;
            this.isRequired = isRequired;
        }

        public String getShortName() {
            return shortName;
        }
        public String getLongName() {
            return longName;
        }
        public String getDescription() {
            return description;
        }
        public boolean isWithArgument() {
            return withArgument;
        }
        public boolean isRequired() {
            return isRequired;
        }
        public void setValue(String value) {
            this.value = value;
        }
        public String getValue() {
            return this.value;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((shortName == null) ? 0 : shortName.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            OptionsData other = (OptionsData) obj;
            if (shortName == null) {
                if (other.shortName != null)
                    return false;
            } else if (!shortName.equals(other.shortName))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return String.format("-%s/--%s : %s", shortName, longName, description);
        }
    }

    Options addOption(String shortName, String longName, boolean withArgument, String description) {
        OptionsData.add(optionData, shortName, longName, withArgument, description, false);
        return this;
    }

    Options addRequiredOption(String shortName, String longName, boolean withArgument, String description) {
        OptionsData.add(optionData, shortName, longName, withArgument, description, true);
        return this;
    }

    OptionsData getOption(String arg) {
        String adjustedArg = arg.trim().replaceFirst("^[-]*", "");
        for(OptionsData oneOptionData: optionData) {
            if(adjustedArg.equals(oneOptionData.getShortName()) || adjustedArg.equals(oneOptionData.getLongName())) {
                return oneOptionData;
            }
        }
        return null;
    }

    List<OptionsData> getAllOptions() {
        return this.optionData;
    }

    void printHelpToStdErr() {
        for(OptionsData oneOptionData: optionData) {
            System.err.println(String.format("-%s/--%s : %s", oneOptionData.shortName, oneOptionData.longName, oneOptionData.description));
        }
    }
}
