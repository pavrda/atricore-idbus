package org.atricore.idbus.capabilities.sso.main.select.internal;

import org.atricore.idbus.kernel.main.mediation.claim.UserClaimSet;
import org.atricore.idbus.common.sso._1_0.protocol.SelectEntityRequestType;

/**
 * Created with IntelliJ IDEA.
 * User: sgonzalez
 * Date: 12/4/12
 * Time: 4:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class EntitySelectionState implements java.io.Serializable {

    private UserClaimSet attributes;

    private SelectEntityRequestType request;

    private Integer attributesEndpointIdx;

    public UserClaimSet getAttributes() {
        return attributes;
    }

    public void setAttributes(UserClaimSet attributes) {
        this.attributes = attributes;
    }

    public SelectEntityRequestType getRequest() {
        return request;
    }

    public void setRequest(SelectEntityRequestType request) {
        this.request = request;
    }

    public Integer getAttributesEndpointIdx() {
        return attributesEndpointIdx;
    }

    public void setAttributesEndpointIdx(Integer attributesEndpointIdx) {
        this.attributesEndpointIdx = attributesEndpointIdx;
    }

}
