package com.google;

import com.google.api.services.bigquery.model.TableRow;
import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.Write.CreateDisposition;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.Validation;
import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionTuple;
import org.apache.beam.sdk.values.TupleTagList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnifiedLogging {

  private static final Logger LOG = LoggerFactory.getLogger(UnifiedLogging.class);

  public static void main(String[] args) {
    Options options = PipelineOptionsFactory
        .fromArgs(args)
        .withValidation()
        .as(Options.class);

    Pipeline p = Pipeline.create(options);

    PCollectionTuple logProcessingOutcome = p
        .apply("Read from PubSub",
            PubsubIO.readStrings().fromSubscription(options.getSubscriptionName()))
        .apply("Transform LogEntry into TableRow",
            ParDo.of(new LogToTableRowTransformer())
                .withOutputTags(
                    LogToTableRowTransformer.cleanData,
                    TupleTagList.of(LogToTableRowTransformer.badData)));

    PCollection<TableRow> cleanLogRows = logProcessingOutcome
        .get(LogToTableRowTransformer.cleanData);

    cleanLogRows
        .apply("Store Clean Logs to BigQuery",
            BigQueryIO.writeTableRows().to(options.getOutputTable())
                .withCreateDisposition(CreateDisposition.CREATE_NEVER)
                .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_APPEND));

    // In a real application badData from logProcessingOutcome also need to be stored.
    // We omitted the code, which would be very similar to the block above, for brevity.
    p.run();
  }

  public interface Options extends DataflowPipelineOptions {

    @Validation.Required
    @Description("Subscription name")
    ValueProvider<String> getSubscriptionName();

    void setSubscriptionName(ValueProvider<String> value);

    @Validation.Required
    @Description("Output table to write to")
    ValueProvider<String> getOutputTable();

    void setOutputTable(ValueProvider<String> value);
  }
}
