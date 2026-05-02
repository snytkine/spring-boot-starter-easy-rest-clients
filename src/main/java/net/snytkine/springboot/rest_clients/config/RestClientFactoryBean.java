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

/**
 * Factory bean that creates and configures RestClient instances based on configuration properties.
 *
 * <p>This factory bean is registered by {@link RestClientBeanDefinitionRegistrar} for each client
 * configuration defined in the application properties. It implements {@link FactoryBean} to control
 * the bean creation process and {@link ApplicationContextAware} to gain access to the Spring
 * application context for resolving interceptors and custom request factories.
 *
 * <p><b>Lifecycle:</b>
 *
 * <ol>
 *   <li>The factory bean is instantiated with a {@link RestClientProperties.ClientConfig} during
 *       bean definition registration
 *   <li>Spring calls {@link #setApplicationContext} to provide the ApplicationContext reference
 *   <li>When the RestClient bean is first requested, {@link #getObject()} is called to create the
 *       actual RestClient instance
 *   <li>The created RestClient is cached as a singleton (see {@link #isSingleton()})
 * </ol>
 *
 * <p><b>RestClient Configuration:</b>
 *
 * <p>The factory bean configures the RestClient with the following features:
 *
 * <ul>
 *   <li><b>Base URL:</b> Sets the base URL from the {@code base-url} configuration property
 *   <li><b>Request Factory:</b> Uses a custom {@link
 *       org.springframework.http.client.ClientHttpRequestFactory} if specified via {@code
 *       request-factory-bean}, otherwise creates a default {@link
 *       org.springframework.http.client.JdkClientHttpRequestFactory} with configured timeouts
 *   <li><b>Timeouts:</b> Configures connect and read timeouts (defaults: 5000ms and 10000ms)
 *   <li><b>Interceptors:</b> Resolves and applies {@link ClientHttpRequestInterceptor} beans in
 *       order based on the {@code order} property
 * </ul>
 *
 * <p><b>Default Request Factory:</b>
 *
 * <p>When no custom request factory is specified, the factory bean creates a {@link
 * org.springframework.http.client.JdkClientHttpRequestFactory} which supports all standard HTTP
 * methods including GET, POST, PUT, DELETE, PATCH, HEAD, and OPTIONS. This implementation:
 *
 * <ul>
 *   <li>Uses Java's built-in {@link java.net.http.HttpClient} (no external dependencies)
 *   <li>Supports HTTP/1.1 and HTTP/2
 *   <li>Configures connect and read timeouts from the client configuration
 * </ul>
 *
 * <p><b>Custom Request Factory:</b>
 *
 * <p>Applications can provide custom {@link
 * org.springframework.http.client.ClientHttpRequestFactory} beans for advanced use cases such as:
 *
 * <ul>
 *   <li>SSL/TLS configuration with custom certificate bundles
 *   <li>Connection pooling with Apache HttpComponents
 *   <li>Proxy configuration
 *   <li>Custom authentication schemes
 * </ul>
 *
 * <p>Example configuration with custom request factory:
 *
 * <pre>
 * rest-clients:
 *   clients:
 *     - name: secureClient
 *       base-url: https://api.example.com
 *       request-factory-bean: customSslRequestFactory
 * </pre>
 *
 * <p><b>Interceptor Resolution:</b>
 *
 * <p>Interceptors are resolved from the ApplicationContext by bean name and applied in order based
 * on the {@code order} property (lower values have higher priority). Interceptors without an
 * explicit order are assigned {@link Integer#MAX_VALUE} to place them last.
 *
 * <p>Example configuration with ordered interceptors:
 *
 * <pre>
 * rest-clients:
 *   clients:
 *     - name: apiClient
 *       base-url: https://api.example.com
 *       interceptors:
 *         - bean-name: loggingInterceptor
 *           order: 1
 *         - bean-name: authInterceptor
 *           order: 2
 * </pre>
 *
 * <p><b>Error Handling:</b>
 *
 * <ul>
 *   <li>If an interceptor is missing the {@code bean-name} property, an {@link
 *       IllegalArgumentException} is thrown
 *   <li>If an interceptor or request factory bean cannot be resolved from the ApplicationContext,
 *       an {@link IllegalStateException} is thrown
 *   <li>All errors are logged at the error level before throwing exceptions
 * </ul>
 *
 * @see RestClientBeanDefinitionRegistrar
 * @see RestClientProperties.ClientConfig
 * @see RestClient
 * @since 0.0.1
 */
