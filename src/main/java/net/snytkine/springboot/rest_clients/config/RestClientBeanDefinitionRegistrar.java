package net.snytkine.springboot.rest_clients.config;

import lombok.extern.slf4j.Slf4j;
import net.snytkine.springboot.rest_clients.config.properties.RestClientProperties;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

@Slf4j
public class RestClientBeanDefinitionRegistrar
    implements ImportBeanDefinitionRegistrar, EnvironmentAware {

  private Environment environment;

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

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
