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
      "rest-clients.clients[0].interceptors[0].bean-name=interceptor1",
      "rest-clients.clients[0].interceptors[0].order=1",
      "rest-clients.clients[0].interceptors[1].bean-name=interceptor2",
      "rest-clients.clients[0].interceptors[1].order=2",
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
    assertThat(client1.getInterceptors().get(0).getBeanName()).isEqualTo("interceptor1");
    assertThat(client1.getInterceptors().get(0).getOrder()).isEqualTo(1);
    assertThat(client1.getInterceptors().get(1).getBeanName()).isEqualTo("interceptor2");
    assertThat(client1.getInterceptors().get(1).getOrder()).isEqualTo(2);

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
