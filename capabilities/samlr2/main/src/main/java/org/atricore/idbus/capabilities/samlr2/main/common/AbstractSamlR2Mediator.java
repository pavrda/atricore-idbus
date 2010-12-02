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

package org.atricore.idbus.capabilities.samlr2.main.common;

import oasis.names.tc.saml._2_0.metadata.EndpointType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.capabilities.samlr2.main.binding.SamlArtifactEncoder;
import org.atricore.idbus.capabilities.samlr2.main.binding.SamlR2ArtifactEncoderImpl;
import org.atricore.idbus.capabilities.samlr2.support.binding.SamlR2Binding;
import org.atricore.idbus.capabilities.samlr2.support.core.encryption.SamlR2Encrypter;
import org.atricore.idbus.capabilities.samlr2.support.core.signature.SamlR2Signer;
import org.atricore.idbus.kernel.main.federation.metadata.EndpointDescriptor;
import org.atricore.idbus.kernel.main.federation.metadata.EndpointDescriptorImpl;
import org.atricore.idbus.kernel.main.mediation.Channel;
import org.atricore.idbus.kernel.main.mediation.IdentityMediationException;
import org.atricore.idbus.kernel.main.mediation.MessageQueueManager;
import org.atricore.idbus.kernel.main.mediation.camel.AbstractCamelMediator;
import org.atricore.idbus.kernel.main.mediation.endpoint.IdentityMediationEndpoint;

/**
 * @author <a href="mailto:sgonzalez@atricore.org">Sebastian Gonzalez Oyuela</a>
 * @version $Id: AbstractSamlR2Mediator.java 1245 2009-06-05 19:32:53Z sgonzalez $
 */
public abstract class AbstractSamlR2Mediator extends AbstractCamelMediator {

    private static final Log logger = LogFactory.getLog(AbstractSamlR2Mediator.class);

    private boolean enableSignatureValidation;

    private boolean enableEncryption;

    private boolean enableSignature;

    private long requestTimeToLive = 6000000L; // Default to ten minutes

    private SamlR2Signer signer;

    private SamlR2Encrypter encrypter;

    protected AbstractSamlR2Mediator() {

    }

