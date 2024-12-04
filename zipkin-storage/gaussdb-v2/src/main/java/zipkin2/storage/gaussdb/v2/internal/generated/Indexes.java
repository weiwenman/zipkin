/*
 * This file is generated by jOOQ.
 */
package zipkin2.storage.gaussdb.v2.internal.generated;


import org.jooq.Index;
import org.jooq.OrderField;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;

import zipkin2.storage.gaussdb.v2.internal.generated.tables.ZipkinAnnotations;
import zipkin2.storage.gaussdb.v2.internal.generated.tables.ZipkinSpans;


/**
 * A class modelling indexes of tables in public.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Indexes {

    // -------------------------------------------------------------------------
    // INDEX definitions
    // -------------------------------------------------------------------------

    public static final Index ZIPKIN_ANNOTATIONS_INDEX_SPAN_ID_A_KEY_ID = Internal.createIndex(DSL.name("zipkin_annotations_index_span_id_a_key_id"), ZipkinAnnotations.ZIPKIN_ANNOTATIONS, new OrderField[] { ZipkinAnnotations.ZIPKIN_ANNOTATIONS.SPAN_ID, ZipkinAnnotations.ZIPKIN_ANNOTATIONS.A_KEY, ZipkinAnnotations.ZIPKIN_ANNOTATIONS.A_TIMESTAMP }, false);
    public static final Index ZIPKIN_SPANS_INDEX_TRACE_ID_KIND_ID = Internal.createIndex(DSL.name("zipkin_spans_index_trace_id_kind_id"), ZipkinSpans.ZIPKIN_SPANS, new OrderField[] { ZipkinSpans.ZIPKIN_SPANS.TRACE_ID, ZipkinSpans.ZIPKIN_SPANS.ID, ZipkinSpans.ZIPKIN_SPANS.KIND }, false);
}
