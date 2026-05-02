package net.snytkine.springboot.rest_clients.config.properties;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for declarative RestClient bean creation.
 *
 * <p>This class binds configuration properties from the {@code rest-clients} prefix in the
 * application configuration files (application.yml, application.properties, etc.). It enables
 * declarative configuration of multiple RestClient instances without writing Java configuration
 * code.
 *
 * <p><b>Configuration Example:</b>
 *
 * <pre>
 * rest-clients:
 *   clients:
 *     - name: githubClient
 *       base-url: https://api.github.com
 *       connect-timeout: 5000
 *       read-timeout: 10000
 *       interceptors:
 *         - bean-name: loggingInterceptor
 *           order: 1
 *         - bean-name: authInterceptor
 *           order: 2
 *     - name: jsonPlaceholderClient
 *       base-url: https://jsonplaceholder.typicode.com
 *       request-factory-bean: customSslRequestFactory
 * </pre>
 *
 * <p><b>Usage in Application:</b>
 *
 * <p>Once configured, the RestClient beans can be injected using {@code @Qualifier} or by name:
 *
 * <pre>
 * {@code @Autowired}
 * {@code @Qualifier("githubClient")}
 * private RestClient githubClient;
 * </pre>
 *
 * <p>Or using constructor injection:
 *
 * <pre>
 * {@code @Service}
 * public class GitHubService {
 *   private final RestClient githubClient;
 *
 *   public GitHubService({@code @Qualifier("githubClient")} RestClient githubClient) {
 *     this.githubClient = githubClient;
 *   }
 * }
 * </pre>
 *
 * <p><b>Bean Registration:</b>
 *
 * <p>These properties are processed by {@link
 * net.snytkine.springboot.rest_clients.config.RestClientBeanDefinitionRegistrar} during the Spring
 * context initialization phase to dynamically register RestClient beans.
 *
 * <p><b>IDE Support:</b>
 *
 * <p>When the {@code spring-boot-configuration-processor} dependency is included, IDEs will provide
 * auto-completion and validation for these configuration properties in YAML/properties files.
 *
 * @see ClientConfig
 * @see InterceptorConfig
 * @see net.snytkine.springboot.rest_clients.config.RestClientBeanDefinitionRegistrar
 * @see net.snytkine.springboot.rest_clients.config.RestClientFactoryBean
 * @since 0.0.1
 */
@ConfigurationProperties(prefix = "rest-clients")
public class RestClientProperties {

  /**
   * List of RestClient configurations. Each configuration will result in a separate RestClient bean
   * being created and registered in the Spring ApplicationContext.
   */
  private List<ClientConfig> clients = new ArrayList<>();

  /**
   * Gets the list of RestClient configurations.
   *
   * @return the list of client configurations
   */
  public List<ClientConfig> getClients() {
    return clients;
  }

  /**
   * Sets the list of RestClient configurations.
   *
   * <p>This method is called by Spring during property binding to populate the client
   * configurations from the application configuration files.
   *
   * @param clients the list of client configurations to set
   */
  public void setClients(List<ClientConfig> clients) {
    this.clients = clients;
  }

  /**
   * Configuration for a single RestClient instance.
   *
   * <p>Each ClientConfig represents the configuration for one RestClient bean that will be created
   * and registered in the Spring ApplicationContext. The configuration includes the bean name, base
   * URL, timeout settings, optional custom request factory, and optional interceptors.
   *
   * <p><b>Required Properties:</b>
   *
   * <ul>
   *   <li>{@code name} - The bean name/qualifier for the RestClient (required, non-empty)
   * </ul>
   *
   * <p><b>Optional Properties:</b>
   *
   * <ul>
   *   <li>{@code base-url} - The base URL for all requests made by this client
   *   <li>{@code connect-timeout} - Connection timeout in milliseconds (default: 5000)
   *   <li>{@code read-timeout} - Read timeout in milliseconds (default: 10000)
   *   <li>{@code request-factory-bean} - Name of a custom ClientHttpRequestFactory bean
   *   <li>{@code interceptors} - List of interceptor configurations to apply to requests
   * </ul>
   *
   * <p><b>Configuration Example:</b>
   *
   * <pre>
   * rest-clients:
   *   clients:
   *     - name: apiClient
   *       base-url: https://api.example.com
   *       connect-timeout: 3000
   *       read-timeout: 5000
   *       interceptors:
   *         - bean-name: authInterceptor
   *           order: 1
   * </pre>
   *
   * @see InterceptorConfig
   * @see net.snytkine.springboot.rest_clients.config.RestClientFactoryBean
   */
  public static class ClientConfig {

