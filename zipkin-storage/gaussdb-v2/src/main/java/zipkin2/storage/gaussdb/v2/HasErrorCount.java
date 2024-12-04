/*
 * Copyright The OpenZipkin Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package zipkin2.storage.gaussdb.v2;

import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.exception.DataAccessException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static zipkin2.storage.gaussdb.v2.internal.generated.tables.ZipkinDependencies.ZIPKIN_DEPENDENCIES;

final class HasErrorCount {
  private static final Logger LOG = Logger.getLogger(HasErrorCount.class.getName());

  static boolean test(DataSource datasource, DSLContexts context) {
    try (Connection conn = datasource.getConnection()) {
      DSLContext dsl = context.get(conn);
      Result<Record1<Long>> fetch = dsl.select(ZIPKIN_DEPENDENCIES.ERROR_COUNT).from(ZIPKIN_DEPENDENCIES).fetch();
      return fetch.isNotEmpty();
    } catch (DataAccessException e) {
      if (e.sqlState().equals("42S22")) {
        LOG.warning(
          """
            zipkin_dependencies.error_count doesn't exist, so DependencyLink.errorCount is not supported. \
            Execute: alter table zipkin_dependencies add `error_count` BIGINT\
            """);
        return false;
      }
      problemReading(e);
    } catch (SQLException | RuntimeException e) {
      problemReading(e);
    }
    return false;
  }

  static void problemReading(Exception e) {
    LOG.log(Level.WARNING, "problem reading zipkin_dependencies.error_count", e);
  }
}
