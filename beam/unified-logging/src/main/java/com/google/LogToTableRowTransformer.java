package com.google;

import com.google.api.client.googleapis.util.Utils;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.logging.v2.model.LogEntry;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.TupleTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LogToTableRowTransformer extends DoFn<String, TableRow> {

  private static final Logger LOG = LoggerFactory.getLogger(LogToTableRowTransformer.class);
  private static final JsonFactory jsonFactory = Utils.getDefaultJsonFactory();

  static final TupleTag<TableRow> badData = new TupleTag<TableRow>() {
    private static final long serialVersionUID = -767009006608923756L;
  };
  static final TupleTag<TableRow> cleanData = new TupleTag<TableRow>() {
    private static final long serialVersionUID = 2136448626752693877L;
  };
  private static final long serialVersionUID = -8532541222456695376L;

  // This function will create a TableRow (BigQuery row) out of String data
  // for later debugging.
  public TableRow createBadRow(String data) {
    TableRow output = new TableRow();
    output.set("timestamp", null /* TODO */);
    output.set("raw", data);
    output.set("uuid", UUID.randomUUID());
    return output;
  }

  // Our main decoder function.
  @SuppressWarnings("unchecked")
  @ProcessElement
  public void processElement(ProcessContext c) {
    // Get the JSON data as a string from our stream.
    String data = c.element();

    LogEntry logEntry;
    try {
      logEntry = jsonFactory.fromString(data, LogEntry.class);
    } catch (IOException e) {
      LOG.error("Failed to parse payload: " + data, e);
      c.output(badData, createBadRow(data));
      return;
    }

    TableRow output = new TableRow();
    output.set("severity", logEntry.getSeverity());
    output.set("timestamp", logEntry.getTimestamp());
    output.set("resource_type", logEntry.getResource().get("type"));
    output.set("project_id", logEntry.getResource().getLabels().get("project_id"));
    output.set("zone", logEntry.getResource().getLabels().get("zone"));
    output.set("text_payload", logEntry.getTextPayload());
    output.set("json_payload", convertMapToJson(logEntry.getJsonPayload()));
    output.set("proto_payload", convertMapToJson(logEntry.getProtoPayload()));
    output.set("raw", data);
    output.set("uuid", UUID.randomUUID());

    c.output(cleanData, output);
  }

  private static GenericJson convertMapToJson(Map<String, Object> objectMap) {
    if(objectMap == null) {
      return null;
    }
    GenericJson result = new GenericJson();
    result.putAll(objectMap);
    return result;
  }
}
