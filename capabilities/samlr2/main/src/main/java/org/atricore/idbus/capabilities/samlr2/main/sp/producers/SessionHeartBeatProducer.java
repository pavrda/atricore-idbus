package org.atricore.idbus.capabilities.samlr2.main.sp.producers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.capabilities.samlr2.main.SamlR2Exception;
import org.atricore.idbus.capabilities.samlr2.main.common.producers.SamlR2Producer;
import org.atricore.idbus.capabilities.samlr2.main.sp.SPSecurityContext;
import org.atricore.idbus.capabilities.samlr2.main.sp.SamlR2SPMediator;
import org.atricore.idbus.capabilities.samlr2.support.binding.SamlR2Binding;
import org.atricore.idbus.capabilities.samlr2.support.metadata.SamlR2Service;
import org.atricore.idbus.common.sso._1_0.protocol.IDPSessionHeartBeatRequestType;
import org.atricore.idbus.common.sso._1_0.protocol.IDPSessionHeartBeatResponseType;
import org.atricore.idbus.common.sso._1_0.protocol.SPSessionHeartBeatRequestType;
import org.atricore.idbus.common.sso._1_0.protocol.SPSessionHeartBeatResponseType;
import org.atricore.idbus.kernel.main.federation.metadata.*;
import org.atricore.idbus.kernel.main.mediation.IdentityMediationException;
import org.atricore.idbus.kernel.main.mediation.MediationMessageImpl;
import org.atricore.idbus.kernel.main.mediation.binding.BindingChannel;
import org.atricore.idbus.kernel.main.mediation.camel.AbstractCamelEndpoint;
import org.atricore.idbus.kernel.main.mediation.camel.component.binding.CamelMediationExchange;
import org.atricore.idbus.kernel.main.mediation.camel.component.binding.CamelMediationMessage;
import org.atricore.idbus.kernel.main.mediation.channel.FederationChannel;
import org.atricore.idbus.kernel.main.mediation.channel.IdPChannel;
import org.atricore.idbus.kernel.main.mediation.channel.SPChannel;
import org.atricore.idbus.kernel.main.mediation.endpoint.IdentityMediationEndpoint;
import org.atricore.idbus.kernel.main.mediation.provider.LocalProvider;
import org.atricore.idbus.kernel.main.mediation.provider.Provider;
import org.atricore.idbus.kernel.main.session.SSOSessionManager;
import org.atricore.idbus.kernel.main.session.exceptions.NoSuchSessionException;
import org.atricore.idbus.kernel.main.session.exceptions.SSOSessionException;
import org.atricore.idbus.kernel.main.util.UUIDGenerator;
import org.atricore.idbus.kernel.planning.*;

/**
 * @author <a href="mailto:sgonzalez@atricore.org">Sebastian Gonzalez Oyuela</a>
 * @version $Id$
 */
public class SessionHeartBeatProducer extends SamlR2Producer {

    private UUIDGenerator uuidGenerator = new UUIDGenerator();

    private static final Log logger = LogFactory.getLog(SessionHeartBeatProducer.class);

    public SessionHeartBeatProducer(AbstractCamelEndpoint<CamelMediationExchange> endpoint) throws Exception {
        super(endpoint);
    }

    @Override
    protected void doProcess(CamelMediationExchange exchange) throws Exception {
        CamelMediationMessage in = (CamelMediationMessage) exchange.getIn();
        logger.debug("Processing Session Heart-Beat Message : " + in.getMessage().getContent());

        if (in.getMessage().getContent() instanceof SPSessionHeartBeatRequestType) {
            doProcessSPSessionHeartBeat(exchange, (SPSessionHeartBeatRequestType) in.getMessage().getContent());
        } else {
            throw new SamlR2Exception("Unsupported message type " + in.getMessage().getContent());
        }
    }

