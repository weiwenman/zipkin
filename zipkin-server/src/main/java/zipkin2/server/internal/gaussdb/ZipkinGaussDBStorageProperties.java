/*
 * Copyright The OpenZipkin Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package zipkin2.server.internal.gaussdb;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.io.Serial;
import java.io.Serializable;

@ConfigurationProperties("zipkin.storage.gaussdb")
class ZipkinGaussDBStorageProperties implements Serializable { // for Spark jobs
  @Serial
  private static final long serialVersionUID = 0L;

  private String jdbcUrl;
  private String host = "localhost";
  private int port = 8000;
  private String username;
  private String password;
  private String driverClassName = "org.postgresql.Driver";
  private String db = "zipkin";
  private String schema = "public";
  private int maxActive = 10;
  private boolean useSsl;

  public String getJdbcUrl() {
    return jdbcUrl;
  }

  public void setJdbcUrl(String jdbcUrl) {
    this.jdbcUrl = jdbcUrl;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = "".equals(username) ? null : username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = "".equals(password) ? null : password;
  }

  public String getDriverClassName() {
    return driverClassName;
  }

  public void setDriverClassName(String driverClassName) {
    this.driverClassName = driverClassName;
  }

  public String getDb() {
    return db;
  }

  public void setDb(String db) {
    this.db = db;
  }

  public int getMaxActive() {
    return maxActive;
  }

  public void setMaxActive(int maxActive) {
    this.maxActive = maxActive;
  }

  public boolean isUseSsl() {
    return useSsl;
  }

  public void setUseSsl(boolean useSsl) {
    this.useSsl = useSsl;
  }

  public String getSchema() {
    return schema;
  }

  public void setSchema(String schema) {
    this.schema = schema;
  }

  public DataSource toDataSource() {
    HikariDataSource result = new HikariDataSource();
    result.setDriverClassName(getDriverClassName());
    result.setJdbcUrl(determineJdbcUrl());
    result.setMaximumPoolSize(getMaxActive());
    result.setUsername(getUsername());
    result.setPassword(getPassword());
    result.setSchema(getSchema());
    return result;
  }

  private String determineJdbcUrl() {
    if (StringUtils.hasText(getJdbcUrl())) {
      return getJdbcUrl();
    }

    return "jdbc:postgresql://"
      + getHost() + ":" + getPort()
      + "/" + getDb();
  }
}
