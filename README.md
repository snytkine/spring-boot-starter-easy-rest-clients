# REST Clients Spring Boot Starter

A Spring Boot starter library that enables declarative configuration of multiple `RestClient` instances through YAML configuration files.

## Features

- **Declarative Configuration**: Define multiple REST clients in `application.yaml`
- **Custom Timeouts**: Configure connect and read timeouts per client
- **Interceptor Support**: Add ordered interceptors to each client
- **Default Request Factory**: Uses `JdkClientHttpRequestFactory` by default, supporting all standard HTTP methods (GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS) and any other HTTP methods supported by the underlying Java HttpClient
- **Custom Request Factories**: Optional support for custom `ClientHttpRequestFactory` implementations (e.g., for custom SSL, connection pooling)
- **Auto-Configuration**: Automatically registers `RestClient` beans on application startup

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>net.snytkine.springboot</groupId>
    <artifactId>rest-clients</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## Quick Start

### Basic Configuration

Define your REST clients in `application.yaml`:

```yaml
rest-clients:
  clients:
    - name: userServiceClient
      base-url: https://api.example.com/users
      connect-timeout: 5000
      read-timeout: 10000
```

This configuration will create a `RestClient` bean named `userServiceClient` using the default `JdkClientHttpRequestFactory` with the specified timeouts. The default implementation supports all standard HTTP methods (GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS) and any other methods supported by Java's HttpClient.

### Inject and Use

```java
@Service
public class UserService {

    private final RestClient userServiceClient;

    public UserService(@Qualifier("userServiceClient") RestClient userServiceClient) {
        this.userServiceClient = userServiceClient;
    }

    public User getUser(Long id) {
        return userServiceClient.get()
            .uri("/{id}", id)
            .retrieve()
            .body(User.class);
    }
}
```

## Configuration Reference

### Client Configuration Properties

| Property | Type | Required | Default | Description |
|----------|------|----------|---------|-------------|
| `name` | String | Yes | - | Bean name for the RestClient |
| `base-url` | String | No | - | Base URL for all requests |
| `connect-timeout` | Integer | No | 5000 | Connection timeout in milliseconds |
| `read-timeout` | Integer | No | 10000 | Read timeout in milliseconds |
| `request-factory-bean` | String | No | - | Custom `ClientHttpRequestFactory` bean name |
| `interceptors` | List | No | - | List of interceptor configurations |

### Interceptor Configuration

The `interceptors` property is a list of interceptor bean names (strings). Interceptors are applied in the order they are listed.

## Advanced Usage

### Multiple Clients

```yaml
rest-clients:
  clients:
    - name: userServiceClient
      base-url: https://api.example.com/users
      connect-timeout: 5000
      read-timeout: 10000

    - name: paymentServiceClient
      base-url: https://payments.example.com/api
      connect-timeout: 3000
      read-timeout: 15000
```

### Custom Interceptors

Create a component that implements `ClientHttpRequestInterceptor`:

```java
@Component("loggingInterceptor")
public class LoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        log.info("Request: {} {}", request.getMethod(), request.getURI());

        ClientHttpResponse response = execution.execute(request, body);

        log.info("Response: {}", response.getStatusCode());

        return response;
    }
}
```

Configure the interceptor in `application.yaml`:

```yaml
rest-clients:
  clients:
    - name: userServiceClient
      base-url: https://api.example.com/users
      interceptors:
        - loggingInterceptor
```

### Multiple Interceptors

Interceptors are applied in the order they are listed:

```java
@Component("authInterceptor")
public class AuthInterceptor implements ClientHttpRequestInterceptor {
    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        request.getHeaders().setBearerAuth("your-token");
        return execution.execute(request, body);
    }
}

@Component("loggingInterceptor")
public class LoggingInterceptor implements ClientHttpRequestInterceptor {
    // ... implementation
}
```

```yaml
rest-clients:
  clients:
    - name: secureApiClient
      base-url: https://secure-api.example.com
      interceptors:
        - authInterceptor      # Executes first (adds auth header)
        - loggingInterceptor   # Executes second (logs with auth header)
```

### Custom Request Factory for SSL

For advanced scenarios like custom SSL certificates, create a custom `ClientHttpRequestFactory` bean:

#### Using Apache HttpComponents (Recommended for SSL)

First, add the dependency:

```xml
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
</dependency>
```

Create a custom request factory bean:

```java
@Configuration
public class HttpClientConfig {

    @Bean
    public ClientHttpRequestFactory customSslRequestFactory() throws Exception {
        // Load custom trust store
        KeyStore trustStore = KeyStore.getInstance("JKS");
        try (InputStream is = new FileInputStream("/path/to/truststore.jks")) {
            trustStore.load(is, "password".toCharArray());
        }

        // Build SSL context
        SSLContext sslContext = SSLContexts.custom()
            .loadTrustMaterial(trustStore, new TrustSelfSignedStrategy())
            .build();

        // Create HttpClient with custom SSL context
        HttpClient httpClient = HttpClients.custom()
            .setSSLContext(sslContext)
            .build();

        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }
}
```

