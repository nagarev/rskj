/*
 * This file is part of RskJ
 * Copyright (C) 2023 RSK Labs Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package co.rsk.jmh.runners;

import org.openjdk.jmh.runner.options.CommandLineOptionException;
import org.openjdk.jmh.runner.options.CommandLineOptions;
import org.openjdk.jmh.util.Optional;

import java.util.Collection;

public class ParamsHelper {

    private ParamsHelper() {
    }

    static String getRequired(String name, String[] args) throws CommandLineOptionException {
        CommandLineOptions commandLine = new CommandLineOptions(args);
        Optional<Collection<String>> paramCol = commandLine.getParameter(name);

        if (!paramCol.hasValue()) {
            throw new IllegalStateException("Providing a " + name + " is required");
        }

        String field = paramCol.get().stream().findFirst().orElse(null);
        if (field == null) {
            throw new IllegalStateException("Providing a " + name + " is required");
        }

        return field;
    }

}
