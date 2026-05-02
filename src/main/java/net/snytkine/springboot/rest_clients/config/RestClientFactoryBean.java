package net.snytkine.springboot.rest_clients.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.snytkine.springboot.rest_clients.config.properties.RestClientProperties;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

@Slf4j
public class RestClientFactoryBean implements FactoryBean<RestClient>, ApplicationContextAware {

  private final RestClientProperties.ClientConfig clientConfig;
  private ApplicationContext applicationContext;

  public RestClientFactoryBean(RestClientProperties.ClientConfig clientConfig) {
    this.clientConfig = clientConfig;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @Override
  public RestClient getObject() throws Exception {
    log.debug("Creating RestClient bean '{}'", clientConfig.getName());

    RestClient.Builder builder = RestClient.builder();

    // Set base URL
    if (clientConfig.getBaseUrl() != null && !clientConfig.getBaseUrl().isBlank()) {
      log.debug(
          "Setting base URL for RestClient '{}': {}",
          clientConfig.getName(),
          clientConfig.getBaseUrl());
      builder.baseUrl(clientConfig.getBaseUrl());
    }

    // Configure request factory (custom or default)
    org.springframework.http.client.ClientHttpRequestFactory requestFactory =
        resolveRequestFactory();
    if (requestFactory != null) {
      builder.requestFactory(requestFactory);
    }

    // Resolve and apply interceptors
    if (clientConfig.getInterceptors() != null && !clientConfig.getInterceptors().isEmpty()) {
      log.debug(
          "Resolving {} interceptor(s) for RestClient '{}'",
          clientConfig.getInterceptors().size(),
          clientConfig.getName());
      List<ClientHttpRequestInterceptor> interceptors = resolveInterceptors();
      builder.requestInterceptors(interceptorsList -> interceptorsList.addAll(interceptors));
      log.debug(
          "Successfully added {} interceptor(s) to RestClient '{}'",
          interceptors.size(),
          clientConfig.getName());
    }

    log.debug("Successfully created RestClient bean '{}'", clientConfig.getName());
    return builder.build();
  }

  private org.springframework.http.client.ClientHttpRequestFactory resolveRequestFactory() {
    // If custom request factory bean is specified, use it
    if (clientConfig.getRequestFactoryBean() != null
        && !clientConfig.getRequestFactoryBean().isBlank()) {
      log.debug(
          "Resolving custom request factory bean '{}' for RestClient '{}'",
          clientConfig.getRequestFactoryBean(),
          clientConfig.getName());
      try {
        org.springframework.http.client.ClientHttpRequestFactory factory =
            applicationContext.getBean(
                clientConfig.getRequestFactoryBean(),
                org.springframework.http.client.ClientHttpRequestFactory.class);
        log.debug(
            "Successfully resolved custom request factory bean '{}' of type {} for RestClient '{}'",
            clientConfig.getRequestFactoryBean(),
            factory.getClass().getSimpleName(),
            clientConfig.getName());
        return factory;
      } catch (BeansException e) {
        log.error(
            "Failed to resolve request factory bean '{}' for RestClient '{}': {}",
            clientConfig.getRequestFactoryBean(),
            clientConfig.getName(),
            e.getMessage());
        throw new IllegalStateException(
            "Failed to resolve request factory bean '"
                + clientConfig.getRequestFactoryBean()
                + "' for RestClient '"
                + clientConfig.getName()
                + "'",
            e);
      }
    }

    // Use default JdkClientHttpRequestFactory with timeout configuration
    if (clientConfig.getConnectTimeout() != null || clientConfig.getReadTimeout() != null) {
      log.debug(
          "Creating default JdkClientHttpRequestFactory for RestClient '{}' with connectTimeout={}ms, readTimeout={}ms",
          clientConfig.getName(),
          clientConfig.getConnectTimeout(),
          clientConfig.getReadTimeout());

      java.net.http.HttpClient.Builder httpClientBuilder = java.net.http.HttpClient.newBuilder();

      if (clientConfig.getConnectTimeout() != null) {
        httpClientBuilder.connectTimeout(Duration.ofMillis(clientConfig.getConnectTimeout()));
      }

      org.springframework.http.client.JdkClientHttpRequestFactory requestFactory =
          new org.springframework.http.client.JdkClientHttpRequestFactory(
              httpClientBuilder.build());

      if (clientConfig.getReadTimeout() != null) {
        requestFactory.setReadTimeout(Duration.ofMillis(clientConfig.getReadTimeout()));
      }

      log.debug(
          "Successfully created default JdkClientHttpRequestFactory for RestClient '{}'",
          clientConfig.getName());
      return requestFactory;
    }

    log.debug(
        "No request factory configuration specified for RestClient '{}', using RestClient default",
        clientConfig.getName());
    return null;
  }

  private List<ClientHttpRequestInterceptor> resolveInterceptors() {
    List<RestClientProperties.InterceptorConfig> interceptorConfigs =
        new ArrayList<>(clientConfig.getInterceptors());

    // Sort by order
    interceptorConfigs.sort(
        Comparator.comparing(
            config -> config.getOrder() != null ? config.getOrder() : Integer.MAX_VALUE));

    List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
    for (RestClientProperties.InterceptorConfig config : interceptorConfigs) {
      if (config.getBeanName() == null || config.getBeanName().isBlank()) {
        log.error(
            "Interceptor configuration for RestClient '{}' is missing required 'bean-name' property",
            clientConfig.getName());
        throw new IllegalArgumentException(
            "Interceptor configuration for RestClient '"
                + clientConfig.getName()
                + "' must have a non-empty 'bean-name' property");
      }

      log.debug(
          "Resolving interceptor bean '{}' with order {} for RestClient '{}'",
          config.getBeanName(),
          config.getOrder(),
          clientConfig.getName());

      try {
        ClientHttpRequestInterceptor interceptor =
            applicationContext.getBean(config.getBeanName(), ClientHttpRequestInterceptor.class);
        interceptors.add(interceptor);
        log.debug(
            "Successfully resolved interceptor bean '{}' of type {} for RestClient '{}'",
            config.getBeanName(),
            interceptor.getClass().getSimpleName(),
            clientConfig.getName());
      } catch (BeansException e) {
        log.error(
            "Failed to resolve interceptor bean '{}' for RestClient '{}': {}",
            config.getBeanName(),
            clientConfig.getName(),
            e.getMessage());
        throw new IllegalStateException(
            "Failed to resolve interceptor bean '"
                + config.getBeanName()
                + "' for RestClient '"
                + clientConfig.getName()
                + "'",
            e);
      }
    }

    return interceptors;
  }

  @Override
  public Class<?> getObjectType() {
    return RestClient.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }
}
