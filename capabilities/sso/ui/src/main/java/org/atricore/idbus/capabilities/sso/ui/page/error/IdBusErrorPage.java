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
package org.atricore.idbus.capabilities.sso.ui.page.error;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.atricore.idbus.capabilities.sso.main.claims.SSOCredentialClaimsRequest;
import org.atricore.idbus.capabilities.sso.ui.page.BasePage;
import org.atricore.idbus.kernel.main.mediation.ArtifactImpl;
import org.atricore.idbus.kernel.main.mediation.IdentityMediationFault;
import org.atricore.idbus.kernel.main.mediation.MediationMessage;
import org.atricore.idbus.kernel.main.mediation.MessageQueueManager;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Page for dumping single sign-on errors.
 *
 * @author <a href="mailto:gbrigandi@atricore.org">Gianluca Brigandi</a>
 */
public class IdBusErrorPage extends BasePage {

    private static final Log logger = LogFactory.getLog(IdBusErrorPage.class);

    public IdBusErrorPage() throws Exception {
        this(null);
    }

    public IdBusErrorPage(PageParameters parameters) throws Exception {

        SSOCredentialClaimsRequest credentialClaimsRequest = null;
        getSession().bind();

        if (parameters != null) {

            String artifactId = parameters.get("IDBusErrArt").toString();

            try {
                MediationMessage fault = artifactId != null ? getFault(artifactId) : null;

                if (fault != null) {

                    IdentityMediationFault err = fault.getFault();
                    List<String> causes = buildCauses(err);

                    add(new Label("status", getString(err.getFaultCode(), null, "N/A")));
                    add(new Label("secStatus", getString(err.getSecFaultCode(), null, "")));

                    // Build a details message
                    String defaultDetails = err.getFault() != null ? err.getFault().getMessage() : err.getMessage();
                    String details = null;
                    String statusDetails = err.getStatusDetails();
                    if (statusDetails != null) {
                        details = getString(statusDetails, null, defaultDetails);
                    } else {
                        details = defaultDetails;
                    }
                    add(new Label("details", details));

                    fillCausesList(new CausesModel(causes));
                } else {
                    add(new Label("status", "N/A"));
                    add(new Label("secStatus", ""));
                    add(new Label("details", "N/A"));
                }

            } catch (Exception e) {
                logger.error("Cannot display error information:" + e.getMessage(), e);
                add(new Label("status", getString("urn:org:atricore:idbus:samlr2:status:InternalError", null, "")));
            }

        }
    }

    protected MediationMessage getFault(String artifactId) throws Exception {
        return (MediationMessage) artifactQueueManager.pullMessage(new ArtifactImpl(artifactId));
    }

    public MessageQueueManager getArtifactQueueManager() {
        return artifactQueueManager;
    }

    public void setArtifactQueueManager(MessageQueueManager artifactQueueManager) {
        this.artifactQueueManager = artifactQueueManager;
    }

    /**
     * For now, only the root cause will be displayed
     * @param cause
     * @return
     */
    protected List<String> buildCauses(Throwable cause) {

        List<String> causes = new ArrayList<String>();

        Throwable rootCause = cause;
        while (cause != null) {

//            Writer errorWriter = new StringWriter();
//            PrintWriter errorPrintWriter = new PrintWriter(errorWriter);

//            cause.printStackTrace(errorPrintWriter);
//            causes.add(errorWriter.toString());

            rootCause = cause;
            cause = cause.getCause();
        }

        Writer errorWriter = new StringWriter();
        PrintWriter errorPrintWriter = new PrintWriter(errorWriter);

        rootCause.printStackTrace(errorPrintWriter);
        causes.add(errorWriter.toString());

        return causes;

    }

    private void fillCausesList(IModel<List<String>> model) {
        add(new ListView<String>("causes", model) {
            @Override
            protected void populateItem(ListItem<String> item) {
                String cause = item.getModelObject();
                item.add(new Label("cause", cause));
            }
        });
    }

    private class CausesModel extends LoadableDetachableModel<List<String>> {

        private List<String> causes;

        public CausesModel(List<String> causes) {
            this.causes = causes;
        }


        @Override
        protected List<String> load() {
            return causes;
        }

    }

}
