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

package org.atricore.idbus.capabilities.samlr2.main.idp.plans;

import org.atricore.idbus.kernel.planning.jbpm.AbstractJbpmIdentityPlan;

/**
 * @org.apache.xbean.XBean element="idpinitiatedauthnreq-to-samlr2autnreq-plan"
 *
 * @author <a href="mailto:gbrigandi@atricore.org">Gianluca Brigandi</a>
 * @version $Id: SPInitiatedAuthnReqToSamlR2AuthnReqPlan.java 1327 2009-06-23 02:35:28Z sgonzalez $
 */
public class IDPInitiatedAuthnReqToSamlR2AuthnReqPlan extends AbstractJbpmIdentityPlan {

    protected String getProcessDescriptorName() {
        return "idpsso-idpinitiatedauthnreq-to-samlr2authnreq";
    }


}