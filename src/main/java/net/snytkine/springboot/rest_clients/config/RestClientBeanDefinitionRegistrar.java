package net.snytkine.springboot.rest_clients.config;

import lombok.extern.slf4j.Slf4j;
import net.snytkine.springboot.rest_clients.config.properties.RestClientProperties;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Registers RestClient bean definitions dynamically based on configuration properties.
 *
 * <p>This registrar is invoked during the Spring application context initialization phase, before
 * most beans are created. It reads RestClient configurations from the application properties (under
 * the {@code rest-clients.clients} prefix) and registers a {@link RestClientFactoryBean} for each
 * configured client.
 *
 * <p>The registration happens early in the Spring lifecycle, during the {@link
 * BeanDefinitionRegistry} phase, which allows the RestClient beans to be available for dependency
 * injection throughout the application.
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
 *         - authInterceptor
 * </pre>
 *
 * <p>This will register a RestClient bean named {@code githubClient} that can be injected as:
 *
 * <pre>
 * {@code @Autowired}
 * {@code @Qualifier("githubClient")}
 * private RestClient githubClient;
 * </pre>
 *
 * @see RestClientFactoryBean
 * @see RestClientProperties
 * @see RestClientAutoConfiguration
 * @since 0.0.1
 */
@Slf4j
public class RestClientBeanDefinitionRegistrar
    implements ImportBeanDefinitionRegistrar, EnvironmentAware {

  private Environment environment;

  /**
   * Sets the Spring Environment to use for binding configuration properties.
   *
   * <p>This method is called by Spring before {@link #registerBeanDefinitions} to provide access to
   * the application's environment and configuration properties.
   *
   * @param environment the Spring Environment containing application properties
   */
  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  /**
   * Registers RestClient bean definitions for each client configuration found in application
   * properties.
   *
   * <p>This method executes during the early phase of Spring context initialization, before regular
   * bean instantiation. It performs the following steps:
   *
   * <ol>
   *   <li>Binds configuration properties from {@code rest-clients.clients} to {@link
   *       RestClientProperties}
   *   <li>Validates that each client configuration has a required {@code name} property
   *   <li>Creates a {@link GenericBeanDefinition} for each client using {@link
   *       RestClientFactoryBean}
   *   <li>Registers the bean definition with the provided {@link BeanDefinitionRegistry}
   * </ol>
   *
   * <p>If no clients are configured, this method returns without registering any beans.
   *
   * <p><b>Bean Naming:</b> Each RestClient bean is registered with the name specified in the {@code
   * name} property of the client configuration. This name is used as both the bean name and
   * qualifier for dependency injection.
   *
   * <p><b>Error Handling:</b>
   *
   * <ul>
   *   <li>If a client configuration is missing the {@code name} property, an {@link
   *       IllegalArgumentException} is thrown
   *   <li>Debug-level logging is performed throughout the registration process
   *   <li>Error-level logging is performed when validation fails
   * </ul>
   *
   * @param importingClassMetadata metadata about the importing configuration class (not used)
   * @param registry the bean definition registry to register RestClient beans with
   * @throws IllegalArgumentException if any client configuration is missing the required {@code
   *     name} property
   * @see RestClientFactoryBean
   * @see RestClientProperties.ClientConfig
   */
  @Override
  public void registerBeanDefinitions(
      AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
    log.debug("Starting RestClient bean definition registration");

    // Bind configuration properties manually since we're in the early phase
    RestClientProperties properties = new RestClientProperties();
    org.springframework.boot.context.properties.bind.Binder.get(environment)
        .bind(
            "rest-clients",
            org.springframework.boot.context.properties.bind.Bindable.ofInstance(properties));

    if (properties.getClients() == null || properties.getClients().isEmpty()) {
      log.debug("No RestClient configurations found in 'rest-clients.clients' property");
      return;
    }

    log.debug("Found {} RestClient configuration(s) to register", properties.getClients().size());

    // Register a RestClient bean for each client configuration
    for (RestClientProperties.ClientConfig clientConfig : properties.getClients()) {
      if (clientConfig.getName() == null || clientConfig.getName().isBlank()) {
        log.error("RestClient configuration is missing required 'name' property");
        throw new IllegalArgumentException(
            "RestClient configuration must have a non-empty 'name' property");
      }

      String beanName = clientConfig.getName();
      log.debug(
          "Registering RestClient bean definition with name '{}' and base URL '{}'",
          beanName,
          clientConfig.getBaseUrl());

      // Create bean definition for RestClientFactoryBean
      GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
      beanDefinition.setBeanClass(RestClientFactoryBean.class);
      beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0, clientConfig);
      beanDefinition.setPrimary(false);

      // Register the bean
      registry.registerBeanDefinition(beanName, beanDefinition);
      log.debug("Successfully registered RestClient bean definition '{}'", beanName);
    }

    log.debug(
        "Completed RestClient bean definition registration for {} client(s)",
        properties.getClients().size());
  }
}
