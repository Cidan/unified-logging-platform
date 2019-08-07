package com.google.logs;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.google.api.services.bigquery.model.TableRow;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.TupleTag;
import org.apache.flume.Event;
import org.apache.flume.source.SyslogParser;

// TODO: Never used.
public class Syslog {
  
  static final TupleTag<TableRow> badData = new TupleTag<TableRow>(){
    private static final long serialVersionUID = -767009006608923756L;
  };
  
  static final TupleTag<TableRow> rawData = new TupleTag<TableRow>(){
    private static final long serialVersionUID = 2136448626752693877L;
  };

  static class DecodeMessage extends DoFn<String, TableRow> {

    private static final long serialVersionUID = -5234027155154450764L;

    public TableRow createBadRow(String data) {
      TableRow output = new TableRow();
      output.set("msg", data);
      return output;
    }

    // Our main decoder function.
    @ProcessElement
    public void processElement(ProcessContext c) {
      String data = c.element();
      SyslogParser syslogParser = new SyslogParser();
      TableRow decoded = new TableRow();
      Charset charset = Charset.forName("UTF8");
      Set<String> keepFields = new HashSet<String>();
      Event event;

      try {
        event = syslogParser.parseMessage(data, charset, keepFields);
      } catch (Exception e) {
        c.output(badData, createBadRow(data));
        return;
      }

      // Set all of our syslog headers as fields
      event.getHeaders().forEach((k,v) ->
        decoded.set(k, v)
      );

      // Set our syslog message
      decoded.set("msg", event.getBody().toString());

      // Set a UUID if one doesn't exist
      if (!decoded.containsKey("uuid")) {
        decoded.set("uuid", UUID.randomUUID());
      }

      // Output our log data
      c.output(decoded);
    }
  }
}