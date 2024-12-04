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

record SelectSpanNames(Schema schema, String serviceName) implements Function<DSLContext, List<String>> {

  private static final Logger LOG = Logger.getLogger(SelectSpanNames.class.getName());

  @Override
  public List<String> apply(DSLContext context) {
    LOG.info("begin select span names data");
    return context
      .selectDistinct(ZIPKIN_SPANS.NAME)
      .from(ZIPKIN_SPANS)
      .join(ZIPKIN_ANNOTATIONS)
      .on(schema.joinCondition(ZIPKIN_ANNOTATIONS))
      .where(
        localServiceNameCondition()
          .and(ZIPKIN_SPANS.NAME.notEqual("")))
      .orderBy(ZIPKIN_SPANS.NAME)
      .fetch(ZIPKIN_SPANS.NAME);
  }

  @Override
  public String toString() {
    return "SelectSpanNames{serviceName=" + serviceName + "}";
  }
}
