/*
 * Copyright The OpenZipkin Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package zipkin2.storage.gaussdb.v1;

import org.jooq.DSLContext;

import java.util.List;
import java.util.function.Function;

import static zipkin2.storage.gaussdb.v1.SelectAnnotationServiceNames.localServiceNameCondition;
import static zipkin2.storage.gaussdb.v1.internal.generated.tables.ZipkinAnnotations.ZIPKIN_ANNOTATIONS;
import static zipkin2.storage.gaussdb.v1.internal.generated.tables.ZipkinSpans.ZIPKIN_SPANS;

record SelectRemoteServiceNames(Schema schema, String serviceName) implements Function<DSLContext, List<String>> {

  @Override
  public List<String> apply(DSLContext context) {
    return context
      .selectDistinct(ZIPKIN_SPANS.REMOTE_SERVICE_NAME)
      .from(ZIPKIN_SPANS)
      .join(ZIPKIN_ANNOTATIONS)
      .on(schema.joinCondition(ZIPKIN_ANNOTATIONS))
      .where(
        localServiceNameCondition().and(ZIPKIN_ANNOTATIONS.ENDPOINT_SERVICE_NAME.eq(serviceName)))
      .and(ZIPKIN_SPANS.REMOTE_SERVICE_NAME.notEqual(""))
      .orderBy(ZIPKIN_SPANS.REMOTE_SERVICE_NAME)
      .fetch(ZIPKIN_SPANS.REMOTE_SERVICE_NAME);
  }

  @Override
  public String toString() {
    return "SelectRemoteServiceNames{serviceName=" + serviceName + "}";
  }
}
