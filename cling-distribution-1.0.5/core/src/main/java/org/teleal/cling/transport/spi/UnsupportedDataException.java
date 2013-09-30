/*
 * Copyright (C) 2010 Teleal GmbH, Switzerland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.teleal.cling.transport.spi;

/**
 * Thrown by processors/converters when errors occured.
 * <p>
 * This exception indicates that received data was in an invalid format and/or could
 * not be parsed or converted. You typically can recover from this failure after
 * catching (and logging?) the exception.
 * </p>
 *
 * @author Christian Bauer
 */
public class UnsupportedDataException extends RuntimeException {

    public UnsupportedDataException(String s) {
        super(s);
    }

    public UnsupportedDataException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
