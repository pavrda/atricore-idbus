package org.atricore.idbus.kernel.main.mediation;

/**
 * @author <a href="mailto:sgonzalez@atricore.org">Sebastian Gonzalez Oyuela</a>
 * @version $Id$
 */
public interface MediationUnitLifecycleEvent {

    String getType();

    String getUnitName();
}