    protected void doProcessSPSessionHeartBeat(CamelMediationExchange exchange, SPSessionHeartBeatRequestType request) throws SamlR2Exception,
            IdentityPlanningException,
            SSOSessionException {
        CamelMediationMessage in = (CamelMediationMessage) exchange.getIn();
        CamelMediationMessage out = (CamelMediationMessage) exchange.getOut();

        // Recover local session information
        SPSecurityContext secCtx =
                (SPSecurityContext) in.getMessage().getState().getLocalVariable(channel.getProvider().getName().toUpperCase() + "_SECURITY_CTX");

        SPSessionHeartBeatResponseType response = new SPSessionHeartBeatResponseType();
        response.setID(uuidGenerator.generateId());
        response.setInReplayTo(request.getID());
        response.setSsoSessionId(request.getSsoSessionId());

        if (secCtx == null || secCtx.getSessionIndex() == null) {

            if (logger.isDebugEnabled())
                logger.debug("No Security Context found for " + channel.getProvider().getName().toUpperCase() + "_SECURITY_CTX: " + secCtx);
            // No SSO Session available, send response.
            response.setValid(false);

        } else {

            if (logger.isDebugEnabled())
                logger.debug("Security Context found " + secCtx);

            try {
                // Update local context and validate local session
                updateSPSecurityContext(secCtx, exchange);

                // Check if heartbeat is needed now.
                SamlR2SPMediator mediator = (SamlR2SPMediator) channel.getIdentityMediator();
                if (logger.isTraceEnabled())
                    logger.trace("Checking IDP Session heart beat for:" + secCtx + ".  Configured interval: " + mediator.getIdpSessionHeartBeatInterval());

                long now = System.currentTimeMillis();
                if (secCtx.getLastIdPSessionHeartBeat() == null ||
                        secCtx.getLastIdPSessionHeartBeat() + mediator.getIdpSessionHeartBeatInterval() * 1000L <  now) {

                    // Send HB request to IDP.  If we get a null response, HB was not sent.
                    IDPSessionHeartBeatResponseType idpResp = performIdPSessionHeartBeat(exchange, secCtx);
                    secCtx.setLastIdPSessionHeartBeat(now);
                    if (idpResp != null) {
                        logger.debug("IDP HeartBeat sent, response: " + idpResp.getID() + " isValid:" + idpResp.isValid());
                        response.setValid(idpResp.isValid());
                    } else {
                        logger.debug("IDP HeartBeat not send");
                        response.setValid(true);
                    }

                } else {
                    if (logger.isTraceEnabled())
                        logger.debug("IDP Session heart beat not necessary");
                    response.setValid(true);
                }

            } catch (NoSuchSessionException e) {
                if (logger.isDebugEnabled())
                    logger.debug("SP Session not found or invalid : " + secCtx.getSessionIndex());
                // Do not send heart beat for invalid sessions.
                response.setValid(false);
            }
        }

        // Send response back
        EndpointDescriptor destination = new EndpointDescriptorImpl("SPSessionHeartBeatService",
                "SPSessionHeartBeatService",
                endpoint.getBinding(),
                null, null);

        out.setMessage(new MediationMessageImpl(uuidGenerator.generateId(),
                response, "SPSessionHeartBeatResponse", null, destination, in.getMessage().getState()));

    }

    protected IDPSessionHeartBeatResponseType performIdPSessionHeartBeat(CamelMediationExchange exchange,
                                                                         SPSecurityContext secCtx) throws SamlR2Exception {

        try {

            if (logger.isDebugEnabled())
                logger.debug("Triggering IDP Session heart beat");

            CircleOfTrustMemberDescriptor idp = resolveIdp(secCtx.getIdpAlias());
            logger.debug("Using IdP " + idp.getAlias());

            // If no SP Channel is found is because the IDP is probably a remote provider and no SPChannel definition
            // can be found.
            // TODO : Add Heart Beat service to metadata to be able to send heartbeat requests to remote IDPS (only for Atricore IDPS)
            SPChannel spChannel = resolveSpChannel(idp);
            EndpointDescriptor ed = spChannel != null ? resolveIdpHeartBeatEndpoint(spChannel) : null;

            if (ed == null) {
                logger.debug("No HeartBeat endpoint found for " + idp.getAlias() + ". Heart Beat ignored.");
                return null;
            }

            IDPSessionHeartBeatRequestType req = buildIDPSessionHeartBeatRequest(secCtx);

            if (logger.isDebugEnabled())
                logger.debug("Sending IDPSessionHeartBeatRequest " + req.getID() +
                        " to IDP " + idp.getAlias() +
                        " using endpoint " + ed.getLocation());


            // We might need the
            IDPSessionHeartBeatResponseType res =
                    (IDPSessionHeartBeatResponseType) channel.getIdentityMediator().sendMessage(req, ed, spChannel);

            if (res.getInReplayTo() == null || !res.getInReplayTo().equals(req.getID()))
                throw new SamlR2Exception("Received response is not expected, invalid 'inReplayTo' attribute " +
                        res.getInReplayTo() + ", expected " + req.getID());

            return res;
        } catch (IdentityMediationException e) {
            throw new SamlR2Exception(e);
        } catch (IdentityPlanningException e) {
            throw new SamlR2Exception(e);
        }


    }

    protected CircleOfTrustMemberDescriptor resolveIdp(String idpAlias) {
        return getCotManager().loolkupMemberByAlias(idpAlias);
    }