Configure in `application.yaml`:

```yaml
rest-clients:
  clients:
    - name: secureClient
      base-url: https://secure-api.example.com
      request-factory-bean: customSslRequestFactory  # Override default JdkClientHttpRequestFactory
```

#### Using JDK HttpClient

Alternatively, use the built-in JDK HttpClient:

```java
@Configuration
public class HttpClientConfig {

    @Bean
    public ClientHttpRequestFactory jdkSslRequestFactory() throws Exception {
        // Load custom trust store
        KeyStore trustStore = KeyStore.getInstance("JKS");
        try (InputStream is = new FileInputStream("/path/to/truststore.jks")) {
            trustStore.load(is, "password".toCharArray());
        }

        // Build SSL context
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);

        // Create HttpClient with custom SSL context
        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
            .sslContext(sslContext)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

        return new JdkClientHttpRequestFactory(httpClient);
    }
}
```

### Custom Request Factory with Connection Pooling

```java
@Configuration
public class HttpClientConfig {

    @Bean
    public ClientHttpRequestFactory poolingRequestFactory() {
        PoolingHttpClientConnectionManager connectionManager =
            PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(100)
                .setMaxConnPerRoute(20)
                .build();

        HttpClient httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .build();

        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }
}
```

```yaml
rest-clients:
  clients:
    - name: highVolumeClient
      base-url: https://api.example.com
      request-factory-bean: poolingRequestFactory  # Override default JdkClientHttpRequestFactory
```

**Important Notes:**
- By default, all RestClient beans use `JdkClientHttpRequestFactory` when timeouts are configured, which supports all standard HTTP methods (GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS) and any other methods supported by Java's HttpClient
- When you specify a custom `request-factory-bean`, it overrides the default `JdkClientHttpRequestFactory`
- When using a custom `request-factory-bean`, the `connect-timeout` and `read-timeout` properties are ignored - configure timeouts in your custom factory instead

## Debugging

Enable debug logging to see detailed information about RestClient bean creation:

```yaml
logging:
  level:
    net.snytkine.springboot.rest_clients: DEBUG
```

Debug logs include:
- Bean registration details
- Base URL configuration
- Request factory resolution (custom or default)
- Interceptor resolution with order and types
- Any configuration errors

## Complete Example

### application.yaml

```yaml
spring:
  application:
    name: my-application

rest-clients:
  clients:
    - name: githubClient
      base-url: https://api.github.com
      connect-timeout: 5000
      read-timeout: 10000
      interceptors:
        - authInterceptor
        - loggingInterceptor

    - name: securePaymentClient
      base-url: https://payments.example.com/api
      request-factory-bean: customSslRequestFactory
      interceptors:
        - loggingInterceptor

logging:
  level:
    net.snytkine.springboot.rest_clients: DEBUG
```

### Interceptors

```java
@Component("authInterceptor")
public class AuthInterceptor implements ClientHttpRequestInterceptor {

    @Value("${github.token}")
    private String token;

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().setBearerAuth(token);
        return execution.execute(request, body);
    }
}

@Component("loggingInterceptor")
@Slf4j
public class LoggingInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        log.info("Request: {} {}", request.getMethod(), request.getURI());
        long startTime = System.currentTimeMillis();

        ClientHttpResponse response = execution.execute(request, body);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Response: {} ({}ms)", response.getStatusCode(), duration);

        return response;
    }
}
```

### Service

```java
@Service
public class GitHubService {

    private final RestClient githubClient;

    public GitHubService(@Qualifier("githubClient") RestClient githubClient) {
        this.githubClient = githubClient;
    }

    public Repository getRepository(String owner, String repo) {
        return githubClient.get()
            .uri("/repos/{owner}/{repo}", owner, repo)
            .retrieve()
            .body(Repository.class);
    }

    public void createIssue(String owner, String repo, Issue issue) {
        githubClient.post()
            .uri("/repos/{owner}/{repo}/issues", owner, repo)
            .contentType(MediaType.APPLICATION_JSON)
            .body(issue)
            .retrieve()
            .toBodilessEntity();
    }
}
```

## Error Handling

The library validates configuration at startup and will throw exceptions if:

- A client configuration is missing the required `name` property
- An interceptor bean name in the list is null or blank
- A specified interceptor bean cannot be found in the ApplicationContext
- A specified `request-factory-bean` cannot be found in the ApplicationContext

These are fail-fast validations to ensure configuration errors are caught early.

## Requirements

- Java 21+
- Spring Boot 4.0.6+

## License

[Your License Here]
