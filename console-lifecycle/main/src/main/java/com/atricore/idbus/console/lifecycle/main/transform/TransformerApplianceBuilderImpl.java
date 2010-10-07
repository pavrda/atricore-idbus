package com.atricore.idbus.console.lifecycle.main.transform;

import com.atricore.idbus.console.lifecycle.main.domain.IdentityAppliance;
import com.atricore.idbus.console.lifecycle.main.domain.IdentityApplianceDeployment;
import com.atricore.idbus.console.lifecycle.main.spi.ApplianceBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author <a href="mailto:sgonzalez@atricore.org">Sebastian Gonzalez Oyuela</a>
 * @version $Id$
 */
public class TransformerApplianceBuilderImpl implements ApplianceBuilder {

    private static final Log logger = LogFactory.getLog(TransformerApplianceBuilderImpl.class);

    private TransformationEngine engine;

    public TransformationEngine getEngine() {
        return engine;
    }

    public void setEngine(TransformationEngine engine) {
        this.engine = engine;
    }

    public IdentityAppliance build(IdentityAppliance appliance) {
        return buildAppliance(appliance).getProject().getIdAppliance();
    }

    public byte[] exportProject(IdentityAppliance appliance) {
        IdApplianceTransformationContext ctx = buildAppliance(appliance);

        IdApplianceProject prj = ctx.getProject();
        IdProjectModule module = prj.getRootModule();
        ProjectModuleLayout layout = prj.getRootModule().getLayout();

        // TODO !!!!

        return null;
    }

    protected IdApplianceTransformationContext buildAppliance(IdentityAppliance appliance) {

        try {
            IdentityApplianceDeployment deployment = appliance.getIdApplianceDeployment();
            if (deployment == null) {

                if (logger.isDebugEnabled())
                    logger.debug("Creating new IdentityApplianceDeployment instance");
                deployment = new IdentityApplianceDeployment();
                appliance.setIdApplianceDeployment(deployment);
            }

            return engine.transform(appliance);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



}
