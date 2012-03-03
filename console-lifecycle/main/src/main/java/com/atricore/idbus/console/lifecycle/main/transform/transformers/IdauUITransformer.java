package com.atricore.idbus.console.lifecycle.main.transform.transformers;

import com.atricore.idbus.console.lifecycle.main.domain.IdentityAppliance;
import com.atricore.idbus.console.lifecycle.main.domain.metadata.IdentityApplianceDefinition;
import com.atricore.idbus.console.lifecycle.main.domain.metadata.Location;
import com.atricore.idbus.console.lifecycle.main.domain.metadata.UserDashboardBranding;
import com.atricore.idbus.console.lifecycle.main.transform.*;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.*;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.pax.wicket.Application;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.pax.wicket.ContextParam;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static com.atricore.idbus.console.lifecycle.support.springmetadata.util.BeanUtils.*;

/**
 * @author <a href=mailto:sgonzalez@atricore.org>Sebastian Gonzalez Oyuela</a>
 */
public class IdauUITransformer extends AbstractTransformer {

    private static Log logger = LogFactory.getLog(IdauBaseComponentsTransformer.class);

    @Override
    public boolean accept(TransformEvent event) {
        return event.getData() instanceof IdentityApplianceDefinition;
    }

    @Override
    public void before(TransformEvent event) {

        IdentityApplianceDefinition ida = (IdentityApplianceDefinition) event.getData();
        IdentityAppliance appliance = event.getContext().getProject().getIdAppliance();
        IdApplianceTransformationContext context = event.getContext();
        IdProjectModule module = context.getCurrentModule();

        Date now = new Date();

        // ----------------------------------------

        // ----------------------------------------

        // ----------------------------------------
        // UI Beans
        // ----------------------------------------

        Beans idauBeansUi = newBeans(ida.getName() + " UI : IdAU Configuration generated by Atricore Console on " + now.toGMTString());
        String uiBasePath = "/IDBUS-UI";
        Location uiLocation = ida.getUiLocation();
        if (uiLocation != null) {
            uiBasePath = resolveLocationPath(uiLocation);
        }

        // Branding configuration
        // If the appliance has a defined skin, configure it for this application!
        List<ContextParam> appParams = new ArrayList<ContextParam>();
        if (ida.getUserDashboardBranding() != null) {

            ContextParam branding = new ContextParam();
            branding.setParamName("branding");
            branding.setParamValue(ida.getUserDashboardBranding().getId());
            appParams.add(branding);


            if (ida.getUserDashboardBranding().getSkin() != null) {

                if (logger.isDebugEnabled())
                    logger.debug("Using 'SKIN' " + ida.getUserDashboardBranding().getSkin() + " from branding " + ida.getUserDashboardBranding().getId());

                ContextParam skin = new ContextParam();
                skin.setParamName("skin");
                skin.setParamValue(ida.getUserDashboardBranding().getSkin());
                appParams.add(skin);
            }


        }


        // ----------------------------------------
        // SSO Capability application
        // ----------------------------------------
        {


            String path = module.getPath();
            String pkg = module.getPackage();
            String clazz = "SSOUIApplication";
            String parentClazz = "org.atricore.idbus.capabilities.sso.ui.internal.SSOUIApplication";

            IdProjectSource s = new IdProjectSource(clazz, path, clazz, "java", "extends");
            s.setExtension("java");
            s.setClassifier("velocity");

            java.util.Map<String, Object> params = new HashMap<String, Object>();
            params.put("package", pkg);
            params.put("clazz", clazz);
            params.put("parentClazz", parentClazz);
            s.setParams(params);
            module.addSource(s);

            Application ssoUiApp = new Application();
            ssoUiApp.setId(ida.getName().toLowerCase() + "-sso-ui");
            ssoUiApp.setApplicationName(ida.getName().toLowerCase() + "-sso-ui");
            ssoUiApp.setClazz(pkg + "." + clazz);
            ssoUiApp.setMountPoint(uiBasePath + "/" + ida.getName().toUpperCase() + "/SSO");
            ssoUiApp.getContextParams().addAll(appParams);
            ssoUiApp.setInjectionSource("spring");

            idauBeansUi.getImportsAndAliasAndBeen().add(ssoUiApp);
        }

        // ----------------------------------------
        // OpenID Capability application
        // ----------------------------------------
        {

            String path = module.getPath();
            String pkg = module.getPackage();
            String clazz = "OpenIDUIApplication";
            String parentClazz = "org.atricore.idbus.capabilities.openid.ui.internal.OpenIDUIApplication";

            IdProjectSource s = new IdProjectSource(clazz, path, clazz, "java", "extends");
            s.setExtension("java");
            s.setClassifier("velocity");

            java.util.Map<String, Object> params = new HashMap<String, Object>();
            params.put("package", pkg);
            params.put("clazz", clazz);
            params.put("parentClazz", parentClazz);
            s.setParams(params);
            module.addSource(s);

            Application openIdUiApp = new Application();
            openIdUiApp.setId(ida.getName().toLowerCase() + "-openid-ui");
            openIdUiApp.setApplicationName(ida.getName().toLowerCase() + "-openid-ui");
            openIdUiApp.setClazz(pkg + "." + clazz);
            openIdUiApp.setMountPoint(uiBasePath + "/" + ida.getName().toUpperCase() + "/OPENID");
            openIdUiApp.getContextParams().addAll(appParams);
            openIdUiApp.setInjectionSource("spring");

            idauBeansUi.getImportsAndAliasAndBeen().add(openIdUiApp);
        }

        // ----------------------------------------
        // WebApp branding
        // ----------------------------------------
        UserDashboardBranding branding = appliance.getIdApplianceDefinition().getUserDashboardBranding();
        // The name 'branding' is expected in by the UI components defined in the capabilities.
        Bean brandingBean = newBean(idauBeansUi, "branding", org.atricore.idbus.capabilities.sso.ui.WebAppBranding.class);
        setPropertyValue(brandingBean, "skin", branding.getSkin());

        // ----------------------------------------
        // Add all the beans to the list
        // ----------------------------------------
        IdProjectResource<Beans> rBeansUi =  new IdProjectResource<Beans>(idGen.generateId(), "beans-ui", "spring-beans", idauBeansUi);
        rBeansUi.setClassifier("jaxb");
        module.addResource(rBeansUi);

    }

}
