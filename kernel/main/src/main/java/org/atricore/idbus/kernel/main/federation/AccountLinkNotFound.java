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

package org.atricore.idbus.kernel.main.federation;

/**
 *
 * @author <a href="mailto:gbrigand@josso.org">Gianluca Brigandi</a>
 * @version $Rev: 1040 $ $Date: 2009-03-04 22:56:52 -0200 (Wed, 04 Mar 2009) $
 */
public class AccountLinkNotFound extends Exception {
    public AccountLinkNotFound() {
        super();    //To change body of overridden methods use File | Settings | File Templates.
    }

    public AccountLinkNotFound(String message) {
        super(message);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public AccountLinkNotFound(String message, Throwable cause) {
        super(message, cause);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public AccountLinkNotFound(Throwable cause) {
        super(cause);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
