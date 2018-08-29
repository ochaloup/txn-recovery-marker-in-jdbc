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

import java.util.HashSet;
import java.util.Set;

/**
 * Parses arguments from command line and joining the knowledge
 * with the permitted options.
 */
class ArgumentParser {
    private Set<Options.OptionsData> optionsDeclared = new HashSet<Options.OptionsData>();

    ArgumentParser() {
        // package private
    }

    void parse(Options options, String[] args) throws ArgumentParserException {
        if(options == null) throw new NullPointerException("options");
        if(args == null) return;

        Options.OptionsData currentOptionData = null;
        for(String arg: args) {
            if(currentOptionData == null) {
                // read argument
                Options.OptionsData data = options.getOption(arg);
                if(data == null)
                    throw new ArgumentParserException("Unknown argument '" + arg + "'");
                optionsDeclared.add(data);
                if(data.withArgument) currentOptionData = data;
            } else {
                // data read
                currentOptionData.setValue(arg);
                currentOptionData = null;
            }
        }

        // check required args
        for(Options.OptionsData optionData: options.getAllOptions()) {
            if(optionData.isRequired && !optionsDeclared.contains(optionData)) {
                throw new ArgumentParserException("The argument '" + optionData + "' is required");
            }
        }
    }

    String getOptionValue(String name) {
        for(Options.OptionsData option: optionsDeclared) {
            if(option.getLongName().equals(name) || option.getShortName().equals(name)) {
                return option.getValue();
            }
        }
        return null;
    }

    String getOptionValue(String name, String defaultValue) {
        String value = getOptionValue(name);
        if (value == null) return defaultValue;
        return value;
    }

    boolean hasOption(String name) {
        for(Options.OptionsData option: optionsDeclared) {
            if(option.getLongName().equals(name) || option.getShortName().equals(name)) {
                return true;
            }
        }
        return false;
    }
}
