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
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import net.snytkine.springboot.rest_clients.config.properties.RestClientProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
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
        .isInstanceOf(BeanCreationException.class)
        .hasMessageContaining("Failed to resolve request factory bean 'missingFactory'");
  }

  @Test
  void shouldApplyInterceptorsInDeclarationOrder() throws Exception {
    ClientHttpRequestInterceptor interceptor1 = mock(ClientHttpRequestInterceptor.class);
    ClientHttpRequestInterceptor interceptor2 = mock(ClientHttpRequestInterceptor.class);

    List<String> interceptors = new ArrayList<>();
    interceptors.add("interceptor1");
    interceptors.add("interceptor2");

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
  void shouldThrowExceptionWhenInterceptorBeanNameIsBlank() {
    List<String> interceptors = new ArrayList<>();
    interceptors.add("");

    clientConfig.setName("testClient");
    clientConfig.setInterceptors(interceptors);

    factoryBean = new RestClientFactoryBean(clientConfig);
    factoryBean.setApplicationContext(applicationContext);

    assertThatThrownBy(() -> factoryBean.getObject())
        .isInstanceOf(BeanCreationException.class)
        .hasMessageContaining("must be a non-empty string");
  }

  @Test
  void shouldThrowExceptionWhenInterceptorNotFound() {
    List<String> interceptors = new ArrayList<>();
    interceptors.add("missingInterceptor");

    clientConfig.setName("testClient");
    clientConfig.setInterceptors(interceptors);

    when(applicationContext.getBean("missingInterceptor", ClientHttpRequestInterceptor.class))
        .thenThrow(new NoSuchBeanDefinitionException("missingInterceptor"));

    factoryBean = new RestClientFactoryBean(clientConfig);
    factoryBean.setApplicationContext(applicationContext);

    assertThatThrownBy(() -> factoryBean.getObject())
        .isInstanceOf(BeanCreationException.class)
        .hasMessageContaining("Failed to resolve interceptor bean 'missingInterceptor'");
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
