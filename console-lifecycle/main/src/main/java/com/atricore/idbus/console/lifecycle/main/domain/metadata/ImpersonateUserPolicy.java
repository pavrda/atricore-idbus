package com.atricore.idbus.console.lifecycle.main.domain.metadata;

import java.io.Serializable;

/**
 * @author <a href=mailto:sgonzalez@atricore.org>Sebastian Gonzalez Oyuela</a>
 */
public class ImpersonateUserPolicy implements Serializable {

	private static final long serialVersionUID = -2352040927848266989L;

    private long id;

	private String name;

    private ImpersonateUserPolicyType impersonateUserPolicyType;

    private String customImpersonateUserPolicy;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

    public ImpersonateUserPolicyType getImpersonateUserPolicyType() {
        return impersonateUserPolicyType;
    }

    public void setImpersonateUserPolicyType(ImpersonateUserPolicyType impersonateUserPolicyType) {
        this.impersonateUserPolicyType = impersonateUserPolicyType;
    }

    public String getCustomImpersonateUserPolicy() {
        return customImpersonateUserPolicy;
    }

    public void setCustomImpersonateUserPolicy(String customImpersonateUserPolicy) {
        this.customImpersonateUserPolicy = customImpersonateUserPolicy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImpersonateUserPolicy)) return false;

        ImpersonateUserPolicy that = (ImpersonateUserPolicy) o;

        if(id == 0) return false;

        if (id != that.id) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}
