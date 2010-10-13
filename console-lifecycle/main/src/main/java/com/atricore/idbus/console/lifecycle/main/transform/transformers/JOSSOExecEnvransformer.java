package com.atricore.idbus.console.lifecycle.main.transform.transformers;

import com.atricore.idbus.console.lifecycle.main.domain.IdentityAppliance;
import com.atricore.idbus.console.lifecycle.main.domain.metadata.*;
import com.atricore.idbus.console.lifecycle.main.exception.TransformException;
import com.atricore.idbus.console.lifecycle.main.transform.IdProjectModule;
import com.atricore.idbus.console.lifecycle.main.transform.IdProjectResource;
import com.atricore.idbus.console.lifecycle.main.transform.TransformEvent;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.Bean;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.Beans;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.Description;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.Entry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.capabilities.josso.main.JossoMediator;
import org.atricore.idbus.capabilities.josso.main.JossoService;
import org.atricore.idbus.capabilities.josso.main.binding.JossoBinding;
import org.atricore.idbus.capabilities.josso.main.binding.JossoBindingFactory;
import org.atricore.idbus.capabilities.josso.main.binding.logging.JossoLogMessageBuilder;
import org.atricore.idbus.capabilities.samlr2.main.SamlR2CircleOfTrustManager;
import org.atricore.idbus.capabilities.samlr2.support.binding.SamlR2Binding;
import org.atricore.idbus.capabilities.samlr2.support.metadata.SAMLR2MetadataConstants;
import org.atricore.idbus.kernel.main.federation.metadata.CircleOfTrustImpl;
import org.atricore.idbus.kernel.main.mediation.binding.BindingChannelImpl;
import org.atricore.idbus.kernel.main.mediation.camel.component.logging.CamelLogMessageBuilder;
import org.atricore.idbus.kernel.main.mediation.camel.component.logging.HttpLogMessageBuilder;
import org.atricore.idbus.kernel.main.mediation.camel.logging.DefaultMediationLogger;
import org.atricore.idbus.kernel.main.mediation.endpoint.IdentityMediationEndpointImpl;
import org.atricore.idbus.kernel.main.mediation.osgi.OsgiIdentityMediationUnit;
import org.atricore.idbus.kernel.main.mediation.provider.BindingProviderImpl;

import java.util.*;

import static com.atricore.idbus.console.lifecycle.support.springmetadata.util.BeanUtils.*;

/**
 * @author <a href="mailto:sgonzalez@atricore.org">Sebastian Gonzalez Oyuela</a>
 * @version $Id$
 */
public class JOSSOExecEnvransformer extends AbstractTransformer {

    private static final Log logger = LogFactory.getLog(IdPLocalTransformer.class);

    private Map<String, ExecutionEnvironmentProperties> execEnvProperties =
            new HashMap<String, ExecutionEnvironmentProperties>();

    @Override
    public boolean accept(TransformEvent event) {
        return event.getData() instanceof ExecutionEnvironment;
    }

