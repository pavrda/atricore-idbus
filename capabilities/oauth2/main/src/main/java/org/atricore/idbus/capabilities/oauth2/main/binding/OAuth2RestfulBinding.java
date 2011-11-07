package org.atricore.idbus.capabilities.oauth2.main.binding;

import org.apache.camel.Exchange;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.kernel.main.mediation.Channel;
import org.atricore.idbus.kernel.main.mediation.MediationMessage;
import org.atricore.idbus.kernel.main.mediation.camel.component.binding.AbstractMediationHttpBinding;
import org.atricore.idbus.kernel.main.mediation.camel.component.binding.CamelMediationMessage;

/**
 * @author <a href=mailto:sgonzalez@atricore.org>Sebastian Gonzalez Oyuela</a>
 */
public class OAuth2RestfulBinding extends AbstractMediationHttpBinding {

    private static final Log logger = LogFactory.getLog(OAuth2RestfulBinding.class);

    public OAuth2RestfulBinding(Channel channel) {
        super(OAuth2Binding.OAUTH2_RESTFUL.getValue(), channel);
    }

    public MediationMessage createMessage(CamelMediationMessage message) {
        throw new UnsupportedOperationException("Not Implemented!");
    }

    public void copyMessageToExchange(CamelMediationMessage message, Exchange exchange) {
        throw new UnsupportedOperationException("Not Implemented!");
    }
}