    /**
     * This util will create an EndpointDescriptor based on the received channel and endpoint information.
     * 
     * @param channel
     * @param endpoint
     * @return
     * @throws org.atricore.idbus.capabilities.samlr2.main.SamlR2Exception
     */
    public EndpointDescriptor resolveEndpoint(Channel channel, IdentityMediationEndpoint endpoint) throws IdentityMediationException {

        // SAMLR2 Endpoint springmetadata definition
        String type = null;
        String location;
        String responseLocation;
        SamlR2Binding binding = null;

        if (endpoint.getMetadata() != null &&
                endpoint.getMetadata().getEntry() instanceof EndpointType) {

            EndpointType samlr2Endpoint = (EndpointType) endpoint.getMetadata().getEntry();
            logger.debug("Found SAMLR2 Endpoint metadata for endpoint " + endpoint.getName());

            // ---------------------------------------------
            // Resolve Endpoint binding value.
            // ---------------------------------------------
            if (endpoint.getBinding() != null &&  samlr2Endpoint.getBinding() != null &&
                    !samlr2Endpoint.getBinding().equals(endpoint.getBinding())) {
                logger.warn("SAMLR2 Metadata Endpoint binding does not match binding for Identity Mediation Endpoint "
                        + endpoint.getName() + "IGNORING METADATA");
            }

            String b = endpoint.getBinding() != null ? endpoint.getBinding() : samlr2Endpoint.getBinding();
            if (b != null)
                binding = SamlR2Binding.asEnum(b);
            else
                logger.warn("No SamlR2Binding found in endpoint " + endpoint.getName());

            // ---------------------------------------------
            // Resolve Endpoint location
            // ---------------------------------------------
            if (endpoint.getLocation() != null && samlr2Endpoint.getLocation() != null &&
                    !endpoint.getLocation().equals(samlr2Endpoint.getLocation())) {
                logger.warn("SAMLR2 Metadata Endpoint location does not match location for Identity Mediation Endpoint "
                        + endpoint.getName() + ", IGNORING METADATA!");
            }

            location = endpoint.getLocation() != null ? endpoint.getLocation() : samlr2Endpoint.getLocation();
            if (location == null)
                throw new IdentityMediationException("Endpoint location cannot be null.  " + endpoint);

            if (location.startsWith("/"))
                location = channel.getLocation() + location;

            // ---------------------------------------------
            // Resolve Endpoint response location
            // ---------------------------------------------
            if (endpoint.getResponseLocation() != null && samlr2Endpoint.getResponseLocation() != null &&
                    !endpoint.getResponseLocation().equals(samlr2Endpoint.getResponseLocation())) {
                logger.warn("SAMLR2 Metadata Endpoint response location does not match response location for Identity Mediation Endpoint "
                        + endpoint.getName() + "IGNORING METADATA");
            }

            responseLocation = endpoint.getResponseLocation() != null ?
                    endpoint.getResponseLocation() : samlr2Endpoint.getResponseLocation();

            if (responseLocation != null && responseLocation.startsWith("/"))
                responseLocation = channel.getLocation() + responseLocation;

            // ---------------------------------------------
            // Resolve Endpoint type
            // ---------------------------------------------
            // If no ':' is present, lastIndexOf should resturn -1 and the entire type is used.
            // Remove qualifier, format can be :
            // 1 - {qualifier}type
            // 2 - qualifier:type
            int bracketPos = endpoint.getType().lastIndexOf("}");
            if (bracketPos > 0)
                type = endpoint.getType().substring(bracketPos + 1);
            else
                type = endpoint.getType().substring(endpoint.getType().lastIndexOf(":") + 1);


        } else {

            logger.debug("Creating Endpoint Descriptor without SAMLR2 Metadata for : " + endpoint.getName());

            // ---------------------------------------------
            // Resolve Endpoint binding
            // ---------------------------------------------
            if (endpoint.getBinding() != null)
                binding = SamlR2Binding.asEnum(endpoint.getBinding());
            else
                logger.warn("No SamlR2Binding found in endpoint " + endpoint.getName());

            // ---------------------------------------------
            // Resolve Endpoint location
            // ---------------------------------------------
            location = endpoint.getLocation();
            if (location == null)
                throw new IdentityMediationException("Endpoint location cannot be null.  " + endpoint);
            
            if (location.startsWith("/"))
                location = channel.getLocation() + location;

            // ---------------------------------------------
            // Resolve Endpoint response location
            // ---------------------------------------------
            responseLocation = endpoint.getResponseLocation();
            if (responseLocation != null && responseLocation.startsWith("/"))
                responseLocation = channel.getLocation() + responseLocation;

            // ---------------------------------------------
            // Resolve Endpoint type
            // ---------------------------------------------

            // Remove qualifier, format can be :
            // 1 - {qualifier}type
            // 2 - qualifier:type
            int bracketPos = endpoint.getType().lastIndexOf("}");
            if (bracketPos > 0)
                type = endpoint.getType().substring(bracketPos + 1);
            else
                type = endpoint.getType().substring(endpoint.getType().lastIndexOf(":") + 1);

        }

        return new EndpointDescriptorImpl(endpoint.getName(),
                type,
                binding.getValue(),
                location,
                responseLocation);

    }

    /**
     * @org.apache.xbean.Property alias="artifact-queue-mgr"
     *
     * @return
     */
    public MessageQueueManager getArtifactQueueManager() {
        return super.getArtifactQueueManager();
    }

    public void setArtifactQueueManager(MessageQueueManager artifactQueueManager) {
        super.setArtifactQueueManager(artifactQueueManager);
    }


    /**
     * @org.apache.xbean.Property alias="log-messages"
     *
     * @return
     */
    @Override
    public boolean isLogMessages() {
        return super.isLogMessages();
    }

    @Override
    public void setLogMessages(boolean logMessages) {
        super.setLogMessages(logMessages);    //To change body of overridden methods use File | Settings | File Templates.
    }


    public SamlR2Signer getSigner() {
        return signer;
    }

    public void setSigner(SamlR2Signer signer) {
        this.signer = signer;
    }


    public SamlR2Encrypter getEncrypter() {
        return encrypter;
    }

    public void setEncrypter(SamlR2Encrypter encrypter) {
        this.encrypter = encrypter;
    }

    public boolean isEnableSignatureValidation() {
        return enableSignatureValidation;
    }

    public void setEnableSignatureValidation(boolean enableSignatureValidation) {
        this.enableSignatureValidation = enableSignatureValidation;
    }

    public boolean isEnableEncryption() {
        return enableEncryption;
    }

    public void setEnableEncryption(boolean enableEncryption) {
        this.enableEncryption = enableEncryption;
    }

    public boolean isEnableSignature() {
        return enableSignature;
    }

    public void setEnableSignature(boolean enableSignature) {
        this.enableSignature = enableSignature;
    }

    public long getRequestTimeToLive() {
        return requestTimeToLive;
    }

    public void setRequestTimeToLive(long requestTimeToLive) {
        this.requestTimeToLive = requestTimeToLive;
    }

}
