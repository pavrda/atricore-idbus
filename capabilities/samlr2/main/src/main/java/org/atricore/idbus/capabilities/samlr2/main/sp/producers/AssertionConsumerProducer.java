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

package org.atricore.idbus.capabilities.samlr2.main.sp.producers;

import oasis.names.tc.saml._2_0.assertion.*;
import oasis.names.tc.saml._2_0.metadata.EntityDescriptorType;
import oasis.names.tc.saml._2_0.metadata.IDPSSODescriptorType;
import oasis.names.tc.saml._2_0.metadata.RoleDescriptorType;
import oasis.names.tc.saml._2_0.protocol.AuthnRequestType;
import oasis.names.tc.saml._2_0.protocol.ResponseType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.capabilities.samlr2.main.SamlR2Exception;
import org.atricore.idbus.capabilities.samlr2.main.common.AbstractSamlR2Mediator;
import org.atricore.idbus.capabilities.samlr2.main.common.producers.SamlR2Producer;
import org.atricore.idbus.capabilities.samlr2.main.sp.SPSecurityContext;
import org.atricore.idbus.capabilities.samlr2.main.sp.SamlR2SPMediator;
import org.atricore.idbus.capabilities.samlr2.support.SAMLR2Constants;
import org.atricore.idbus.capabilities.samlr2.support.binding.SamlR2Binding;
import org.atricore.idbus.capabilities.samlr2.support.core.NameIDFormat;
import org.atricore.idbus.capabilities.samlr2.support.core.SamlR2ResponseException;
import org.atricore.idbus.capabilities.samlr2.support.core.StatusCode;
import org.atricore.idbus.capabilities.samlr2.support.core.StatusDetails;
import org.atricore.idbus.capabilities.samlr2.support.core.encryption.SamlR2Encrypter;
import org.atricore.idbus.capabilities.samlr2.support.core.encryption.SamlR2EncrypterException;
import org.atricore.idbus.capabilities.samlr2.support.core.signature.SamlR2SignatureException;
import org.atricore.idbus.capabilities.samlr2.support.core.signature.SamlR2SignatureValidationException;
import org.atricore.idbus.capabilities.samlr2.support.core.signature.SamlR2Signer;
import org.atricore.idbus.common.sso._1_0.protocol.SPAuthnResponseType;
import org.atricore.idbus.common.sso._1_0.protocol.SPInitiatedAuthnRequestType;
import org.atricore.idbus.kernel.main.authn.SecurityToken;
import org.atricore.idbus.kernel.main.authn.SecurityTokenImpl;
import org.atricore.idbus.kernel.main.federation.*;
import org.atricore.idbus.kernel.main.federation.metadata.*;
import org.atricore.idbus.kernel.main.mediation.IdentityMediationException;
import org.atricore.idbus.kernel.main.mediation.IdentityMediationFault;
import org.atricore.idbus.kernel.main.mediation.MediationMessageImpl;
import org.atricore.idbus.kernel.main.mediation.MediationState;
import org.atricore.idbus.kernel.main.mediation.camel.AbstractCamelEndpoint;
import org.atricore.idbus.kernel.main.mediation.camel.component.binding.CamelMediationExchange;
import org.atricore.idbus.kernel.main.mediation.camel.component.binding.CamelMediationMessage;
import org.atricore.idbus.kernel.main.mediation.channel.FederationChannel;
import org.atricore.idbus.kernel.main.mediation.channel.IdPChannel;
import org.atricore.idbus.kernel.main.session.SSOSessionManager;
import org.atricore.idbus.kernel.main.session.exceptions.NoSuchSessionException;
import org.atricore.idbus.kernel.main.session.exceptions.SSOSessionException;
import org.atricore.idbus.kernel.main.util.UUIDGenerator;
import org.w3._2001._04.xmlenc_.EncryptedType;
import org.w3c.dom.Element;

import javax.security.auth.Subject;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;

/**
 * @author <a href="mailto:sgonzalez@atricore.org">Sebastian Gonzalez Oyuela</a>
 * @version $Id$
 */
public class AssertionConsumerProducer extends SamlR2Producer {

    private static final Log logger = LogFactory.getLog(AssertionConsumerProducer.class);

    private UUIDGenerator uuidGenerator = new UUIDGenerator();

    public AssertionConsumerProducer(AbstractCamelEndpoint endpoint) throws Exception {
        super(endpoint);
    }