    /** The bean name for the RestClient. This name is used as the bean qualifier for injection. */
    private String name;

    /**
     * The base URL for the RestClient. All relative URLs in requests will be resolved against this
     * base URL. Optional - can be null if requests will use absolute URLs.
     */
    private String baseUrl;

    /**
     * Connection timeout in milliseconds. This is the maximum time to wait when establishing a
     * connection to the remote server. Default: 5000ms (5 seconds).
     */
    private Integer connectTimeout = 5000;

    /**
     * Read timeout in milliseconds. This is the maximum time to wait for data to be received after
     * the connection is established. Default: 10000ms (10 seconds).
     */
    private Integer readTimeout = 10000;

    /**
     * Name of a custom ClientHttpRequestFactory bean to use instead of the default
     * JdkClientHttpRequestFactory. This allows advanced use cases such as custom SSL configuration,
     * connection pooling, or proxy settings. Optional - if not specified, a default
     * JdkClientHttpRequestFactory is created with the configured timeouts.
     */
    private String requestFactoryBean;

    /**
     * List of interceptor configurations to apply to this RestClient. Interceptors are applied in
     * order based on their {@code order} property. Optional - can be empty if no interceptors are
     * needed.
     */
    private List<InterceptorConfig> interceptors = new ArrayList<>();

    /**
     * Gets the bean name for the RestClient.
     *
     * @return the bean name
     */
    public String getName() {
      return name;
    }

    /**
     * Sets the bean name for the RestClient.
     *
     * <p>This name is used as both the bean name and qualifier for dependency injection. It must be
     * unique across all RestClient configurations.
     *
     * @param name the bean name (required, must be non-empty)
     */
    public void setName(String name) {
      this.name = name;
    }

    /**
     * Gets the base URL for the RestClient.
     *
     * @return the base URL, or null if not configured
     */
    public String getBaseUrl() {
      return baseUrl;
    }

    /**
     * Sets the base URL for the RestClient.
     *
     * <p>All relative URLs in requests will be resolved against this base URL. For example, if the
     * base URL is "https://api.example.com" and a request is made to "/users", the full URL will be
     * "https://api.example.com/users".
     *
     * @param baseUrl the base URL (optional)
     */
    public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
    }

    /**
     * Gets the connection timeout in milliseconds.
     *
     * @return the connection timeout
     */
    public Integer getConnectTimeout() {
      return connectTimeout;
    }

    /**
     * Sets the connection timeout in milliseconds.
     *
     * <p>This is the maximum time to wait when establishing a TCP connection to the remote server.
     * If the connection cannot be established within this time, the request fails with a timeout
     * exception.
     *
     * @param connectTimeout the connection timeout in milliseconds (default: 5000)
     */
    public void setConnectTimeout(Integer connectTimeout) {
      this.connectTimeout = connectTimeout;
    }

    /**
     * Gets the read timeout in milliseconds.
     *
     * @return the read timeout
     */
    public Integer getReadTimeout() {
      return readTimeout;
    }

    /**
     * Sets the read timeout in milliseconds.
     *
     * <p>This is the maximum time to wait for data to be received after the connection is
     * established. If no data is received within this time, the request fails with a timeout
     * exception.
     *
     * @param readTimeout the read timeout in milliseconds (default: 10000)
     */
    public void setReadTimeout(Integer readTimeout) {
      this.readTimeout = readTimeout;
    }

    /**
     * Gets the list of interceptor configurations.
     *
     * @return the list of interceptor configurations
     */
    public List<InterceptorConfig> getInterceptors() {
      return interceptors;
    }

    /**
     * Sets the list of interceptor configurations.
     *
     * <p>Interceptors are applied to all requests made by this RestClient in the order specified by
     * their {@code order} property. Each interceptor must reference an existing bean in the
     * ApplicationContext.
     *
     * @param interceptors the list of interceptor configurations (optional)
     */
    public void setInterceptors(List<InterceptorConfig> interceptors) {
      this.interceptors = interceptors;
    }

    /**
     * Gets the name of the custom request factory bean.
     *
     * @return the request factory bean name, or null if using default
     */
    public String getRequestFactoryBean() {
      return requestFactoryBean;
    }

