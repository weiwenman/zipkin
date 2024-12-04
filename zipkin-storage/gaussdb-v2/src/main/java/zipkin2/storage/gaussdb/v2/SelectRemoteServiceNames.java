/*
 * Copyright The OpenZipkin Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package zipkin2.storage.gaussdb.v2;

import org.jooq.DSLContext;

import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

import static zipkin2.storage.gaussdb.v2.SelectAnnotationServiceNames.localServiceNameCondition;
import static zipkin2.storage.gaussdb.v2.internal.generated.tables.ZipkinAnnotations.ZIPKIN_ANNOTATIONS;
import static zipkin2.storage.gaussdb.v2.internal.generated.tables.ZipkinSpans.ZIPKIN_SPANS;

record SelectRemoteServiceNames(Schema schema, String serviceName) implements Function<DSLContext, List<String>> {

  private static final Logger LOG = Logger.getLogger(SelectRemoteServiceNames.class.getName());
  @Override
  public List<String> apply(DSLContext context) {
    LOG.info("begin select remote service names data");
    return List.of();
  }

  @Override
  public String toString() {
    return "SelectRemoteServiceNames{serviceName=" + serviceName + "}";
  }
}
