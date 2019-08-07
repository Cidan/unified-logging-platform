package com.google.logs;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.bigquery.model.TableRow;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.TupleTag;

// TODO: Never used.
public class Generic {
  private static final ObjectMapper objectMapper = new ObjectMapper();
  
  static final TupleTag<TableRow> badData = new TupleTag<TableRow>(){
    private static final long serialVersionUID = -767009006608923756L;
  };
  
  static final TupleTag<TableRow> rawData = new TupleTag<TableRow>(){
    private static final long serialVersionUID = 2136448626752693877L;
  };

  static class DecodeMessage extends DoFn<String, TableRow> {
    private static final long serialVersionUID = -8532541222456695376L;

    public TableRow createBadRow(String data) {
      TableRow output = new TableRow();
      output.set("json", data);
      return output;
    }

    // Our main decoder function.
    @ProcessElement
    public void processElement(ProcessContext c) {
      // Get the JSON data as a string from  our stream.
      String data = c.element();
      TableRow decoded;

      // Attempt to decode our JSON data into a TableRow.
      try {
        decoded = objectMapper.readValue(data, TableRow.class);
      } catch (Exception e) {
        // We were unable to decode the JSON, let's put this string
        // into a TableRow manually, without decoding it, so we can debug
        // it later, and output it as "bad data".
        c.output(badData, createBadRow(data));
        return;
      }

      if (!decoded.containsKey("uuid")) {
        decoded.set("uuid", UUID.randomUUID());
      }

      // Output our log data
      c.output(decoded);
    }
  }
}