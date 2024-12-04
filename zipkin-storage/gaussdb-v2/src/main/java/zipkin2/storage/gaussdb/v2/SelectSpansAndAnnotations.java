/*
 * Copyright The OpenZipkin Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package zipkin2.storage.gaussdb.v2;

import org.jooq.Record;
import org.jooq.*;
import zipkin2.Endpoint;
import zipkin2.Span;
import zipkin2.storage.QueryRequest;
import zipkin2.storage.gaussdb.v2.internal.generated.tables.ZipkinAnnotations;
import zipkin2.v1.V1BinaryAnnotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;

import static org.jooq.impl.DSL.max;
import static zipkin2.storage.gaussdb.v2.Schema.maybeGet;
import static zipkin2.storage.gaussdb.v2.SelectAnnotationServiceNames.localServiceNameCondition;
import static zipkin2.storage.gaussdb.v2.internal.generated.tables.ZipkinAnnotations.ZIPKIN_ANNOTATIONS;
import static zipkin2.storage.gaussdb.v2.internal.generated.tables.ZipkinSpans.ZIPKIN_SPANS;

abstract class SelectSpansAndAnnotations implements Function<DSLContext, List<Span>> {
  private static final Logger LOG = Logger.getLogger(SelectSpansAndAnnotations.class.getName());

  static final class Factory {
    final Schema schema;
    final boolean strictTraceId;

    Factory(Schema schema, boolean strictTraceId) {
      this.schema = schema;
      this.strictTraceId = strictTraceId;
    }

    SelectSpansAndAnnotations create(String traceId) {
      return new SelectSpansAndAnnotations(schema) {
        @Override
        Condition traceIdCondition(DSLContext context) {
          return ZIPKIN_SPANS.TRACE_ID.eq(traceId);
        }
      };
    }

    SelectSpansAndAnnotations create(Set<Pair> traceIdPairs) {
      return new SelectSpansAndAnnotations(schema) {
        @Override
        Condition traceIdCondition(DSLContext context) {
          return ZIPKIN_SPANS.TRACE_ID.in(traceIdPairs);
        }
      };
    }

    SelectSpansAndAnnotations create(QueryRequest request) {
      if (request.remoteServiceName() != null) {
        throw new IllegalArgumentException("remoteService=" + request.remoteServiceName()
          + " unsupported due to missing column zipkin_spans.remote_service_name");
      }
      return new SelectSpansAndAnnotations(schema) {
        @Override
        Condition traceIdCondition(DSLContext context) {
          return schema.spanTraceIdCondition(toTraceIdQuery(context, request));
        }
      };
    }
  }

  final Schema schema;

  SelectSpansAndAnnotations(Schema schema) {
    this.schema = schema;
  }

  abstract Condition traceIdCondition(DSLContext context);

  @Override
  public List<Span> apply(DSLContext context) {
    LOG.info("begin select span and annotations data");
    List<Span> spans = new ArrayList<>();
    Result<Record> records = context
      .select(schema.spanFields)
      .from(ZIPKIN_SPANS)
      .where(traceIdCondition(context)).fetch();

    for (Record r : records) {
      Long spanId = r.get(ZIPKIN_SPANS.SPAN_ID);
      Span.Builder builder = Span.newBuilder()
        .traceId(r.getValue(ZIPKIN_SPANS.TRACE_ID))
        .name(r.getValue(ZIPKIN_SPANS.NAME))
        .id(r.getValue(ZIPKIN_SPANS.ID))
        .parentId(maybeGet(r, ZIPKIN_SPANS.PARENT_ID, "0"))
        .timestamp(maybeGet(r, ZIPKIN_SPANS.START_TS, 0L))
        .duration(maybeGet(r, ZIPKIN_SPANS.DURATION, 0))
        .debug(r.getValue(ZIPKIN_SPANS.DEBUG));
      Result<Record> records1 = context.selectFrom(ZIPKIN_ANNOTATIONS).where(ZIPKIN_ANNOTATIONS.SPAN_ID.eq(spanId)).fetch();
      if (records1.isNotEmpty()) {
        Record record1 = records1.get(0);
        Endpoint endpoint = Endpoint.newBuilder().serviceName(record1.get(ZIPKIN_ANNOTATIONS.SERVICE_NAME))
          .ip(record1.get(ZIPKIN_ANNOTATIONS.IPV4)).build();
        builder.localEndpoint(endpoint);
      }
      spans.add(builder.build());
    }

    return spans;
  }

  SelectOffsetStep<? extends Record> toTraceIdQuery(DSLContext context, QueryRequest request) {
    long endTs = request.endTs() * 1000;

    TableOnConditionStep<?> table =
      ZIPKIN_SPANS.join(ZIPKIN_ANNOTATIONS).on(schema.joinCondition(ZIPKIN_ANNOTATIONS));

    int i = 0;
    for (Map.Entry<String, String> kv : request.annotationQuery().entrySet()) {
      ZipkinAnnotations aTable = ZIPKIN_ANNOTATIONS.as("a" + i++);
      if (kv.getValue().isEmpty()) {
        table =
          maybeOnService(
            table
              .join(aTable)
              .on(schema.joinCondition(aTable))
              .and(aTable.A_KEY.eq(kv.getKey())),
            aTable,
            request.serviceName());
      } else {
        table =
          maybeOnService(
            table
              .join(aTable)
              .on(schema.joinCondition(aTable))
              .and(aTable.A_TYPE.eq(V1BinaryAnnotation.TYPE_STRING))
              .and(aTable.A_KEY.eq(kv.getKey()))
              .and(aTable.A_VALUE.eq(kv.getValue())),
            aTable,
            request.serviceName());
      }
    }

    List<SelectField<?>> distinctFields = new ArrayList<>(schema.spanIdFields);
    distinctFields.add(max(ZIPKIN_SPANS.START_TS));
    SelectConditionStep<Record> dsl = context.selectDistinct(distinctFields)
      .from(table)
      .where(ZIPKIN_SPANS.START_TS.between(endTs - request.lookback() * 1000, endTs));

    if (request.serviceName() != null) {
      dsl = dsl.and(localServiceNameCondition()
        .and(ZIPKIN_ANNOTATIONS.SERVICE_NAME.eq(request.serviceName())));
    }

    if (request.spanName() != null) {
      dsl = dsl.and(ZIPKIN_SPANS.NAME.eq(request.spanName()));
    }

    if (request.minDuration() != null && request.maxDuration() != null) {
      dsl = dsl.and(ZIPKIN_SPANS.DURATION.between(request.minDuration().intValue(), request.maxDuration().intValue()));
    } else if (request.minDuration() != null) {
      dsl = dsl.and(ZIPKIN_SPANS.DURATION.greaterOrEqual(request.minDuration().intValue()));
    }
    return dsl.groupBy(schema.spanIdFields)
      .orderBy(max(ZIPKIN_SPANS.START_TS).desc())
      .limit(request.limit());
  }

  static TableOnConditionStep<?> maybeOnService(
    TableOnConditionStep<Record> table, ZipkinAnnotations aTable, String serviceName) {
    if (serviceName == null) return table;
    return table.and(aTable.SERVICE_NAME.eq(serviceName));
  }
}
