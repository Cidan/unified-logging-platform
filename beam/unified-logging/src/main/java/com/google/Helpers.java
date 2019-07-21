package com.google;

import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableSchema;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Helpers {
  // Define our BigQuery schema in code
  static final Map<String, String> rawSchema;
  static {
    rawSchema = new HashMap<String, String>();
    rawSchema.put("timestamp", "TIMESTAMP");
    rawSchema.put("resource_type", "STRING");
    rawSchema.put("project_id", "STRING");
    rawSchema.put("zone", "STRING");
    rawSchema.put("text_payload", "STRING");
    rawSchema.put("json_payload", "STRING");
    rawSchema.put("proto_payload", "STRING");
    rawSchema.put("uuid", "STRING");
    rawSchema.put("raw", "STRING");
  }
  
  public static TableSchema generateSchema(Map<String,String> fi) {
    List<TableFieldSchema> fields = new ArrayList<>();
    Iterator<Map.Entry<String,String>> it = fi.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, String> pair = (Map.Entry<String, String>)it.next();
      fields.add(new TableFieldSchema().setName(pair.getKey()).setType(pair.getValue()));
    }
    return new TableSchema().setFields(fields);
  }

}
