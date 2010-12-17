package com.atricore.idbus.console.lifecycle.main.transform.transformers;

import com.atricore.idbus.console.lifecycle.main.domain.metadata.*;
import com.atricore.idbus.console.lifecycle.main.exception.TransformException;
import com.atricore.idbus.console.lifecycle.main.transform.IdApplianceTransformationContext;
import com.atricore.idbus.console.lifecycle.main.transform.TransformEvent;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.Bean;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.Beans;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.Ref;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.capabilities.samlr2.support.binding.SamlR2Binding;
import org.atricore.idbus.capabilities.samlr2.support.metadata.SAMLR2MetadataConstants;
import org.atricore.idbus.kernel.main.mediation.channel.SPChannelImpl;
import org.atricore.idbus.kernel.main.mediation.endpoint.IdentityMediationEndpointImpl;
import org.atricore.idbus.kernel.main.mediation.osgi.OsgiIdentityMediationUnit;
import org.atricore.idbus.kernel.main.mediation.provider.IdentityProviderImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.atricore.idbus.console.lifecycle.support.springmetadata.util.BeanUtils.*;

/**
 * @author <a href="mailto:sgonzalez@atricore.org">Sebastian Gonzalez Oyuela</a>
 * @version $Id$
 */
public class IdpFederatedConnectionTransformer extends AbstractTransformer {

    private static final Log logger = LogFactory.getLog(IdpFederatedConnectionTransformer.class);

    private boolean roleA;

    public boolean isRoleA() {
        return roleA;
    }

    public void setRoleA(boolean roleA) {
        this.roleA = roleA;
    }

    @Override
    public boolean accept(TransformEvent event) {
        if (event.getData() instanceof ServiceProviderChannel) {

            ServiceProviderChannel spChannel = (ServiceProviderChannel) event.getData();
            FederatedConnection fc = (FederatedConnection) event.getContext().getParentNode();

            if (roleA) {
                return fc.getRoleA() instanceof IdentityProvider
                        && !fc.getRoleA().isRemote();
            } else {
                return fc.getRoleB() instanceof IdentityProvider
                        && !fc.getRoleB().isRemote();
            }

        }

        return false;
    }

    @Override
    public void before(TransformEvent event) throws TransformException {

        FederatedConnection federatedConnection = (FederatedConnection) event.getContext().getParentNode();
        ServiceProviderChannel spChannel = (ServiceProviderChannel) event.getData();

        IdentityProvider idp;

        FederatedProvider target;
        FederatedChannel targetChannel;

        if (roleA) {

            assert spChannel == federatedConnection.getChannelA() :
                    "SP Channel " + spChannel.getName() + " should be 'A' channel in federated connection " +
                            federatedConnection.getName();

            idp = (IdentityProvider) federatedConnection.getRoleA();
            spChannel = (ServiceProviderChannel) federatedConnection.getChannelA();

            target = federatedConnection.getRoleB();
            targetChannel = federatedConnection.getChannelB();

            if (!idp.getName().equals(federatedConnection.getRoleA().getName()))
                throw new IllegalStateException("Context provider " + idp +
                        " is not roleA provider in Federated Connection " + federatedConnection.getName());

        } else {

            assert spChannel == federatedConnection.getChannelB() :
                    "SP Channel " + spChannel.getName() + " should be 'B' channel in federated connection " +
                            federatedConnection.getName();


            idp = (IdentityProvider) federatedConnection.getRoleB();
            spChannel = (ServiceProviderChannel) federatedConnection.getChannelB();

            target = federatedConnection.getRoleA();
            targetChannel = federatedConnection.getChannelA();

            if (!idp.getName().equals(federatedConnection.getRoleB().getName()))
                throw new IllegalStateException("Context provider " + idp +
                        " is not roleB provider in Federated Connection " + federatedConnection.getName());

        }

        generateIdPComponents(idp, spChannel, federatedConnection, target, targetChannel, event.getContext());

    }