    /**
     * Sets the name of a custom ClientHttpRequestFactory bean to use.
     *
     * <p>This allows applications to provide custom request factory implementations for advanced
     * use cases such as:
     *
     * <ul>
     *   <li>Custom SSL/TLS configuration with certificate bundles
     *   <li>Connection pooling with Apache HttpComponents
     *   <li>Proxy server configuration
     *   <li>Custom authentication schemes
     * </ul>
     *
     * <p>If not specified, a default JdkClientHttpRequestFactory is created with the configured
     * timeouts.
     *
     * @param requestFactoryBean the name of a ClientHttpRequestFactory bean (optional)
     */
    public void setRequestFactoryBean(String requestFactoryBean) {
      this.requestFactoryBean = requestFactoryBean;
    }
  }

  /**
   * Configuration for a single interceptor to be applied to a RestClient.
   *
   * <p>Interceptors allow you to intercept and modify HTTP requests and responses, or add
   * cross-cutting concerns such as logging, authentication, metrics, or error handling. Each
   * InterceptorConfig references an existing {@link
   * org.springframework.http.client.ClientHttpRequestInterceptor} bean by name and specifies its
   * execution order.
   *
   * <p><b>Required Properties:</b>
   *
   * <ul>
   *   <li>{@code bean-name} - The name of a ClientHttpRequestInterceptor bean in the
   *       ApplicationContext (required, non-empty)
   * </ul>
   *
   * <p><b>Optional Properties:</b>
   *
   * <ul>
   *   <li>{@code order} - The execution order (lower values execute first, default:
   *       Integer.MAX_VALUE)
   * </ul>
   *
   * <p><b>Execution Order:</b>
   *
   * <p>Interceptors are executed in ascending order based on their {@code order} value. Lower
   * values have higher priority and execute first in the request chain. Interceptors without an
   * explicit order are assigned {@link Integer#MAX_VALUE}, placing them last.
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
   *     # no order specified, defaults to Integer.MAX_VALUE
   * </pre>
   *
   * <p>The execution order will be: loggingInterceptor (1) → authInterceptor (2) →
   * metricsInterceptor (MAX_VALUE)
   *
   * <p><b>Creating Interceptor Beans:</b>
   *
   * <p>Interceptors must be registered as Spring beans that implement {@link
   * org.springframework.http.client.ClientHttpRequestInterceptor}:
   *
   * <pre>
   * {@code @Component("loggingInterceptor")}
   * public class LoggingInterceptor implements ClientHttpRequestInterceptor {
   *   {@code @Override}
   *   public ClientHttpResponse intercept(
   *       HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
   *       throws IOException {
   *     // Log request details
   *     log.info("Request: {} {}", request.getMethod(), request.getURI());
   *     // Execute the request
   *     ClientHttpResponse response = execution.execute(request, body);
   *     // Log response details
   *     log.info("Response: {}", response.getStatusCode());
   *     return response;
   *   }
   * }
   * </pre>
   *
   * @see org.springframework.http.client.ClientHttpRequestInterceptor
   * @see net.snytkine.springboot.rest_clients.config.RestClientFactoryBean#resolveInterceptors()
   */
  public static class InterceptorConfig {

    /**
     * The name of the ClientHttpRequestInterceptor bean to apply. This bean must exist in the
     * Spring ApplicationContext. Required - must be non-empty.
     */
    private String beanName;

    /**
     * The execution order for this interceptor. Lower values have higher priority and execute first
     * in the request chain. Optional - if not specified, defaults to Integer.MAX_VALUE (lowest
     * priority, executes last).
     */
    private Integer order;

    /**
     * Gets the bean name of the interceptor.
     *
     * @return the bean name
     */
    public String getBeanName() {
      return beanName;
    }

    /**
     * Sets the bean name of the ClientHttpRequestInterceptor to apply.
     *
     * <p>The bean with this name must exist in the ApplicationContext and implement {@link
     * org.springframework.http.client.ClientHttpRequestInterceptor}.
     *
     * @param beanName the bean name (required, must be non-empty)
     */
    public void setBeanName(String beanName) {
      this.beanName = beanName;
    }

    /**
     * Gets the execution order of this interceptor.
     *
     * @return the order value, or null if not specified
     */
    public Integer getOrder() {
      return order;
    }

    /**
     * Sets the execution order for this interceptor.
     *
     * <p>Interceptors are executed in ascending order. Lower values have higher priority and
     * execute first. For example, an interceptor with order=1 will execute before an interceptor
     * with order=2.
     *
     * <p>If not specified, the interceptor is assigned {@link Integer#MAX_VALUE}, placing it last
     * in the execution chain.
     *
     * @param order the execution order (optional, defaults to Integer.MAX_VALUE)
     */
    public void setOrder(Integer order) {
      this.order = order;
    }
  }
}
