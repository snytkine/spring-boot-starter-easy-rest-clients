package net.snytkine.springboot.rest_clients.config;

import net.snytkine.springboot.rest_clients.config.properties.RestClientProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnProperty(prefix = "rest-clients", name = "clients")
@EnableConfigurationProperties(RestClientProperties.class)
@Import(RestClientBeanDefinitionRegistrar.class)
public class RestClientAutoConfiguration {
  // Auto-configuration class that triggers RestClient bean registration
}