@Slf4j
public class RestClientFactoryBean implements FactoryBean<RestClient>, ApplicationContextAware {

  private final RestClientProperties.ClientConfig clientConfig;
  private ApplicationContext applicationContext;

  /**
   * Constructs a new RestClientFactoryBean with the specified client configuration.
   *
   * <p>This constructor is called by {@link RestClientBeanDefinitionRegistrar} during bean
   * definition registration. The provided configuration contains all the settings needed to create
   * and configure the RestClient instance.
   *
   * @param clientConfig the client configuration containing base URL, timeouts, interceptors, and
   *     request factory settings
   */
  public RestClientFactoryBean(RestClientProperties.ClientConfig clientConfig) {
    this.clientConfig = clientConfig;
  }

  /**
   * Sets the Spring ApplicationContext for this factory bean.
   *
   * <p>This method is called by Spring after the factory bean is instantiated but before {@link
   * #getObject()} is called. The ApplicationContext is used to resolve interceptor beans and custom
   * request factory beans by name.
   *
   * @param applicationContext the Spring ApplicationContext containing all registered beans
   * @throws BeansException if the ApplicationContext cannot be set
   */
  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  /**
   * Creates and returns a fully configured RestClient instance.
   *
   * <p>This method is called by Spring when the RestClient bean is first requested. It performs the
   * following steps to create and configure the RestClient:
   *
   * <ol>
   *   <li>Creates a new {@link RestClient.Builder}
   *   <li>Sets the base URL if configured
   *   <li>Resolves and configures the request factory (custom or default)
   *   <li>Resolves interceptors from the ApplicationContext and applies them in order
   *   <li>Builds and returns the configured RestClient instance
   * </ol>
   *
   * <p>The created RestClient is cached as a singleton (see {@link #isSingleton()}), so this method
   * is only called once per factory bean instance.
   *
   * <p><b>Debug Logging:</b>
   *
   * <p>This method logs the following at debug level:
   *
   * <ul>
   *   <li>Start of RestClient creation with client name
   *   <li>Base URL being set (if configured)
   *   <li>Number of interceptors being resolved
   *   <li>Number of interceptors successfully added
   *   <li>Successful completion of RestClient creation
   * </ul>
   *
   * @return a fully configured RestClient instance
   * @throws IllegalArgumentException if an interceptor configuration is missing the required {@code
   *     bean-name} property
   * @throws IllegalStateException if an interceptor or request factory bean cannot be resolved from
   *     the ApplicationContext
   * @see #resolveRequestFactory()
   * @see #resolveInterceptors()
   */
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

  /**
   * Resolves and configures the {@link org.springframework.http.client.ClientHttpRequestFactory}
   * for the RestClient.
   *
   * <p>This method determines which request factory implementation to use based on the client
   * configuration. It follows this priority order:
   *
   * <ol>
   *   <li><b>Custom Factory:</b> If {@code request-factory-bean} is specified, resolves and returns
   *       that bean from the ApplicationContext
   *   <li><b>Default Factory with Timeouts:</b> If connect or read timeouts are configured, creates
   *       a {@link org.springframework.http.client.JdkClientHttpRequestFactory} with those timeouts
   *   <li><b>No Factory:</b> If no custom factory or timeouts are specified, returns null to use
   *       RestClient's default factory
   * </ol>
   *
   * <p><b>Custom Request Factory:</b>
   *
   * <p>When a custom request factory bean is specified, it must exist in the ApplicationContext and
   * implement {@link org.springframework.http.client.ClientHttpRequestFactory}. This allows
   * applications to provide custom implementations for advanced use cases such as:
   *
   * <ul>
   *   <li>SSL/TLS configuration with custom certificate bundles
   *   <li>Connection pooling with Apache HttpComponents
   *   <li>Proxy configuration
   *   <li>Custom authentication schemes
   * </ul>
   *
   * <p><b>Default JdkClientHttpRequestFactory:</b>
   *
   * <p>When no custom factory is specified, the default {@link
   * org.springframework.http.client.JdkClientHttpRequestFactory} is used. This implementation:
   *
   * <ul>
   *   <li>Uses Java's built-in {@link java.net.http.HttpClient} (no external dependencies)
   *   <li>Supports all standard HTTP methods: GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS
   *   <li>Supports HTTP/1.1 and HTTP/2
   *   <li>Configures connect timeout via {@link java.net.http.HttpClient.Builder#connectTimeout}
   *   <li>Configures read timeout via {@link
   *       org.springframework.http.client.JdkClientHttpRequestFactory#setReadTimeout}
   * </ul>
   *
   * <p><b>Debug Logging:</b>
   *
   * <p>This method logs the following at debug level:
   *
   * <ul>
   *   <li>When resolving a custom request factory bean (before resolution)
   *   <li>When successfully resolving a custom request factory (with factory type)
   *   <li>When creating a default factory (with configured timeouts)
   *   <li>When successfully creating a default factory
   *   <li>When no factory configuration is specified
   * </ul>
   *
   * @return the configured ClientHttpRequestFactory, or null to use RestClient's default
   * @throws IllegalStateException if a custom request factory bean is specified but cannot be
   *     resolved from the ApplicationContext
   */
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

