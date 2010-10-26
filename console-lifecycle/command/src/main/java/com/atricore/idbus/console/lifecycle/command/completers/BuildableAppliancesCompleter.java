package com.atricore.idbus.console.lifecycle.command.completers;

import com.atricore.idbus.console.lifecycle.main.domain.IdentityAppliance;
import com.atricore.idbus.console.lifecycle.main.domain.IdentityApplianceState;
import com.atricore.idbus.console.lifecycle.main.exception.IdentityServerException;
import com.atricore.idbus.console.lifecycle.main.spi.IdentityApplianceManagementService;
import com.atricore.idbus.console.lifecycle.main.spi.request.ListIdentityAppliancesByStateRequest;
import com.atricore.idbus.console.lifecycle.main.spi.response.ListIdentityAppliancesByStateResponse;
import org.apache.karaf.shell.console.completer.StringsCompleter;

import java.util.List;

/**
 * @author <a href="mailto:sgonzalez@atricore.org">Sebastian Gonzalez Oyuela</a>
 * @version $Id$
 */
public class BuildableAppliancesCompleter extends OsgiCompleterSupport {

    @Override
    protected int complete(IdentityApplianceManagementService applianceMgrService, final String buffer, final int cursor, final List candidates) {

        StringsCompleter delegate = new StringsCompleter();

        try {
            ListIdentityAppliancesByStateRequest req = new ListIdentityAppliancesByStateRequest ();
            // TODO : List appliances that CAN be deployed ?
            req.setState(IdentityApplianceState.PROJECTED.toString());
            ListIdentityAppliancesByStateResponse res = applianceMgrService.listIdentityAppliancesByState(req);

            for (IdentityAppliance appliance : res.getAppliances()) {
                delegate.getStrings().add(appliance.getId() + "");
            }
        } catch (IdentityServerException e) {
            // Ignore
        }

        return delegate.complete(buffer, cursor, candidates);
    }

}
