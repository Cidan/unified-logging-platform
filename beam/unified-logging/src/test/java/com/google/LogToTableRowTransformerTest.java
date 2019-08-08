package com.google;

import com.google.api.services.bigquery.model.TableRow;
import java.util.UUID;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionTuple;
import org.apache.beam.sdk.values.TupleTagList;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;

public class LogToTableRowTransformerTest {

  @Rule
  public final transient TestPipeline p = TestPipeline.create();

  @Test
  public void processElement() {

    PCollection<String> input = p
        .apply(TextIO.read().from("src/test/java/com/google/example-container-log.json"));
    PCollectionTuple output = input.apply(ParDo
        .of(new LogToTableRowTransformer())
        .withOutputTags(LogToTableRowTransformer.cleanData, TupleTagList
            .of(LogToTableRowTransformer.badData)));

    PAssert.that(output.get(LogToTableRowTransformer.badData)).empty();

    PAssert.that(output.get(LogToTableRowTransformer.cleanData)).satisfies(
        (SerializableFunction<Iterable<TableRow>, Void>) data -> {
          assertTrue("Data is not empty", data.iterator().hasNext());
          TableRow row = data.iterator().next();
          assertEquals("INFO", row.get("severity"));
          assertEquals("2019-08-08T00:41:27.90582699Z", row.get("timestamp"));
          assertEquals("container", row.get("resource_type"));
          assertEquals("universal-logging-test", row.get("project_id"));
          assertEquals("us-central1-a", row.get("zone"));
          assertEquals("2019-08-08T00:41:27+0000 WARN A warning that should be ignored is usually at this level and should be actionable.\n", row.get("text_payload"));
          assertNull(row.get("json_payload"));
          assertNull(row.get("proto_payload"));
          assertTrue(row.get("raw").toString().startsWith("{\"insertId\":\"fju5h6g6h9cdwu\""));
          assertEquals("Testing for valid UUID", 4, UUID.fromString(row.get("uuid").toString()).version());
          return null;
        });

    p.run();
  }
}