    @Override
    protected void doProcess(CamelMediationExchange exchange) throws Exception {
        
        // Incomming message
        CamelMediationMessage in = (CamelMediationMessage) exchange.getIn();

        // Mediation state
        MediationState state = in.getMessage().getState();

        // Originally received Authn request from binding channel
        // When using IdP initiated SSO, this will be null!
        SPInitiatedAuthnRequestType ssoRequest =
                (SPInitiatedAuthnRequestType) state.getLocalVariable("urn:org:atricore:idbus:sso:protocol:SPInitiatedAuthnRequest");
        state.removeLocalVariable("urn:org:atricore:idbus:sso:protocol:SPInitiatedAuthnRequest");

        // Destination, where to respond the above referred request
        String destinationLocation = ((SamlR2SPMediator) channel.getIdentityMediator()).getSpBindingACS();
        if (destinationLocation == null)
            throw new SamlR2Exception("SP Mediator does not have resource location!");

        EndpointDescriptor destination =
                new EndpointDescriptorImpl("EmbeddedSPAcs",
                        "AssertionConsumerService",
                        SamlR2Binding.SSO_ARTIFACT.getValue(),
                        destinationLocation, null);

        // ------------------------------------------------------
        // Resolve IDP configuration!
        // ------------------------------------------------------
        // Create in/out artifacts
        ResponseType response = (ResponseType) in.getMessage().getContent();

        // Stored by SP Initiated producer
        AuthnRequestType authnRequest =
                (AuthnRequestType) state.getLocalVariable(SAMLR2Constants.SAML_PROTOCOL_NS + ":AuthnRequest");
        state.removeLocalVariable(SAMLR2Constants.SAML_PROTOCOL_NS + ":AuthnRequest");

        if (logger.isDebugEnabled())
            logger.debug("Previous AuthnRequest " + (authnRequest != null ? authnRequest.getID() : "<NONE>"));

        validateResponse(authnRequest, response, in.getMessage().getRawContent());

        // Response is valid, check received status!
        StatusCode status = StatusCode.asEnum(response.getStatus().getStatusCode().getValue());
        StatusCode secStatus = response.getStatus().getStatusCode().getStatusCode() != null ?
                StatusCode.asEnum(response.getStatus().getStatusCode().getStatusCode().getValue()) : null;

        if (logger.isDebugEnabled())
                logger.debug("Received status code " + status.getValue() +
                        (secStatus != null ? "/" + secStatus.getValue() : ""));

        if (status.equals(StatusCode.TOP_RESPONDER) &&
            secStatus != null &&
            secStatus.equals(StatusCode.NO_PASSIVE)) {

            // Automatic Login failed
            if (logger.isDebugEnabled())
                logger.debug("IDP Reports Passive login failed");

            // if This is  SP initiated SSO or  we did not requested passive authentication
            if (authnRequest == null || authnRequest.isForceAuthn()) {
                throw new SamlR2Exception("IDP Sent " + StatusCode.NO_PASSIVE + " but passive was not requested.");
            }

            // Send a 'no-passive' status response
            // TODO : Use plan samlr2resposne to spauthnresponse
            SPAuthnResponseType ssoResponse = new SPAuthnResponseType();
            ssoResponse.setID(response.getID());
            if (ssoRequest != null) {
                ssoResponse.setInReplayTo(ssoRequest.getID());
                if (ssoRequest.getReplyTo() != null) {
                    destination = new EndpointDescriptorImpl("EmbeddedSPAcs",
                            "AssertionConsumerService",
                            SamlR2Binding.SSO_ARTIFACT.getValue(),
                            ssoRequest.getReplyTo(), null);
                }
            }

            CamelMediationMessage out = (CamelMediationMessage) exchange.getOut();
            out.setMessage(new MediationMessageImpl(ssoResponse.getID(),
                    ssoResponse, "SPAuthnResposne", null, destination, in.getMessage().getState()));

            exchange.setOut(out);
            return;


        } else if (!status.equals(StatusCode.TOP_SUCCESS)) {
            throw new SamlR2Exception("Unexpected IDP Status Code " + status.getValue() +
                    (secStatus != null ? "/" + secStatus.getValue() : ""));

        }

        // check if there is an existing session for the user
        // if not, check if channel is federation-capable
        FederationChannel fChannel = (FederationChannel) channel;
        if (fChannel.getAccountLinkLifecycle() == null) {

            // cannot map subject to local account, terminate
            logger.error("No Account Lifecycle configured for Channel [" + fChannel.getName() + "] " +
                    " Response [" + response.getID() + "]");
            throw new SamlR2Exception("No Account Lifecycle configured for Channel [" + fChannel.getName() + "] " +
                    " Response [" + response.getID() + "]");
        }


        AccountLinkLifecycle accountLinkLifecycle = fChannel.getAccountLinkLifecycle();

        // ------------------------------------------------------------------
        // Build IDP Subject from response
        // ------------------------------------------------------------------
        Subject idpSubject = buildSubjectFromResponse(response);

        // check if there is an existing account link for the assertion's subject
        AccountLink acctLink = null;

        /* TODO : For now, only dymanic link is supported!
        if (accountLinkLifecycle.persistentForIDPSubjectExists(idpSubject)) {
            acctLink = accountLinkLifecycle.findByIDPAccount(idpSubject);
            logger.debug("Persistent Account Link Found for Channel [" + fChannel.getName() + "] " +
                        "IDP Subject [" + idpSubject + "]" );
        } else if (accountLinkLifecycle.transientForIDPSubjectExists(idpSubject)) {
            acctLink = accountLinkLifecycle.findByIDPAccount(idpSubject);
            logger.debug("Transient Account Link Found for Channel [" + fChannel.getName() + "] " +
                        "IDP Subject [" + idpSubject + "]"
                       );
        } else {
            // there isn't an account link, therefore emit one using the configured
            // account link emitter
            AccountLinkEmitter accountLinkEmitter = fChannel.getAccountLinkEmitter();

            logger.debug("Account Link Emitter Found for Channel [" + fChannel.getName() + "] " +
                        "IDP Subject [" + idpSubject + "]"
                       );

            if (accountLinkEmitter != null) {

                acctLink = accountLinkEmitter.emit(idpSubject);
                logger.debug("Emitter Account Link [" + (acctLink != null ? acctLink.getRegion() : "null") + "] [" + fChannel.getName() + "] " +
                            "IDP Subject [" + idpSubject + "]"
                           );
            }
        } */

        // there isn't an account link, therefore emit one using the configured
        // account link emitter
        AccountLinkEmitter accountLinkEmitter = fChannel.getAccountLinkEmitter();
        logger.trace("Account Link Emitter Found for Channel [" + fChannel.getName() + "]");

        if (accountLinkEmitter != null) {
            acctLink = accountLinkEmitter.emit(idpSubject);

            if (logger.isDebugEnabled())
                logger.debug("Emitted Account Link [" +
                        (acctLink != null ? "[" + acctLink.getId() + "]" + acctLink.getLocalAccountNameIdentifier() : "null") +
                        "] [" + fChannel.getName() + "] " +
                        " for IDP Subject [" + idpSubject + "]" );
        }

        if (acctLink == null) {

            logger.error("No Account Link for Channel [" + fChannel.getName() + "] " +
                    " Response [" + response.getID() + "]");

            throw new IdentityMediationFault(StatusCode.TOP_REQUESTER.getValue(),
                    null,
                    StatusDetails.NO_ACCOUNT_LINK.getValue(),
                    idpSubject.toString(), null);
        }

        // ------------------------------------------------------------------
        // fetch local account for subject, if any
        // ------------------------------------------------------------------
        Subject localAccountSubject = accountLinkLifecycle.resolve(acctLink);
        if (logger.isTraceEnabled())
            logger.trace("Account Link [" + acctLink.getId() + "] resolved to " +
                     "Local Subject [" + localAccountSubject + "] ");

        Subject federatedSubject = localAccountSubject; // if no identity mapping, the local account
                                                        // subject is used

        // having both idp and local account is now time to apply custom identity mapping rules
        if (fChannel.getIdentityMapper() != null) {
            IdentityMapper im = fChannel.getIdentityMapper();

            if (logger.isTraceEnabled())
                logger.trace("Using identity mapper : " + im.getClass().getName());

            federatedSubject = im.map(idpSubject, localAccountSubject);
        }


        if (logger.isDebugEnabled())
            logger.debug("IDP Subject [" + idpSubject + "] mapped to Subject [" + federatedSubject + "] " +
                     "through Account Link [" + acctLink.getId() + "]" );

        // ---------------------------------------------------
        // Create SP Security context and session!
        // ---------------------------------------------------

        CircleOfTrustMemberDescriptor idp = resolveIdp(exchange);

        SPSecurityContext spSecurityCtx = createSPSecurityContext(exchange,
                ssoRequest.getReplyTo(),
                idp,
                acctLink,
                federatedSubject,
                idpSubject);

        // ---------------------------------------------------
        // Send SPAuthnResponse
        // ---------------------------------------------------

        // TODO : Use plan samlr2resposne to spauthnresponse
        SPAuthnResponseType ssoResponse = new SPAuthnResponseType();
        ssoResponse.setID(response.getID());
        if (ssoRequest != null) {
            ssoResponse.setInReplayTo(ssoRequest.getID());
            if (ssoRequest.getReplyTo() != null) {
                destination = new EndpointDescriptorImpl("EmbeddedSPAcs",
                        "AssertionConsumerService",
                        SamlR2Binding.SSO_ARTIFACT.getValue(),
                        ssoRequest.getReplyTo(), null);
            }
        }

        ssoResponse.setSubject(toSubjectType(spSecurityCtx.getSubject()));
        ssoResponse.setSessionIndex(spSecurityCtx.getSessionIndex());

        CamelMediationMessage out = (CamelMediationMessage) exchange.getOut();
        out.setMessage(new MediationMessageImpl(ssoResponse.getID(),
                ssoResponse, "SPAuthnResposne", null, destination, in.getMessage().getState()));

        exchange.setOut(out);

    }

