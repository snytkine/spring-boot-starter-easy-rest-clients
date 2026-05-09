/*
 * Copyright 2026 - 2026 Dmitri Snytkine. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.snytkine.springboot.rest_clients.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.mock.env.MockEnvironment;

class RestClientBeanDefinitionRegistrarTest {

  private RestClientBeanDefinitionRegistrar registrar;
  private BeanDefinitionRegistry registry;
  private AnnotationMetadata metadata;

  @BeforeEach
  void setUp() {
    registrar = new RestClientBeanDefinitionRegistrar();
    registry = new SimpleBeanDefinitionRegistry();
    metadata = mock(AnnotationMetadata.class);
  }

  @Test
  void shouldRegisterBeanDefinitionsForMultipleClients() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("rest-clients.clients[0].name", "client1");
    environment.setProperty("rest-clients.clients[0].base-url", "http://localhost:8081");
    environment.setProperty("rest-clients.clients[1].name", "client2");
    environment.setProperty("rest-clients.clients[1].base-url", "http://localhost:8082");

    registrar.setEnvironment(environment);
    registrar.registerBeanDefinitions(metadata, registry);

    assertThat(registry.getBeanDefinitionNames()).containsExactlyInAnyOrder("client1", "client2");
    assertThat(registry.getBeanDefinition("client1").getBeanClassName())
        .isEqualTo(RestClientFactoryBean.class.getName());
    assertThat(registry.getBeanDefinition("client2").getBeanClassName())
        .isEqualTo(RestClientFactoryBean.class.getName());
  }

  @Test
  void shouldNotRegisterBeansWhenNoClientsConfigured() {
    MockEnvironment environment = new MockEnvironment();
    registrar.setEnvironment(environment);
    registrar.registerBeanDefinitions(metadata, registry);

    assertThat(registry.getBeanDefinitionNames()).isEmpty();
  }

  @Test
  void shouldThrowExceptionWhenClientNameIsMissing() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("rest-clients.clients[0].base-url", "http://localhost:8081");

    registrar.setEnvironment(environment);

    assertThatThrownBy(() -> registrar.registerBeanDefinitions(metadata, registry))
        .isInstanceOf(BeanCreationException.class)
        .hasMessageContaining("RestClient configuration must have a non-empty 'name' property");
  }

  @Test
  void shouldThrowExceptionWhenClientNameIsBlank() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("rest-clients.clients[0].name", "");
    environment.setProperty("rest-clients.clients[0].base-url", "http://localhost:8081");

    registrar.setEnvironment(environment);

    assertThatThrownBy(() -> registrar.registerBeanDefinitions(metadata, registry))
        .isInstanceOf(BeanCreationException.class)
        .hasMessageContaining("RestClient configuration must have a non-empty 'name' property");
  }

  @Test
  void shouldRegisterBeanWithCorrectConfiguration() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("rest-clients.clients[0].name", "testClient");
    environment.setProperty("rest-clients.clients[0].base-url", "http://localhost:9000");
    environment.setProperty("rest-clients.clients[0].connect-timeout", "3000");
    environment.setProperty("rest-clients.clients[0].read-timeout", "6000");

    registrar.setEnvironment(environment);
    registrar.registerBeanDefinitions(metadata, registry);

    assertThat(registry.containsBeanDefinition("testClient")).isTrue();
  }
}
