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

package org.teleal.cling.support.renderingcontrol;

import org.teleal.cling.model.action.ActionException;
import org.teleal.cling.model.types.ErrorCode;

/**
 *
 */
public class RenderingControlException extends ActionException {

    public RenderingControlException(int errorCode, String message) {
        super(errorCode, message);
    }

    public RenderingControlException(int errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public RenderingControlException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public RenderingControlException(ErrorCode errorCode) {
        super(errorCode);
    }

    public RenderingControlException(RenderingControlErrorCode errorCode, String message) {
        super(errorCode.getCode(), errorCode.getDescription() + ". " + message + ".");
    }

    public RenderingControlException(RenderingControlErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getDescription());
    }
}