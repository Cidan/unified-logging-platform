package com.google;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.bigquery.model.TableRow;

import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TupleTag;

public class UnifiedLogging {
  static String projectName;
  private static final ObjectMapper objectMapper = new ObjectMapper();
  
  static final TupleTag<TableRow> badData = new TupleTag<TableRow>(){
    private static final long serialVersionUID = -767009006608923756L;
  };
  static final TupleTag<TableRow> rawData = new TupleTag<TableRow>(){
    private static final long serialVersionUID = 2136448626752693877L;
  };

  static class DecodeMessage extends DoFn<String, KV<String,TableRow>> {
    private static final long serialVersionUID = -8532541222456695376L;

    // This function will create a TableRow (BigQuery row) out of String data
    // for later debugging.
    public TableRow createBadRow(String data) {
      TableRow output = new TableRow();
      output.set("json", data);
      return output;
    }

    // Our main decoder function.
    @ProcessElement
    public void processElement(ProcessContext c) {
      // Get the JSON data as a string from our stream.
      String data = c.element();
      TableRow output;

      // Attempt to decode our JSON data into a TableRow.
      try {
        output = objectMapper.readValue(data, TableRow.class);
      } catch (Exception e) {
        // We were unable to decode the JSON, let's put this string
        // into a TableRow manually, without decoding it, so we can debug
        // it later, and output it as "bad data".
        c.output(badData, createBadRow(data));
        return;
      }

      // Incredibly simple validation of data -- we are simply making sure
      // the event key exists in our decoded JSON. If it doesn't, it's bad data.
      // If you need to validate your data stream, this would be the place to do it!
      if (!output.containsKey("Name")) {
        c.output(badData, createBadRow(data));        
        return;
      }

      // Output our good data twice to two different streams. rawData will eventually
      // be directly inserted into BigQuery without any special processing, where as
      // our other output (the "default" output), outputs a KV of (event, TableRow), where
      // event is the name of our event. This allows us to do some easy rollups of our event
      // data a bit further down.
      c.output(rawData, output);
      c.output(KV.of((String)output.get("Name"), output));
    }
  }
  
  public static void main(String[] args) {
    
    DataflowPipelineOptions options = PipelineOptionsFactory
    .fromArgs(args)
    .withValidation()
    .as(DataflowPipelineOptions.class);

    projectName = options.getProject();
    
    String subscription = "projects/"
    + projectName
    + "/subscriptions/"
    + "unified-logging";

    Pipeline p = Pipeline.create(options);

    PCollection<String> pubsubStream = p
      .apply("Read from Pub/Sub", PubsubIO
        .readStrings()
      .fromSubscription(subscription));
    
    p.run();
  }
}
