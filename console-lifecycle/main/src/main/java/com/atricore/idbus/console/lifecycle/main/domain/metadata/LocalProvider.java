package com.atricore.idbus.console.lifecycle.main.domain.metadata;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:sgonzalez@atricore.org">Sebastian Gonzalez Oyuela</a>
 * @version $Id$
 */
public class LocalProvider extends Provider {

    private static final long serialVersionUID = 2967386484748634148L;

    private ProviderConfig config;

    // RFU
    private Set<Binding> activeBindings = new HashSet<Binding>();

    // RFU
    private Set<Profile> activeProfiles = new HashSet<Profile>();

    private IdentityLookup identityLookup;


    public ProviderConfig getConfig() {
        return config;
    }

    public void setConfig(ProviderConfig config) {
        this.config = config;
    }

    public Set<Binding> getActiveBindings() {
		if(activeBindings == null){
			activeBindings = new HashSet<Binding>();
		}
		return activeBindings;
	}

    public void setActiveBindings(Set<Binding> activeBindings) {
        this.activeBindings = activeBindings;
    }

    public Set<Profile> getActiveProfiles() {
		if(activeProfiles == null){
			activeProfiles = new HashSet<Profile>();
		}
		return activeProfiles;
	}

    public void setActiveProfiles(Set<Profile> activeProfiles) {
        this.activeProfiles = activeProfiles;
    }

    public IdentityLookup getIdentityLookup() {
        return identityLookup;
    }

    public void setIdentityLookup(IdentityLookup identityLookup) {
        this.identityLookup = identityLookup;
    }
}