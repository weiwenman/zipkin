/*
 * This file is generated by jOOQ.
 */
package zipkin2.storage.gaussdb.v1.internal.generated;


import java.util.Arrays;
import java.util.List;

import org.jooq.Catalog;
import org.jooq.Table;
import org.jooq.impl.SchemaImpl;

import zipkin2.storage.gaussdb.v1.internal.generated.tables.ZipkinAnnotations;
import zipkin2.storage.gaussdb.v1.internal.generated.tables.ZipkinDependencies;
import zipkin2.storage.gaussdb.v1.internal.generated.tables.ZipkinSpans;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Public extends SchemaImpl {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>public</code>
     */
    public static final Public PUBLIC = new Public();

    /**
     * The table <code>public.zipkin_annotations</code>.
     */
    public final ZipkinAnnotations ZIPKIN_ANNOTATIONS = ZipkinAnnotations.ZIPKIN_ANNOTATIONS;

    /**
     * The table <code>public.zipkin_dependencies</code>.
     */
    public final ZipkinDependencies ZIPKIN_DEPENDENCIES = ZipkinDependencies.ZIPKIN_DEPENDENCIES;

    /**
     * The table <code>public.zipkin_spans</code>.
     */
    public final ZipkinSpans ZIPKIN_SPANS = ZipkinSpans.ZIPKIN_SPANS;

    /**
     * No further instances allowed
     */
    private Public() {
        super("public", null);
    }


    @Override
    public Catalog getCatalog() {
        return DefaultCatalog.DEFAULT_CATALOG;
    }

    @Override
    public final List<Table<?>> getTables() {
        return Arrays.asList(
            ZipkinAnnotations.ZIPKIN_ANNOTATIONS,
            ZipkinDependencies.ZIPKIN_DEPENDENCIES,
            ZipkinSpans.ZIPKIN_SPANS
        );
    }
}
