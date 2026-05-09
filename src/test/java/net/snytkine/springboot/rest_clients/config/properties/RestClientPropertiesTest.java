/**
 * Copyright 2026 - 2026 Dmitri Snytkine. All rights reserved.
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.
 *
 * <p>See the License for the specific language governing permissions and limitations under the
 * License.
 */
package net.snytkine.springboot.rest_clients.config.properties;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = RestClientPropertiesTest.TestConfig.class)
@TestPropertySource(
    properties = {
      "rest-clients.clients[0].name=client1",
      "rest-clients.clients[0].base-url=http://localhost:8081",
      "rest-clients.clients[0].connect-timeout=3000",
      "rest-clients.clients[0].read-timeout=5000",
      "rest-clients.clients[0].request-factory-bean=customFactory",
      "rest-clients.clients[0].interceptors[0]=interceptor1",
      "rest-clients.clients[0].interceptors[1]=interceptor2",
      "rest-clients.clients[1].name=client2",
      "rest-clients.clients[1].base-url=http://localhost:8082"
    })
class RestClientPropertiesTest {

  @Autowired private RestClientProperties properties;

  @Test
  void shouldBindPropertiesCorrectly() {
    assertThat(properties.getClients()).hasSize(2);

    // Verify first client
    RestClientProperties.ClientConfig client1 = properties.getClients().get(0);
    assertThat(client1.getName()).isEqualTo("client1");
    assertThat(client1.getBaseUrl()).isEqualTo("http://localhost:8081");
    assertThat(client1.getConnectTimeout()).isEqualTo(3000);
    assertThat(client1.getReadTimeout()).isEqualTo(5000);
    assertThat(client1.getRequestFactoryBean()).isEqualTo("customFactory");

    // Verify interceptors
    assertThat(client1.getInterceptors()).hasSize(2);
    assertThat(client1.getInterceptors().get(0)).isEqualTo("interceptor1");
    assertThat(client1.getInterceptors().get(1)).isEqualTo("interceptor2");

    // Verify second client
    RestClientProperties.ClientConfig client2 = properties.getClients().get(1);
    assertThat(client2.getName()).isEqualTo("client2");
    assertThat(client2.getBaseUrl()).isEqualTo("http://localhost:8082");
    assertThat(client2.getConnectTimeout()).isEqualTo(5000); // default
    assertThat(client2.getReadTimeout()).isEqualTo(10000); // default
    assertThat(client2.getRequestFactoryBean()).isNull();
    assertThat(client2.getInterceptors()).isEmpty();
  }

  @Test
  void shouldUseDefaultTimeouts() {
    RestClientProperties.ClientConfig client2 = properties.getClients().get(1);
    assertThat(client2.getConnectTimeout()).isEqualTo(5000);
    assertThat(client2.getReadTimeout()).isEqualTo(10000);
  }

  @EnableConfigurationProperties(RestClientProperties.class)
  static class TestConfig {}
}
