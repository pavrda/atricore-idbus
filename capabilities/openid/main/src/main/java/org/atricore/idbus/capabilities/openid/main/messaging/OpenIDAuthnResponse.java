package org.atricore.idbus.capabilities.openid.main.messaging;

import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;

public class OpenIDAuthnResponse implements Serializable {
    private HashMap parameterMap = new HashMap<String,String>();
    private String receivingUrl;

    public HashMap getParameterMap() {
        return parameterMap;
    }

    public void setParameterMap(HashMap parameterMap) {
        this.parameterMap = parameterMap;
    }

    public String getReceivingUrl() {
        return receivingUrl;
    }

    public void setReceivingUrl(String receivingUrl) {
        this.receivingUrl = receivingUrl;
    }
}
