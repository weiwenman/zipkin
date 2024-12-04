/*
 * Copyright The OpenZipkin Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package zipkin2.storage.gaussdb.v2;

import org.jooq.Record;
import org.jooq.*;
import zipkin2.storage.gaussdb.v2.internal.generated.tables.ZipkinAnnotations;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static zipkin2.storage.gaussdb.v2.internal.generated.tables.ZipkinAnnotations.ZIPKIN_ANNOTATIONS;
import static zipkin2.storage.gaussdb.v2.internal.generated.tables.ZipkinDependencies.ZIPKIN_DEPENDENCIES;
import static zipkin2.storage.gaussdb.v2.internal.generated.tables.ZipkinSpans.ZIPKIN_SPANS;

final class Schema {
  final List<Field<?>> spanIdFields;
  final List<Field<?>> spanFields;
  final List<Field<?>> annotationFields;
  final List<Field<?>> dependencyLinkerFields;
  final List<Field<?>> dependencyLinkerGroupByFields;
  final List<Field<?>> dependencyLinkFields;
  final boolean hasPreAggregatedDependencies;
  final boolean hasIpv6;
  final boolean hasErrorCount;
  final boolean strictTraceId;

  Schema(DataSource datasource, DSLContexts context, boolean strictTraceId) {
    hasPreAggregatedDependencies = HasPreAggregatedDependencies.test(datasource, context);
    hasIpv6 = HasIpv6.test(datasource, context);
    hasErrorCount = HasErrorCount.test(datasource, context);
    this.strictTraceId = strictTraceId;

    spanIdFields = list(ZIPKIN_SPANS.KIND, ZIPKIN_SPANS.TRACE_ID, ZIPKIN_SPANS.ID);
    spanFields = list(ZIPKIN_SPANS.fields());
    annotationFields = list(ZIPKIN_ANNOTATIONS.fields());
    dependencyLinkFields = list(ZIPKIN_DEPENDENCIES.fields());
    dependencyLinkerFields =
      list(
        ZIPKIN_SPANS.ID,
        ZIPKIN_SPANS.TRACE_ID,
        ZIPKIN_SPANS.PARENT_ID,
        ZIPKIN_ANNOTATIONS.A_KEY,
        ZIPKIN_ANNOTATIONS.A_TYPE,
        ZIPKIN_ANNOTATIONS.SERVICE_NAME);
    dependencyLinkerGroupByFields = new ArrayList<>(dependencyLinkerFields);
//    dependencyLinkerGroupByFields.remove(ZIPKIN_SPANS.PARENT_ID);
    if (!hasIpv6) {
      annotationFields.remove(ZIPKIN_ANNOTATIONS.IPV6);
    }
    if (!hasErrorCount) {
      dependencyLinkFields.remove(ZIPKIN_DEPENDENCIES.ERROR_COUNT);
    }
  }

  Condition joinCondition(ZipkinAnnotations annotationTable) {
    return ZIPKIN_SPANS
      .SPAN_ID
      .eq(annotationTable.SPAN_ID);
  }

  /**
   * Returns a mutable list
   */
  static <T> List<T> list(T... elements) {
    return new ArrayList<>(Arrays.asList(elements));
  }

  Condition spanTraceIdCondition(SelectOffsetStep<? extends Record> traceIdQuery) {
    List<String> traceIds = traceIdQuery.fetch(ZIPKIN_SPANS.TRACE_ID);
    return ZIPKIN_SPANS.TRACE_ID.in(traceIds);
  }

  /**
   * returns the default value if the column doesn't exist or the result was null
   */
  static <T> T maybeGet(Record record, TableField<Record, T> field, T defaultValue) {
    if (record.fieldsRow().indexOf(field) < 0) {
      return defaultValue;
    } else {
      T result = record.get(field);
      return result != null ? result : defaultValue;
    }
  }

  public static String getTraceIdHi(String traceId) {
    return traceId != null && traceId.length() == 32 ? traceId.substring(0, 16) : "0";
  }
}
