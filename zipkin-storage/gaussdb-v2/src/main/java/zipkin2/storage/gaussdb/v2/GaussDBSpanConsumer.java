/*
 * Copyright The OpenZipkin Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package zipkin2.storage.gaussdb.v2;

import org.jooq.DSLContext;
import org.jooq.InsertSetMoreStep;
import org.jooq.Record;
import org.jooq.Result;
import zipkin2.Annotation;
import zipkin2.Call;
import zipkin2.Span;
import zipkin2.storage.SpanConsumer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

import static zipkin2.storage.gaussdb.v2.internal.generated.tables.ZipkinAnnotations.ZIPKIN_ANNOTATIONS;
import static zipkin2.storage.gaussdb.v2.internal.generated.tables.ZipkinDependencies.ZIPKIN_DEPENDENCIES;
import static zipkin2.storage.gaussdb.v2.internal.generated.tables.ZipkinSpans.ZIPKIN_SPANS;

final class GaussDBSpanConsumer implements SpanConsumer {

  private static final Logger LOG = Logger.getLogger(GaussDBSpanConsumer.class.getName());
  private final DataSource dataSource;
  private final DSLContexts context;
  private final Schema schema;

  public GaussDBSpanConsumer(DataSource dataSource, DSLContexts context, Schema schema) {
    this.dataSource = dataSource;
    this.context = context;
    this.schema = schema;
  }

  @Override
  public Call<Void> accept(List<Span> spans) {
//    LOG.info("begin accept trace data:" + spans);
    if (spans.isEmpty()) return Call.create(null);
    try (Connection conn = dataSource.getConnection()) {
      DSLContext ctx = context.get(conn);
      for (Span span : spans) {
        Record record = ctx.selectFrom(ZIPKIN_SPANS).where(
          ZIPKIN_SPANS.TRACE_ID.eq(span.traceId())
            .and(ZIPKIN_SPANS.TRACE_ID.eq(span.traceId()))
            .and(ZIPKIN_SPANS.KIND.eq(span.kind().name()))
        ).fetchOne();
        Long spanId;
        if (Objects.isNull(record)) {
          spanId = Objects.requireNonNull(ctx.insertInto(ZIPKIN_SPANS)
              .set(ZIPKIN_SPANS.TRACE_ID, span.traceId())
              .set(ZIPKIN_SPANS.ID, span.id())
              .set(ZIPKIN_SPANS.PARENT_ID, span.parentId())
              .set(ZIPKIN_SPANS.KIND, span.kind().name())
              .set(ZIPKIN_SPANS.NAME, span.name())
              .set(ZIPKIN_SPANS.DEBUG, span.debug())
              .set(ZIPKIN_SPANS.SHARE, span.shared())
              .set(ZIPKIN_SPANS.START_TS, span.timestamp())
              .set(ZIPKIN_SPANS.DURATION, span.duration() == null ? 0 : span.duration().intValue())
              .returning(ZIPKIN_SPANS.SPAN_ID)
              .fetchOne())
            .getValue(ZIPKIN_SPANS.SPAN_ID);
          LOG.info("begin accept trace data:" + span);
        } else {
          spanId = record.getValue(ZIPKIN_SPANS.SPAN_ID);
        }

        if (Objects.nonNull(span.parentId())) {
          String sql = "insert into zipkin_dependencies values(?,?,?,?,?) on duplicate key update nothing";
          int rs = ctx.execute(sql,
            LocalDate.now(),
            span.parentId(),
            span.traceId(),
            span.duration(), 0L
          );
        }

        for (Annotation annotation : span.annotations()) {
          Result<Record> records1 = ctx.selectFrom(ZIPKIN_ANNOTATIONS).where(
            ZIPKIN_ANNOTATIONS.SPAN_ID.eq(spanId)
              .and(ZIPKIN_ANNOTATIONS.A_KEY.eq(annotation.value()))
              .and(ZIPKIN_ANNOTATIONS.A_TIMESTAMP.eq(annotation.timestamp()))
          ).fetch();
          if (records1.isEmpty()) {
            InsertSetMoreStep<Record> ann = ctx.insertInto(ZIPKIN_ANNOTATIONS)
              .set(ZIPKIN_ANNOTATIONS.SPAN_ID, spanId)
              .set(ZIPKIN_ANNOTATIONS.A_KEY, annotation.value())
              .set(ZIPKIN_ANNOTATIONS.A_TYPE, -1)
              .set(ZIPKIN_ANNOTATIONS.A_TIMESTAMP, annotation.timestamp());

            if (span.localEndpoint() != null) {
              ann = ann.set(ZIPKIN_ANNOTATIONS.SERVICE_NAME, span.localEndpoint().serviceName())
                .set(ZIPKIN_ANNOTATIONS.IPV4, span.localEndpoint().ipv4());
              if (schema.hasIpv6 && Objects.nonNull(span.localEndpoint().ipv6())) {
                ann = ann.set(ZIPKIN_ANNOTATIONS.IPV6, span.localEndpoint().ipv6());
              }
              if (span.localEndpoint().port() != null) {
                ann = ann.set(ZIPKIN_ANNOTATIONS.PORT, span.localEndpoint().port().shortValue());
              }
            }
            ann.execute();
          }
        }
        for (String key : span.tags().keySet()) {
          Result<Record> records1 = ctx.selectFrom(ZIPKIN_ANNOTATIONS).where(
            ZIPKIN_ANNOTATIONS.SPAN_ID.eq(spanId)
              .and(ZIPKIN_ANNOTATIONS.A_KEY.eq(key))
              .and(ZIPKIN_ANNOTATIONS.A_TIMESTAMP.eq(span.timestamp()))
          ).fetch();
          if (records1.isEmpty()) {
            InsertSetMoreStep<Record> anno = ctx.insertInto(ZIPKIN_ANNOTATIONS)
              .set(ZIPKIN_ANNOTATIONS.SPAN_ID, spanId)
              .set(ZIPKIN_ANNOTATIONS.A_KEY, key)
              .set(ZIPKIN_ANNOTATIONS.A_TYPE, 0)
              .set(ZIPKIN_ANNOTATIONS.A_VALUE, span.tags().get(key))
              .set(ZIPKIN_ANNOTATIONS.A_TIMESTAMP, span.timestamp());

            if (span.localEndpoint() != null) {
              anno = anno.set(ZIPKIN_ANNOTATIONS.SERVICE_NAME, span.localEndpoint().serviceName())
                .set(ZIPKIN_ANNOTATIONS.IPV4, span.localEndpoint().ipv4());
              if (schema.hasIpv6) {
                anno = anno.set(ZIPKIN_ANNOTATIONS.IPV6, span.localEndpoint().ipv6());
              }
              if (span.localEndpoint().port() != null) {
                anno = anno.set(ZIPKIN_ANNOTATIONS.PORT, span.localEndpoint().port().shortValue());
              }
            }
            anno.execute();
          }
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return Call.create(null);
  }


}