    private Subject buildSubjectFromResponse(ResponseType response) {

        Subject outSubject = new Subject();

        if (response.getAssertionOrEncryptedAssertion().size() > 0) {

            AssertionType assertion = null;

            if (response.getAssertionOrEncryptedAssertion().get(0) instanceof AssertionType) {
                assertion = (AssertionType) response.getAssertionOrEncryptedAssertion().get(0);
            } else {
                throw new RuntimeException("Response should be already decripted!");
            }

            // store subject identification information
            if (assertion.getSubject() != null) {

                List subjectContentItems = assertion.getSubject().getContent();

                for (Object o: subjectContentItems) {

                    JAXBElement subjectContent = (JAXBElement) o;

                    if (subjectContent.getValue() instanceof NameIDType ) {

                        NameIDType nameId = (NameIDType) subjectContent.getValue();
                        // Create Subject ID Attribute
                        if (logger.isDebugEnabled()) {
                            logger.debug("Adding NameID to IDP Subject {"+nameId.getSPNameQualifier()+"}" + nameId.getValue() +  ":" + nameId.getFormat());
                        }
                        outSubject.getPrincipals().add(
                                new SubjectNameID(nameId.getValue(),
                                        nameId.getFormat(),
                                        nameId.getNameQualifier(),
                                        nameId.getSPNameQualifier()));


                    } else if (subjectContent.getValue() instanceof BaseIDAbstractType) {
                        // TODO : Can we do something with this ?
                        throw new IllegalArgumentException("Unsupported Subject BaseID type "+ subjectContent.getValue() .getClass().getName());

                    } else if (subjectContent.getValue() instanceof EncryptedType) {
                        throw new IllegalArgumentException("Response should be already decripted!");

                    } else if (subjectContent.getValue() instanceof SubjectConfirmationType) {
                        // TODO : Store subject confirmation data ?
                    } else {
                        logger.error("Unknown subject content type : " + subjectContent.getClass().getName());
                    }


                }

            }

            // store subject user attributes
            List<StatementAbstractType> stmts = assertion.getStatementOrAuthnStatementOrAuthzDecisionStatement();
            if (logger.isDebugEnabled())
                logger.debug("Found " + stmts.size() + " statements") ;

            for (StatementAbstractType stmt : stmts) {

                if (logger.isDebugEnabled())
                    logger.debug("Processing statement " + stmts) ;

                if (stmt instanceof AttributeStatementType) {

                    AttributeStatementType attrStmt = (AttributeStatementType) stmt;

                    List attrs = attrStmt.getAttributeOrEncryptedAttribute();

                    if (logger.isDebugEnabled())
                        logger.debug("Found " + attrs.size() + " attributes in attribute statement") ;

                    for (Object attrOrEncAttr : attrs) {

                        if (attrOrEncAttr instanceof AttributeType) {

                            AttributeType attr = (AttributeType) attrOrEncAttr;

                            List<Object> attributeValues = attr.getAttributeValue();

                            if (logger.isDebugEnabled())
                                logger.debug("Processing attribute " + attr.getName()) ;

                            for (Object attributeValue : attributeValues) {

                                if (logger.isDebugEnabled())
                                    logger.debug("Processing attribute value " + attributeValue) ;

                                if (attributeValue instanceof String ) {

                                    if (logger.isDebugEnabled()) {
                                        logger.debug("Adding Attribute Statement to IDP Subject " +
                                                attr.getName() + ":" +
                                                attr.getNameFormat() + "=" +
                                                attr.getAttributeValue()) ;
                                    }

                                    outSubject.getPrincipals().add(
                                            new SubjectAttribute(
                                                attr.getName(),
                                                (String) attributeValue
                                            )

                                    );

                                } else if (attributeValue instanceof Element) {
                                    Element e = (Element) attributeValue;
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("Adding Attribute Statement to IDP Subject from DOM Element " +
                                                attr.getName() + ":" +
                                                attr.getNameFormat() + "=" +
                                                e.getTextContent()) ;
                                    }

                                    outSubject.getPrincipals().add(
                                            new SubjectAttribute(
                                                attr.getName(),
                                                e.getTextContent()
                                            )

                                    );


                                } else {
                                    logger.error("Unknown Attribute Value type " + attributeValue.getClass().getName());
                                }

                            }
                        } else {
                            // TODO : Decrypt attribute using IDP's encryption key
                            logger.debug("Unknown attribute type " + attrOrEncAttr);
                        }
                    }
                }

                // store subject authentication attributes
                if (stmt instanceof AuthnStatementType) {
                    AuthnStatementType authnStmt = (AuthnStatementType) stmt;

                    List<JAXBElement<?>> authnContextItems = authnStmt.getAuthnContext().getContent();

                    for (JAXBElement<?> authnContext : authnContextItems) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Adding Authentiation Context to IDP Subject " +
                                    authnContext.getValue() + ":" +
                                    SubjectAuthenticationAttribute.Name.AUTHENTICATION_CONTEXT) ;
                        }

                        outSubject.getPrincipals().add(
                                new SubjectAuthenticationAttribute(
                                        SubjectAuthenticationAttribute.Name.AUTHENTICATION_CONTEXT,
                                        (String) authnContext.getValue()
                                )
                        );

                    }

