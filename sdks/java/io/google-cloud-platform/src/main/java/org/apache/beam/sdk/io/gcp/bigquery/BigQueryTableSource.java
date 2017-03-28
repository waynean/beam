/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.beam.sdk.io.gcp.bigquery;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.api.services.bigquery.model.TableReference;
import com.google.api.services.bigquery.model.TableRow;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryHelpers.TableRefToJson;
import org.apache.beam.sdk.options.BigQueryOptions;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.options.ValueProvider.NestedValueProvider;
import org.apache.beam.sdk.transforms.display.DisplayData;

/**
 * A {@link BigQuerySourceBase} for reading BigQuery tables.
 */
@VisibleForTesting
class BigQueryTableSource extends BigQuerySourceBase {

  static BigQueryTableSource create(
      ValueProvider<String> jobIdToken,
      ValueProvider<TableReference> table,
      String extractDestinationDir,
      BigQueryServices bqServices,
      ValueProvider<String> executingProject) {
    return new BigQueryTableSource(
        jobIdToken, table, extractDestinationDir, bqServices, executingProject);
  }

  private final ValueProvider<String> jsonTable;
  private final AtomicReference<Long> tableSizeBytes;

  private BigQueryTableSource(
      ValueProvider<String> jobIdToken,
      ValueProvider<TableReference> table,
      String extractDestinationDir,
      BigQueryServices bqServices,
      ValueProvider<String> executingProject) {
    super(jobIdToken, extractDestinationDir, bqServices, executingProject);
    this.jsonTable = NestedValueProvider.of(checkNotNull(table, "table"), new TableRefToJson());
    this.tableSizeBytes = new AtomicReference<>();
  }

  @Override
  protected TableReference getTableToExtract(BigQueryOptions bqOptions) throws IOException {
    checkState(jsonTable.isAccessible());
    return BigQueryIO.JSON_FACTORY.fromString(jsonTable.get(), TableReference.class);
  }

  @Override
  public BoundedReader<TableRow> createReader(PipelineOptions options) throws IOException {
    BigQueryOptions bqOptions = options.as(BigQueryOptions.class);
    checkState(jsonTable.isAccessible());
    TableReference tableRef = BigQueryIO.JSON_FACTORY.fromString(jsonTable.get(),
        TableReference.class);
    return new BigQueryReader(this, bqServices.getReaderFromTable(bqOptions, tableRef));
  }

  @Override
  public synchronized long getEstimatedSizeBytes(PipelineOptions options) throws Exception {
    if (tableSizeBytes.get() == null) {
      TableReference table = BigQueryIO.JSON_FACTORY.fromString(jsonTable.get(),
          TableReference.class);

      Long numBytes = bqServices.getDatasetService(options.as(BigQueryOptions.class))
          .getTable(table).getNumBytes();
      tableSizeBytes.compareAndSet(null, numBytes);
    }
    return tableSizeBytes.get();
  }

  @Override
  protected void cleanupTempResource(BigQueryOptions bqOptions) throws Exception {
    // Do nothing.
  }

  @Override
  public void populateDisplayData(DisplayData.Builder builder) {
    super.populateDisplayData(builder);
    builder.add(DisplayData.item("table", jsonTable));
  }
}
