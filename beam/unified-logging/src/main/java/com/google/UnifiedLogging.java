package com.google;

import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.values.PCollection;

public class UnifiedLogging {
  static String projectName;
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