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

package org.atricore.idbus.kernel.planning;

import javax.xml.namespace.QName;
import java.io.Serializable;

/**
 *
 * @author <a href="mailto:gbrigand@josso.org">Gianluca Brigandi</a>
 * @version $Id: IdentityMediationException.java 1040 2009-03-05 00:56:52Z gbrigand $
 */
public class IdentityArtifactImpl<E> implements IdentityArtifact<E>, Serializable {

    private QName qname;
    private IdentityArtifactStatus status;
    private E content;

    public IdentityArtifactImpl(QName qname, E content) {
        this.qname = qname;
        this.content = content;
    }

    public QName getQName() {
        return qname;
    }

    public IdentityArtifactStatus getStatus() {
        return status;
    }

    public E getContent() {
        return content;
    }

    public void replaceContent(E newContent) {
        this.content = newContent;
    }

    @Override
    public String toString() {
        return (qname != null ? qname.toString() + ":" : "") + content;
    }
}
