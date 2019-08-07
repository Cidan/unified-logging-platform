package com.google;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.bigquery.model.TableRow;
import java.util.HashMap;
import java.util.UUID;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.TupleTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LogToTableRowTransformer extends DoFn<String, TableRow> {

  private static final Logger LOG = LoggerFactory.getLogger(LogToTableRowTransformer.class);

  static final TupleTag<TableRow> badData = new TupleTag<TableRow>() {
    private static final long serialVersionUID = -767009006608923756L;
  };
  static final TupleTag<TableRow> cleanData = new TupleTag<TableRow>() {
    private static final long serialVersionUID = 2136448626752693877L;
  };
  private static final long serialVersionUID = -8532541222456695376L;

  private static final ObjectMapper objectMapper = new ObjectMapper();

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
      // TODO: potential data leakage, but better than no errors.
      LOG.error("Failed to process message: " + data.substring(50), e);
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
      LOG.error("Failed to serialized JSON: ", e);
    }
    output.set("uuid", UUID.randomUUID());
    // Set the raw string here.
    output.set("raw", data);

    // Output our log data
    c.output(output);
  }
}
