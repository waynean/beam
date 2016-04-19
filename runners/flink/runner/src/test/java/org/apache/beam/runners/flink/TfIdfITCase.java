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
package org.apache.beam.runners.flink;

import org.apache.beam.examples.complete.TfIdf;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.coders.StringDelegateCoder;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.Keys;
import org.apache.beam.sdk.transforms.RemoveDuplicates;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;

import com.google.common.base.Joiner;

import org.apache.flink.test.util.JavaProgramTestBase;

import java.net.URI;


public class TfIdfITCase extends JavaProgramTestBase {

  protected String resultPath;

  public TfIdfITCase(){
  }

  static final String[] EXPECTED_RESULT = new String[] {
      "a", "m", "n", "b", "c", "d"};

  @Override
  protected void preSubmit() throws Exception {
    resultPath = getTempDirPath("result");
  }

  @Override
  protected void postSubmit() throws Exception {
    compareResultsByLinesInMemory(Joiner.on('\n').join(EXPECTED_RESULT), resultPath);
  }

  @Override
  protected void testProgram() throws Exception {

    Pipeline pipeline = FlinkTestPipeline.createForBatch();

    pipeline.getCoderRegistry().registerCoder(URI.class, StringDelegateCoder.of(URI.class));

    PCollection<KV<String, KV<URI, Double>>> wordToUriAndTfIdf = pipeline
        .apply(Create.of(
            KV.of(new URI("x"), "a b c d"),
            KV.of(new URI("y"), "a b c"),
            KV.of(new URI("z"), "a m n")))
        .apply(new TfIdf.ComputeTfIdf());

    PCollection<String> words = wordToUriAndTfIdf
        .apply(Keys.<String>create())
        .apply(RemoveDuplicates.<String>create());

    words.apply(TextIO.Write.to(resultPath));

    pipeline.run();
  }
}

