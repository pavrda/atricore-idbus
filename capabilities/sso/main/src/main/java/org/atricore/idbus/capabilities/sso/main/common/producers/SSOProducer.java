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

package org.atricore.idbus.capabilities.sso.main.common.producers;

import oasis.names.tc.saml._2_0.assertion.NameIDType;
import oasis.names.tc.saml._2_0.metadata.*;
import oasis.names.tc.saml._2_0.protocol.StatusCodeType;
import oasis.names.tc.saml._2_0.protocol.StatusDetailType;
import oasis.names.tc.saml._2_0.protocol.StatusType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.capabilities.sso.main.SSOException;
import org.atricore.idbus.capabilities.sso.main.common.AbstractSSOMediator;
import org.atricore.idbus.capabilities.sso.main.common.plans.SSOPlanningConstants;
import org.atricore.idbus.capabilities.sso.main.select.spi.EntitySelectorConstants;
import org.atricore.idbus.capabilities.sso.main.sp.SPSecurityContext;
import org.atricore.idbus.capabilities.sso.main.sp.SSOSPMediator;
import org.atricore.idbus.capabilities.sso.support.SAMLR2Constants;
import org.atricore.idbus.capabilities.sso.support.SAMLR2MessagingConstants;
import org.atricore.idbus.capabilities.sso.support.binding.SSOBinding;
import org.atricore.idbus.capabilities.sso.support.core.NameIDFormat;
import org.atricore.idbus.capabilities.sso.support.core.StatusCode;
import org.atricore.idbus.capabilities.sso.support.core.util.ProtocolUtils;
import org.atricore.idbus.capabilities.sso.support.metadata.SSOMetadataConstants;
import org.atricore.idbus.capabilities.sso.support.metadata.SSOService;
import org.atricore.idbus.common.sso._1_0.protocol.*;
import org.atricore.idbus.kernel.auditing.core.ActionOutcome;
import org.atricore.idbus.kernel.auditing.core.AuditingServer;
import org.atricore.idbus.kernel.main.federation.metadata.*;
import org.atricore.idbus.kernel.main.mediation.Channel;
import org.atricore.idbus.kernel.main.mediation.IdentityMediationException;
import org.atricore.idbus.kernel.main.mediation.binding.BindingChannel;
import org.atricore.idbus.kernel.main.mediation.camel.AbstractCamelEndpoint;
import org.atricore.idbus.kernel.main.mediation.camel.AbstractCamelProducer;
import org.atricore.idbus.kernel.main.mediation.camel.component.binding.CamelMediationExchange;
import org.atricore.idbus.kernel.main.mediation.camel.component.binding.CamelMediationMessage;
import org.atricore.idbus.kernel.main.mediation.channel.FederationChannel;
import org.atricore.idbus.kernel.main.mediation.channel.IdPChannel;
import org.atricore.idbus.kernel.main.mediation.claim.ClaimChannel;
import org.atricore.idbus.kernel.main.mediation.endpoint.IdentityMediationEndpoint;
import org.atricore.idbus.kernel.main.mediation.provider.*;
import org.atricore.idbus.kernel.main.mediation.select.SelectorChannel;
import org.atricore.idbus.kernel.main.session.SSOSessionManager;
import org.atricore.idbus.kernel.main.session.exceptions.NoSuchSessionException;
import org.atricore.idbus.kernel.main.util.UUIDGenerator;
import org.atricore.idbus.kernel.planning.IdentityPlan;
import org.atricore.idbus.kernel.planning.IdentityPlanExecutionExchange;
import org.atricore.idbus.kernel.planning.IdentityPlanExecutionExchangeImpl;

import javax.security.auth.Subject;
import javax.xml.bind.JAXBElement;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * @author <a href="mailto:sgonzalez@atricore.org">Sebastian Gonzalez Oyuela</a>
 * @version $Id: SSOProducer.java 1359 2009-07-19 16:57:57Z sgonzalez $
 */
