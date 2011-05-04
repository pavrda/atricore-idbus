package com.atricore.idbus.console.lifecycle.main.transform.transformers;

import com.atricore.idbus.console.lifecycle.main.domain.IdentityAppliance;
import com.atricore.idbus.console.lifecycle.main.domain.metadata.*;
import com.atricore.idbus.console.lifecycle.main.exception.TransformException;
import com.atricore.idbus.console.lifecycle.main.transform.TransformEvent;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.Bean;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.Beans;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.capabilities.samlr2.main.binding.SamlR2BindingFactory;
import org.atricore.idbus.capabilities.samlr2.main.binding.logging.SamlR2LogMessageBuilder;
import org.atricore.idbus.capabilities.samlr2.main.claims.SamlR2ClaimsMediator;
import org.atricore.idbus.capabilities.samlr2.support.auth.AuthnCtxClass;
import org.atricore.idbus.capabilities.samlr2.support.binding.SamlR2Binding;
import org.atricore.idbus.kernel.main.mediation.camel.component.logging.CamelLogMessageBuilder;
import org.atricore.idbus.kernel.main.mediation.camel.component.logging.HttpLogMessageBuilder;
import org.atricore.idbus.kernel.main.mediation.camel.logging.DefaultMediationLogger;
import org.atricore.idbus.kernel.main.mediation.claim.ClaimChannelImpl;
import org.atricore.idbus.kernel.main.mediation.endpoint.IdentityMediationEndpointImpl;
import org.atricore.idbus.kernel.main.mediation.osgi.OsgiIdentityMediationUnit;
import org.atricore.idbus.kernel.main.mediation.provider.IdentityProviderImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.atricore.idbus.console.lifecycle.support.springmetadata.util.BeanUtils.*;

/**
 * @author <a href="mailto:sgonzalez@atricore.org">Sebastian Gonzalez Oyuela</a>
 * @version $Id$
 */
public class ClaimsChannelTransformer extends AbstractTransformer {

    private static final Log logger = LogFactory.getLog(ClaimsChannelTransformer.class);

    @Override
    public boolean accept(TransformEvent event) {
        return event.getData() instanceof IdentityProvider &&
                !((IdentityProvider)event.getData()).isRemote();
    }

