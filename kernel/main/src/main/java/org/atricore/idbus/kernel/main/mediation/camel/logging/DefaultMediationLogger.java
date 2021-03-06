/*
 * Atricore IDBus
 *
 * Copyright (c) 2009, Atricore Inc.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.atricore.idbus.kernel.main.mediation.camel.logging;

import org.apache.camel.Message;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.kernel.main.federation.metadata.EndpointDescriptor;
import org.atricore.idbus.kernel.main.mediation.MediationMessage;
import org.atricore.idbus.kernel.main.mediation.camel.AbstractCamelEndpoint;
import org.atricore.idbus.kernel.main.mediation.camel.component.binding.CamelMediationEndpoint;
import org.atricore.idbus.kernel.main.mediation.camel.component.binding.CamelMediationExchange;
import org.atricore.idbus.kernel.main.mediation.camel.component.binding.CamelMediationMessage;
import org.springframework.beans.factory.InitializingBean;

import java.util.Collection;

/**
 * @author <a href="mailto:sgonzalez@atricore.org">Sebastian Gonzalez Oyuela</a>
 * @version $Id: DefaultMediationLogger.java 1359 2009-07-19 16:57:57Z sgonzalez $
 */
public class DefaultMediationLogger implements MediationLogger, InitializingBean {

    private static final Log logger = LogFactory.getLog(DefaultMediationLogger.class);

    private String category = "org.atricore.idbus.kernel.main.mediation.wire";

    private Log log;

    private Collection<LogMessageBuilder> builders = new java.util.ArrayList<LogMessageBuilder>();

    public void afterPropertiesSet() throws Exception {
        logger.debug("Starting Commons Logging log service in category '" + category +
                "' using " + builders.size() + " configured LogMessageBuilders");
        log = LogFactory.getLog(category);
    }

    public void logOutgoing(Message message) {

        if (log.isTraceEnabled()) {

            if (logger.isDebugEnabled())
                logger.debug("Logging outgoing ...");

            StringBuffer logEntry = new StringBuffer(2048);
            logEntry.append(" message-id=\"").append(message.getMessageId()).append("\"direction=\"OUT\"");

            if (message instanceof CamelMediationMessage) {
                CamelMediationMessage camlMsg = (CamelMediationMessage) message;
                MediationMessage msg = camlMsg.getMessage();

                if (msg != null) {
                    EndpointDescriptor destination = msg.getDestination();

                    if (destination != null) {
                        logEntry.append(" location=\"").append(destination.getLocation()).append("\"");
                        logEntry.append(" resposneLocation=\"").append(destination.getResponseLocation()).append("\"");
                        logEntry.append(" binding=\"").append(destination.getBinding()).append("\"");
                        logEntry.append(" type=\"").append(destination.getType()).append("\"");
                    }


                }
            }

            logMessageDetails(message, logEntry);

            log.trace(logEntry.toString());
        }

    }

    public void logFault(Message message) {

        if (log.isTraceEnabled()) {

            if (logger.isDebugEnabled())
                logger.debug("Logging fault ...");

            StringBuffer logEntry = new StringBuffer(2048);
            logEntry.append(" message id=\"").append(message.getMessageId()).append("\"direction=\"OUT\" fault=\"true\"");

            if (message instanceof CamelMediationMessage) {
                CamelMediationMessage camlMsg = (CamelMediationMessage) message;

                MediationMessage msg = camlMsg.getMessage();

                CamelMediationExchange camlExchange = camlMsg.getExchange();
                CamelMediationEndpoint camlEndpoint = camlExchange.getEndpoint();

                if (msg != null) {
                    EndpointDescriptor destination = msg.getDestination();

                    logEntry.append(" channel=\"" + camlEndpoint.getChannelRef() + "\"");
                    logEntry.append(" location=\"").append(destination.getLocation()).append("\"");
                    logEntry.append(" resposneLocation=\"").append(destination.getResponseLocation()).append("\"");
                    logEntry.append(" binding=\"").append(destination.getBinding()).append("\"");
                    logEntry.append(" type=\"").append(destination.getType()).append("\"");

                }
            }

            logMessageDetails(message, logEntry);

            log.trace(logEntry.toString());
        }
    }

    public void logIncomming(Message message) {
        if (log.isTraceEnabled()) {

            if (logger.isDebugEnabled())
                logger.debug("Logging incoming ...");


            StringBuffer logEntry = new StringBuffer(2048);
            logEntry.append(" message id=\"").append(message.getMessageId()).append("\" direction=\"IN\"");

            if (message instanceof CamelMediationMessage) {

                CamelMediationMessage camlMsg = (CamelMediationMessage) message;
                CamelMediationExchange camlExchange = camlMsg.getExchange();
                CamelMediationEndpoint camlEndpoint = camlExchange.getEndpoint();

                logEntry.append(" channel=\"" + camlEndpoint.getChannelRef() + "\"");
                logEntry.append(" endpoint-uri=\"").append(camlEndpoint.getEndpointUri()).append("\"");
                logEntry.append(" binding=\"").append(camlEndpoint.getBinding()).append("\"");

            }

            logMessageDetails(message, logEntry);

            log.trace(logEntry.toString());

        }

    }

    protected boolean logMessageDetails(Message message, StringBuffer logEntry) {

        boolean handled = false;

        for (LogMessageBuilder builder: builders) {

            if (logger.isDebugEnabled())
                logger.debug("Builder: " + builder);

            if (builder.canHandle(message)) {

                handled = true;

                String logMsg = builder.buildLogMessage(message);
                if (logMsg == null || logMsg.length() == 0) {
                    if (logger.isDebugEnabled())
                        logger.debug("Log Message Builder " + builder+ " produced null or empty log message");
                }

                logEntry.append("\n\tmessage-detail type=\"").append(builder.getType()).append("\"");
                logEntry.append(" ").append(logMsg);

            }
        }

        return handled;
    }

    /**
     * @org.apache.xbean.Property alias="msg-builders" nestedType="org.atricore.idbus.kernel.main.mediation.camel.logging.LogMessageBuilder"
     */
    public Collection<LogMessageBuilder> getMessageBuilders() {
        return builders;
    }

    /**
     * @org.apache.xbean.Property alias="msg-builders" nestedType="org.atricore.idbus.kernel.main.mediation.camel.logging.LogMessageBuilder"
     */
    public void  setMessageBuilders(Collection<LogMessageBuilder> builders) {
        this.builders = builders;
    }


    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
