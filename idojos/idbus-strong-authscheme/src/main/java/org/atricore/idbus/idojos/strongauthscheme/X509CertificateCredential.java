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
package org.atricore.idbus.idojos.strongauthscheme;

import org.atricore.idbus.kernel.main.authn.BaseCredential;

/**
 * X.509 Credential used for Strong Authentication.
 * <p/>
 * Acts as a wrapper of an java.security.cert.X509Certificate instance.
 *
 * @author <a href="mailto:gbrigand@josso.org">Gianluca Brigandi</a>
 * @version CVS $Id: X509CertificateCredential.java 1040 2009-03-05 00:56:52Z gbrigand $
 */

public class X509CertificateCredential extends BaseCredential {

    public X509CertificateCredential(Object credential) {
        super(credential);
    }

}