    /**
     * Generate IDP Components for a federated connection:
     *
     * @param idp IdP definition
     * @param spChannel SP Channel
     * @param fc Federated connection
     * @param target target provider (SP or IdP)
     * @param targetChannel (Target Channel IdP or SP)
     * @param ctx
     * @throws TransformException
     */
    protected void generateIdPComponents(IdentityProvider idp,
                                     ServiceProviderChannel spChannel,
                                     FederatedConnection fc,
                                     FederatedProvider target,
                                     FederatedChannel targetChannel,
                                     IdApplianceTransformationContext ctx) throws TransformException {

        Beans idpBeans = (Beans) ctx.get("idpBeans");
        if (logger.isTraceEnabled())
            logger.trace("Generating Beans for SP Channel " + spChannel.getName()  + " of IdP " + idp.getName());

        Bean idpBean = null;

        Collection<Bean> b = getBeansOfType(idpBeans, IdentityProviderImpl.class.getName());
        if (b.size() != 1) {
            throw new TransformException("Invalid IdP definition count : " + b.size());
        }
        idpBean = b.iterator().next();

        String spChannelName = idpBean.getName() +  "-" + (!spChannel.isOverrideProviderSetup() ? "default" : normalizeBeanName(target.getName())) + "-sp-channel";

        // Check if we already created this channel
        if (!spChannel.isOverrideProviderSetup() && getPropertyRef(idpBean, "channel") != null) {
            ctx.put("spChannelBean", getBean(idpBeans, spChannelName));
            return;
        }

        Set<Bean> spChannelBeans = getPropertyBeansFromSet(idpBeans, idpBean, "channels");
        if (spChannelBeans != null) {
            for (Bean spChannelBean : spChannelBeans) {
                if (getPropertyValue(spChannelBean, "name").equals(spChannelName)) {
                    // Do not re-process a default channel definition
                    if (logger.isTraceEnabled())
                        logger.trace("Ignoring channel " + spChannel.getName() + ". It was alredy processed");

                    ctx.put("spChannelBean", spChannelBean);
                    return;
                }
            }
        }

        if (logger.isDebugEnabled())
            logger.debug("Creating SP Channel definition for " + spChannelName);

        // -------------------------------------------------------
        // SP Channel
        // -------------------------------------------------------
        Bean spChannelBean = newBean(idpBeans, spChannelName, SPChannelImpl.class.getName());
        ctx.put("spChannelBean", spChannelBean);

        setPropertyValue(spChannelBean, "name", spChannelName);
        setPropertyValue(spChannelBean, "description", spChannel.getDisplayName());
        setPropertyValue(spChannelBean, "location", resolveLocationUrl(idp, spChannel));
        setPropertyRef(spChannelBean, "provider", normalizeBeanName(idp.getName()));
        if (spChannel.isOverrideProviderSetup())
            setPropertyRef(spChannelBean, "targetProvider", normalizeBeanName(target.getName()));
        setPropertyRef(spChannelBean, "sessionManager", idpBean.getName() + "-session-manager");
        setPropertyRef(spChannelBean, "identityManager", idpBean.getName() + "-identity-manager");
        setPropertyRef(spChannelBean, "member", idpBean.getName() + "-md");

        // identityMediator
        Bean identityMediatorBean = getBean(idpBeans, idpBean.getName() + "-samlr2-mediator");
        if (identityMediatorBean == null)
            throw new TransformException("No identity mediator defined for " + idpBean.getName() + "-samlr2-identity-mediator");
        setPropertyRef(spChannelBean, "identityMediator", identityMediatorBean.getName());

        // -------------------------------------------------------
        // endpoints
        // -------------------------------------------------------
        List<Bean> endpoints = new ArrayList<Bean>();

        // profiles
        // spChannel.activeProfiles contains idp active profiles or user overridden profiles
        boolean ssoEnabled = false;
        boolean sloEnabled = false;
        for (Profile profile : spChannel.getActiveProfiles()) {
            if (profile.equals(Profile.SSO)) {
                ssoEnabled = true;
            } else if (profile.equals(Profile.SSO_SLO)) {
                sloEnabled = true;
            }
        }

        // bindings
        // spChannel.activeBindings contains idp active bindings or user overridden bindings
        boolean postEnabled = false;
        boolean redirectEnabled = false;
        boolean artifactEnabled = false;
        boolean soapEnabled = false;
        for (Binding binding : spChannel.getActiveBindings()) {
            if (binding.equals(Binding.SAMLR2_HTTP_POST)) {
                postEnabled = true;
            } else if (binding.equals(Binding.SAMLR2_HTTP_REDIRECT)) {
                redirectEnabled = true;
            } else if (binding.equals(Binding.SAMLR2_ARTIFACT)) {
                artifactEnabled = true;
            } else if (binding.equals(Binding.SAMLR2_SOAP)) {
                soapEnabled = true;
            }
        }

        // SingleLogoutService

        if (sloEnabled) {
            // SAML2 SLO HTTP POST
            if (postEnabled) {
                Bean sloHttpPost = newAnonymousBean(IdentityMediationEndpointImpl.class);
                sloHttpPost.setName(spChannelBean.getName() + "-saml2-slo-http-post");
                setPropertyValue(sloHttpPost, "name", sloHttpPost.getName());
                setPropertyValue(sloHttpPost, "type", SAMLR2MetadataConstants.SingleLogoutService_QNAME.toString());
                setPropertyValue(sloHttpPost, "binding", SamlR2Binding.SAMLR2_POST.getValue());
                List<Ref> plansList = new ArrayList<Ref>();
                Ref plan = new Ref();
                plan.setBean(idpBean.getName() + "-samlr2sloreq-to-samlr2resp-plan");
                plansList.add(plan);
                Ref plan2 = new Ref();
                plan2.setBean(idpBean.getName() + "-samlr2sloreq-to-samlr2spsloreq-plan");
                plansList.add(plan2);
                setPropertyRefs(sloHttpPost, "identityPlans", plansList);
                endpoints.add(sloHttpPost);
            }

            // SAML2 SLO HTTP ARTIFACT
            if (artifactEnabled) {
                Bean sloHttpArtifact = newAnonymousBean(IdentityMediationEndpointImpl.class);
                sloHttpArtifact.setName(spChannelBean.getName() + "-saml2-slo-http-artifact");
                setPropertyValue(sloHttpArtifact, "name", sloHttpArtifact.getName());
                setPropertyValue(sloHttpArtifact, "type", SAMLR2MetadataConstants.SingleLogoutService_QNAME.toString());
                setPropertyValue(sloHttpArtifact, "binding", SamlR2Binding.SAMLR2_ARTIFACT.getValue());
                List<Ref> plansList = new ArrayList<Ref>();
                Ref plan = new Ref();
                plan.setBean(idpBean.getName() + "-samlr2sloreq-to-samlr2resp-plan");
                plansList.add(plan);
                Ref plan2 = new Ref();
                plan2.setBean(idpBean.getName() + "-samlr2sloreq-to-samlr2spsloreq-plan");
                plansList.add(plan2);
                setPropertyRefs(sloHttpArtifact, "identityPlans", plansList);
                endpoints.add(sloHttpArtifact);
            }

            // SAML2 SLO HTTP REDIRECT
            if (redirectEnabled) {
                Bean sloHttpRedirect = newAnonymousBean(IdentityMediationEndpointImpl.class);
                sloHttpRedirect.setName(spChannelBean.getName() + "-saml2-slo-http-redirect");
                setPropertyValue(sloHttpRedirect, "name", sloHttpRedirect.getName());
                setPropertyValue(sloHttpRedirect, "type", SAMLR2MetadataConstants.SingleLogoutService_QNAME.toString());
                setPropertyValue(sloHttpRedirect, "binding", SamlR2Binding.SAMLR2_REDIRECT.getValue());
                List<Ref> plansList = new ArrayList<Ref>();
                Ref plan = new Ref();
                plan.setBean(idpBean.getName() + "-samlr2sloreq-to-samlr2resp-plan");
                plansList.add(plan);
                Ref plan2 = new Ref();
                plan2.setBean(idpBean.getName() + "-samlr2sloreq-to-samlr2spsloreq-plan");
                plansList.add(plan2);
                setPropertyRefs(sloHttpRedirect, "identityPlans", plansList);
                endpoints.add(sloHttpRedirect);
            }

            // SAML2 SLO SOAP
            if (soapEnabled) {
                Bean sloSoap = newAnonymousBean(IdentityMediationEndpointImpl.class);
                sloSoap.setName(spChannelBean.getName() + "-saml2-slo-soap");
                setPropertyValue(sloSoap, "name", sloSoap.getName());
                setPropertyValue(sloSoap, "type", SAMLR2MetadataConstants.SingleLogoutService_QNAME.toString());
                setPropertyValue(sloSoap, "binding", SamlR2Binding.SAMLR2_SOAP.getValue());
                List<Ref> plansList = new ArrayList<Ref>();
                Ref plan = new Ref();
                plan.setBean(idpBean.getName() + "-samlr2sloreq-to-samlr2resp-plan");
                plansList.add(plan);
                Ref plan2 = new Ref();
                plan2.setBean(idpBean.getName() + "-samlr2sloreq-to-samlr2spsloreq-plan");
                plansList.add(plan2);
                setPropertyRefs(sloSoap, "identityPlans", plansList);
                endpoints.add(sloSoap);
            }

            // SAML2 SLO LOCAL
            Bean sloLocal = newAnonymousBean(IdentityMediationEndpointImpl.class);
            sloLocal.setName(spChannelBean.getName() + "-saml2-slo-local");
            setPropertyValue(sloLocal, "name", sloLocal.getName());
            setPropertyValue(sloLocal, "type", SAMLR2MetadataConstants.SingleLogoutService_QNAME.toString());
            setPropertyValue(sloLocal, "binding", SamlR2Binding.SAMLR2_LOCAL.getValue());
            List<Ref> plansList = new ArrayList<Ref>();
            Ref plan = new Ref();
            plan.setBean(idpBean.getName() + "-samlr2sloreq-to-samlr2resp-plan");
            plansList.add(plan);
            Ref plan2 = new Ref();
            plan2.setBean(idpBean.getName() + "-samlr2sloreq-to-samlr2spsloreq-plan");
            plansList.add(plan2);
            setPropertyRefs(sloLocal, "identityPlans", plansList);
            endpoints.add(sloLocal);
        }

        // SingleSignOnService

        if (ssoEnabled) {
            // SAML2 SSO HTTP POST
            if (postEnabled) {
                Bean ssoHttpPost = newAnonymousBean(IdentityMediationEndpointImpl.class);
                ssoHttpPost.setName(spChannelBean.getName() + "-saml2-sso-http-post");
                setPropertyValue(ssoHttpPost, "name", ssoHttpPost.getName());
                setPropertyValue(ssoHttpPost, "type", SAMLR2MetadataConstants.SingleSignOnService_QNAME.toString());
                setPropertyValue(ssoHttpPost, "binding", SamlR2Binding.SAMLR2_POST.getValue());
                List<Ref> plansList = new ArrayList<Ref>();
                Ref plan = new Ref();
                plan.setBean(idpBean.getName() + "-samlr2authnreq-to-samlr2resp-plan");
                plansList.add(plan);
                Ref plan2 = new Ref();
                plan2.setBean(idpBean.getName() + "-samlr2authnstmt-to-samlr2assertion-plan");
                plansList.add(plan2);
                setPropertyRefs(ssoHttpPost, "identityPlans", plansList);
                endpoints.add(ssoHttpPost);
            }

            // SAML2 SSO HTTP ARTIFACT
            if (artifactEnabled) {
                Bean ssoHttpArtifact = newAnonymousBean(IdentityMediationEndpointImpl.class);
                ssoHttpArtifact.setName(spChannelBean.getName() + "-saml2-sso-http-artifact");
                setPropertyValue(ssoHttpArtifact, "name", ssoHttpArtifact.getName());
                setPropertyValue(ssoHttpArtifact, "type", SAMLR2MetadataConstants.SingleSignOnService_QNAME.toString());
                setPropertyValue(ssoHttpArtifact, "binding", SamlR2Binding.SAMLR2_ARTIFACT.getValue());
                List<Ref> plansList = new ArrayList<Ref>();
                Ref plan = new Ref();
                plan.setBean(idpBean.getName() + "-samlr2authnreq-to-samlr2resp-plan");
                plansList.add(plan);
                Ref plan2 = new Ref();
                plan2.setBean(idpBean.getName() + "-samlr2authnstmt-to-samlr2assertion-plan");
                plansList.add(plan2);
                setPropertyRefs(ssoHttpArtifact, "identityPlans", plansList);
                endpoints.add(ssoHttpArtifact);
            }

            // SAML2 SSO HTTP REDIRECT
            if (redirectEnabled) {
                Bean ssoHttpRedirect = newAnonymousBean(IdentityMediationEndpointImpl.class);
                ssoHttpRedirect.setName(spChannelBean.getName() + "-saml2-sso-http-redirect");
                setPropertyValue(ssoHttpRedirect, "name", ssoHttpRedirect.getName());
                setPropertyValue(ssoHttpRedirect, "type", SAMLR2MetadataConstants.SingleSignOnService_QNAME.toString());
                setPropertyValue(ssoHttpRedirect, "binding", SamlR2Binding.SAMLR2_REDIRECT.getValue());
                List<Ref> plansList = new ArrayList<Ref>();
                Ref plan = new Ref();
                plan.setBean(idpBean.getName() + "-samlr2authnreq-to-samlr2resp-plan");
                plansList.add(plan);
                Ref plan2 = new Ref();
                plan2.setBean(idpBean.getName() + "-samlr2authnstmt-to-samlr2assertion-plan");
                plansList.add(plan2);
                setPropertyRefs(ssoHttpRedirect, "identityPlans", plansList);
                endpoints.add(ssoHttpRedirect);
            }
        }

        // ArtifactResolutionService
        if (artifactEnabled) {
            Bean arSoap = newAnonymousBean(IdentityMediationEndpointImpl.class);
            arSoap.setName(spChannelBean.getName() + "-saml2-ar-soap");
            setPropertyValue(arSoap, "name", arSoap.getName());
            setPropertyValue(arSoap, "type", SAMLR2MetadataConstants.ArtifactResolutionService_QNAME.toString());
            setPropertyValue(arSoap, "binding", SamlR2Binding.SAMLR2_SOAP.getValue());
            List<Ref> plansList = new ArrayList<Ref>();
            Ref plan = new Ref();
            plan.setBean(idpBean.getName() + "-samlr2artresolve-to-samlr2artresponse-plan");
            plansList.add(plan);
            Ref plan2 = new Ref();
            plan2.setBean(idpBean.getName() + "-samlr2art-to-samlr2artresolve-plan");
            plansList.add(plan2);
            setPropertyRefs(arSoap, "identityPlans", plansList);
            endpoints.add(arSoap);

            Bean arLocal = newAnonymousBean(IdentityMediationEndpointImpl.class);
            arLocal.setName(spChannelBean.getName() + "-saml2-ar-local");
            setPropertyValue(arLocal, "name", arLocal.getName());
            setPropertyValue(arLocal, "type", SAMLR2MetadataConstants.ArtifactResolutionService_QNAME.toString());
            setPropertyValue(arLocal, "binding", SamlR2Binding.SAMLR2_LOCAL.getValue());
            plansList = new ArrayList<Ref>();
            plan = new Ref();
            plan.setBean(idpBean.getName() + "-samlr2artresolve-to-samlr2artresponse-plan");
            plansList.add(plan);
            plan2 = new Ref();
            plan2.setBean(idpBean.getName() + "-samlr2art-to-samlr2artresolve-plan");
            plansList.add(plan2);
            setPropertyRefs(arLocal, "identityPlans", plansList);
            endpoints.add(arLocal);

            Bean arSoap11 = newAnonymousBean(IdentityMediationEndpointImpl.class);
            arSoap11.setName(spChannelBean.getName() + "-saml11-ar-soap");
            setPropertyValue(arSoap11, "name", arSoap11.getName());
            setPropertyValue(arSoap11, "type", SAMLR2MetadataConstants.ArtifactResolutionService_QNAME.toString());
            setPropertyValue(arSoap11, "binding", SamlR2Binding.SAMLR11_SOAP.getValue());
            plansList = new ArrayList<Ref>();
            plan = new Ref();
            plan.setBean(idpBean.getName() + "-samlr2artresolve-to-samlr2artresponse-plan");
            plansList.add(plan);
            plan2 = new Ref();
            plan2.setBean(idpBean.getName() + "-samlr2art-to-samlr2artresolve-plan");
            plansList.add(plan2);
            setPropertyRefs(arSoap11, "identityPlans", plansList);
            endpoints.add(arSoap11);
        }

        // IDP Initiated SSO
        if (ssoEnabled) {
            Bean idpSsoInit = newAnonymousBean(IdentityMediationEndpointImpl.class);
            idpSsoInit.setName(spChannelBean.getName() + "-idp-initiated-saml2");
            setPropertyValue(idpSsoInit, "name", idpSsoInit.getName());
            setPropertyValue(idpSsoInit, "type", SAMLR2MetadataConstants.SingleSignOnService_QNAME.toString());
            setPropertyValue(idpSsoInit, "binding", SamlR2Binding.SSO_IDP_INITIATED_SSO_HTTP_SAML11.getValue());
            setPropertyValue(idpSsoInit, "location", "/SAML11/SSO/IDP_INITIATE");
            List<Ref> plansList = new ArrayList<Ref>();
            Ref plan = new Ref();
            plan.setBean(idpBean.getName() + "-samlr2idpinitiatedauthnreq-to-samlr2authnreq-plan");
            plansList.add(plan);
            setPropertyRefs(idpSsoInit, "identityPlans", plansList);
            endpoints.add(idpSsoInit);

            Bean idpSsoInit11 = newAnonymousBean(IdentityMediationEndpointImpl.class);
            idpSsoInit11.setName(spChannelBean.getName() + "-idp-initiated-saml11");
            setPropertyValue(idpSsoInit11, "name", idpSsoInit11.getName());
            setPropertyValue(idpSsoInit11, "type", SAMLR2MetadataConstants.SingleSignOnService_QNAME.toString());
            setPropertyValue(idpSsoInit11, "binding", SamlR2Binding.SSO_IDP_INITIATED_SSO_HTTP_SAML2.getValue());
            setPropertyValue(idpSsoInit11, "location", "/SAML2/SSO/IDP_INITIATE");
            plansList = new ArrayList<Ref>();
            plan = new Ref();
            plan.setBean(idpBean.getName() + "-samlr2idpinitiatedauthnreq-to-samlr2authnreq-plan");
            plansList.add(plan);
            setPropertyRefs(idpSsoInit11, "identityPlans", plansList);
            endpoints.add(idpSsoInit11);
        }

        // SessionHeartBeatService (non-saml)

        // SSO SHB SOAP
        Bean shbSOAP = newAnonymousBean(IdentityMediationEndpointImpl.class);
        shbSOAP.setName(spChannelBean.getName() + "-sso-shb-soap");
        setPropertyValue(shbSOAP, "name", shbSOAP.getName());
        setPropertyValue(shbSOAP, "type", SAMLR2MetadataConstants.IDPSessionHeartBeatService_QNAME.toString());
        setPropertyValue(shbSOAP, "binding", SamlR2Binding.SSO_SOAP.getValue());
        setPropertyValue(shbSOAP, "location", "/SSO/SSHB/SOAP");
        endpoints.add(shbSOAP);

        // SSO SHB LOCAL
        Bean shbLocal = newAnonymousBean(IdentityMediationEndpointImpl.class);
        shbLocal.setName(spChannelBean.getName() + "-sso-shb-local");
        setPropertyValue(shbLocal, "name", shbLocal.getName());
        setPropertyValue(shbLocal, "type", SAMLR2MetadataConstants.IDPSessionHeartBeatService_QNAME.toString());
        setPropertyValue(shbLocal, "binding", SamlR2Binding.SSO_LOCAL.getValue());
        setPropertyValue(shbLocal, "location", "local://" + idp.getLocation().getUri().toUpperCase() + "/SSO/SSHB/LOCAL");
        endpoints.add(shbLocal);

        // SSO SSO HTTP ARTIFACT
        if (ssoEnabled) {
            Bean ssoSsoHttpArtifact = newAnonymousBean(IdentityMediationEndpointImpl.class);
            ssoSsoHttpArtifact.setName(spChannelBean.getName() + "-sso-sso-http-artifact");
            setPropertyValue(ssoSsoHttpArtifact, "name", ssoSsoHttpArtifact.getName());
            setPropertyValue(ssoSsoHttpArtifact, "type", SAMLR2MetadataConstants.SingleSignOnService_QNAME.toString());
            setPropertyValue(ssoSsoHttpArtifact, "binding", SamlR2Binding.SSO_ARTIFACT.getValue());
            setPropertyValue(ssoSsoHttpArtifact, "location", "/SSO/SSO/ARTIFACT");
            List<Ref> plansList = new ArrayList<Ref>();
            Ref plan = new Ref();
            plan.setBean(idpBean.getName() + "-samlr2authnreq-to-samlr2resp-plan");
            plansList.add(plan);
            Ref plan2 = new Ref();
            plan2.setBean(idpBean.getName() + "-samlr2authnstmt-to-samlr2assertion-plan");
            plansList.add(plan2);
            setPropertyRefs(ssoSsoHttpArtifact, "identityPlans", plansList);
            endpoints.add(ssoSsoHttpArtifact);
        }

        // IDP Initiated SLO
        if (sloEnabled) {
            // SSO SLO SOAP
            Bean ssoSloSoap = newAnonymousBean(IdentityMediationEndpointImpl.class);
            ssoSloSoap.setName(spChannelBean.getName() + "-sso-slo-soap");
            setPropertyValue(ssoSloSoap, "name", ssoSloSoap.getName());
            setPropertyValue(ssoSloSoap, "type", SAMLR2MetadataConstants.IDPInitiatedSingleLogoutService_QNAME.toString());
            setPropertyValue(ssoSloSoap, "binding", SamlR2Binding.SSO_SOAP.getValue());
            setPropertyValue(ssoSloSoap, "location", "/SSO/SLO/SOAP");
            List<Ref> plansList = new ArrayList<Ref>();
            Ref plan = new Ref();
            plan.setBean(idpBean.getName() + "-samlr2sloreq-to-samlr2spsloreq-plan");
            plansList.add(plan);
            setPropertyRefs(ssoSloSoap, "identityPlans", plansList);
            endpoints.add(ssoSloSoap);

            // SSO SLO LOCAL
            Bean ssoSloLocal = newAnonymousBean(IdentityMediationEndpointImpl.class);
            ssoSloLocal.setName(spChannelBean.getName() + "-sso-slo-local");
            setPropertyValue(ssoSloLocal, "name", ssoSloLocal.getName());
            setPropertyValue(ssoSloLocal, "type", SAMLR2MetadataConstants.IDPInitiatedSingleLogoutService_QNAME.toString());
            setPropertyValue(ssoSloLocal, "binding", SamlR2Binding.SSO_LOCAL.getValue());
            setPropertyValue(ssoSloLocal, "location", "local://" + idp.getLocation().getUri().toUpperCase() + "/" + idpBean.getName().toUpperCase() + "/SSO/SLO/LOCAL");
            plansList = new ArrayList<Ref>();
            plan = new Ref();
            plan.setBean(idpBean.getName() + "-samlr2sloreq-to-samlr2spsloreq-plan");
            plansList.add(plan);
            setPropertyRefs(ssoSloLocal, "identityPlans", plansList);
            endpoints.add(ssoSloLocal);
        }
        
        setPropertyAsBeans(spChannelBean, "endpoints", endpoints);
        
        //Bean authnToSamlResponsePlan = newBean(idpBeans, "samlr2authnreq-to-samlr2response-plan", SamlR2AuthnReqToSamlR2RespPlan.class);
        //setPropertyRef(authnToSamlResponsePlan, "bpmsManager", "bpms-manager");

        if (spChannel.isOverrideProviderSetup())
            addPropertyBeansAsRefsToSet(idpBean, "channels", spChannelBean);
        else
            setPropertyRef(idpBean, "channel", spChannelBean.getName());
    }

