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

import java.io.IOException;
import net.snytkine.springboot.rest_clients.config.properties.RestClientProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

class RestClientAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class));

  @Test
  void shouldAutoConfigureWhenClientsPropertyExists() {
    contextRunner
        .withPropertyValues(
            "rest-clients.clients[0].name=testClient",
            "rest-clients.clients[0].base-url=http://localhost:8080")
        .run(
            context -> {
              assertThat(context).hasSingleBean(RestClientProperties.class);
              assertThat(context).hasBean("testClient");
              assertThat(context.getBean("testClient")).isInstanceOf(RestClient.class);
            });
  }

  @Test
  void shouldNotCreateBeansWhenClientsPropertyMissing() {
    contextRunner.run(
        context -> {
          // RestClientProperties bean should exist (auto-configured)
          assertThat(context).hasSingleBean(RestClientProperties.class);
          // But no RestClient beans should be created
          assertThat(context).doesNotHaveBean("testClient");
        });
  }

  @Test
  void shouldCreateMultipleRestClientBeans() {
    contextRunner
        .withPropertyValues(
            "rest-clients.clients[0].name=client1",
            "rest-clients.clients[0].base-url=http://localhost:8081",
            "rest-clients.clients[1].name=client2",
            "rest-clients.clients[1].base-url=http://localhost:8082")
        .run(
            context -> {
              assertThat(context).hasBean("client1");
              assertThat(context).hasBean("client2");
              assertThat(context.getBean("client1")).isInstanceOf(RestClient.class);
              assertThat(context.getBean("client2")).isInstanceOf(RestClient.class);
              assertThat(context.getBean("client1")).isNotSameAs(context.getBean("client2"));
            });
  }

  @Test
  void shouldApplyInterceptorsToRestClient() {
    contextRunner
        .withUserConfiguration(TestInterceptorConfig.class)
        .withPropertyValues(
            "rest-clients.clients[0].name=clientWithInterceptor",
            "rest-clients.clients[0].base-url=http://localhost:8080",
            "rest-clients.clients[0].interceptors[0]=testInterceptor")
        .run(
            context -> {
              assertThat(context).hasBean("clientWithInterceptor");
              assertThat(context).hasBean("testInterceptor");
              RestClient restClient = context.getBean("clientWithInterceptor", RestClient.class);
              assertThat(restClient).isNotNull();
            });
  }

  @Test
  void shouldCreateRestClientWithTimeouts() {
    contextRunner
        .withPropertyValues(
            "rest-clients.clients[0].name=clientWithTimeouts",
            "rest-clients.clients[0].base-url=http://localhost:8080",
            "rest-clients.clients[0].connect-timeout=3000",
            "rest-clients.clients[0].read-timeout=5000")
        .run(
            context -> {
              assertThat(context).hasBean("clientWithTimeouts");
              RestClient restClient = context.getBean("clientWithTimeouts", RestClient.class);
              assertThat(restClient).isNotNull();
            });
  }

  @Configuration
  static class TestInterceptorConfig {
    @Bean
    public ClientHttpRequestInterceptor testInterceptor() {
      return new ClientHttpRequestInterceptor() {
        @Override
        public ClientHttpResponse intercept(
            HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
          return execution.execute(request, body);
        }
      };
    }
  }
}
