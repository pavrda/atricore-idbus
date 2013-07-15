package com.atricore.idbus.console.lifecycle.main.transform.transformers.oauth2;

import com.atricore.idbus.console.lifecycle.main.domain.IdentityAppliance;
import com.atricore.idbus.console.lifecycle.main.domain.metadata.FederatedConnection;
import com.atricore.idbus.console.lifecycle.main.domain.metadata.IdentityProvider;
import com.atricore.idbus.console.lifecycle.main.domain.metadata.InternalSaml2ServiceProvider;
import com.atricore.idbus.console.lifecycle.main.domain.metadata.SelfServicesResource;
import com.atricore.idbus.console.lifecycle.main.exception.TransformException;
import com.atricore.idbus.console.lifecycle.main.transform.TransformEvent;
import com.atricore.idbus.console.lifecycle.main.transform.transformers.AbstractTransformer;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.Bean;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.Beans;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.capabilities.oauth2.main.OAuth2Client;
import org.atricore.idbus.capabilities.oauth2.main.OAuth2IdPMediator;
import org.atricore.idbus.capabilities.oauth2.main.util.JasonUtils;
import org.atricore.idbus.kernel.main.mediation.camel.component.logging.CamelLogMessageBuilder;
import org.atricore.idbus.kernel.main.mediation.camel.component.logging.HttpLogMessageBuilder;
import org.atricore.idbus.kernel.main.mediation.camel.logging.DefaultMediationLogger;
import org.springframework.beans.factory.InitializingBean;

import org.atricore.idbus.capabilities.oauth2.main.binding.OAuth2BindingFactory;
import org.atricore.idbus.capabilities.oauth2.main.binding.logging.OAuth2LogMessageBuilder;
import org.springframework.transaction.TransactionSuspensionNotSupportedException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.atricore.idbus.console.lifecycle.support.springmetadata.util.BeanUtils.*;
import static com.atricore.idbus.console.lifecycle.support.springmetadata.util.BeanUtils.setPropertyBean;
import static com.atricore.idbus.console.lifecycle.support.springmetadata.util.BeanUtils.setPropertyValue;

/**
 * Transformer for OAuth 2.0 IdP local services
 *
 * @author <a href=mailto:sgonzalez@atricore.org>Sebastian Gonzalez Oyuela</a>
 */
public class OAuth2IdPTransformer extends AbstractTransformer implements InitializingBean {

    private static final Log logger = LogFactory.getLog(OAuth2IdPTransformer.class);

    public void afterPropertiesSet() throws Exception {

    }

    @Override
    public boolean accept(TransformEvent event) {
        return event.getData() instanceof IdentityProvider &&
                !((IdentityProvider)event.getData()).isRemote();
    }

