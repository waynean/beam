/*
 * Copyright (C) 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.dataflow.sdk.util;

import com.google.cloud.dataflow.sdk.coders.Coder;
import com.google.cloud.dataflow.sdk.coders.StringUtf8Coder;
import com.google.cloud.dataflow.sdk.transforms.windowing.BoundedWindow;
import com.google.cloud.dataflow.sdk.util.GroupAlsoByWindowsProperties.GroupAlsoByWindowsDoFnFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit tests for {@link GroupAlsoByWindowsViaOutputBufferDoFn}.
 */
@RunWith(JUnit4.class)
public class GroupAlsoByWindowsViaOutputBufferDoFnTest {

  private class BufferingGABWViaOutputBufferDoFnFactory<K, InputT>
  implements GroupAlsoByWindowsDoFnFactory<K, InputT, Iterable<InputT>> {

    private final Coder<InputT> inputCoder;

    public BufferingGABWViaOutputBufferDoFnFactory(Coder<InputT> inputCoder) {
      this.inputCoder = inputCoder;
    }

    @Override
    public <W extends BoundedWindow> GroupAlsoByWindowsDoFn<K, InputT, Iterable<InputT>, W>
        forStrategy(WindowingStrategy<?, W> windowingStrategy) {
      return new GroupAlsoByWindowsViaOutputBufferDoFn<K, InputT, Iterable<InputT>, W>(
          windowingStrategy,
          SystemReduceFn.<K, InputT, W>buffering(inputCoder));
    }
  }

  @Test
  public void testEmptyInputEmptyOutput() throws Exception {
    GroupAlsoByWindowsProperties.emptyInputEmptyOutput(
        new BufferingGABWViaOutputBufferDoFnFactory<>(StringUtf8Coder.of()));
  }

  @Test
  public void testGroupsElementsIntoFixedWindows() throws Exception {
    GroupAlsoByWindowsProperties.groupsElementsIntoFixedWindows(
        new BufferingGABWViaOutputBufferDoFnFactory<String, String>(StringUtf8Coder.of()));
  }

  @Test
  public void testGroupsElementsIntoSlidingWindows() throws Exception {
    GroupAlsoByWindowsProperties.groupsElementsIntoSlidingWindows(
        new BufferingGABWViaOutputBufferDoFnFactory<String, String>(StringUtf8Coder.of()));
  }

  @Test
  public void testGroupsIntoOverlappingNonmergingWindows() throws Exception {
    GroupAlsoByWindowsProperties.groupsIntoOverlappingNonmergingWindows(
        new BufferingGABWViaOutputBufferDoFnFactory<String, String>(StringUtf8Coder.of()));
  }

  @Test
  public void testGroupsIntoSessions() throws Exception {
    GroupAlsoByWindowsProperties.groupsElementsInMergedSessions(
        new BufferingGABWViaOutputBufferDoFnFactory<String, String>(StringUtf8Coder.of()));
  }

  @Test
  public void testGroupsElementsIntoFixedWindowsWithEndOfWindowTimestamp() throws Exception {
    GroupAlsoByWindowsProperties.groupsElementsIntoFixedWindowsWithEndOfWindowTimestamp(
        new BufferingGABWViaOutputBufferDoFnFactory<String, String>(StringUtf8Coder.of()));
  }

  @Test
  public void testGroupsElementsIntoFixedWindowsWithLatestTimestamp() throws Exception {
    GroupAlsoByWindowsProperties.groupsElementsIntoFixedWindowsWithLatestTimestamp(
        new BufferingGABWViaOutputBufferDoFnFactory<String, String>(StringUtf8Coder.of()));
  }

  @Test
  public void testGroupsElementsIntoFixedWindowsWithCustomTimestamp() throws Exception {
    GroupAlsoByWindowsProperties.groupsElementsIntoFixedWindowsWithCustomTimestamp(
        new BufferingGABWViaOutputBufferDoFnFactory<String, String>(StringUtf8Coder.of()));
  }

  @Test
  public void testGroupsElementsIntoSessionsWithEndOfWindowTimestamp() throws Exception {
    GroupAlsoByWindowsProperties.groupsElementsInMergedSessionsWithEndOfWindowTimestamp(
        new BufferingGABWViaOutputBufferDoFnFactory<String, String>(StringUtf8Coder.of()));
  }

  @Test
  public void testGroupsElementsIntoSessionsWithLatestTimestamp() throws Exception {
    GroupAlsoByWindowsProperties.groupsElementsInMergedSessionsWithLatestTimestamp(
        new BufferingGABWViaOutputBufferDoFnFactory<String, String>(StringUtf8Coder.of()));
  }
}
