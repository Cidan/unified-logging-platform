package com.google;

import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;

public class UnifiedLogging {
  static String projectName;
  public static void main(String[] args) {
    DataflowPipelineOptions options = PipelineOptionsFactory
    .fromArgs(args)
    .withValidation()
    .as(DataflowPipelineOptions.class);
  projectName = options.getProject();
  }
}