    @Override
    public void before(TransformEvent event) throws TransformException {


        IdentityProvider provider = (IdentityProvider) event.getData();

        // Take Idp beans from context, previous transformer created them.
        Beans idpBeans = (Beans) event.getContext().get("idpBeans");
        Beans baseBeans = (Beans) event.getContext().get("beans");
        Beans beansOsgi = (Beans) event.getContext().get("beansOsgi");

        String idauPath = (String) event.getContext().get("idauPath");

        // Publish root element so that other transformers can use it.
        event.getContext().put("idpBeans", idpBeans);

        if (logger.isDebugEnabled())
            logger.debug("Generating IDP " + provider.getName() + " configuration model");

        IdentityAppliance appliance = event.getContext().getProject().getIdAppliance();

        // ----------------------------------------
        // Identity Provider
        // ----------------------------------------

        Bean idpBean = getBean(idpBeans, normalizeBeanName(provider.getName()));

        // ----------------------------------------
        // OAuth 2 Identity Provider Mediator
        // ----------------------------------------
        if (provider.isOauth2Enabled()) {

            Bean idpMediator = newBean(idpBeans, idpBean.getName() + "-oauth2-mediator",
                    OAuth2IdPMediator.class.getName());
            setPropertyValue(idpMediator, "logMessages", true);

            // artifactQueueManager
            // setPropertyRef(idpMediator, "artifactQueueManager", provider.getIdentityAppliance().getName() + "-aqm");
            setPropertyRef(idpMediator, "artifactQueueManager", "artifactQueueManager");

            // bindingFactory
            setPropertyBean(idpMediator, "bindingFactory", newAnonymousBean(OAuth2BindingFactory.class));

            // logger
            List<Bean> idpLogBuilders = new ArrayList<Bean>();
            idpLogBuilders.add(newAnonymousBean(OAuth2LogMessageBuilder.class));
            idpLogBuilders.add(newAnonymousBean(CamelLogMessageBuilder.class));
            idpLogBuilders.add(newAnonymousBean(HttpLogMessageBuilder.class));

            Bean idpLogger = newAnonymousBean(DefaultMediationLogger.class.getName());
            idpLogger.setName(idpBean.getName() + "-mediation-logger");
            setPropertyValue(idpLogger, "category", appliance.getNamespace() + "." + appliance.getName() + ".wire." + idpBean.getName());
            setPropertyAsBeans(idpLogger, "messageBuilders", idpLogBuilders);
            setPropertyBean(idpMediator, "logger", idpLogger);

            // errorUrl
            setPropertyValue(idpMediator, "errorUrl", resolveUiErrorLocation(appliance, provider));

            // warningUrl
            setPropertyValue(idpMediator, "warningUrl", resolveUiWarningLocation(appliance, provider));

            // we need to create OAuth2 Client definitions, for now use Client Config string as JSON serialization

            if (provider.getOauth2ClientsConfig() != null && !"".equals(provider.getOauth2ClientsConfig())) {

                try {
                    // TODO : Use a metadata-specific class ?!

                    // OAUth 2.0 Clients are NOT SPs (resource servers), they're SP consumers (external apps)
                    List<OAuth2Client> clients = JasonUtils.unmarshallClients(provider.getOauth2ClientsConfig());
                    if (clients != null) {
                        for (OAuth2Client oauth2ClientDef : clients) {
                            Bean oauth2ClientBean = newAnonymousBean(OAuth2Client.class);
                            setPropertyValue(oauth2ClientBean, "id", oauth2ClientDef.getId());
                            setPropertyValue(oauth2ClientBean, "secret", oauth2ClientDef.getSecret());

                            addPropertyBean(idpMediator, "clients", oauth2ClientBean);
                        }
                    }

                } catch (IOException e) {
                    throw new TransactionSuspensionNotSupportedException(e.getMessage(), e);
                }
            }

            // Do we have self-services ? Automatically add them as oauth2 clients

            for (FederatedConnection fc : provider.getFederatedConnectionsB()) {
                if (fc.getRoleA() instanceof InternalSaml2ServiceProvider) {
                    InternalSaml2ServiceProvider localSp = (InternalSaml2ServiceProvider) fc.getRoleA();
                    if (localSp.getServiceConnection().getResource() instanceof SelfServicesResource) {

                        SelfServicesResource selfServicesResource = (SelfServicesResource) localSp.getServiceConnection().getResource();
                        Bean oauth2ClientBean = newAnonymousBean(OAuth2Client.class);
                        setPropertyValue(oauth2ClientBean, "id", localSp.getServiceConnection().getResource().getName());
                        setPropertyValue(oauth2ClientBean, "secret", selfServicesResource.getSecret());

                        addPropertyBean(idpMediator, "clients", oauth2ClientBean);

                    }
                }
            }

            for (FederatedConnection fc : provider.getFederatedConnectionsA()) {
                if (fc.getRoleB() instanceof InternalSaml2ServiceProvider) {
                    InternalSaml2ServiceProvider localSp = (InternalSaml2ServiceProvider) fc.getRoleB();
                    if (localSp.getServiceConnection().getResource() instanceof SelfServicesResource) {

                        SelfServicesResource selfServicesResource = (SelfServicesResource) localSp.getServiceConnection().getResource();
                        Bean oauth2ClientBean = newAnonymousBean(OAuth2Client.class);
                        setPropertyValue(oauth2ClientBean, "id", localSp.getServiceConnection().getResource().getName());
                        setPropertyValue(oauth2ClientBean, "secret", selfServicesResource.getSecret());

                        addPropertyBean(idpMediator, "clients", oauth2ClientBean);

                    }
                }
            }


        }


    }

    @Override
    public Object after(TransformEvent event) throws TransformException {
        return null;
    }
}