    @Override
    public void before(TransformEvent event) throws TransformException {

        ExecutionEnvironment execEnv = (ExecutionEnvironment) event.getData();
        IdentityApplianceDefinition applianceDef = (IdentityApplianceDefinition) event.getContext().getParentNode();
        IdentityAppliance appliance = event.getContext().getProject().getIdAppliance();

        Date now = new Date();

        Beans bpBeans = new Beans();

        Description descr = new Description();
        descr.getContent().add(execEnv.getName() + " : BP Configuration generated by Atricore Identity Bus Server on " + now.toGMTString());
        descr.getContent().add(execEnv.getDescription());

        bpBeans.setDescription(descr);

        Beans baseBeans = (Beans) event.getContext().get("beans");

        // Publish root element so that other transformers can use it.
        event.getContext().put("bpBeans", bpBeans);

        if (logger.isDebugEnabled())
            logger.debug("Generating BP " + execEnv.getName() + " configuration model");

        Bean bpBean = newBean(bpBeans, normalizeBeanName(execEnv.getName()),
                BindingProviderImpl.class.getName());

        // Name
        setPropertyValue(bpBean, "name", bpBean.getName());

        // Role (Only JOSSO role supported)
        setPropertyValue(bpBean, "role", "{urn:org:atricore:idbus:josso:metadata}JOSSOAgentDescriptor");

        // unitContainer
        setPropertyRef(bpBean, "unitContainer", applianceDef.getName() + "-container");

        // COT Manager
        Collection<Bean> cotMgrs = getBeansOfType(baseBeans, SamlR2CircleOfTrustManager.class.getName());
        if (cotMgrs.size() == 1) {
            Bean cotMgr = cotMgrs.iterator().next();
            setPropertyRef(bpBean, "cotManager", cotMgr.getName());
        }

        // State Manager
        setPropertyRef(bpBean, "stateManager", applianceDef.getName() + "-state-manager");

        // MBean
        Bean mBean = newBean(bpBeans, bpBean.getName() + "-mbean",
                "org.atricore.idbus.capabilities.samlr2.management.internal.BindingProviderMBeanImpl");
        setPropertyRef(mBean, "bindingProvider", bpBean.getName());

        // MBean Exporter
        Bean mBeanExporter = newBean(bpBeans, bpBean.getName() + "-mbean-exporter", "org.springframework.jmx.export.MBeanExporter");
        setPropertyRef(mBeanExporter, "server", "mBeanServer");

        Bean mBeanEntryKeyBean = newBean(bpBeans, mBean.getName() + "-key", String.class);
        setConstructorArg(mBeanEntryKeyBean, 0, "java.lang.String", appliance.getNamespace() + "." +
                event.getContext().getCurrentModule().getId() + ":type=BindingProvider,name=" + applianceDef.getName() + "." + bpBean.getName());

        Entry mBeanEntry = new Entry();
        mBeanEntry.setKeyRef(mBeanEntryKeyBean.getName());
        mBeanEntry.setValueRef(mBean.getName());

        addEntryToMap(mBeanExporter, "beans", mBeanEntry);

        // mbean assembler
        /*Bean mBeanAssembler = newAnonymousBean("org.springframework.jmx.export.assembler.MethodNameBasedMBeanInfoAssembler");

        List<Prop> props = new ArrayList<Prop>();

        Prop prop = new Prop();
        prop.setKey("org.atricore.idbus." + event.getContext().getCurrentModule().getId() +
                ":type=BindingProvider,name=" + bpBean.getName());
        prop.getContent().add("listStatesAsTable,listStateEntriesAsTable");
        props.add(prop);

        setPropertyValue(mBeanAssembler, "methodMappings", props);

        setPropertyBean(mBeanExporter, "assembler", mBeanAssembler);*/

        // Binding Channel
        Bean bc = newBean(bpBeans, normalizeBeanName(execEnv.getName()) + "-josso-binding-channel",
                "org.atricore.idbus.kernel.main.mediation.binding.BindingChannelImpl");
        setPropertyValue(bc, "name", bc.getName());
        setPropertyValue(bc, "description", execEnv.getDisplayName());
        setPropertyRef(bc, "provider", bpBean.getName());

        String locationPath = resolveLocationPath(applianceDef.getLocation()) + "/" + execEnv.getName().toUpperCase();
        String location = resolveLocationBaseUrl(applianceDef.getLocation()) + locationPath;

        setPropertyValue(bc, "location", location);

        setPropertyRef(bc, "unitContainer", applianceDef.getName() + "-container");
        setPropertyRef(bc, "identityMediator", bpBean.getName() + "-binding-mediator");

        // endpoints
        List<Bean> endpoints = new ArrayList<Bean>();

        Bean sloArtifact = newAnonymousBean(IdentityMediationEndpointImpl.class);
        sloArtifact.setName(bpBean.getName() + "-binding-ssop-slo-artifact");
        setPropertyValue(sloArtifact, "name", sloArtifact.getName());
        setPropertyValue(sloArtifact, "type", "{urn:org:atricore:idbus:sso:metadata}SingleLogoutService");
        setPropertyValue(sloArtifact, "binding", SamlR2Binding.SSO_ARTIFACT.getValue());
        setPropertyValue(sloArtifact, "location", "/SSO/SLO/ARTIFACT");
        endpoints.add(sloArtifact);

        Bean acsArtifact = newAnonymousBean(IdentityMediationEndpointImpl.class);
        acsArtifact.setName(bpBean.getName() + "-binding-ssop-acs-artifact");
        setPropertyValue(acsArtifact, "name", acsArtifact.getName());
        setPropertyValue(acsArtifact, "type", SAMLR2MetadataConstants.SPBindingAssertionConsumerService_QNAME.toString());
        setPropertyValue(acsArtifact, "binding", SamlR2Binding.SSO_ARTIFACT.getValue());
        setPropertyValue(acsArtifact, "location", "/SSO/ACS/ARTIFACT");
        endpoints.add(acsArtifact);

        Bean ssoRedirect = newAnonymousBean(IdentityMediationEndpointImpl.class);
        ssoRedirect.setName(bpBean.getName() + "-binding-josso-sso-redir");
        setPropertyValue(ssoRedirect, "name", ssoRedirect.getName());
        setPropertyValue(ssoRedirect, "type", JossoService.SingleSignOnService.toString());
        setPropertyValue(ssoRedirect, "binding", SamlR2Binding.SS0_REDIRECT.getValue());
        setPropertyValue(ssoRedirect, "location", "/JOSSO/SSO/REDIR");
        endpoints.add(ssoRedirect);

        Bean sloRedirect = newAnonymousBean(IdentityMediationEndpointImpl.class);
        sloRedirect.setName(bpBean.getName() + "-binding-josso-slo-redir");
        setPropertyValue(sloRedirect, "name", sloRedirect.getName());
        setPropertyValue(sloRedirect, "type", JossoService.SingleLogoutService.toString());
        setPropertyValue(sloRedirect, "binding", SamlR2Binding.SS0_REDIRECT.getValue());
        setPropertyValue(sloRedirect, "location", "/JOSSO/SLO/REDIR");
        endpoints.add(sloRedirect);

        Bean ssoIdmSoap = newAnonymousBean(IdentityMediationEndpointImpl.class);
        ssoIdmSoap.setName(bpBean.getName() + "-binding-josso-ssoidm-soap");
        setPropertyValue(ssoIdmSoap, "name", ssoIdmSoap.getName());
        setPropertyValue(ssoIdmSoap, "type", JossoService.IdentityManager.toString());
        setPropertyValue(ssoIdmSoap, "binding", JossoBinding.JOSSO_SOAP.getValue());
        setPropertyValue(ssoIdmSoap, "location", "/JOSSO/SSOIdentityManager/SOAP");
        endpoints.add(ssoIdmSoap);

        Bean ssoIdpSoap = newAnonymousBean(IdentityMediationEndpointImpl.class);
        ssoIdpSoap.setName(bpBean.getName() + "-binding-josso-ssoidp-soap");
        setPropertyValue(ssoIdpSoap, "name", ssoIdpSoap.getName());
        setPropertyValue(ssoIdpSoap, "type", JossoService.IdentityProvider.toString());
        setPropertyValue(ssoIdpSoap, "binding", JossoBinding.JOSSO_SOAP.getValue());
        setPropertyValue(ssoIdpSoap, "location", "/JOSSO/SSOIdentityProvider/SOAP");
        endpoints.add(ssoIdpSoap);

        Bean ssoSmSoap = newAnonymousBean(IdentityMediationEndpointImpl.class);
        ssoSmSoap.setName(bpBean.getName() + "-binding-josso-ssosm-soap");
        setPropertyValue(ssoSmSoap, "name", ssoSmSoap.getName());
        setPropertyValue(ssoSmSoap, "type", JossoService.SessionManager.toString());
        setPropertyValue(ssoSmSoap, "binding", JossoBinding.JOSSO_SOAP.getValue());
        setPropertyValue(ssoSmSoap, "location", "/JOSSO/SSOSessionManager/SOAP");
        endpoints.add(ssoSmSoap);

        setPropertyAsBeans(bc, "endpoints", endpoints);

        setPropertyRef(bpBean, "bindingChannel", bc.getName());


        // binding-mediator
        Bean bindingMediator = newBean(bpBeans, bpBean.getName() + "-binding-mediator", JossoMediator.class);
        setPropertyValue(bindingMediator, "logMessages", true);
        setPropertyBean(bindingMediator, "bindingFactory", newAnonymousBean(JossoBindingFactory.class));

        // artifactQueueManager
        setPropertyRef(bindingMediator, "artifactQueueManager", applianceDef.getName() + "-aqm");

        setPropertyValue(bindingMediator, "errorUrl", resolveLocationBaseUrl(applianceDef.getLocation()) + "/idbus-ui/error.do");

        // logger
        List<Bean> bpLogBuilders = new ArrayList<Bean>();
        bpLogBuilders.add(newAnonymousBean(JossoLogMessageBuilder.class));
        bpLogBuilders.add(newAnonymousBean(CamelLogMessageBuilder.class));
        bpLogBuilders.add(newAnonymousBean(HttpLogMessageBuilder.class));

        Bean bpLogger = newAnonymousBean(DefaultMediationLogger.class.getName());
        bpLogger.setName(bpBean.getName() + "-mediation-logger");
        setPropertyValue(bpLogger, "category", appliance.getNamespace() + "." + appliance.getName() + ".wire." + bpBean.getName());
        setPropertyAsBeans(bpLogger, "messageBuilders", bpLogBuilders);
        setPropertyBean(bindingMediator, "logger", bpLogger);

        ExecutionEnvironmentProperties execEnvProps = this.execEnvProperties.get(execEnv.getPlatformId());

        // Clear previous beans, just in case.
        event.getContext().put("agentBeans", null);
        event.getContext().put("agentBean", null);

        if (execEnvProps != null) {
            String agentJavaClass = execEnvProps.getJavaAgentClass();

            if (agentJavaClass != null) {

                // We need to generate a Spring josso agent config file.
                Beans agentBeans = new Beans();
                Bean agentBean = newBean(agentBeans, "josso-" + execEnv.getPlatformId() + "-agent", agentJavaClass);

                event.getContext().put("agentBeans", agentBeans);
                event.getContext().put("agentBean", agentBean);

                setPropertyValue(agentBean, "sessionAccessMinInterval", "1000");
                setPropertyValue(agentBean, "isStateOnClient", "true");

                setPropertyValue(agentBean, "gatewayLoginUrl", location + "/JOSSO/SSO/REDIR");
                setPropertyValue(agentBean, "gatewayLogoutUrl", location + "/JOSSO/SLO/REDIR");

                // ------------------ GatewayServiceLocator
                //    endpoint
                //    username (OPTIONAL)
                //    transportSecurity = TRANSPORT_SECURITY_NONE; (OPTIONAL)
                //    servicesWebContext (OPTIONAL)
                //    sessionManagerServicePath
                //    identityManagerServicePath
                //    identityProviderServicePath

                Bean gatewayServiceLocator = newAnonymousBean("org.josso.gateway.WebserviceGatewayServiceLocator");

                setPropertyValue(gatewayServiceLocator, "endpoint",
                        applianceDef.getLocation().getHost() + ":" + applianceDef.getLocation().getPort());

                // Remove starting slash
                setPropertyValue(gatewayServiceLocator, "sessionManagerServicePath",
                        (locationPath.startsWith("/") ? locationPath.substring(1) : locationPath) + "/JOSSO/SSOSessionManager/SOAP");
                // Remove starting slash
                setPropertyValue(gatewayServiceLocator, "identityManagerServicePath",
                        (locationPath.startsWith("/") ? locationPath.substring(1) : locationPath) + "/JOSSO/SSOIdentityManager/SOAP");
                // Remove starting slash
                setPropertyValue(gatewayServiceLocator, "identityProviderServicePath",
                        (locationPath.startsWith("/") ? locationPath.substring(1) : locationPath) + "/JOSSO/SSOIdentityProvider/SOAP");

                setPropertyBean(agentBean, "gatewayServiceLocator", gatewayServiceLocator);

                // FrontChannelParametersBuilder

                List<Bean> parametersBuildersBeans = new ArrayList<Bean>();
                Bean appIdParamsBuilderBean = newAnonymousBean("org.josso.agent.http.AppIdParametersBuilder");

                parametersBuildersBeans.add(appIdParamsBuilderBean);
                setPropertyAsBeans(agentBean, "parametersBuilders", parametersBuildersBeans);

                // AutomaticLoginStrategy, disabled for liferay
                List<Bean> autoLoginStrats = new ArrayList<Bean>();
                if (!execEnvProps.isEnableAutoLogin()) {
                    Bean disabledAutomaticLoginStrategy = newAnonymousBean("org.josso.agent.http.DisableAutomaticLoginStrategy");
                    setPropertyValue(disabledAutomaticLoginStrategy, "mode", "REQUIRED");
                    autoLoginStrats.add(disabledAutomaticLoginStrategy);
                } else {
                    Bean defaultAutomaticLoginStrategyBean = newAnonymousBean("org.josso.agent.http.DefaultAutomaticLoginStrategy");
                    setPropertyValue(defaultAutomaticLoginStrategyBean, "mode", "REQUIRED");

                    List<String> ignoredReferers = new ArrayList<String>();
                    ignoredReferers.add(resolveLocationUrl(applianceDef.getLocation()));
                    setPropertyAsValues(defaultAutomaticLoginStrategyBean, "ignoredReferrers", ignoredReferers);
                    autoLoginStrats.add(defaultAutomaticLoginStrategyBean);
                }


                setPropertyAsBeans(agentBean, "automaticLoginStrategies", autoLoginStrats);

                Bean parnterAppConfigBean = newAnonymousBean("org.josso.agent.SSOAgentConfigurationImpl");
                setPropertyBean(agentBean, "configuration", parnterAppConfigBean);
            }

            // TODO : Generate agent config files for non-java agentes : IIS, PHP, Apache, etc

        }
    }

