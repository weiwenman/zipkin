/*
 * Copyright The OpenZipkin Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package zipkin2.server.internal.gaussdb;

import org.jooq.ExecuteListenerProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import zipkin2.storage.StorageComponent;
import zipkin2.storage.gaussdb.v1.GaussDBStorage;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.Executor;

@EnableConfigurationProperties(ZipkinGaussDBStorageProperties.class)
@ConditionalOnClass(GaussDBStorage.class)
@ConditionalOnProperty(name = "zipkin.storage.type", havingValue = "gaussdb")
@ConditionalOnMissingBean(StorageComponent.class)
@Import(ZipkinSelfTracingGaussDBStorageConfiguration.class)
public class ZipkinGaussDBStorageConfiguration {
  @Autowired(required = false)
  ZipkinGaussDBStorageProperties gaussdb;
  @Autowired(required = false)
  ExecuteListenerProvider gaussDBListener;

  @Bean
  @ConditionalOnMissingBean
  Executor gaussDBExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix("ZipkinGaussDBStorage-");
    executor.initialize();
    return executor;
  }

  @Bean
  @ConditionalOnMissingBean
  DataSource gaussDBDataSource() {
    return gaussdb.toDataSource();
  }

  @Bean
  StorageComponent storage(
    Executor gaussDBExecutor,
    DataSource gaussDBDataSource,
    @Value("${zipkin.storage.strict-trace-id:true}") boolean strictTraceId,
    @Value("${zipkin.storage.search-enabled:true}") boolean searchEnabled,
    @Value("${zipkin.storage.autocomplete-keys:}") List<String> autocompleteKeys) {
    return GaussDBStorage.newBuilder()
      .strictTraceId(strictTraceId)
      .searchEnabled(searchEnabled)
      .autocompleteKeys(autocompleteKeys)
      .executor(gaussDBExecutor)
      .datasource(gaussDBDataSource)
      .listenerProvider(gaussDBListener)
      .build();
  }
}
