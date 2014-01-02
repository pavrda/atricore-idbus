/*
 * Atricore IDBus
 *
 *   Copyright 2009, Atricore Inc.
 *
 *   This is free software; you can redistribute it and/or modify it
 *   under the terms of the GNU Lesser General Public License as
 *   published by the Free Software Foundation; either version 2.1 of
 *   the License, or (at your option) any later version.
 *
 *   This software is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *   Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 *   License along with this software; if not, write to the Free
 *   Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *   02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package com.atricore.idbus.console.services.dto;

import java.util.HashSet;
import java.util.Set;

public class IdentityProviderDTO extends FederatedProviderDTO {

	private static final long serialVersionUID = 141137856095909986L;

    private boolean wantAuthnRequestsSigned;

    private boolean signRequests;

    private boolean wantSignedRequests;

    private boolean ignoreRequestedNameIDPolicy = true;

    private int ssoSessionTimeout;

    private int maxSessionsPerUser;

    private boolean destroyPreviousSession;

    private String oauth2ClientsConfig;

    private String oauth2Key;

    private boolean oauth2Enabled;

    private boolean openIdEnabled;

    private String dashboardUrl;

    private String userDashboardBranding;

    // USERNAME, EMAIL, TRANSIENT, PERSISTENT, X509 Principal Name, Windows DC Principal
    private SubjectNameIdentifierPolicyDTO subjectNameIDPolicy;

    // RFU
    private AttributeProfileDTO attributeProfile;

    // RFU
    private Set<AuthenticationMechanismDTO> authenticationMechanisms;

    // RFU
    private AuthenticationContractDTO authenticationContract;

    // RFU
    private AuthenticationAssertionEmissionPolicyDTO emissionPolicy;

    // RFU
    //TODO check whether LocalProvider will have bindings or IdentityProvider
//    private Set<BindingDTO> activeBindings;

    // RFU
    //TODO check whether LocalProvider will have profiles or IdentityProvider
//    private Set<ProfileDTO> activeProfiles;

    //private DelegatedAuthenticationDTO delegatedAuthentication;
    private Set<DelegatedAuthenticationDTO> delegatedAuthentications;

    private int messageTtl;

    private int messageTtlTolerance;

    private boolean identityConfirmationEnabled;

    private ExtensionDTO identityConfirmationPolicy;

    private String identityConfirmationOAuth2ClientId;

    private String identityConfirmationOAuth2ClientSecret;

    private boolean externallyHostedIdentityConfirmationTokenService;

    private String identityConfirmationOAuth2AuthorizationServerEndpoint;

    @Override
    public ProviderRoleDTO getRole() {
        return ProviderRoleDTO.SSOIdentityProvider;
    }

    @Override
    public void setRole(ProviderRoleDTO role) {
        throw new UnsupportedOperationException("Cannot change provider role");
    }

    public boolean isWantAuthnRequestsSigned() {
        return wantAuthnRequestsSigned;
    }

    public void setWantAuthnRequestsSigned(boolean wantAuthnRequestsSigned) {
        this.wantAuthnRequestsSigned = wantAuthnRequestsSigned;
    }

    public boolean isSignRequests() {
        return signRequests;
    }

    public void setSignRequests(boolean signRequests) {
        this.signRequests = signRequests;
    }

    public boolean isWantSignedRequests() {
        return wantSignedRequests;
    }

    public void setWantSignedRequests(boolean wantSignedRequests) {
        this.wantSignedRequests = wantSignedRequests;
    }

    public int getSsoSessionTimeout() {
        return ssoSessionTimeout;
    }

    public void setSsoSessionTimeout(int ssoSessionTimeout) {
        this.ssoSessionTimeout = ssoSessionTimeout;
    }

    public int getMaxSessionsPerUser() {
        return maxSessionsPerUser;
    }

    public void setMaxSessionsPerUser(int maxSessionsPerUser) {
        this.maxSessionsPerUser = maxSessionsPerUser;
    }

    public boolean isDestroyPreviousSession() {
        return destroyPreviousSession;
    }

    public void setDestroyPreviousSession(boolean destroyPreviousSession) {
        this.destroyPreviousSession = destroyPreviousSession;
    }

    public AttributeProfileDTO getAttributeProfile() {
        return attributeProfile;
    }

    public void setAttributeProfile(AttributeProfileDTO attributeProfile) {
        this.attributeProfile = attributeProfile;
    }

    public AuthenticationContractDTO getAuthenticationContract() {
        return authenticationContract;
    }

    public void setAuthenticationContract(AuthenticationContractDTO authenticationContract) {
        this.authenticationContract = authenticationContract;
    }

    public Set<AuthenticationMechanismDTO> getAuthenticationMechanisms() {
        if(authenticationMechanisms == null){
            authenticationMechanisms = new HashSet<AuthenticationMechanismDTO>();
        }
        return authenticationMechanisms;
    }

    public void setAuthenticationMechanisms(Set<AuthenticationMechanismDTO> authenticationMechanisms) {
        this.authenticationMechanisms = authenticationMechanisms;
    }

    public AuthenticationAssertionEmissionPolicyDTO getEmissionPolicy() {
        return emissionPolicy;
    }

    public void setEmissionPolicy(AuthenticationAssertionEmissionPolicyDTO emissionPolicy) {
        this.emissionPolicy = emissionPolicy;
    }

    public Set<DelegatedAuthenticationDTO> getDelegatedAuthentications() {
        return delegatedAuthentications;
    }

    public void setDelegatedAuthentications(Set<DelegatedAuthenticationDTO> delegatedAuthentications) {
        this.delegatedAuthentications = delegatedAuthentications;
    }

    public boolean isIgnoreRequestedNameIDPolicy() {
        return ignoreRequestedNameIDPolicy;
    }

    public void setIgnoreRequestedNameIDPolicy(boolean ignoreRequestedNameIDPolicy) {
        this.ignoreRequestedNameIDPolicy = ignoreRequestedNameIDPolicy;
    }

    public SubjectNameIdentifierPolicyDTO getSubjectNameIDPolicy() {
        return subjectNameIDPolicy;
    }

    public void setSubjectNameIDPolicy(SubjectNameIdentifierPolicyDTO subjectNameIDPolicy) {
        this.subjectNameIDPolicy = subjectNameIDPolicy;
    }

    public String getOauth2ClientsConfig() {
        return oauth2ClientsConfig;
    }

    public void setOauth2ClientsConfig(String oauth2ClientsConfig) {
        this.oauth2ClientsConfig = oauth2ClientsConfig;
    }

    public String getOauth2Key() {
        return oauth2Key;
    }

    public void setOauth2Key(String oauth2Key) {
        this.oauth2Key = oauth2Key;
    }

    public boolean isOauth2Enabled() {
        return oauth2Enabled;
    }

    public void setOauth2Enabled(boolean oauth2Enabled) {
        this.oauth2Enabled = oauth2Enabled;
    }

    public boolean isOpenIdEnabled() {
        return openIdEnabled;
    }

    public void setOpenIdEnabled(boolean openIdEnabled) {
        this.openIdEnabled = openIdEnabled;
    }

    public int getMessageTtl() {
        return messageTtl;
    }

    public void setMessageTtl(int messageTtl) {
        this.messageTtl = messageTtl;
    }

    public int getMessageTtlTolerance() {
        return messageTtlTolerance;
    }

    public void setMessageTtlTolerance(int messageTtlTolerance) {
        this.messageTtlTolerance = messageTtlTolerance;
    }

    public String getDashboardUrl() {
        return dashboardUrl;
    }

    public void setDashboardUrl(String dashboardUrl) {
        this.dashboardUrl = dashboardUrl;

    }

    public String getUserDashboardBranding() {
        return userDashboardBranding;
    }

    public void setUserDashboardBranding(String userDashboardBranding) {
        this.userDashboardBranding = userDashboardBranding;
    }

    public boolean isIdentityConfirmationEnabled() {
        return identityConfirmationEnabled;
    }

    public void setIdentityConfirmationEnabled(boolean identityConfirmationEnabled) {
        this.identityConfirmationEnabled = identityConfirmationEnabled;
    }

    public String getIdentityConfirmationOAuth2ClientId() {
        return identityConfirmationOAuth2ClientId;
    }

    public ExtensionDTO getIdentityConfirmationPolicy() {
        return identityConfirmationPolicy;
    }

    public void setIdentityConfirmationPolicy(ExtensionDTO identityConfirmationPolicy) {
        this.identityConfirmationPolicy = identityConfirmationPolicy;
    }

    public void setIdentityConfirmationOAuth2ClientId(String identityConfirmationOAuth2ClientId) {
        this.identityConfirmationOAuth2ClientId = identityConfirmationOAuth2ClientId;
    }

    public String getIdentityConfirmationOAuth2ClientSecret() {
        return identityConfirmationOAuth2ClientSecret;
    }

    public void setIdentityConfirmationOAuth2ClientSecret(String identityConfirmationOAuth2ClientSecret) {
        this.identityConfirmationOAuth2ClientSecret = identityConfirmationOAuth2ClientSecret;
    }

    public boolean isExternallyHostedIdentityConfirmationTokenService() {
        return externallyHostedIdentityConfirmationTokenService;
    }

    public void setExternallyHostedIdentityConfirmationTokenService(boolean externallyHostedIdentityConfirmationTokenService) {
        this.externallyHostedIdentityConfirmationTokenService = externallyHostedIdentityConfirmationTokenService;
    }

    public String getIdentityConfirmationOAuth2AuthorizationServerEndpoint() {
        return identityConfirmationOAuth2AuthorizationServerEndpoint;
    }

    public void setIdentityConfirmationOAuth2AuthorizationServerEndpoint(String identityConfirmationOAuth2AuthorizationServerEndpoint) {
        this.identityConfirmationOAuth2AuthorizationServerEndpoint = identityConfirmationOAuth2AuthorizationServerEndpoint;
    }


}
