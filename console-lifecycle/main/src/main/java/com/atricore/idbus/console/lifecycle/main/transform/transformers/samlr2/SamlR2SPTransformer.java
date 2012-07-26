package com.atricore.idbus.console.lifecycle.main.transform.transformers.samlr2;

import com.atricore.idbus.console.lifecycle.main.domain.metadata.*;
import com.atricore.idbus.console.lifecycle.main.exception.TransformException;
import com.atricore.idbus.console.lifecycle.main.transform.IdProjectModule;
import com.atricore.idbus.console.lifecycle.main.transform.IdProjectResource;
import com.atricore.idbus.console.lifecycle.main.transform.TransformEvent;
import com.atricore.idbus.console.lifecycle.main.transform.transformers.AbstractTransformer;
import com.atricore.idbus.console.lifecycle.main.util.MetadataUtil;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.Bean;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.Beans;
import com.atricore.idbus.console.lifecycle.support.springmetadata.model.Description;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.capabilities.sso.main.SamlR2MetadataDefinitionIntrospector;
import org.atricore.idbus.capabilities.sso.support.metadata.SSOMetadataConstants;
import org.atricore.idbus.kernel.main.federation.metadata.CircleOfTrustImpl;
import org.atricore.idbus.kernel.main.federation.metadata.MetadataDefinition;
import org.atricore.idbus.kernel.main.federation.metadata.ResourceCircleOfTrustMemberDescriptorImpl;
import org.atricore.idbus.kernel.main.mediation.provider.FederatedRemoteProviderImpl;
import org.atricore.idbus.kernel.main.util.HashGenerator;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Date;

import static com.atricore.idbus.console.lifecycle.support.springmetadata.util.BeanUtils.*;

/**
 * @author <a href=mailto:sgonzalez@atricore.org>Sebastian Gonzalez Oyuela</a>
 */
public class SamlR2SPTransformer extends AbstractTransformer {

    private static final Log logger = LogFactory.getLog(SamlR2SPTransformer.class);

    @Override
    public boolean accept(TransformEvent event) {
        return event.getData() instanceof ExternalSaml2ServiceProvider ||
               event.getData() instanceof GoogleAppsServiceProvider ||
               event.getData() instanceof SugarCRMServiceProvider;
    }

    @Override
    public void before(TransformEvent event) throws TransformException {

        FederatedProvider provider = (FederatedProvider) event.getData();

        Date now = new Date();

        Beans spBeans = new Beans();

        Description descr = new Description();
        descr.getContent().add(provider.getName() + " : SP Configuration generated by Atricore Identity Bus Server on " + now.toGMTString());
        descr.getContent().add(provider.getDescription());

        Beans baseBeans = (Beans) event.getContext().get("beans");
        String idauPath = (String) event.getContext().get("idauPath");

        spBeans.setDescription(descr);

        // Publish root element so that other transformers can use it.
        event.getContext().put("spBeans", spBeans);

        if (logger.isDebugEnabled())
            logger.debug("Generating SP " + provider.getName() + " configuration model");

        // Define all required beans! (We can break down this in the future ...)

        // ----------------------------------------
        // Service Provider
        // ----------------------------------------

        Bean sp = newBean(spBeans, normalizeBeanName(provider.getName()),
                FederatedRemoteProviderImpl.class.getName());

        // Name
        setPropertyValue(sp, "name", sp.getName());

        // Role
        setPropertyValue(sp, "role", SSOMetadataConstants.SPSSODescriptor_QNAME.toString());

        // Wire provider to COT
        Collection<Bean> cots = getBeansOfType(baseBeans, CircleOfTrustImpl.class.getName());
        if (cots.size() == 1) {
            Bean cot = cots.iterator().next();
            setPropertyRef(sp, "circleOfTrust", cot.getName());
        }

        // ResourceCircleOfTrustMemberDescriptor
        Bean spMd = newBean(spBeans, sp.getName() + "-md", ResourceCircleOfTrustMemberDescriptorImpl.class);
        String alias;
        try {
            MetadataDefinition md = MetadataUtil.loadMetadataDefinition(provider.getMetadata().getValue());
            alias = MetadataUtil.findEntityId(md);
        } catch (Exception e) {
            throw new TransformException("Error loading metadata definition for " + provider.getName());
        }
        try {
            setPropertyValue(spMd, "id", HashGenerator.sha1(alias));
        } catch (UnsupportedEncodingException e) {
            throw new TransformException("Error generating SHA-1 hash for alias '" + alias + "': unsupported encoding");
        } catch (NoSuchAlgorithmException e) {
            throw new TransformException("Error generating SHA-1 hash for alias '" + alias + "': no such algorithm");
        }
        setPropertyValue(spMd, "alias", alias);
        setPropertyValue(spMd, "resource", "classpath:" + idauPath + sp.getName() + "/" + sp.getName() + "-samlr2-metadata.xml");
        Bean mdIntrospector = newAnonymousBean(SamlR2MetadataDefinitionIntrospector.class);
        setPropertyBean(spMd, "metadataIntrospector", mdIntrospector);

        // members
        addPropertyBeansAsRefs(sp, "members", spMd);

        // metadata file
        IdProjectResource<byte[]> metadataResource = new IdProjectResource<byte[]>(idGen.generateId(),
                idauPath + sp.getName() + "/", sp.getName() + "-samlr2-metadata.xml",
                "binary", provider.getMetadata().getValue());
        metadataResource.setClassifier("byte");
        event.getContext().getCurrentModule().addResource(metadataResource);
    }

    @Override
    public Object after(TransformEvent event) throws TransformException {

        ExternalSaml2ServiceProvider provider = (ExternalSaml2ServiceProvider) event.getData();
        IdProjectModule module = event.getContext().getCurrentModule();
        Beans baseBeans = (Beans) event.getContext().get("beans");
        Beans spBeans = (Beans) event.getContext().get("spBeans");
        
        Bean spBean = getBeansOfType(spBeans, FederatedRemoteProviderImpl.class.getName()).iterator().next();

        // Wire provider to COT
        Collection<Bean> cots = getBeansOfType(baseBeans, CircleOfTrustImpl.class.getName());
        if (cots.size() == 1) {
            Bean cot = cots.iterator().next();
            addPropertyBeansAsRefsToSet(cot, "providers", spBean);
            String dependsOn = cot.getDependsOn();
            if (dependsOn == null || dependsOn.equals("")) {
                cot.setDependsOn(spBean.getName());
            } else {
                cot.setDependsOn(dependsOn + "," + spBean.getName());
            }
        }

        IdProjectResource<Beans> rBeans =  new IdProjectResource<Beans>(idGen.generateId(), spBean.getName(), spBean.getName(), "spring-beans", spBeans);
        rBeans.setClassifier("jaxb");
        rBeans.setNameSpace(spBean.getName());

        module.addResource(rBeans);

        return rBeans;
    }
}
