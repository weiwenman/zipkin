DROP TABLE zipkin_spans;
CREATE TABLE IF NOT EXISTS zipkin_spans (
  "span_id" serial8 NOT NULL,
  "id" VARCHAR(32) NOT NULL,
  "trace_id" VARCHAR(32) NOT NULL,
  "kind" VARCHAR(32) NOT NULL,
  "name" VARCHAR(255) NOT NULL,
  "parent_id" VARCHAR(32),
  "debug" BOOLEAN,
  "share" BOOLEAN,
  "start_ts" int8 ,
  "duration" int ,
  primary key ("span_id"),
  constraint KEY_zipkin_spans_id_trace_kind unique("id", "trace_id", "kind")
  ) WITH (OIDS=FALSE);

comment on column zipkin_spans.trace_id is 'If non zero, this means the trace uses 128 bit traceIds instead of 64 bit';
comment on column zipkin_spans.start_ts is 'Span.timestamp(): epoch micros used for endTs query and to implement TTL';
comment on column zipkin_spans.duration is 'Span.duration(): micros used for minDuration and maxDuration query';

CREATE INDEX zipkin_spans_index_trace_id_kind_id ON zipkin_spans ("trace_id", "id", "kind");


DROP TABLE zipkin_annotations;
CREATE TABLE IF NOT EXISTS zipkin_annotations (
  "span_id" int8 NOT NULL,
  "a_key" VARCHAR(255) NOT NULL,
  "a_value" VARCHAR(2000),
  "a_type" INT NOT NULL,
  "a_timestamp" int8,
  "ipv4" VARCHAR(255) ,
  "ipv6" VARCHAR(255) ,
  "port" SMALLINT ,
  "service_name" VARCHAR(255),
  constraint KEY_zipkin_annotations_span_id_a_key unique("span_id", "a_key", "a_timestamp")
  ) WITH (OIDS=FALSE);

comment on column zipkin_annotations.span_id is 'coincides with zipkin_spans.id';
comment on column zipkin_annotations.a_key is 'BinaryAnnotation.key or Annotation.value if type == -1';
comment on column zipkin_annotations.a_value is 'BinaryAnnotation.value(), which must be smaller than 64KB';
comment on column zipkin_annotations.a_type is 'BinaryAnnotation.type() or -1 if Annotation';
comment on column zipkin_annotations.a_timestamp is 'Used to implement TTL; Annotation.timestamp or zipkin_spans.timestamp';
comment on column zipkin_annotations.ipv4 is 'Null when Binary/Annotation.endpoint is null';
comment on column zipkin_annotations.ipv6 is 'Null when Binary/Annotation.endpoint is null, or no IPv6 address';
comment on column zipkin_annotations.port is 'Null when Binary/Annotation.endpoint is null';
comment on column zipkin_annotations.service_name is 'Null when Binary/Annotation.endpoint is null';

CREATE INDEX zipkin_annotations_index_span_id_a_key_id ON zipkin_annotations ("span_id", "a_key", "a_timestamp");


DROP TABLE zipkin_dependencies;
CREATE TABLE IF NOT EXISTS zipkin_dependencies (
  "day" DATE NOT NULL,
  "parent" VARCHAR(32) NOT NULL,
  "child" VARCHAR(32) NOT NULL,
  "call_count" int8,
  "error_count" int8,
  constraint KEY_zipkin_dependencies_day unique("day", "parent", "child")
  ) WITH (OIDS=FALSE);
