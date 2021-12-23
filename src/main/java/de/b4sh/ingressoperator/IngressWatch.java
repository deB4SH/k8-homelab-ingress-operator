package de.b4sh.ingressoperator;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import de.b4sh.ingressoperator.model.IngressDto;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
import io.kubernetes.client.openapi.models.V1Ingress;
import io.kubernetes.client.openapi.models.V1IngressList;
import io.kubernetes.client.openapi.models.V1IngressRule;
import io.kubernetes.client.openapi.models.V1IngressSpec;
import io.kubernetes.client.openapi.models.V1IngressStatus;
import io.kubernetes.client.openapi.models.V1LoadBalancerIngress;
import io.kubernetes.client.util.Config;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IngressWatch {
    private static final Logger log = Logger.getLogger(IngressWatch.class.getName());

    public static void main(final String[] args) throws IOException, ApiException {
        final ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);
        final NetworkingV1Api netApi = new NetworkingV1Api();
        final V1IngressList ingresses = netApi.listIngressForAllNamespaces(null,
                                                                           null,
                                                                           null,
                                                                           null,
                                                                           null,
                                                                           null,
                                                                           null,
                                                                           null,
                                                                           null,
                                                                           null);
        for (final V1Ingress ingress : ingresses.getItems()) {
            log.log(Level.FINE, String.format("Found ingress: %s",ingress.getMetadata().getName()));

            final List<String> hostUrls = getHostFromSpec(Objects.requireNonNull(ingress.getSpec()));
            if(hostUrls == null || hostUrls.isEmpty()){
                log.log(Level.INFO, String.format("Seems that there was nothing configured for ingress: %s. Jumping to next entry",ingress.getMetadata().getName()));
                continue;
            }
            final List<String> ipList = getIpFromStatus(Objects.requireNonNull(ingress.getStatus()));
            if(ipList == null || ipList.isEmpty()){
                log.log(Level.INFO, String.format("Seems that there was no status element or ip registered to ingress: %s. Jumping to next entry",ingress.getMetadata().getName()));
            }
            final JSONObject dto = new JSONObject(new IngressDto(hostUrls,ipList));

            //TODO: transfer dto to pi hole ip updater
        }
    }

    /**
     * Gets, if specification is avilable, all host urls defined in ingress.
     * @param spec specification of ingress
     * @return String list of host urls
     */
    private static List<String> getHostFromSpec(V1IngressSpec spec) {
        List<V1IngressRule> rules = spec.getRules();
        if (rules == null || rules.size() == 0) {
            log.warning("Seems that there is no Specification Rule available for Ingress. Can't look for any host url.");
            return null;
        }
        log.log(Level.FINE, "Ingress specification contains %s rules. Parsing them into list.", rules.size());
        List<String> urls = new ArrayList<>();
        for (V1IngressRule rule : rules) {
            log.log(Level.FINE, String.format("Adding %s to url list.", rule.getHost()));
            urls.add(rule.getHost());
        }
        log.log(Level.FINE, "Done processing ingress host spec. Found %s rules and urls.", urls.size());
        return urls;
    }

    private static List<String> getIpFromStatus(V1IngressStatus status){
        List<V1LoadBalancerIngress> ingresses = status.getLoadBalancer().getIngress();
        if(ingresses == null || ingresses.size() == 0){
            log.warning("Seems that there was no status available for ingress. Can't look for any registered ip.");
            return null;
        }
        List<String> ips = new ArrayList<>();
        for(V1LoadBalancerIngress ingress: ingresses){
            log.log(Level.FINE, String.format("Adding %s to ip list",ingress.getIp()));
            ips.add(ingress.getIp());
        }
        log.log(Level.FINE, "Done processing ingress status. Found %s ips.", ips.size());
        return ips;
    }
}
