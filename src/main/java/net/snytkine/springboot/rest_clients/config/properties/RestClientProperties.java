package net.snytkine.springboot.rest_clients.config.properties;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rest-clients")
public class RestClientProperties {

  private List<ClientConfig> clients = new ArrayList<>();

  public List<ClientConfig> getClients() {
    return clients;
  }

  public void setClients(List<ClientConfig> clients) {
    this.clients = clients;
  }

  public static class ClientConfig {
    private String name;
    private String baseUrl;
    private Integer connectTimeout = 5000;
    private Integer readTimeout = 10000;
    private String requestFactoryBean;
    private List<InterceptorConfig> interceptors = new ArrayList<>();

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
    }

    public Integer getConnectTimeout() {
      return connectTimeout;
    }

    public void setConnectTimeout(Integer connectTimeout) {
      this.connectTimeout = connectTimeout;
    }

    public Integer getReadTimeout() {
      return readTimeout;
    }

    public void setReadTimeout(Integer readTimeout) {
      this.readTimeout = readTimeout;
    }

    public List<InterceptorConfig> getInterceptors() {
      return interceptors;
    }

    public void setInterceptors(List<InterceptorConfig> interceptors) {
      this.interceptors = interceptors;
    }

    public String getRequestFactoryBean() {
      return requestFactoryBean;
    }

    public void setRequestFactoryBean(String requestFactoryBean) {
      this.requestFactoryBean = requestFactoryBean;
    }
  }

  public static class InterceptorConfig {
    private String beanName;
    private Integer order;

    public String getBeanName() {
      return beanName;
    }

    public void setBeanName(String beanName) {
      this.beanName = beanName;
    }

    public Integer getOrder() {
      return order;
    }

    public void setOrder(Integer order) {
      this.order = order;
    }
  }
}
