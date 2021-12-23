package de.b4sh.ingressoperator.model;

import org.json.JSONObject;

import java.util.List;

public final class IngressDto {
    private final List<String> url;
    private final List<String> ip;

    public IngressDto(List<String> url, List<String> ip) {
        this.url = url;
        this.ip = ip;
    }

    public List<String> getUrl() {
        return url;
    }

    public List<String> getIp() {
        return ip;
    }
}