    protected FederationChannel resolveIdpChannel(CircleOfTrustMemberDescriptor idpDescriptor) {
        // Resolve IdP channel, then look for the ACS endpoint
        BindingChannel bChannel = (BindingChannel) channel;
        LocalProvider sp = bChannel.getProvider();

        FederationChannel idpChannel = sp.getChannel();
        for (FederationChannel fChannel : sp.getChannels()) {

            Provider idp = fChannel.getTargetProvider();
            for (CircleOfTrustMemberDescriptor member : idp.getMembers()) {
                if (member.getAlias().equals(idpDescriptor.getAlias())) {

                    if (logger.isDebugEnabled())
                        logger.debug("Selected IdP channel " + fChannel.getName() + " for provider " + idp.getName());
                    idpChannel = fChannel;
                    break;
                }

            }

        }

        return idpChannel;

    }

    protected SPChannel resolveSpChannel(CircleOfTrustMemberDescriptor idp) throws SamlR2Exception {
        // The channel might be a binding or federation channel, get the main channel from the provider.
        CircleOfTrust cot = channel.getProvider().getChannel().getCircleOfTrust();

        // The channel we need to send messages to
        SPChannel spChannel = null;
        for (Provider p : cot.getProviders()) {

            // Because we send heartbeat events using SSO Protocol, we look for local providers ...
            // TODO : Support MD for SSO Protocol to use remote providers

            if (p instanceof LocalProvider) {

                LocalProvider lp = (LocalProvider) p;

                // Provider is probably a binding provider.
                if (lp.getChannel() == null || lp.getChannel().getMember() == null)
                    continue;

                if (lp.getChannel().getMember().getAlias().equals(idp.getAlias())) {
                    spChannel = (SPChannel) lp.getChannel();
                    break;
                }

                for (FederationChannel c : lp.getChannels()) {
                    if (c.getMember().getAlias().equals(idp.getAlias())) {
                        spChannel = (SPChannel) c;
                        break;
                    }
                }

                if (spChannel != null)
                    break;
            }

        }

        if (spChannel == null) {
            logger.debug("No SP Channel defined in local providers for " + idp.getAlias());
        }

        return spChannel;

    }

    protected EndpointDescriptor resolveIdpHeartBeatEndpoint(SPChannel spChannel) throws SamlR2Exception {

        IdentityMediationEndpoint endpoint = null;
        for (IdentityMediationEndpoint ep : spChannel.getEndpoints()) {

            if (ep.getType().equals(SamlR2Service.IDPSessionHeartBeatService.toString())) {

                // Local endpoints are preferred
                if (ep.getBinding().equals(SamlR2Binding.SSO_LOCAL.getValue())) {
                    endpoint = ep;
                    break;
                } else if (ep.getBinding().equals(SamlR2Binding.SSO_SOAP.getValue())) {
                    endpoint = ep; // keep looking, maybe there is a local endpoint
                }
            }
        }

        if (endpoint == null) {
            logger.warn("No IDP Endpoint supporting service/binding " + SamlR2Service.IDPSessionHeartBeatService + " in channel " + spChannel.getName());
            return null;
        }

        try {
            return channel.getIdentityMediator().resolveEndpoint(spChannel, endpoint);
        } catch (IdentityMediationException e) {
            logger.error("Cannot resolve endpoint " + endpoint.getName() + ". " + e.getMessage(), e);
            return null;
        }

    }

    protected IDPSessionHeartBeatRequestType buildIDPSessionHeartBeatRequest(SPSecurityContext secCtx) throws SamlR2Exception, IdentityPlanningException {

        IDPSessionHeartBeatRequestType request = new IDPSessionHeartBeatRequestType();
        request.setID(uuidGenerator.generateId());
        request.setSsoSessionId(secCtx.getIdpSsoSession());
        // TODO : request.setIssuer();

        return request;
    }


    protected SPSecurityContext updateSPSecurityContext(SPSecurityContext secCtx,
                                                        CamelMediationExchange exchange)
            throws SamlR2Exception, SSOSessionException {

        if (logger.isDebugEnabled())
            logger.debug("Updating SP Security Context for " + secCtx.getSessionIndex());

        // Use the main SSO Session manager, this should the same for all channels !
        IdPChannel idPChannel = (IdPChannel) channel.getProvider().getChannel();
        SSOSessionManager ssoSessionManager = idPChannel.getSessionManager();
        ssoSessionManager.accessSession(secCtx.getSessionIndex());

        if (logger.isDebugEnabled())
            logger.debug("Updated SP security context " + secCtx);

        return secCtx;


    }

}