public abstract class SSOProducer extends AbstractCamelProducer<CamelMediationExchange>
        implements SAMLR2Constants, SAMLR2MessagingConstants, SSOPlanningConstants {

    private static final Log logger = LogFactory.getLog(SSOProducer.class);

    private static final UUIDGenerator uuidGenerator = new UUIDGenerator();

    protected SSOProducer(AbstractCamelEndpoint<CamelMediationExchange> endpoint) {
        super(endpoint);
    }

    @Override
    protected void doProcess(CamelMediationExchange e) throws Exception {
        // DO Nothing!
    }

    protected StatefulProvider getProvider() {
        if (channel instanceof FederationChannel) {
            return ((FederationChannel) channel).getFederatedProvider();
        } else if (channel instanceof BindingChannel) {
            return ((BindingChannel) channel).getFederatedProvider();
        } else if (channel instanceof ClaimChannel) {
            return ((ClaimChannel) channel).getFederatedProvider();
        } else if (channel instanceof SelectorChannel) {
            return ((SelectorChannel) channel).getProvider();
        } else {
            throw new IllegalStateException("Configured channel does not support Federated Provider : " + channel);
        }
    }

    protected IdentityPlan findIdentityPlanOfType(Class planClass) throws SSOException {

        Collection<IdentityPlan> plans = this.endpoint.getIdentityPlans();
        if (plans != null) {
            for (IdentityPlan plan : plans) {
                if (planClass.isInstance(plan))
                    return plan;
            }
        }

        logger.warn("No identity plan of class " + planClass.getName() + " was found for endpoint " + endpoint.getName());
        return null;

    }

    protected Collection<IdentityPlan> findIdentityPlansOfType(Class planClass) throws SSOException {

        java.util.List<IdentityPlan> found = new java.util.ArrayList<IdentityPlan>();

        Collection<IdentityPlan> plans = this.endpoint.getIdentityPlans();
        for (IdentityPlan plan : plans) {
            if (planClass.isInstance(plan))
                found.add(plan);
        }

        return found;

    }

    protected IdentityPlanExecutionExchange createIdentityPlanExecutionExchange() {

        IdentityPlanExecutionExchange ex = new IdentityPlanExecutionExchangeImpl();

        // Publish some important attributes:
        // Circle of trust will allow actions to access identity configuration

        ex.setProperty(VAR_COT, this.getCot());
        ex.setProperty(VAR_COT_MEMBER, this.getCotMemberDescriptor());
        ex.setProperty(VAR_CHANNEL, this.channel);
        ex.setProperty(VAR_ENDPOINT, this.endpoint);

        return ex;

    }

    protected CircleOfTrust getCot() {
        if (this.channel instanceof FederationChannel) {
            return ((FederationChannel) channel).getCircleOfTrust();
        }

        if (logger.isDebugEnabled())
            logger.debug("There is no associated circle of trust, channel is not a federation channel");

        return null;
    }

    protected CircleOfTrustManager getCotManager() {
        if (this.channel instanceof FederationChannel) {
            return ((FederationChannel) channel).getFederatedProvider().getCotManager();
        } else if (this.channel instanceof BindingChannel) {
            return ((BindingChannel) channel).getFederatedProvider().getCotManager();
        } else if (this.channel instanceof SelectorChannel) {
            return ((SelectorChannel) channel).getProvider().getCotManager();
        }

        if (logger.isDebugEnabled())
            logger.debug("There is no associated circle of trust, channel is not a federation channel");

        return null;
    }


    protected CircleOfTrustMemberDescriptor getCotMemberDescriptor() {
        if (this.channel instanceof FederationChannel) {
            return ((FederationChannel) channel).getMember();
        }

        if (logger.isDebugEnabled())
            logger.debug("There is no associated circle of trust member descriptor, channel is not a federation channel");

        return null;
    }

    protected CircleOfTrustMemberDescriptor resolveProviderDescriptor(NameIDType issuer) {

        if (issuer.getFormat() != null && !issuer.getFormat().equals(NameIDFormat.ENTITY.getValue())) {
            logger.warn("Invalid issuer format for entity : " + issuer.getFormat());
            return null;
        }

        return getCotManager().lookupMemberByAlias(issuer.getValue());
    }

    protected EndpointDescriptor resolveSpSloEndpoint(String spAlias,
                                                      SSOBinding[] preferredBindings,
                                                      boolean onlyPreferredBinding)
            throws SSOException {

        try {

            CircleOfTrustManager cotMgr = ((FederatedLocalProvider)getProvider()).getCotManager();

            MetadataEntry md = cotMgr.findEntityRoleMetadata(spAlias,
                    "urn:oasis:names:tc:SAML:2.0:metadata:SPSSODescriptor");

            SPSSODescriptorType samlr2sp = (SPSSODescriptorType) md.getEntry();

            EndpointDescriptor ed = resolveEndpoint(samlr2sp.getSingleLogoutService(),
                    preferredBindings, SSOService.SingleLogoutService, onlyPreferredBinding);

            if (ed == null) {
                String bindings = "";
                for (SSOBinding preferredBinding : preferredBindings) {
                    bindings += preferredBinding.getValue();
                }
                logger.warn("No SP SLO Endpoint found [" + spAlias + "] " +
                    SSOService.SingleLogoutService.toString() + "/" + bindings);
            }

            return ed;

        } catch (CircleOfTrustManagerException e) {
            throw new SSOException(e);
        }

    }

    protected EndpointDescriptor resolveSpSloEndpoint(NameIDType spId,
                                                      SSOBinding[] preferredBindings,
                                                      boolean onlyPreferredBinding)
            throws SSOException {

        CircleOfTrustMemberDescriptor sp = resolveProviderDescriptor(spId);
        return resolveSpSloEndpoint(sp.getAlias(), preferredBindings, onlyPreferredBinding);
    }

    protected EndpointDescriptor resolveIdPSloEndpoint(NameIDType idpId,
                                                      SSOBinding[] preferredBindings,
                                                      boolean onlyPreferredBinding)
            throws SSOException {

        CircleOfTrustMemberDescriptor idp = resolveProviderDescriptor(idpId);
        return resolveIdPSloEndpoint(idp.getAlias(), preferredBindings, onlyPreferredBinding);
    }


    protected EndpointDescriptor resolveIdPSloEndpoint(String idpAlias,
                                                      SSOBinding[] preferredBindings,
                                                      boolean onlyPreferredBinding)
            throws SSOException {

        try {

            CircleOfTrustManager cotMgr = ((FederatedLocalProvider)getProvider()).getCotManager();

            MetadataEntry md = cotMgr.findEntityRoleMetadata(idpAlias,
                    "urn:oasis:names:tc:SAML:2.0:metadata:IDPSSODescriptor");

            IDPSSODescriptorType samlr2idp = (IDPSSODescriptorType) md.getEntry();

            EndpointDescriptor ed = resolveEndpoint(samlr2idp.getSingleLogoutService(),
                    preferredBindings, SSOService.SingleLogoutService, true);

            if (ed == null) {
                String bindings = "";
                for (SSOBinding preferredBinding : preferredBindings) {
                    bindings += preferredBinding.getValue();
                }
                logger.warn("No IDP SLO Endpoint found [" + idpAlias + "] " +
                        SSOService.SingleLogoutService.toString() + "/" + bindings);
            }

            return ed;

        } catch (CircleOfTrustManagerException e) {
            throw new SSOException(e);
        }

    }


    protected EndpointDescriptor resolveEndpoint(List<EndpointType> endpointTypes,
                                                 SSOBinding[] preferredBindings,
                                                 SSOService service,
                                                 boolean onlyPreferredBinding) {

        EndpointType endpointType = null;
        EndpointType preferredEndpointType = null;

        // Preferred bindings are in preference order
        for (SSOBinding preferredBinding : preferredBindings) {

            for (EndpointType currentSloEndpoint : endpointTypes) {

                if (endpointType == null)
                    endpointType = currentSloEndpoint ;

                if (currentSloEndpoint.getBinding().equals(preferredBinding.getValue()))
                    preferredEndpointType = currentSloEndpoint;

                if (preferredEndpointType != null)
                    break;
            }

            if (preferredEndpointType != null)
                break;
        }

        if (onlyPreferredBinding || preferredEndpointType != null)
            endpointType = preferredEndpointType;

        if (logger.isDebugEnabled())
            logger.debug("Selected endpoint " + (endpointType != null ? endpointType.getBinding() : "<NONE>"));

        if (endpointType == null)
            return null;

        return new EndpointDescriptorImpl(endpointType.getBinding(),
                service.toString(),
                endpointType.getBinding(),
                endpointType.getLocation(),
                endpointType.getResponseLocation());

    }

    protected IDPSSODescriptorType getIDPSSODescriptor() {
        CircleOfTrustMemberDescriptor cotDescr = this.getCotMemberDescriptor();

        EntityDescriptorType samlMd = (EntityDescriptorType) cotDescr.getMetadata().getEntry();

        for (RoleDescriptorType roleDescr : samlMd.getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor()) {
            if (roleDescr instanceof IDPSSODescriptorType)
                return (IDPSSODescriptorType) roleDescr;
        }
        return null;

    }

    protected SPSSODescriptorType getSPSSODescriptor() {
        CircleOfTrustMemberDescriptor cotDescr = this.getCotMemberDescriptor();

        EntityDescriptorType samlMd = (EntityDescriptorType) cotDescr.getMetadata().getEntry();

        for (RoleDescriptorType roleDescr : samlMd.getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor()) {
            if (roleDescr instanceof SPSSODescriptorType)
                return (SPSSODescriptorType) roleDescr;
        }
        return null;

    }

    protected boolean isStatusCodeValid(String statusCode) {
        try {
            StatusCode.asEnum(statusCode);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }


    protected SubjectType toSubjectType(Subject subject) {
        return ProtocolUtils.toSubjectType(subject);
    }

    protected Subject toSubjectType(SubjectType subjectType) {
        return ProtocolUtils.toSubject(subjectType);
    }

    /**
     * Obtains the SP Channel that the target SP must use to send messages to the current provider
     */
    protected FederationChannel resolveSpChannel(CircleOfTrustMemberDescriptor targetSp) {
        // Resolve IdP channel, then look for the ACS endpoint
        FederatedLocalProvider idp = (FederatedLocalProvider) getProvider();

        FederationChannel spChannel = idp.getChannel();

        if (targetSp == null)
            return spChannel;

        for (FederationChannel fChannel : idp.getChannels()) {

            FederatedProvider sp = fChannel.getTargetProvider();
            for (CircleOfTrustMemberDescriptor member : sp.getMembers()) {
                if (member.getAlias().equals(targetSp.getAlias())) {

                    if (logger.isDebugEnabled())
                        logger.debug("Selected IdP channel " + fChannel.getName() + " for provider " + sp.getName());
                    spChannel = fChannel;
                    break;
                }

            }

        }
        return spChannel;
    }

    /**
     * Obtains the IdP Channel that the target IdP must use to send messages to the current provider (SP)
     */
    protected FederationChannel resolveIdpChannel(CircleOfTrustMemberDescriptor targetIdp) {
        // Resolve IdP channel, then look for the ACS endpoint

        FederatedLocalProvider sp = (FederatedLocalProvider) getProvider();

        // Default IdP channel
        FederationChannel idpChannel = sp.getChannel();

        // Look for overrides
        for (FederationChannel fChannel : sp.getChannels()) {

            FederatedProvider idp = fChannel.getTargetProvider();
            for (CircleOfTrustMemberDescriptor member : idp.getMembers()) {
                if (member.getAlias().equals(targetIdp.getAlias())) {

                    if (logger.isDebugEnabled())
                        logger.debug("Selected IdP channel " + fChannel.getName() + " for provider " + idp.getName());
                    idpChannel = fChannel;
                    break;
                }

            }

        }

        return idpChannel;
    }

    protected void destroySPSecurityContext(CamelMediationExchange exchange,
                                            SPSecurityContext secCtx) throws SSOException {

        CircleOfTrustMemberDescriptor idp = getCotManager().lookupMemberByAlias(secCtx.getIdpAlias());
        IdPChannel idpChannel = (IdPChannel) resolveIdpChannel(idp);
        SSOSessionManager ssoSessionManager = idpChannel.getSessionManager();
        secCtx.clear();

        try {
            ssoSessionManager.invalidate(secCtx.getSessionIndex());
            CamelMediationMessage in = (CamelMediationMessage) exchange.getIn();
            in.getMessage().getState().removeRemoteVariable(getProvider().getName().toUpperCase() + "_SECURITY_CTX");
        } catch (NoSuchSessionException e) {
            logger.debug("SSO Session already invalidated " + secCtx.getSessionIndex());
        } catch (Exception e) {
            throw new SSOException(e);
        }

    }

    protected void recordInfoAuditTrail(String action, ActionOutcome actionOutcome, String principal, CamelMediationExchange exchange) {
        recordInfoAuditTrail(action, actionOutcome, principal, exchange, null);
    }

    protected void recordInfoAuditTrail(String action, ActionOutcome actionOutcome, String principal, CamelMediationExchange exchange, Properties otherProps) {

        AbstractSSOMediator mediator = (AbstractSSOMediator) channel.getIdentityMediator();
        AuditingServer aServer = mediator.getAuditingServer();

        Properties props = new Properties();
        String providerName = getProvider().getName();
        props.setProperty("provider", providerName);

        String remoteAddr = (String) exchange.getIn().getHeader("org.atricore.idbus.http.RemoteAddress");
        if (remoteAddr != null) {
            props.setProperty("remoteAddress", remoteAddr);
        }

        String session = (String) exchange.getIn().getHeader("org.atricore.idbus.http.Cookie.JSESSIONID");
        if (session != null) {
            props.setProperty("httpSession", session);
        }

        if (otherProps != null) {
            props.putAll(otherProps);
        }

        aServer.processAuditTrail(mediator.getAuditCategory(), "INFO", action, actionOutcome, principal != null ? principal : "UNKNOWN", new java.util.Date(), null, props);
    }

    /**
     * SPs can build an IdP selectio request using this method, specifically designed for SP binding channel producers
     */
    protected SelectEntityRequestType buildSelectIdPRequest(BindingChannel bChannel,
                                                            SSORequestAbstractType ssoRequest,
                                                            Collection<CircleOfTrustMemberDescriptor> availableIdPs) {

        SSOSPMediator mediator = (SSOSPMediator) bChannel.getIdentityMediator();

        SelectEntityRequestType selectIdPRequest = new SelectEntityRequestType();
        selectIdPRequest.setID(uuidGenerator.generateId());
        selectIdPRequest.setIssuer(bChannel.getFederatedProvider().getName());
        for (IdentityMediationEndpoint ed : channel.getEndpoints()) {
            if (ed.getBinding().equals(SSOBinding.SSO_ARTIFACT.getValue()) &&
                    ed.getType().equals(endpoint.getType())) {
                selectIdPRequest.setReplyTo(channel.getLocation() + ed.getLocation());
                break;
            }
        }

        for (CircleOfTrustMemberDescriptor idp : availableIdPs) {

            RequestAttributeType idpAttr = new RequestAttributeType();
            idpAttr.setName(SSOMetadataConstants.IDPSSODescriptor_QNAME.toString());
            idpAttr.setValue(idp.getAlias());

            selectIdPRequest.getRequestAttribute().add(idpAttr);
        }

        // Send the name of the SP asking for the selection
        RequestAttributeType spAttr = new RequestAttributeType();
        spAttr.setName(EntitySelectorConstants.ISSUER_SP_ATTR);
        spAttr.setValue(((BindingChannel)channel).getProvider().getName());

        selectIdPRequest.getRequestAttribute().add(spAttr);

        // Send the preferred IDP alias
        RequestAttributeType preferredIdp = new RequestAttributeType();
        preferredIdp.setName(EntitySelectorConstants.PREFERRED_IDP_ATTR);
        preferredIdp.setValue(mediator.getPreferredIdpAlias());

        selectIdPRequest.getRequestAttribute().add(preferredIdp);

        if (ssoRequest != null) {

            // Add additional information about the environment ...

            if (ssoRequest instanceof SPInitiatedAuthnRequestType) {

                SPInitiatedAuthnRequestType ssoAuthnRequest = (SPInitiatedAuthnRequestType) ssoRequest;

                RequestAttributeType authnCtx = new RequestAttributeType();
                authnCtx.setName(EntitySelectorConstants.AUTHN_CTX_ATTR);
                authnCtx.setValue(ssoAuthnRequest.getAuthnCtxClass());

                selectIdPRequest.getRequestAttribute().add(authnCtx);
            }

            // All request attributes
            if (ssoRequest.getRequestAttribute() != null) {
                for (int i = 0; i < ssoRequest.getRequestAttribute().size(); i++) {
                    RequestAttributeType a =
                            ssoRequest.getRequestAttribute().get(i);

                    RequestAttributeType a1 = new RequestAttributeType();
                    a1.setName(a.getName());
                    a1.setValue(a.getValue());

                    selectIdPRequest.getRequestAttribute().add(a1);

                }
            }
        }

        return selectIdPRequest;
    }

    protected CircleOfTrustMemberDescriptor resolveActualIdP(CircleOfTrustMemberDescriptor selectedIdP) {
        // This is useful when the IdP is local, and overrides the SP channel.


        CircleOfTrust cot = null;
        FederatedProvider sp = null;
        if (channel instanceof FederationChannel) {
            FederationChannel fChannel = (FederationChannel) channel;
            sp = fChannel.getFederatedProvider();
            cot = fChannel.getCircleOfTrust();

            if (logger.isDebugEnabled())
                logger.debug("Resolving actual IdP from FederationChannel " + fChannel.getName());
        } else if (channel instanceof BindingChannel) {
            BindingChannel bChannel = (BindingChannel) channel;
            sp = bChannel.getFederatedProvider();
            cot = sp.getDefaultFederationService().getChannel().getCircleOfTrust();

            if (logger.isDebugEnabled())
                logger.debug("Resolving actual IdP from BindingChannel " + bChannel.getName());

        }


        IdentityProvider idp = null;
        for (FederatedProvider provider : cot.getProviders()) {
            for (CircleOfTrustMemberDescriptor cotDescr : provider.getMembers()) {
                if (cotDescr.getAlias().equals(selectedIdP.getAlias())) {

                    if (provider instanceof IdentityProvider) {
                        idp = (IdentityProvider) provider;
                        break;
                    }
                }
            }

            if (idp != null)
                break;
        }

        if (idp == null) {
            if (logger.isDebugEnabled())
                logger.debug("Local IdP not found for COT Member " + selectedIdP);

            // assume this is a remote IdP
            return selectedIdP;
        }

        if (logger.isDebugEnabled())
            logger.debug("Local IdP " + idp.getName() + " found for COT Member " + selectedIdP);



        for (FederationChannel fChannel : idp.getDefaultFederationService().getOverrideChannels()) {
            if (fChannel.getTargetProvider() != null &&
                    fChannel.getTargetProvider().equals(sp)) {
                return fChannel.getMember();
            }
        }

        return idp.getDefaultFederationService().getChannel().getMember();

    }

    protected EndpointDescriptor resolveAccessSSOSessionEndpoint(Channel myChannel, BindingChannel spBindingChannel) throws IdentityMediationException {

        IdentityMediationEndpoint soapEndpoint = null;

        for (IdentityMediationEndpoint endpoint : spBindingChannel.getEndpoints()) {

            if (endpoint.getType().equals(SSOService.SPSessionHeartBeatService.toString())) {

                if (endpoint.getBinding().equals(SSOBinding.SSO_LOCAL.getValue())) {
                    return myChannel.getIdentityMediator().resolveEndpoint(spBindingChannel, endpoint);
                } else if (endpoint.getBinding().equals(SSOBinding.SSO_SOAP.getValue())) {
                    soapEndpoint = endpoint;
                }


            }

        }

        if (soapEndpoint != null)
            return myChannel.getIdentityMediator().resolveEndpoint(spBindingChannel, soapEndpoint);

        return null;
    }

    protected String getErrorDetails(StatusType status) {
        String errorDetails = null;

        StatusDetailType details = status.getStatusDetail();
        if (details != null && details.getAny() != null && details.getAny().size() > 0) {

            for (Object o : details.getAny()) {
                if (o instanceof JAXBElement) {
                    JAXBElement e = (JAXBElement) o;

                    if (e.getValue() instanceof String) {
                        errorDetails = (String) e.getValue();
                    }
                }
            }
        }

        return errorDetails;
    }

    
}