                    if (authnStmt.getAuthnInstant() != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Adding Authentiation Attribute to IDP Subject " +
                                    authnStmt.getAuthnInstant().toString() + ":" +
                                    SubjectAuthenticationAttribute.Name.AUTHENTICATION_INSTANT) ;
                        }
                        outSubject.getPrincipals().add(
                                new SubjectAuthenticationAttribute(
                                        SubjectAuthenticationAttribute.Name.AUTHENTICATION_INSTANT,
                                        authnStmt.getAuthnInstant().toString()
                                )
                        );
                    }


                    if (authnStmt.getSessionIndex() != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Adding Authentiation Attribute to IDP Subject " +
                                    authnStmt.getSessionIndex() + ":" +
                                    SubjectAuthenticationAttribute.Name.SESSION_INDEX) ;
                        }
                        outSubject.getPrincipals().add(
                                new SubjectAuthenticationAttribute(
                                        SubjectAuthenticationAttribute.Name.SESSION_INDEX,
                                        authnStmt.getSessionIndex()
                                )
                        );
                    }

                    if (authnStmt.getSessionNotOnOrAfter() != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Adding Authentiation Attribute to IDP Subject " +
                                    authnStmt.getSessionNotOnOrAfter().toString() + ":" +
                                    SubjectAuthenticationAttribute.Name.SESSION_NOT_ON_OR_AFTER) ;
                        }
                        outSubject.getPrincipals().add(
                                new SubjectAuthenticationAttribute(
                                        SubjectAuthenticationAttribute.Name.SESSION_NOT_ON_OR_AFTER,
                                        authnStmt.getSessionNotOnOrAfter().toString()
                                )
                        );
                    }

                    if (authnStmt.getSubjectLocality() != null && authnStmt.getSubjectLocality().getAddress() != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Adding Authentiation Attribute to IDP Subject " +
                                    authnStmt.getSubjectLocality().getAddress() + ":" +
                                    SubjectAuthenticationAttribute.Name.SUBJECT_LOCALITY_ADDRESS) ;
                        }
                        outSubject.getPrincipals().add(
                                new SubjectAuthenticationAttribute(
                                        SubjectAuthenticationAttribute.Name.SUBJECT_LOCALITY_ADDRESS,
                                        authnStmt.getSubjectLocality().getAddress()
                                )
                        );
                    }


                    if (authnStmt.getSubjectLocality() != null && authnStmt.getSubjectLocality().getDNSName() != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Adding Authentiation Attribute to IDP Subject " +
                                    authnStmt.getSubjectLocality().getDNSName() + ":" +
                                    SubjectAuthenticationAttribute.Name.SUBJECT_LOCALITY_DNSNAME) ;
                        }
                        outSubject.getPrincipals().add(
                                new SubjectAuthenticationAttribute(
                                        SubjectAuthenticationAttribute.Name.SUBJECT_LOCALITY_DNSNAME,
                                        authnStmt.getSubjectLocality().getDNSName()
                                )
                        );
                    }
                }

                // Store subject authorization attributes
                if (stmt instanceof AuthzDecisionStatementType) {
                    AuthzDecisionStatementType authzStmt = (AuthzDecisionStatementType) stmt;

                    for (ActionType action : authzStmt.getAction()) {

                        if (action.getNamespace() != null) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Adding Authz Decision Action NS to IDP Subject " +
                                        action.getNamespace() + ":" +
                                        SubjectAuthorizationAttribute.Name.ACTION_NAMESPACE) ;
                            }
                            outSubject.getPrincipals().add(
                                    new SubjectAuthorizationAttribute(
                                            SubjectAuthorizationAttribute.Name.ACTION_NAMESPACE,
                                            action.getNamespace()
                                    )
                            );
                        }

                        if (action.getValue() != null) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Adding Authz Decision Action Value to IDP Subject " +
                                        action.getValue() + ":" +
                                        SubjectAuthorizationAttribute.Name.ACTION_VALUE) ;
                            }
                            outSubject.getPrincipals().add(
                                    new SubjectAuthorizationAttribute(
                                            SubjectAuthorizationAttribute.Name.ACTION_VALUE,
                                            action.getValue()
                                    )
                            );
                        }

                    }

                    if (authzStmt.getDecision() != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Adding Authz Decision Action to IDP Subject " +
                                    authzStmt.getDecision().value() + ":" +
                                    SubjectAuthorizationAttribute.Name.DECISION) ;
                        }
                        outSubject.getPrincipals().add(
                                new SubjectAuthorizationAttribute(
                                        SubjectAuthorizationAttribute.Name.DECISION,
                                        authzStmt.getDecision().value()
                                )
                        );
                    }

                    if (authzStmt.getResource() != null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Adding Authz Decision Action to IDP Subject " +
                                    authzStmt.getResource() + ":" +
                                    SubjectAuthorizationAttribute.Name.RESOURCE) ;
                        }
                        outSubject.getPrincipals().add(
                                new SubjectAuthorizationAttribute(
                                        SubjectAuthorizationAttribute.Name.RESOURCE,
                                        authzStmt.getResource()
                                )
                        );
                    }

                    // TODO: store evidence

                }

            }


        } else {
            logger.warn("No Assertion present within Response [" + response.getID() + "]");
        }

        if (outSubject != null && logger.isDebugEnabled()) {
            logger.debug("IDP Subject:" + outSubject) ;
        }

        return outSubject;
    }


    // TODO : Reuse basic SAML request validations ....
    protected ResponseType validateResponse(AuthnRequestType request,
                                            ResponseType response,
                                            String originalResponse)
            throws SamlR2ResponseException, SamlR2Exception {

        AbstractSamlR2Mediator mediator = (AbstractSamlR2Mediator) channel.getIdentityMediator();
        SamlR2Signer signer = mediator.getSigner();
        SamlR2Encrypter encrypter = mediator.getEncrypter();

        // Metadata from the IDP
        String idpAlias = null;
        IDPSSODescriptorType idpMd = null;

        // Request can be null for IDP initiated SSO
    	EndpointDescriptor endpointDesc;
		try {
			endpointDesc = channel.getIdentityMediator().resolveEndpoint(channel, endpoint);

		} catch (IdentityMediationException e1) {
			throw new SamlR2ResponseException(response,
                    StatusCode.TOP_REQUESTER,
                    StatusCode.RESOURCE_NOT_RECOGNIZED,
                    StatusDetails.INTERNAL_ERROR,
                    "Cannot resolve endpoint descriptor", e1);
		}

        try {
            idpAlias = response.getIssuer().getValue();
            MetadataEntry md = getCotManager().findEntityMetadata(idpAlias);
            EntityDescriptorType saml2Md = (EntityDescriptorType) md.getEntry();
            boolean found = false;
            for (RoleDescriptorType roleMd : saml2Md.getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor()) {

                if (roleMd instanceof IDPSSODescriptorType) {
                    idpMd = (IDPSSODescriptorType) roleMd;
                }
            }

        } catch (CircleOfTrustManagerException e) {
            throw new SamlR2ResponseException(response,
                    StatusCode.TOP_RESPONDER,
                    StatusCode.NO_SUPPORTED_IDP,
                    null,
                    response.getIssuer().getValue(),
                    e);
        }


        if  (idpMd == null) {

            logger.debug("No IDP Metadata found");
            // Unknown IDP!
            throw new SamlR2ResponseException(response,
                    StatusCode.TOP_RESPONDER,
                    StatusCode.NO_SUPPORTED_IDP,
                    null, idpAlias);
        }

        // --------------------------------------------------------
        // Validate response:
        // --------------------------------------------------------

        // Destination
    	//saml2 binding, sections 3.4.5.2 & 3.5.5.2
    	if(response.getDestination() != null) {

            //saml2 core, section 3.2.2
            String location = endpointDesc.getResponseLocation();
            if (location ==null)
                location = endpointDesc.getLocation();


    		if(!response.getDestination().equals(location)){
    			throw new SamlR2ResponseException(response,
                        StatusCode.TOP_REQUESTER,
                        StatusCode.REQUEST_DENIED,
                        StatusDetails.INVALID_DESTINATION);
    		}

    	} else if(response.getSignature() != null &&
                (!endpointDesc.getBinding().equals(SamlR2Binding.SAMLR2_LOCAL.getValue()) &&
                 !endpointDesc.getBinding().equals(SamlR2Binding.SAMLR2_ARTIFACT.getValue()))) {

            // If message is signed, the destination is mandatory!
            //saml2 binding, sections 3.4.5.2 & 3.5.5.2
    		throw new SamlR2ResponseException(response,
                    StatusCode.TOP_REQUESTER,
                    StatusCode.REQUEST_DENIED,
                    StatusDetails.NO_DESTINATION);
    	}

        // IssueInstant
		/*
		   -  required 
		   -  check that the response time is not before request time (use UTC) 
		   -  check that time difference is not bigger than X
		   */
    	if(response.getIssueInstant() == null){
    		throw new SamlR2ResponseException(response,
                    StatusCode.TOP_REQUESTER,
                    StatusCode.INVALID_ATTR_NAME_OR_VALUE,
                    StatusDetails.NO_ISSUE_INSTANT);

    	} else if(request != null) {

           	if(response.getIssueInstant().compare(request.getIssueInstant()) <= 0){
    			throw new SamlR2ResponseException(response,
                        StatusCode.TOP_REQUESTER,
                        StatusCode.INVALID_ATTR_NAME_OR_VALUE,
                        StatusDetails.INVALID_ISSUE_INSTANT,
                        response.getIssueInstant().toGregorianCalendar().toString() +
                                    " earlier than request issue instant.");

    		} else {


                long ttl = mediator.getRequestTimeToLive();

                long res = response.getIssueInstant().toGregorianCalendar().getTime().getTime();
                long req = request.getIssueInstant().toGregorianCalendar().getTime().getTime();

                if (logger.isDebugEnabled())
                    logger.debug("TTL : " + res + " - " +  req + " = " + (res - req));

                // If 0, response does not expires!
    			if(ttl > 0 && response.getIssueInstant().toGregorianCalendar().getTime().getTime()
    					- request.getIssueInstant().toGregorianCalendar().getTime().getTime() > ttl) {

    				throw new SamlR2ResponseException(response,
                            StatusCode.TOP_REQUESTER,
                            StatusCode.INVALID_ATTR_NAME_OR_VALUE,
                            StatusDetails.INVALID_ISSUE_INSTANT,
                            response.getIssueInstant().toGregorianCalendar().toString() +
                                    " expired after " + ttl + "ms");
    			} 
    		}
    	}

        // Version, saml2 core, section 3.2.2
    	if(response.getVersion() == null) {
    		throw new SamlR2ResponseException(response,
                    StatusCode.TOP_VERSION_MISSMATCH,
                    null,
                    StatusDetails.INVALID_VERSION);
    	}

        if (!response.getVersion().equals(SAML_VERSION)){

            throw new SamlR2ResponseException(response,
                    StatusCode.TOP_VERSION_MISSMATCH,
                    null, // TODO : Check version!
                    StatusDetails.UNSUPPORTED_VERSION,
                    response.getVersion());
        }
    	
        // InResponseTo, saml2 core, section 3.2.2
    	// Request can be null for IDP initiated SSO


    	if(request != null) {
            if (response.getInResponseTo() == null) {
                throw new SamlR2ResponseException(response,
                        StatusCode.TOP_REQUESTER,
                        null,
                        StatusDetails.NO_IN_RESPONSE_TO);

            } else if (!request.getID().equals(response.getInResponseTo())) {
                throw new SamlR2ResponseException(response,
                        StatusCode.TOP_REQUESTER,
                        null,
                        StatusDetails.INVALID_RESPONSE_ID,
                        request.getID() + "/ " + response.getInResponseTo());
            }

    	}

        // Status.StatusDetails
    	if(response.getStatus() != null) {
    		if(response.getStatus().getStatusCode() != null) {

    			if(StringUtils.isEmpty(response.getStatus().getStatusCode().getValue()) 
    					|| !isStatusCodeValid(response.getStatus().getStatusCode().getValue())){

    				throw new SamlR2ResponseException(response,
                            StatusCode.TOP_REQUESTER,
                            StatusCode.INVALID_ATTR_NAME_OR_VALUE,
                            StatusDetails.INVALID_STATUS_CODE,
                            response.getStatus().getStatusCode().getValue());
    			}
    		} else {
    			throw new SamlR2ResponseException(response,
                        StatusCode.TOP_REQUESTER,
                        StatusCode.INVALID_ATTR_NAME_OR_VALUE,
                        StatusDetails.NO_STATUS_CODE);
    		}
    	} else {
    		throw new SamlR2ResponseException(response,
                    StatusCode.TOP_REQUESTER,
                    StatusCode.INVALID_ATTR_NAME_OR_VALUE,
                    StatusDetails.NO_STATUS);
    	}
    	
		// XML Signature, saml2 core, section 5
        if (mediator.isEnableSignatureValidation()) {

            if (response.getSignature() == null) {
                throw new SamlR2ResponseException(response,
                        StatusCode.TOP_REQUESTER,
                        StatusCode.REQUEST_DENIED,
                        StatusDetails.INVALID_RESPONSE_SIGNATURE);
            }

            try {
                // It's better to validate the original message, when available.

                if (originalResponse != null)
                    signer.validate(idpMd, originalResponse);
                else
                    signer.validate(idpMd, response);

            } catch (SamlR2SignatureValidationException e) {
                throw new SamlR2ResponseException(response,
                        StatusCode.TOP_REQUESTER,
                        StatusCode.REQUEST_DENIED,
                        StatusDetails.INVALID_RESPONSE_SIGNATURE, e);
            } catch (SamlR2SignatureException e) {
                //other exceptions like JAXB, xml parser...
                throw new SamlR2ResponseException(response,
                        StatusCode.TOP_REQUESTER,
                        StatusCode.REQUEST_DENIED,
                        StatusDetails.INVALID_RESPONSE_SIGNATURE, e);
            }
        }

        // --------------------------------------------------------
        // Validate also assertion contained in response!
        // --------------------------------------------------------
        AssertionType assertion = null;

        // Decrypt if encrypted 
        List assertionObjects = response.getAssertionOrEncryptedAssertion();
        for (Object assertionObject : assertionObjects) {
			if(assertionObject instanceof AssertionType){
				assertion = (AssertionType) assertionObject;								
			} else if(assertionObject instanceof EncryptedElementType){

				try {
					assertion = encrypter.decryptAssertion((EncryptedElementType)assertionObject);
				} catch (SamlR2EncrypterException e) {

					throw new SamlR2ResponseException(response,
                            StatusCode.TOP_REQUESTER,
                            StatusCode.REQUEST_DENIED,
                            StatusDetails.INVALID_ASSERTION_ENCRYPTION, e);
				}
			}

			// XML Signature, saml core, section 5
            /* NOT WORKING OK ... */
			if(mediator.isEnableSignatureValidation()){

                if (assertion.getSignature() == null) {
                    throw new SamlR2ResponseException(response,
                            StatusCode.TOP_REQUESTER,
                            StatusCode.REQUEST_DENIED,
                            StatusDetails.INVALID_ASSERTION_SIGNATURE);
                }
                
				try {

                    if (originalResponse != null)
                        signer.validate(idpMd, originalResponse);
                    else
					    signer.validate(idpMd, assertion);

				} catch (SamlR2SignatureValidationException e) {
					throw new SamlR2ResponseException(response,
                            StatusCode.TOP_REQUESTER,
                            StatusCode.REQUEST_DENIED,
                            StatusDetails.INVALID_ASSERTION_SIGNATURE, e);
				} catch (SamlR2SignatureException e) {
					throw new SamlR2ResponseException(response,
                            StatusCode.TOP_REQUESTER,
                            StatusCode.REQUEST_DENIED,
                            StatusDetails.INVALID_ASSERTION_SIGNATURE, e);
				}
			}


        // Conditions
			// - optional

	        validateAssertionConditions(response, assertion.getConditions());

	        // Subject, saml2 core, sections 2.3.3 & 2.7.2
			if(assertion.getSubject() != null){
				for (JAXBElement object : assertion.getSubject().getContent()) {
					Object subjectContent = object.getValue();
					if(subjectContent instanceof SubjectConfirmationType){
						SubjectConfirmationType subConf = (SubjectConfirmationType)subjectContent;
						if(subConf.getMethod() == null){
							throw new SamlR2ResponseException(response,
                                    StatusCode.TOP_REQUESTER,
                                    StatusCode.INVALID_ATTR_NAME_OR_VALUE,
                                    StatusDetails.NO_METHOD);
						}
						
						// saml2 core, section 2.4.1.2
						if(subConf.getSubjectConfirmationData() != null){
							SubjectConfirmationDataType scData = subConf.getSubjectConfirmationData(); 
							if(assertion.getConditions() != null){
								logger.debug("scData.getNotBefore(): " + scData.getNotBefore());
								logger.debug("assertion.getConditions().getNotBefore()" + assertion.getConditions().getNotBefore());
								if(scData.getNotBefore() != null && assertion.getConditions().getNotBefore() != null
										&& scData.getNotBefore().normalize().compare(assertion.getConditions().getNotBefore().normalize()) < 0){
                                    logger.warn("SubjectConfirmationData.NotBefore value SHOULD not be earlier than Conditions.NotBefore.");
									// TODO : Should be configurable : throw new SamlR2ResponseException("SubjectConfirmationData.NotBefore value SHOULD not be earlier than Conditions.NotBefore.");
								}
								logger.debug("scData.getNotOnOrAfter(): " + scData.getNotOnOrAfter());
								logger.debug("assertion.getConditions().getNotOnOrAfter()" + assertion.getConditions().getNotOnOrAfter());
								if(scData.getNotOnOrAfter() != null && assertion.getConditions().getNotOnOrAfter() != null
										&& scData.getNotOnOrAfter().normalize().compare(assertion.getConditions().getNotOnOrAfter().normalize()) > 0){
									// TODO : Should be configurable : throw new SamlR2ResponseException("SubjectConfirmationData.NotOnOrAfter value SHOULD not be later than Conditions.NotOnOrAfter.");
                                    logger.warn("SubjectConfirmationData.NotOnOrAfter value SHOULD not be later than Conditions.NotOnOrAfter.");

								}
							}
							if(scData.getNotBefore() != null && scData.getNotOnOrAfter() != null
									&& scData.getNotOnOrAfter().normalize().compare(scData.getNotBefore().normalize()) < 0){
								// TODO : Should be configurable : throw new SamlR2ResponseException("SubjectConfirmationData.NotBefore value SHOULD be earlier than SubjectConfirmationData.NotOnOrAfter.");
                                logger.warn("SubjectConfirmationData.NotBefore value SHOULD be earlier than SubjectConfirmationData.NotOnOrAfter.");
								
							}
						}
						
					}
				}
								
			} else if (assertion.getStatementOrAuthnStatementOrAuthzDecisionStatement() == null 
						|| assertion.getStatementOrAuthnStatementOrAuthzDecisionStatement().size() == 0){
				throw new SamlR2ResponseException(response,
                        StatusCode.TOP_REQUESTER,
                        StatusCode.INVALID_ATTR_NAME_OR_VALUE,
                        StatusDetails.NO_SUBJECT);
			} else if (getAuthnStatements(assertion).size() != 0){
				throw new SamlR2ResponseException(response,
                        StatusCode.TOP_REQUESTER,
                        StatusCode.INVALID_ATTR_NAME_OR_VALUE,
                        StatusDetails.NO_SUBJECT);
			}
			
	        // AuthnStatement, saml2 core, section 2.7.2
	        List<AuthnStatementType> authnStatementList = getAuthnStatements(assertion);
	        if(authnStatementList.size() != 0){
				for (AuthnStatementType statement : authnStatementList) {
					if(statement.getAuthnInstant() == null){
						throw new SamlR2ResponseException(response,
                                StatusCode.TOP_REQUESTER,
                                StatusCode.INVALID_ATTR_NAME_OR_VALUE,
                                StatusDetails.NO_AUTHN_INSTANT);
					}
					if(statement.getAuthnContext() == null){
                        throw new SamlR2ResponseException(response,
                                StatusCode.TOP_REQUESTER,
                                StatusCode.INVALID_ATTR_NAME_OR_VALUE,
                                StatusDetails.NO_AUTHN_CONTEXT);
					}
				}
	        }	        
		}
        return response;
    }

    /*
     * Saml2 core, section 2.5.1
     */
    private void validateAssertionConditions(ResponseType response, ConditionsType conditions) throws SamlR2Exception, SamlR2ResponseException {

        if (conditions == null)
            return;

		Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		if(conditions.getConditionOrAudienceRestrictionOrOneTimeUse() == null 
				&& conditions.getNotBefore() == null && conditions.getNotOnOrAfter() == null){
			return;
		}

		logger.debug("Current time (UTC): " + utcCalendar.toString());

		XMLGregorianCalendar notBeforeUTC = null;		
		XMLGregorianCalendar notOnOrAfterUTC = null;
		
		if(conditions.getNotBefore() != null){			
			//normalize to UTC			
			logger.debug("Conditions.NotBefore: " + conditions.getNotBefore());

			notBeforeUTC = conditions.getNotBefore().normalize();
			logger.debug("Conditions.NotBefore normalized: " + notBeforeUTC.toString());
			
			if(!notBeforeUTC.isValid()){
				throw new SamlR2ResponseException(response,
                        StatusCode.TOP_REQUESTER,
                        StatusCode.INVALID_ATTR_NAME_OR_VALUE,
                        StatusDetails.INVALID_UTC_VALUE, notBeforeUTC.toString());
			} else if(!notBeforeUTC.toGregorianCalendar().getTime().before(utcCalendar.getTime())){
                throw new SamlR2ResponseException(response,
                        StatusCode.TOP_REQUESTER,
                        StatusCode.INVALID_ATTR_NAME_OR_VALUE,
                        StatusDetails.NOT_BEFORE_VIOLATED,
                        notBeforeUTC.toString());
			}
		}

		if(conditions.getNotOnOrAfter() != null){
			//normalize to UTC
			logger.debug("Conditions.NotOnOrAfter: " + conditions.getNotOnOrAfter().toString());
			notOnOrAfterUTC = conditions.getNotOnOrAfter().normalize();
			logger.debug("Conditions.NotOnOrAfter normalized: " + notOnOrAfterUTC.toString());
			if(!notOnOrAfterUTC.isValid()){
                throw new SamlR2ResponseException(response,
                        StatusCode.TOP_REQUESTER,
                        StatusCode.INVALID_ATTR_NAME_OR_VALUE,
                        StatusDetails.INVALID_UTC_VALUE, notOnOrAfterUTC.toString());

			} else if(!notOnOrAfterUTC.toGregorianCalendar().getTime().after(utcCalendar.getTime())){
                throw new SamlR2ResponseException(response,
                        StatusCode.TOP_REQUESTER,
                        StatusCode.INVALID_ATTR_NAME_OR_VALUE,
                        StatusDetails.NOT_ONORAFTER_VIOLATED, notOnOrAfterUTC.toString());
			}
		}
		
		if(notBeforeUTC != null && notOnOrAfterUTC != null 
				&& notOnOrAfterUTC.compare(notBeforeUTC) <= 0){
            throw new SamlR2ResponseException(response,
                    StatusCode.TOP_REQUESTER,
                    StatusCode.INVALID_ATTR_NAME_OR_VALUE,
                    StatusDetails.INVALID_CONDITION, "'Not On or After' earlier that 'Not Before'");
		}

        // Our SAMLR2 Enityt ID should be part of the audience
        CircleOfTrustMemberDescriptor sp = this.getCotMemberDescriptor();
        MetadataEntry spMd = sp.getMetadata();

        if (spMd == null || spMd.getEntry() == null)
            throw new SamlR2Exception("No metadata descriptor found for SP " + sp);

        EntityDescriptorType md = null;
        if (spMd.getEntry() instanceof EntityDescriptorType) {
            md = (EntityDescriptorType) spMd.getEntry();
        } else
            throw new SamlR2Exception("Unsupported Metadata type " + md + ", SAML 2 Metadata expected");

		if(conditions.getConditionOrAudienceRestrictionOrOneTimeUse() != null){
			boolean audienceRestrictionValid = false;
			boolean spInAllAudiences = false;
			boolean initState = true;
			for (ConditionAbstractType conditionAbs : conditions.getConditionOrAudienceRestrictionOrOneTimeUse()) {
				if(conditionAbs instanceof AudienceRestrictionType){
					AudienceRestrictionType audienceRestriction = (AudienceRestrictionType) conditionAbs;
					if(audienceRestriction.getAudience() != null){
						boolean spInAudience = false;
						for (String audience : audienceRestriction.getAudience()) {
							if(audience.equals(md.getEntityID())){
								spInAudience = true;
								break;
							}
						}
						spInAllAudiences = (initState ? spInAudience : spInAllAudiences && spInAudience );
						initState = false;
					}
				}
				audienceRestrictionValid = audienceRestrictionValid || spInAllAudiences;
			}
			if(!audienceRestrictionValid){
				logger.error("SP is not in Audience list.");
                throw new SamlR2ResponseException(response,
                        StatusCode.TOP_REQUESTER,
                        StatusCode.INVALID_ATTR_NAME_OR_VALUE,
                        StatusDetails.NOT_IN_AUDIENCE);
			}
		}		
		
	}
    
    private List<AuthnStatementType> getAuthnStatements(AssertionType assertion){
    	ArrayList<AuthnStatementType> statementsList = new ArrayList<AuthnStatementType>();
    	
		if (assertion.getStatementOrAuthnStatementOrAuthzDecisionStatement() != null 
				&& assertion.getStatementOrAuthnStatementOrAuthzDecisionStatement().size() != 0){
			for (Object statement : assertion.getStatementOrAuthnStatementOrAuthzDecisionStatement()) {
				if(statement instanceof AuthnStatementType){
					statementsList.add((AuthnStatementType)statement);
				}
			}
		}    	
    	return statementsList;
    }

    protected CircleOfTrustMemberDescriptor resolveIdp(CamelMediationExchange exchange) throws SamlR2Exception {

        CamelMediationMessage in = (CamelMediationMessage) exchange.getIn();
        ResponseType response = (ResponseType) in.getMessage().getContent();
        String idpAlias = response.getIssuer().getValue();

        if (logger.isDebugEnabled())
            logger.debug("IdP alias received " + idpAlias);

        if (idpAlias == null) {
            throw new SamlR2Exception("No IDP available");
        }
        CircleOfTrustMemberDescriptor idp = this.getCotManager().loolkupMemberByAlias(idpAlias);
        if (idp == null) {
            throw new SamlR2Exception("No IDP Member descriptor available for " + idpAlias);
        }

        return idp;

    }

    protected SPSecurityContext createSPSecurityContext(CamelMediationExchange exchange,
                                                        String requester,
                                                        CircleOfTrustMemberDescriptor idp,
                                                        AccountLink acctLink,
                                                        Subject federatedSubject,
                                                        Subject idpSubject)
            throws SamlR2Exception {

        if (logger.isDebugEnabled())
            logger.debug("Creating new SP Security Context for subject " + federatedSubject);

        IdPChannel idPChannel = (IdPChannel) channel;
        SSOSessionManager ssoSessionManager = idPChannel.getSessionManager();
        CamelMediationMessage in = (CamelMediationMessage) exchange.getIn();

        // Remove previous security context if any

        SPSecurityContext secCtx =
                (SPSecurityContext) in.getMessage().getState().getLocalVariable(getProvider().getName().toUpperCase() + "_SECURITY_CTX");

        if (secCtx != null) {

            if (logger.isDebugEnabled())
                logger.debug("Invalidating old sso session " + secCtx.getSessionIndex());
            try {
                ssoSessionManager.invalidate(secCtx.getSessionIndex());
            } catch (NoSuchSessionException e) {
                // Ignore this ...
                if (logger.isDebugEnabled())
                    logger.debug("Invalidating already expired sso session " + secCtx.getSessionIndex());

            } catch (SSOSessionException e) {
                throw new SamlR2Exception(e);
            }

        }

        // Get Subject ID (username ?)
        SubjectNameID nameId = null;
        Set<SubjectNameID> nameIds = federatedSubject.getPrincipals(SubjectNameID.class);
        if (nameIds != null) {
            for (SubjectNameID i : nameIds) {

                if (logger.isTraceEnabled())
                    logger.trace("Checking Subject ID " + i.getName() + "["+i.getFormat()+"] ");

                // TODO : Support other name ID formats
                if (i.getFormat() == null || i.getFormat().equals(NameIDFormat.UNSPECIFIED.getValue())) {
                    nameId = i;
                    break;
                }
            }
        }

        if (nameId == null) {
            logger.error("No suitable Subject Name Identifier (SubjectNameID) found");
            throw new SamlR2Exception("No suitable Subject Name Identifier (SubjectNameID) found");
        }

        String idpSessionIndex = null;
        Collection<SubjectAuthenticationAttribute> authnAttrs = idpSubject.getPrincipals(SubjectAuthenticationAttribute.class);
        for (SubjectAuthenticationAttribute authnAttr : authnAttrs) {
            if (authnAttr.getName().equals(SubjectAuthenticationAttribute.Name.SESSION_INDEX.name())) {
                idpSessionIndex = authnAttr.getValue();
                break;
            }
        }

        // Create a new Security Context
        secCtx = new SPSecurityContext();
        
        secCtx.setIdpAlias(idp.getAlias());
        secCtx.setIdpSsoSession(idpSessionIndex);
        secCtx.setSubject(federatedSubject);
        secCtx.setAccountLink(acctLink);
        secCtx.setRequester(requester);


        SecurityToken<SPSecurityContext> token = new SecurityTokenImpl<SPSecurityContext>(uuidGenerator.generateId(), secCtx);

        try {
            // Create new SSO Session
            // TODO : Should we listen to DESTROYED event?
            String ssoSessionId = ssoSessionManager.initiateSession(nameId.getName(), token);


            if (logger.isTraceEnabled())
                    logger.trace("Created SP SSO Session with id " + ssoSessionId);

            // Update security context with SSO Session ID
            secCtx.setSessionIndex(ssoSessionId);

            // TODO : Use IDP Session information Subject's attributes and update local session: expiration time, etc.
            Set<SubjectAuthenticationAttribute> attrs = idpSubject.getPrincipals(SubjectAuthenticationAttribute.class);
            String idpSsoSessionId = null;
            for (SubjectAuthenticationAttribute attr : attrs) {
                // Session index
                if (attr.getName().equals(SubjectAuthenticationAttribute.Name.SESSION_INDEX.name())) {
                    idpSsoSessionId = attr.getValue();
                    break;
                }
            }

            // SubjectAuthenticationAttribute.Name.SESSION_NOT_ON_OR_AFTER

            if (logger.isDebugEnabled())
                logger.debug("Created SP security context " + secCtx);

            in.getMessage().getState().setLocalVariable(getProvider().getName().toUpperCase() + "_SECURITY_CTX", secCtx);
            in.getMessage().getState().getLocalState().addAlternativeId("ssoSessionId", secCtx.getSessionIndex());
            in.getMessage().getState().getLocalState().addAlternativeId("idpSsoSessionId", idpSsoSessionId);

            if (logger.isTraceEnabled())
                logger.trace("Stored SP Security Context in " + getProvider().getName().toUpperCase() + "_SECURITY_CTX");
            
            return secCtx;
        } catch (SSOSessionException e) {
            throw new SamlR2Exception(e);
        }

    }


}