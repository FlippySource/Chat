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

package org.teleal.cling.model.message;

/**
 * A response message, with a status code and message (OK, NOT FOUND, etc).
 *
 * @author Christian Bauer
 */
public class UpnpResponse extends UpnpOperation {

    public static enum Status {

        OK(200, "OK"),
        BAD_REQUEST(400, "Bad Request"),
        NOT_FOUND(404, "Not Found"),
        METHOD_NOT_SUPPORTED(405, "Method Not Supported"),
        PRECONDITION_FAILED(412, "Precondition Failed"),
        UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
        INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
        NOT_IMPLEMENTED(501, "Not Implemented");

        private int statusCode;
        private String statusMsg;

        Status(int statusCode, String statusMsg) {
            this.statusCode = statusCode;
            this.statusMsg = statusMsg;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getStatusMsg() {
            return statusMsg;
        }

        public Status getByStatusCode(int statusCode) {
            for (Status status : values()) {
                if (status.getStatusCode() == statusCode)
                    return status;
            }
            return null;
        }
    }

    private int statusCode;
    private String statusMessage;

    public UpnpResponse(int statusCode, String statusMessage) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
    }

    public UpnpResponse(Status status) {
        this.statusCode = status.getStatusCode();
        this.statusMessage = status.getStatusMsg();
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    /**
     * @return <code>true</code> if the status code was equal or creater 300.
     */
    public boolean isFailed() {
        return statusCode >= 300;
    }

    /**
     * @return The concatenated string of status code and status message (same as {@link #toString()}.
     */
    public String getResponseDetails() {
        return getStatusCode() + " " + getStatusMessage();
    }

    @Override
    public String toString() {
        return getResponseDetails();
    }
}
