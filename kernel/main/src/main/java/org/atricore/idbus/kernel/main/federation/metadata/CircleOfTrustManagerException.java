/*
 * Atricore IDBus
 *
 * Copyright (c) 2009, Atricore Inc.
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

package org.atricore.idbus.kernel.main.federation.metadata;

/**
 * @author <a href="mailto:sgonzalez@atricore.org">Sebastian Gonzalez Oyuela</a>
 * @version $Id: CircleOfTrustManagerException.java 1236 2009-06-02 14:32:39Z sgonzalez $
 */
public class CircleOfTrustManagerException extends Exception {
    public CircleOfTrustManagerException() {
        super();
    }

    public CircleOfTrustManagerException(String message) {
        super(message);
    }

    public CircleOfTrustManagerException(String message, Throwable cause) {
        super(message, cause);
    }

    public CircleOfTrustManagerException(Throwable cause) {
        super(cause);
    }
}
