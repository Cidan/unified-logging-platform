package com.google;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TimePartitioning;
import java.util.HashMap;
import java.util.UUID;
import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.Write.CreateDisposition;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollectionTuple;
import org.apache.beam.sdk.values.TupleTag;
import org.apache.beam.sdk.values.TupleTagList;

public class UnifiedLogging {
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private static final TupleTag<TableRow> badData = new TupleTag<TableRow>() {
    private static final long serialVersionUID = -767009006608923756L;
  };
  private static final TupleTag<TableRow> rawData = new TupleTag<TableRow>() {
    private static final long serialVersionUID = 2136448626752693877L;
  };

  static class DecodeMessage extends DoFn<String, TableRow> {
    private static final long serialVersionUID = -8532541222456695376L;

    // This function will create a TableRow (BigQuery row) out of String data
    // for later debugging.
    public TableRow createBadRow(String data) {
      TableRow output = new TableRow();
      // TODO: Is it really "json" and not "raw"?
      output.set("json", data);
      return output;
    }

    // Our main decoder function.
    @SuppressWarnings("unchecked")
    @ProcessElement
    public void processElement(ProcessContext c) {
      // Get the JSON data as a string from our stream.
      String data = c.element();
      TableRow decoded;
      TableRow output = new TableRow();

      // Attempt to decode our JSON data into a TableRow.
      try {
        decoded = objectMapper.readValue(data, TableRow.class);
        if (!decoded.containsKey("timestamp")) {
          c.output(badData, createBadRow(data));
          return;
        }
      } catch (Exception e) {
        // We were unable to decode the JSON, let's put this string
        // into a TableRow manually, without decoding it, so we can debug
        // it later, and output it as "bad data".
        c.output(badData, createBadRow(data));
        return;
      }

      // TODO: add at least rudimentary unit tests for ParDo transformation(s).

      // TODO: Any reason why not use LogEntry (https://cloud.google.com/logging/docs/reference/v2/rest/v2/LogEntry) instead of "manual" parsing?
      output.set("timestamp", decoded.get("timestamp").toString());
      HashMap<String, Object> resource =
          (HashMap<String, Object>) decoded.getOrDefault("resource", new HashMap<String, Object>());
      HashMap<String, Object> labels =
          (HashMap<String, Object>) resource.getOrDefault("labels", new HashMap<String, Object>());
      output.set("resource_type", resource.getOrDefault("type", ""));
      output.set("project_id", labels.getOrDefault("project_id", ""));
      output.set("zone", labels.getOrDefault("zone", ""));
      output.set("text_payload", decoded.getOrDefault("textPayload", ""));
      try {
        // TODO: isn't it better to use NULL instead of empty strings?
        output.set("json_payload",
            objectMapper.writeValueAsString(decoded.getOrDefault("jsonPayload", "")));
        output.set("proto_payload",
            objectMapper.writeValueAsString(decoded.getOrDefault("protoPayload", "")));
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }
      output.set("uuid", UUID.randomUUID());
      // Set the raw string here.
      output.set("raw", data);

      // Output our log data
      c.output(output);
    }
  }

  public static void main(String[] args) {

    DataflowPipelineOptions options = PipelineOptionsFactory
        .fromArgs(args)
        .withValidation()
        .as(DataflowPipelineOptions.class);

    String projectName = options.getProject();

    String subscription = "projects/"
        + projectName
        + "/subscriptions/"
        + "unified-logging";

    Pipeline p = Pipeline.create(options);

    PCollectionTuple decoded = p
        .apply("Read from Pubsub", PubsubIO
            .readStrings()
            .fromSubscription(subscription))
        .apply("Parse JSON", ParDo
            .of(new DecodeMessage())
            .withOutputTags(rawData, TupleTagList
                .of(badData)));

    // TODO: Technically, streaming BQ writes can fail too. We can use withFailedInsertRetryPolicy to catch those and write to a GCS.
    // TODO: But it might be getting into the weeds a bit...
    decoded.get(rawData)
        .apply("Logs to BigQuery", BigQueryIO.writeTableRows()
            .to(projectName + ":unified_logging.logs")
            .withTimePartitioning(new TimePartitioning().setType("DAY"))
            .withCreateDisposition(CreateDisposition.CREATE_NEVER)
            .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_APPEND));

    // TODO: deal with badData. Probably better to rename rawData into cleanData or something along those lines.

    p.run();
  }
}