    @Override
    public void before(TransformEvent event) throws TransformException {

        Beans idpBeans = (Beans) event.getContext().get("idpBeans");

        IdentityProvider provider = (IdentityProvider) event.getData();
        IdentityAppliance appliance = event.getContext().getProject().getIdAppliance();

        if (logger.isTraceEnabled())
            logger.trace("Generating Claims Channel Beans for IDP Channel " + provider.getName());

        Beans baseBeans = (Beans) event.getContext().get("beans");
        Beans beansOsgi = (Beans) event.getContext().get("beansOsgi");

        Bean idpBean = null;
        Collection<Bean> b = getBeansOfType(idpBeans, IdentityProviderImpl.class.getName());
        if (b.size() != 1) {
            throw new TransformException("Invalid IdP definition count : " + b.size());
        }
        idpBean = b.iterator().next();
        
        // ----------------------------------------
        // Claims Channel
        // ----------------------------------------

        Bean claimsChannelBean = newBean(idpBeans, idpBean.getName() + "-claims-channel", ClaimChannelImpl.class);

        // name
        setPropertyValue(claimsChannelBean, "name", claimsChannelBean.getName());

        // location
        String locationUrl = resolveLocationUrl(provider) + "/CC";
        setPropertyValue(claimsChannelBean, "location", locationUrl);

        // endpoints
        List<Bean> ccEndpoints = new ArrayList<Bean>();

        for (AuthenticationMechanism authnMechanism : provider.getAuthenticationMechanisms()) {
            // Bind authn is a variant of basic authn
            if (authnMechanism instanceof BasicAuthentication ||
                authnMechanism instanceof BindAuthentication) {
                Bean ccPwdArtifact = newAnonymousBean(IdentityMediationEndpointImpl.class);
                ccPwdArtifact.setName(idpBean.getName() + "-cc-pwd-artifact");
                setPropertyValue(ccPwdArtifact, "name", ccPwdArtifact.getName());
                setPropertyValue(ccPwdArtifact, "binding", SamlR2Binding.SSO_ARTIFACT.getValue());
                setPropertyValue(ccPwdArtifact, "location", "/PWD/ARTIFACT");
                setPropertyValue(ccPwdArtifact, "responseLocation", "/PWD/POST-RESP");
                setPropertyValue(ccPwdArtifact, "type", AuthnCtxClass.PASSWORD_AUTHN_CTX.getValue());
                ccEndpoints.add(ccPwdArtifact);

                Bean ccPwdPost = newAnonymousBean(IdentityMediationEndpointImpl.class);
                ccPwdPost.setName(idpBean.getName() + "-cc-pwd-post");
                setPropertyValue(ccPwdPost, "name", ccPwdPost.getName());
                setPropertyValue(ccPwdPost, "binding", SamlR2Binding.SSO_POST.getValue());
                setPropertyValue(ccPwdPost, "location", "/PWD/POST");
                setPropertyValue(ccPwdPost, "type", AuthnCtxClass.PASSWORD_AUTHN_CTX.getValue());
                ccEndpoints.add(ccPwdPost);
            } else if (authnMechanism instanceof TwoFactorAuthentication) {
                Bean cc2faArtifact = newAnonymousBean(IdentityMediationEndpointImpl.class);
                cc2faArtifact.setName(idpBean.getName() + "-cc-2fa-artifact");
                setPropertyValue(cc2faArtifact, "name", cc2faArtifact.getName());
                setPropertyValue(cc2faArtifact, "binding", SamlR2Binding.SSO_ARTIFACT.getValue());
                setPropertyValue(cc2faArtifact, "location", "/2FA/ARTIFACT");
                setPropertyValue(cc2faArtifact, "responseLocation", "/2FA/POST-RESP");
                setPropertyValue(cc2faArtifact, "type", AuthnCtxClass.TIME_SYNC_TOKEN_AUTHN_CTX.getValue());
                ccEndpoints.add(cc2faArtifact);

                Bean cc2faPost = newAnonymousBean(IdentityMediationEndpointImpl.class);
                cc2faPost.setName(idpBean.getName() + "-cc-2fa-post");
                setPropertyValue(cc2faPost, "name", cc2faPost.getName());
                setPropertyValue(cc2faPost, "binding", SamlR2Binding.SSO_POST.getValue());
                setPropertyValue(cc2faPost, "location", "/2FA/POST");
                setPropertyValue(cc2faPost, "type", AuthnCtxClass.TIME_SYNC_TOKEN_AUTHN_CTX.getValue());
                ccEndpoints.add(cc2faPost);
            }
        }

        setPropertyAsBeans(claimsChannelBean, "endpoints", ccEndpoints);

        // ----------------------------------------
        // Claims Mediator
        // ----------------------------------------
        Bean ccMediator = newBean(idpBeans, idpBean.getName() + "-samlr2-claims-mediator", SamlR2ClaimsMediator.class);

        // logMessages
        setPropertyValue(ccMediator, "logMessages", true);

        // basicAuthnUILocation
        setPropertyValue(ccMediator, "basicAuthnUILocation", resolveLocationBaseUrl(provider) + "/idbus-ui/claims/username-password.do");

        // 2faAuthnUILocation
        setPropertyValue(ccMediator, "twoFactorAuthnUILocation", resolveLocationBaseUrl(provider) + "/idbus-ui/claims/username-passcode.do");

        // artifactQueueManager
        setPropertyRef(ccMediator, "artifactQueueManager", provider.getIdentityAppliance().getName() + "-aqm");
        
        // bindingFactory
        setPropertyBean(ccMediator, "bindingFactory", newAnonymousBean(SamlR2BindingFactory.class));

        List<Bean> ccLogBuilders = new ArrayList<Bean>();
        ccLogBuilders.add(newAnonymousBean(SamlR2LogMessageBuilder.class));
        ccLogBuilders.add(newAnonymousBean(CamelLogMessageBuilder.class));
        ccLogBuilders.add(newAnonymousBean(HttpLogMessageBuilder.class));

        Bean ccLogger = newBean(idpBeans, idpBean.getName() + "-cc-mediation-logger", DefaultMediationLogger.class.getName());
        setPropertyValue(ccLogger, "category", appliance.getNamespace() + "." + appliance.getName() + ".wire.cc1");
        setPropertyAsBeans(ccLogger, "messageBuilders", ccLogBuilders);

        // logger
        setPropertyBean(ccMediator, "logger", ccLogger);

        // errorUrl
        setPropertyValue(ccMediator, "errorUrl", resolveLocationBaseUrl(provider) + "/idbus-ui/error.do");

        // identityMediator
        setPropertyRef(claimsChannelBean, "identityMediator", ccMediator.getName());

        // provider
        setPropertyRef(claimsChannelBean, "provider", idpBean.getName());

        // unitContainer
        setPropertyRef(claimsChannelBean, "unitContainer", provider.getIdentityAppliance().getName() + "-container");
        
        // Mediation Unit
        Collection<Bean> mus = getBeansOfType(baseBeans, OsgiIdentityMediationUnit.class.getName());
        if (mus.size() == 1) {
            Bean mu = mus.iterator().next();
            addPropertyBeansAsRefs(mu, "channels", claimsChannelBean);
        } else {
            throw new TransformException("One and only one Identity Mediation Unit is expected, found " + mus.size());
        }
    }
}
