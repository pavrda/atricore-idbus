package com.atricore.idbus.console.lifecycle.main.transform.transformers.sso;

import com.atricore.idbus.console.lifecycle.main.domain.IdentityAppliance;
import com.atricore.idbus.console.lifecycle.main.domain.metadata.*;
import com.atricore.idbus.console.lifecycle.main.exception.TransformException;
import com.atricore.idbus.console.lifecycle.main.transform.IdProjectModule;
import com.atricore.idbus.console.lifecycle.main.transform.IdProjectResource;
import com.atricore.idbus.console.lifecycle.main.transform.IdProjectSource;
import com.atricore.idbus.console.lifecycle.main.transform.TransformEvent;
import com.atricore.idbus.console.lifecycle.main.transform.transformers.AbstractTransformer;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.Bean;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.Beans;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.osgi.Service;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.pax.wicket.Application;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Date;
import java.util.HashMap;

import static com.atricore.idbus.console.lifecycle.support.springmetadata.util.BeanUtils.*;

/**
 * @author: sgonzalez@atriocore.com
 * @date: 3/1/13
 */
public class IdPUITransformer extends AbstractTransformer {

    private static final Log logger = LogFactory.getLog(IdPUITransformer.class);

    @Override
    public boolean accept(TransformEvent event) {
        return event.getData() instanceof IdentityProvider &&
                !((IdentityProvider)event.getData()).isRemote();
    }

