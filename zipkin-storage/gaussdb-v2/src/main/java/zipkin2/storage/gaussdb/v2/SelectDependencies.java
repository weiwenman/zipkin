/*
 * Copyright The OpenZipkin Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package zipkin2.storage.gaussdb.v2;

import org.jooq.*;
import org.jooq.Record;
import zipkin2.DependencyLink;
import zipkin2.Span;
import zipkin2.internal.DependencyLinker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

import static zipkin2.storage.gaussdb.v2.Schema.maybeGet;
import static zipkin2.storage.gaussdb.v2.internal.generated.tables.ZipkinAnnotations.ZIPKIN_ANNOTATIONS;
import static zipkin2.storage.gaussdb.v2.internal.generated.tables.ZipkinDependencies.ZIPKIN_DEPENDENCIES;
import static zipkin2.storage.gaussdb.v2.internal.generated.tables.ZipkinSpans.ZIPKIN_SPANS;

record SelectDependencies(Schema schema, List<Long> epochDays) implements Function<DSLContext, List<DependencyLink>> {

  private static final Logger LOG = Logger.getLogger(SelectDependencies.class.getName());

  @Override
  public List<DependencyLink> apply(DSLContext context) {
    LOG.info("begin select dependencies data");
    return test(context);
//    List<DependencyLink> unmerged =
//      context
//        .select(schema.dependencyLinkFields)
//        .from(ZIPKIN_DEPENDENCIES)
//        .where(ZIPKIN_DEPENDENCIES.DAY.in(epochDays))
//        .fetch(
//          (Record l) ->
//            DependencyLink.newBuilder()
//              .parent(l.get(ZIPKIN_DEPENDENCIES.PARENT))
//              .child(l.get(ZIPKIN_DEPENDENCIES.CHILD))
//              .callCount(l.get(ZIPKIN_DEPENDENCIES.CALL_COUNT))
//              .errorCount(maybeGet(l, ZIPKIN_DEPENDENCIES.ERROR_COUNT, 0L))
//              .build());
//    return DependencyLinker.merge(unmerged);
  }

  public List<DependencyLink> test(DSLContext context) {
    SelectConditionStep<Record1<String>> traceIDs = context.selectDistinct(ZIPKIN_SPANS.TRACE_ID)
      .from(ZIPKIN_SPANS)
      .where(ZIPKIN_SPANS.START_TS.in(epochDays));
    // Lazy fetching the cursor prevents us from buffering the whole dataset in memory.
    Cursor<Record> cursor = context.selectDistinct(schema.dependencyLinkerFields)
      // left joining allows us to keep a mapping of all span ids, not just ones that have
      // special annotations. We need all span ids to reconstruct the trace tree. We need
      // the whole trace tree so that we can accurately skip local spans.
      .from(ZIPKIN_SPANS.leftJoin(ZIPKIN_ANNOTATIONS)
        // NOTE: we are intentionally grouping only on the low-bits of trace id. This
        // buys time for applications to upgrade to 128-bit instrumentation.
        .on(ZIPKIN_SPANS.SPAN_ID.eq(ZIPKIN_ANNOTATIONS.SPAN_ID))
//        .and(ZIPKIN_ANNOTATIONS.A_KEY.in("lc", "cs", "ca", "sr", "sa", "ma", "mr", "ms", "error"))
      )
      .where(ZIPKIN_SPANS.TRACE_ID.in(traceIDs))
      // Grouping so that later code knows when a span or trace is finished.
      .groupBy(schema.dependencyLinkerGroupByFields)
      .fetchLazy();

    Iterator<Iterator<Span>> traces =
      new DependencyLinkV2SpanIterator.ByTraceId(cursor.iterator(), false);

    if (!traces.hasNext()) return List.of();

    DependencyLinker linker = new DependencyLinker();

    List<Span> nextTrace = new ArrayList<>();
    while (traces.hasNext()) {
      Iterator<Span> i = traces.next();
      while (i.hasNext()) nextTrace.add(i.next());
      linker.putTrace(nextTrace);
      nextTrace.clear();
    }

    return linker.link();
  }

  @Override
  public String toString() {
    return "SelectDependencies{epochDays=" + epochDays + "}";
  }
}
