package net.snytkine.springboot.rest_clients.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import net.snytkine.springboot.rest_clients.config.properties.RestClientProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

class RestClientFactoryBeanTest {

  private RestClientProperties.ClientConfig clientConfig;
  private ApplicationContext applicationContext;
  private RestClientFactoryBean factoryBean;

  @BeforeEach
  void setUp() {
    clientConfig = new RestClientProperties.ClientConfig();
    applicationContext = mock(ApplicationContext.class);
  }

  @Test
  void shouldCreateRestClientWithBaseUrl() throws Exception {
    clientConfig.setName("testClient");
    clientConfig.setBaseUrl("http://localhost:8080");

    factoryBean = new RestClientFactoryBean(clientConfig);
    factoryBean.setApplicationContext(applicationContext);

    RestClient restClient = factoryBean.getObject();

    assertThat(restClient).isNotNull();
  }

  @Test
  void shouldCreateRestClientWithTimeouts() throws Exception {
    clientConfig.setName("testClient");
    clientConfig.setBaseUrl("http://localhost:8080");
    clientConfig.setConnectTimeout(3000);
    clientConfig.setReadTimeout(5000);

    factoryBean = new RestClientFactoryBean(clientConfig);
    factoryBean.setApplicationContext(applicationContext);

    RestClient restClient = factoryBean.getObject();

    assertThat(restClient).isNotNull();
  }

  @Test
  void shouldUseCustomRequestFactory() throws Exception {
    ClientHttpRequestFactory customFactory = mock(ClientHttpRequestFactory.class);

    clientConfig.setName("testClient");
    clientConfig.setBaseUrl("http://localhost:8080");
    clientConfig.setRequestFactoryBean("customFactory");

    when(applicationContext.getBean("customFactory", ClientHttpRequestFactory.class))
        .thenReturn(customFactory);

    factoryBean = new RestClientFactoryBean(clientConfig);
    factoryBean.setApplicationContext(applicationContext);

    RestClient restClient = factoryBean.getObject();

    assertThat(restClient).isNotNull();
  }

  @Test
  void shouldThrowExceptionWhenCustomRequestFactoryNotFound() {
    clientConfig.setName("testClient");
    clientConfig.setRequestFactoryBean("missingFactory");

    when(applicationContext.getBean("missingFactory", ClientHttpRequestFactory.class))
        .thenThrow(new NoSuchBeanDefinitionException("missingFactory"));

    factoryBean = new RestClientFactoryBean(clientConfig);
    factoryBean.setApplicationContext(applicationContext);

    assertThatThrownBy(() -> factoryBean.getObject())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Failed to resolve request factory bean 'missingFactory'")
        .hasMessageContaining("testClient");
  }

  @Test
  void shouldApplyInterceptorsInOrder() throws Exception {
    ClientHttpRequestInterceptor interceptor1 = mock(ClientHttpRequestInterceptor.class);
    ClientHttpRequestInterceptor interceptor2 = mock(ClientHttpRequestInterceptor.class);

    RestClientProperties.InterceptorConfig config1 = new RestClientProperties.InterceptorConfig();
    config1.setBeanName("interceptor1");
    config1.setOrder(2);

    RestClientProperties.InterceptorConfig config2 = new RestClientProperties.InterceptorConfig();
    config2.setBeanName("interceptor2");
    config2.setOrder(1);

    List<RestClientProperties.InterceptorConfig> interceptors = new ArrayList<>();
    interceptors.add(config1);
    interceptors.add(config2);

    clientConfig.setName("testClient");
    clientConfig.setBaseUrl("http://localhost:8080");
    clientConfig.setInterceptors(interceptors);

    when(applicationContext.getBean("interceptor1", ClientHttpRequestInterceptor.class))
        .thenReturn(interceptor1);
    when(applicationContext.getBean("interceptor2", ClientHttpRequestInterceptor.class))
        .thenReturn(interceptor2);

    factoryBean = new RestClientFactoryBean(clientConfig);
    factoryBean.setApplicationContext(applicationContext);

    RestClient restClient = factoryBean.getObject();

    assertThat(restClient).isNotNull();
  }

  @Test
  void shouldThrowExceptionWhenInterceptorBeanNameIsMissing() {
    RestClientProperties.InterceptorConfig config = new RestClientProperties.InterceptorConfig();
    config.setOrder(1);

    List<RestClientProperties.InterceptorConfig> interceptors = new ArrayList<>();
    interceptors.add(config);

    clientConfig.setName("testClient");
    clientConfig.setInterceptors(interceptors);

    factoryBean = new RestClientFactoryBean(clientConfig);
    factoryBean.setApplicationContext(applicationContext);

    assertThatThrownBy(() -> factoryBean.getObject())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must have a non-empty 'bean-name' property")
        .hasMessageContaining("testClient");
  }

  @Test
  void shouldThrowExceptionWhenInterceptorNotFound() {
    RestClientProperties.InterceptorConfig config = new RestClientProperties.InterceptorConfig();
    config.setBeanName("missingInterceptor");
    config.setOrder(1);

    List<RestClientProperties.InterceptorConfig> interceptors = new ArrayList<>();
    interceptors.add(config);

    clientConfig.setName("testClient");
    clientConfig.setInterceptors(interceptors);

    when(applicationContext.getBean("missingInterceptor", ClientHttpRequestInterceptor.class))
        .thenThrow(new NoSuchBeanDefinitionException("missingInterceptor"));

    factoryBean = new RestClientFactoryBean(clientConfig);
    factoryBean.setApplicationContext(applicationContext);

    assertThatThrownBy(() -> factoryBean.getObject())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Failed to resolve interceptor bean 'missingInterceptor'")
        .hasMessageContaining("testClient");
  }

  @Test
  void shouldReturnCorrectObjectType() {
    clientConfig.setName("testClient");
    factoryBean = new RestClientFactoryBean(clientConfig);

    assertThat(factoryBean.getObjectType()).isEqualTo(RestClient.class);
  }

  @Test
  void shouldBeSingleton() {
    clientConfig.setName("testClient");
    factoryBean = new RestClientFactoryBean(clientConfig);

    assertThat(factoryBean.isSingleton()).isTrue();
  }

  @Test
  void shouldCreateRestClientWithoutBaseUrl() throws Exception {
    clientConfig.setName("testClient");

    factoryBean = new RestClientFactoryBean(clientConfig);
    factoryBean.setApplicationContext(applicationContext);

    RestClient restClient = factoryBean.getObject();

    assertThat(restClient).isNotNull();
  }

  @Test
  void shouldCreateRestClientWithoutTimeouts() throws Exception {
    clientConfig.setName("testClient");
    clientConfig.setBaseUrl("http://localhost:8080");

    factoryBean = new RestClientFactoryBean(clientConfig);
    factoryBean.setApplicationContext(applicationContext);

    RestClient restClient = factoryBean.getObject();

    assertThat(restClient).isNotNull();
  }
}