    @Override
    public Object after(TransformEvent event) throws TransformException {

        // SP Channel bean
        Bean spChannelBean = (Bean) event.getContext().get("spChannelBean");
        Bean idpBean = (Bean) event.getContext().get("idpBean");
        Beans idpBeans = (Beans) event.getContext().get("idpBeans");
        Beans beans = (Beans) event.getContext().get("beans");

        // TODO : For now, the same Claims provider and STS are used for ALL channels!
        
        // claimsProvider
        String claimsChannelName = idpBean.getName() + "-claims-channel";

        Bean claimsChannel = getBean(idpBeans, claimsChannelName);
        if (claimsChannel == null)
            throw new TransformException("No claims channel defined as " + claimsChannelName);
        setPropertyRef(spChannelBean, "claimsProvider", claimsChannel.getName());

        // STS
        Bean sts = getBean(idpBeans, idpBean.getName() + "-sts");
        if (sts == null)
            throw new TransformException("No STS defined as " + spChannelBean.getName() + "-sts");
        setPropertyRef(spChannelBean, "securityTokenService", sts.getName());

        // Mediation Unit
        Collection<Bean> mus = getBeansOfType(beans, OsgiIdentityMediationUnit.class.getName());
        if (mus.size() == 1) {
            Bean mu = mus.iterator().next();

            List<Bean> channels = getPropertyBeans(idpBeans, mu, "channels");
            boolean found = false;

            if (channels != null)
                for (Bean bean : channels) {
                    if (getPropertyValue(bean, "name").equals(getPropertyValue(spChannelBean, "name"))) {
                        found = true;
                        break;
                    }
                }

            if (!found)
                addPropertyBeansAsRefs(mu, "channels", spChannelBean);

        } else {
            throw new TransformException("One and only one Identity Mediation Unit is expected, found " + mus.size());
        }

        return spChannelBean;
    }
}