    @Override
    public Object after(TransformEvent event) throws TransformException {

        IdProjectModule module = event.getContext().getCurrentModule();
        Beans baseBeans = (Beans) event.getContext().get("beans");
        Beans bpBeans = (Beans) event.getContext().get("bpBeans");

        Beans agentBeans = (Beans) event.getContext().get("agentBeans");
        Bean agentBean = (Bean) event.getContext().get("agentBean");        

        Bean bpBean = getBeansOfType(bpBeans, BindingProviderImpl.class.getName()).iterator().next();

        if (logger.isDebugEnabled())
            logger.debug("Wiring BP Provider/Channel with mediation components ("+bpBean.getName()+")");

        // Wire provider to COT
        Collection<Bean> cots = getBeansOfType(baseBeans, CircleOfTrustImpl.class.getName());
        if (cots.size() == 1) {
            Bean cot = cots.iterator().next();
            addPropertyBeansAsRefsToSet(cot, "providers", bpBean);
            /*
            String dependsOn = cot.getDependsOn();
            if (dependsOn == null || dependsOn.equals("")) {
                cot.setDependsOn(bpBean.getName());
            } else {
                cot.setDependsOn(dependsOn + "," + bpBean.getName());
            }
            */
        }

        // Mediation Unit
        Collection<Bean> mus = getBeansOfType(baseBeans, OsgiIdentityMediationUnit.class.getName());
        if (mus.size() == 1) {
            Bean mu = mus.iterator().next();
            Collection<Bean> bindingChannels = getBeansOfType(bpBeans, BindingChannelImpl.class.getName());
            for (Bean b : bindingChannels) {
                addPropertyBeansAsRefs(mu, "channels", b);
            }
        } else {
            throw new TransformException("One and only one Identity Mediation Unit is expected, found " + mus.size());
        }

        IdProjectResource<Beans> rBeans =  new IdProjectResource<Beans>(idGen.generateId(),
                bpBean.getName(),
                bpBean.getName(),
                "spring-beans",
                bpBeans);

        rBeans.setClassifier("jaxb");

        module.addResource(rBeans);

        if (agentBeans != null) {

            IdProjectResource<Beans> rAgentBeans =  new IdProjectResource<Beans>(idGen.generateId(),
                    bpBean.getName() + "/josso",
                    "josso-agent-" + bpBean.getName(),
                    "spring-beans",
                    agentBeans);

            rAgentBeans.setClassifier("jaxb");

            module.addResource(rAgentBeans);

        }

        return rBeans;
    }


    public Map<String, ExecutionEnvironmentProperties> getExecEnvProperties() {
        return execEnvProperties;
    }

    public void setExecEnvProperties(Map<String, ExecutionEnvironmentProperties> execEnvProperties) {
        this.execEnvProperties = execEnvProperties;
    }
}
