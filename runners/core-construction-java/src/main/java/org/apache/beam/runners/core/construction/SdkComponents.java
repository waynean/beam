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

package org.apache.beam.runners.core.construction;

import com.google.common.base.Equivalence;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.io.IOException;
import java.util.Set;
import org.apache.beam.sdk.annotations.Experimental;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.common.runner.v1.RunnerApi;
import org.apache.beam.sdk.common.runner.v1.RunnerApi.Components;
import org.apache.beam.sdk.transforms.AppliedPTransform;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.util.NameUtils;
import org.apache.beam.sdk.util.WindowingStrategy;
import org.apache.beam.sdk.values.PCollection;

/** SDK objects that will be represented at some later point within a {@link Components} object. */
class SdkComponents {
  private final RunnerApi.Components.Builder componentsBuilder;

  private final BiMap<AppliedPTransform<?, ?, ?>, String> transformIds;
  private final BiMap<PCollection<?>, String> pCollectionIds;
  private final BiMap<WindowingStrategy<?, ?>, String> windowingStrategyIds;

  /** A map of Coder to IDs. Coders are stored here with identity equivalence. */
  private final BiMap<Equivalence.Wrapper<? extends Coder<?>>, String> coderIds;
  // TODO: Specify environments

  /** Create a new {@link SdkComponents} with no components. */
  static SdkComponents create() {
    return new SdkComponents();
  }

  private SdkComponents() {
    this.componentsBuilder = RunnerApi.Components.newBuilder();
    this.transformIds = HashBiMap.create();
    this.pCollectionIds = HashBiMap.create();
    this.windowingStrategyIds = HashBiMap.create();
    this.coderIds = HashBiMap.create();
  }

  /**
   * Registers the provided {@link AppliedPTransform} into this {@link SdkComponents}, returning a
   * unique ID for the {@link AppliedPTransform}. Multiple registrations of the same
   * {@link AppliedPTransform} will return the same unique ID.
   */
  String registerPTransform(AppliedPTransform<?, ?, ?> pTransform) {
    String existing = transformIds.get(pTransform);
    if (existing != null) {
      return existing;
    }
    String name = pTransform.getFullName();
    if (name.isEmpty()) {
      name = uniqify("unnamed_ptransform", transformIds.values());
    }
    transformIds.put(pTransform, name);
    return name;
  }

  /**
   * Registers the provided {@link PCollection} into this {@link SdkComponents}, returning a unique
   * ID for the {@link PCollection}. Multiple registrations of the same {@link PCollection} will
   * return the same unique ID.
   */
  String registerPCollection(PCollection<?> pCollection) {
    String existing = pCollectionIds.get(pCollection);
    if (existing != null) {
      return existing;
    }
    String uniqueName = uniqify(pCollection.getName(), pCollectionIds.values());
    pCollectionIds.put(pCollection, uniqueName);
    return uniqueName;
  }

  /**
   * Registers the provided {@link WindowingStrategy} into this {@link SdkComponents}, returning a
   * unique ID for the {@link WindowingStrategy}. Multiple registrations of the same {@link
   * WindowingStrategy} will return the same unique ID.
   */
  String registerWindowingStrategy(WindowingStrategy<?, ?> windowingStrategy) {
    String existing = windowingStrategyIds.get(windowingStrategy);
    if (existing != null) {
      return existing;
    }
    String baseName =
        String.format(
            "%s(%s)",
            NameUtils.approximateSimpleName(windowingStrategy),
            NameUtils.approximateSimpleName(windowingStrategy.getWindowFn()));
    String name = uniqify(baseName, windowingStrategyIds.values());
    windowingStrategyIds.put(windowingStrategy, name);
    return name;
  }

  /**
   * Registers the provided {@link Coder} into this {@link SdkComponents}, returning a unique ID for
   * the {@link Coder}. Multiple registrations of the same {@link Coder} will return the same
   * unique ID.
   *
   * <p>Coders are stored by identity to ensure that coders with implementations of {@link
   * #equals(Object)} and {@link #hashCode()} but incompatible binary formats are not considered the
   * same coder.
   */
  String registerCoder(Coder<?> coder) throws IOException {
    String existing = coderIds.get(Equivalence.identity().wrap(coder));
    if (existing != null) {
      return existing;
    }
    String baseName = NameUtils.approximateSimpleName(coder);
    String name = uniqify(baseName, coderIds.values());
    coderIds.put(Equivalence.identity().wrap(coder), name);
    RunnerApi.Coder coderProto = Coders.toProto(coder, this);
    componentsBuilder.putCoders(name, coderProto);
    return name;
  }

  private String uniqify(String baseName, Set<String> existing) {
    String name = baseName;
    int increment = 1;
    while (existing.contains(name)) {
      name = baseName + Integer.toString(increment);
      increment++;
    }
    return name;
  }

  /**
   * Convert this {@link SdkComponents} into a {@link RunnerApi.Components}, including all of the
   * contained {@link Coder coders}, {@link WindowingStrategy windowing strategies}, {@link
   * PCollection PCollections}, and {@link PTransform PTransforms}.
   */
  @Experimental
  RunnerApi.Components toComponents() {
    return componentsBuilder.build();
  }
}
