/*
 * Copyright The OpenZipkin Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package zipkin2.storage.gaussdb.v2;

import org.jooq.Condition;
import org.jooq.DSLContext;
import zipkin2.v1.V1BinaryAnnotation;

import java.util.List;
import java.util.function.Function;

import static zipkin2.storage.gaussdb.v2.internal.generated.tables.ZipkinAnnotations.ZIPKIN_ANNOTATIONS;

final class SelectAnnotationServiceNames implements Function<DSLContext, List<String>> {
  @Override
  public List<String> apply(DSLContext context) {
    return context
      .selectDistinct(ZIPKIN_ANNOTATIONS.SERVICE_NAME)
      .from(ZIPKIN_ANNOTATIONS)
      .where(localServiceNameCondition())
      .orderBy(ZIPKIN_ANNOTATIONS.SERVICE_NAME)
      .fetch(ZIPKIN_ANNOTATIONS.SERVICE_NAME);
  }

  static Condition localServiceNameCondition() {
    return ZIPKIN_ANNOTATIONS.SERVICE_NAME.isNotNull()
      .and(ZIPKIN_ANNOTATIONS.SERVICE_NAME.ne("")) // exclude address annotations
      .and(ZIPKIN_ANNOTATIONS.A_TYPE.ne(V1BinaryAnnotation.TYPE_BOOLEAN));
  }

  @Override
  public String toString() {
    return "SelectAnnotationServiceNames{}";
  }
}
