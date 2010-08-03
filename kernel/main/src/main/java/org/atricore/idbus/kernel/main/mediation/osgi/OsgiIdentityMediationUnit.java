package org.atricore.idbus.kernel.main.mediation.osgi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.kernel.main.federation.metadata.CircleOfTrustManager;
import org.atricore.idbus.kernel.main.federation.metadata.EndpointDescriptorImpl;
import org.atricore.idbus.kernel.main.federation.metadata.MetadataEntry;
import org.atricore.idbus.kernel.main.mediation.*;
import org.atricore.idbus.kernel.main.mediation.channel.AbstractFederationChannel;
import org.atricore.idbus.kernel.main.mediation.channel.FederationChannel;
import org.atricore.idbus.kernel.main.mediation.endpoint.IdentityMediationEndpoint;
import org.atricore.idbus.kernel.main.mediation.endpoint.IdentityMediationEndpointImpl;
import org.osgi.framework.BundleContext;
import org.springframework.context.ApplicationContext;
import org.springframework.osgi.context.BundleContextAware;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:sgonzalez@atricore.org">Sebastian Gonzalez Oyuela</a>
 * @version $Id$
 */
public class OsgiIdentityMediationUnit extends SpringMediationUnit
        implements BundleContextAware {

    private static final Log logger = LogFactory.getLog(OsgiIdentityMediationUnit.class);

    private BundleContext bundleContext;

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        try {

            // We need this code to run here, triggered by spring, so that
            // the identity appliance unit classloader is used ...

            super.afterPropertiesSet();

            ApplicationContext applicationContext = this.getApplicationContext();
            Collection<Channel> channels = this.getChannels();

            long now = System.currentTimeMillis();

            Map<String, CircleOfTrustManager> cots = applicationContext.getBeansOfType(CircleOfTrustManager.class);
            if (cots.values().size() > 1)
                throw new IdentityMediationException("Multiple Circle of Trust managers not supported!");

            // The mediation process can use COTs ...
            CircleOfTrustManager cotMgr = null;
            if (cots.values().size() < 1) {
                logger.warn("No Circle of Trust manager found");
                return;
            }

            cotMgr = cots.values().iterator().next();
            if (logger.isDebugEnabled())
                logger.debug("Initializing Mediation infrastructure using COT manager " + cotMgr);
            cotMgr.init();

            Set<String> channelNames = new HashSet<String>();
            Set<String> endpointNames = new HashSet<String>();

            // Register mediators with container
            for (Channel channel : channels) {

                if (channel.getUnitContainer() == null) {
                    logger.error("Channel " + channel.getName() + " ["+channel.getClass().getSimpleName()+"] does not have a mediation unitContainer!");
                    continue;
                }
                if (channel.getName() == null) {
                    throw new IllegalArgumentException("Channel " + channel + " name cannot be null");
                }
                if (channelNames.contains(channel.getName())) {
                    throw new IllegalArgumentException("Channel name already in use " + channel.getName());
                }
                channelNames.add(channel.getName());

                // Register channel ID Mediator with channel mediation unitContainer

                IdentityMediationUnitContainer unitContainer = channel.getUnitContainer();
                IdentityMediator mediator = channel.getIdentityMediator();

                logger.info("Registering channel " + channel+ " with mediator/unitContainer " + mediator + "/" + unitContainer);

                channel.getUnitContainer().getMediators().add(mediator);

                // Setup Federation Channels (SPs/IDPs)
                if (channel instanceof FederationChannel) {

                    logger.info("Registering Federation channel " + channel);

                    AbstractFederationChannel fedChannel = (AbstractFederationChannel) channel;
                    MetadataEntry md = cotMgr.findEntityRoleMetadata(fedChannel.getMember().getAlias(),
                            fedChannel.getRole());

                    fedChannel.setMetadata(md);
                    fedChannel.setCircleOfTrust(cotMgr.getCot());

                    if (fedChannel.getEndpoints() != null) {

                        for (IdentityMediationEndpoint identityMediationEndpoint : fedChannel.getEndpoints()) {

                            // Endpoints MUST have unique, not null names!
                            IdentityMediationEndpointImpl endpoint = (IdentityMediationEndpointImpl) identityMediationEndpoint;
                            if (endpoint.getName() == null)
                                throw new IllegalArgumentException("Endpoint name cannot be null " + endpoint);

                            // TODO : qualify endpoint name with channel name!
                            if (endpointNames.contains(endpoint.getName())) {
                                throw new IllegalArgumentException("Endpoint name already in use " + endpoint.getName());
                            }
                            endpointNames.add(endpoint.getName());

                            MetadataEntry endpointMetadata = cotMgr.findEndpointMetadata(fedChannel.getMember().getAlias(),
                                    fedChannel.getRole(),
                                    new EndpointDescriptorImpl(identityMediationEndpoint.getName(),
                                            identityMediationEndpoint.getType(),
                                            identityMediationEndpoint.getBinding()));

                            endpoint.setMetadata(endpointMetadata);
                        }
                    } else {
                        logger.warn("Federation channel does not define endpoints : " + fedChannel.getName());
                    }

                }
            }

            // initialize mediation mediation unit container (e.g. create  context)
            IdentityMediationUnitContainer container = this.getContainer();
            container.init(this);

            // Prepare mediators onto engines and start each one (e.g. create routes and components)
            for (Channel channel : channels) {
                if (channel.getIdentityMediator() != null) {
                    logger.info("Setting up endpoints for channel : " + channel.getName());
                    IdentityMediator mediator = channel.getIdentityMediator();
                    mediator.setupEndpoints(channel);
                } else {
                    logger.warn("Channel does not have an Identity Mediator");
                }
            }

            // start container
            container.start();

            logger.info("IDBus Identity Mediation Unit '" + getName() + "' started in " + (System.currentTimeMillis() - now) + "ms");

            // Display message in stdout, so that it's shown in command prompt
            System.out.println("IDBus Identity Mediation Unit '" + getName() + "' started in " + (System.currentTimeMillis() - now) + "ms");
        } catch (Exception e) {
            System.err.println("IDBus Identity Mediation Unit '" + getName() + "' initialization: " + e.getMessage());
            throw new IdentityMediationException("IDBus Identity Mediation Unit '" + getName() + "' initialization error:"  + e.getMessage(), e);
        }
    }

    @Override
    public void destroy() throws Exception {
        super.destroy();
    }
}