    @Override
    public void before(TransformEvent event) throws TransformException {

        IdentityProvider idp = (IdentityProvider) event.getData();

        Date now = new Date();
        Beans idpUiBeans = newBeans(idp.getName() + " : IdP UI Configuration generated by Atricore Identity Bus Server on " + now.toGMTString());
        Beans baseBeans = (Beans) event.getContext().get("beans");
        Beans beansOsgi = (Beans) event.getContext().get("beansOsgi");

        String idauPath = (String) event.getContext().get("idauPath");

        // Publish root element so that other transformers can use it.
        event.getContext().put("idpUiBeans", idpUiBeans);

        if (logger.isDebugEnabled())
            logger.debug("Generating IDP UI " + idp.getName() + " configuration model");

        IdentityAppliance appliance = event.getContext().getProject().getIdAppliance();
        IdentityApplianceDefinition ida = appliance.getIdApplianceDefinition();

        IdProjectModule module = event.getContext().getCurrentModule();

        String uiBasePath = "IDBUS-UI";
        Location uiLocation = ida.getUiLocation();
        if (uiLocation != null) {
            uiBasePath = resolveLocationPath(uiLocation);
        }


        String path = module.getPath();
        String pkg = module.getPackage();
        String idpAppClazz = "SSOIdPApplication";
        String parentClazz = "org.atricore.idbus.capabilities.sso.ui.internal.SSOIdPApplication";

        // IdP specific application class
        IdProjectSource s = new IdProjectSource(idpAppClazz, path, idpAppClazz, "java", "extends");
        s.setExtension("java");
        s.setClassifier("velocity");

        java.util.Map<String, Object> params = new HashMap<String, Object>();
        params.put("package", pkg);
        params.put("clazz", idpAppClazz);
        params.put("parentClazz", parentClazz);
        s.setParams(params);
        module.addSource(s);


        // Look for the SP that has a self-services resource:

        InternalSaml2ServiceProvider sp = null;
        SelfServicesResource spResource = null;

        // There should be one and only one SP for self-services!
        if (idp.getFederatedConnectionsA() != null) {

            for (FederatedConnection fc : idp.getFederatedConnectionsA()) {
                if (fc.getRoleB() instanceof InternalSaml2ServiceProvider) {
                    InternalSaml2ServiceProvider spTmp = (InternalSaml2ServiceProvider) fc.getRoleB();

                    if (spTmp.getServiceConnection().getResource() instanceof SelfServicesResource) {
                        sp = spTmp;
                        spResource = (SelfServicesResource) spTmp.getServiceConnection().getResource();
                        break;
                    }
                }
            }
        }

        if (idp.getFederatedConnectionsB() != null) {

            for (FederatedConnection fc : idp.getFederatedConnectionsB()) {
                if (fc.getRoleA() instanceof InternalSaml2ServiceProvider) {
                    //InternalSaml2ServiceProvider spTmp = (InternalSaml2ServiceProvider) fc.getRoleB();
                    InternalSaml2ServiceProvider spTmp = (InternalSaml2ServiceProvider) fc.getRoleA();

                    if (spTmp.getServiceConnection().getResource() instanceof SelfServicesResource) {
                        sp = spTmp;
                        spResource = (SelfServicesResource) spTmp.getServiceConnection().getResource();
                        break;
                    }
                }
            }
        }

        // Self-Services are OAuth2 based, look for the shared secret:

        String brandingId = idp.getUserDashboardBranding() != null ? idp.getUserDashboardBranding() : ida.getUserDashboardBranding().getId();

        // If the IdP uses a different branding, create an application for it
        // TODO : This should now consider different self-services options for each IdP
        Application idpUiApp = new Application();
        idpUiApp.setId(normalizeBeanName(ida.getName() + "-" + idp.getName() + "-sso-ui"));
        idpUiApp.setApplicationName(ida.getName().toLowerCase() + "-" + idp.getName().toLowerCase() + "-sso-ui");
        //idpUiApp.setClazz(pkg + "." + idpAppClazz); // DO NOT USE THE GENERATED CLASS WITH WICKET 6.X
        idpUiApp.setClazz(parentClazz);
        idpUiApp.setMountPoint(uiBasePath + "/" + ida.getName().toUpperCase() + "/" + idp.getName().toUpperCase() + "/SSO");
        idpUiApp.setInjectionSource("spring");

        idpUiBeans.getImportsAndAliasAndBeen().add(idpUiApp);

        // App Configuration
        Bean idpAppCfgBean = newBean(idpUiBeans, idpUiApp.getId() + "-cfg", "org.atricore.idbus.capabilities.sso.ui.WebAppConfig");
        setPropertyValue(idpAppCfgBean, "appName", idpUiApp.getId());
        setPropertyValue(idpAppCfgBean, "mountPoint", idpUiApp.getMountPoint());
        setPropertyValue(idpAppCfgBean, "brandingId", brandingId);

        setPropertyValue(idpAppCfgBean, "unitName", ida.getName() + "-mediation-unit");
        setPropertyValue(idpAppCfgBean, "idpName", normalizeBeanName(idp.getName()));
        if (logger.isDebugEnabled())
            logger.debug("Self-Services SP " + (sp == null ? "NOT Availabe" : "Avaiable: " + sp.getName()));
        if (sp != null) {
            setPropertyValue(idpAppCfgBean, "selfServicesSpName", normalizeBeanName(sp.getName()));
            setPropertyValue(idpAppCfgBean, "selfServicesSharedSecret", idp.getOauth2Key());
        }

        // Export App Configuration
        Service idpAppCfgBeanOsgi = new Service();
        idpAppCfgBeanOsgi.setId(idpAppCfgBean.getName() + "-osgi");
        idpAppCfgBeanOsgi.setRef(idpAppCfgBean.getName());
        idpAppCfgBeanOsgi.setInterface("org.atricore.idbus.capabilities.sso.ui.WebAppConfig");

        idpUiBeans.getImportsAndAliasAndBeen().add(idpAppCfgBeanOsgi);

    }


    @Override
    public Object after(TransformEvent event) throws TransformException {

        IdentityProvider provider = (IdentityProvider) event.getData();
        IdProjectModule module = event.getContext().getCurrentModule();
        Beans baseBeans = (Beans) event.getContext().get("beans");
        Beans idpUiBeans = (Beans) event.getContext().get("idpUiBeans");

        String idpBeanName = normalizeBeanName(provider.getName());

        // ----------------------------------------
        // Add all the beans to the list
        // ----------------------------------------

        IdProjectResource<Beans> rBeansUi = new IdProjectResource<Beans>(idGen.generateId(), idpBeanName, idpBeanName + "-ui", "spring-beans", idpUiBeans);


        rBeansUi.setClassifier("jaxb");
        rBeansUi.setNameSpace(idpBeanName);

        module.addResource(rBeansUi);

        return rBeansUi;

    }

}
