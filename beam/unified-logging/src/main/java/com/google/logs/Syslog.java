package com.google.logs;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.google.api.services.bigquery.model.TableRow;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.flume.source.SyslogParser;

public class Syslog {
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
      // Get the JSON data as a string from  our stream.
      String data = c.element();
      SyslogParser n = new SyslogParser();
      TableRow decoded = new TableRow();
      Charset charset = Charset.forName("UTF8");
      Set<String> keepFields = new HashSet<String>();

      try {
        n.parseMessage(data, charset, keepFields);
      } catch (Exception e) {

      }
      /*
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
      */
      if (!decoded.containsKey("uuid")) {
        decoded.set("uuid", UUID.randomUUID());
      }

      // Output our log data
      c.output(decoded);
    }
  }
}