  /**
   * Resolves and orders the {@link ClientHttpRequestInterceptor} beans for the RestClient.
   *
   * <p>This method processes the interceptor configurations defined for the client and resolves the
   * actual interceptor beans from the ApplicationContext. It performs the following steps:
   *
   * <ol>
   *   <li>Creates a copy of the interceptor configurations list
   *   <li>Sorts the interceptors by the {@code order} property (lower values have higher priority)
   *   <li>For each interceptor configuration:
   *       <ul>
   *         <li>Validates that the {@code bean-name} property is present and non-empty
   *         <li>Resolves the interceptor bean from the ApplicationContext by name
   *         <li>Adds the resolved interceptor to the result list
   *       </ul>
   * </ol>
   *
   * <p><b>Interceptor Ordering:</b>
   *
   * <p>Interceptors are sorted by their {@code order} property before being applied to the
   * RestClient. The ordering follows these rules:
   *
   * <ul>
   *   <li>Lower order values have higher priority and execute first in the request chain
   *   <li>Interceptors without an explicit {@code order} are assigned {@link Integer#MAX_VALUE},
   *       placing them last in the chain
   *   <li>Interceptors with the same order value maintain their configuration order
   * </ul>
   *
   * <p>For example, with this configuration:
   *
   * <pre>
   * interceptors:
   *   - bean-name: loggingInterceptor
   *     order: 1
   *   - bean-name: authInterceptor
   *     order: 2
   *   - bean-name: metricsInterceptor
   * </pre>
   *
   * <p>The execution order will be: loggingInterceptor → authInterceptor → metricsInterceptor
   *
   * <p><b>Validation:</b>
   *
   * <p>Each interceptor configuration must have a non-empty {@code bean-name} property. If this
   * validation fails, an error is logged and an {@link IllegalArgumentException} is thrown to fail
   * fast rather than silently skipping the interceptor.
   *
   * <p><b>Bean Resolution:</b>
   *
   * <p>Each interceptor is resolved from the ApplicationContext by name and must implement {@link
   * ClientHttpRequestInterceptor}. If the bean cannot be found or is of the wrong type, an error is
   * logged and an {@link IllegalStateException} is thrown.
   *
   * <p><b>Debug Logging:</b>
   *
   * <p>This method logs the following at debug level:
   *
   * <ul>
   *   <li>When resolving each interceptor bean (with bean name and order)
   *   <li>When successfully resolving each interceptor (with bean name and type)
   * </ul>
   *
   * @return a list of resolved and ordered ClientHttpRequestInterceptor beans
   * @throws IllegalArgumentException if any interceptor configuration is missing the required
   *     {@code bean-name} property
   * @throws IllegalStateException if any interceptor bean cannot be resolved from the
   *     ApplicationContext
   */
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

  /**
   * Returns the type of object that this FactoryBean creates.
   *
   * <p>This method is called by Spring to determine the type of bean produced by this factory. It
   * is used for type-based autowiring and bean type checking.
   *
   * @return the class {@link RestClient}
   */
  @Override
  public Class<?> getObjectType() {
    return RestClient.class;
  }

  /**
   * Indicates that this FactoryBean produces singleton beans.
   *
   * <p>When this method returns {@code true}, Spring caches the RestClient instance created by
   * {@link #getObject()} and returns the same instance for all subsequent requests. This means
   * {@link #getObject()} is only called once per factory bean instance.
   *
   * <p>RestClient beans are safe to use as singletons because:
   *
   * <ul>
   *   <li>RestClient is thread-safe and can be shared across multiple threads
   *   <li>The configuration (base URL, timeouts, interceptors) is immutable after creation
   *   <li>Creating a new RestClient for each request would be inefficient
   * </ul>
   *
   * @return {@code true} to indicate that this factory creates singleton beans
   */
  @Override
  public boolean isSingleton() {
    return true;
  }
}
