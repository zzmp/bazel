// Copyright 2018 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License
package com.google.devtools.build.lib.rules.android;

import com.google.devtools.build.lib.actions.ActionAnalysisMetadata;
import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.actions.ArtifactRoot;
import com.google.devtools.build.lib.analysis.FilesToRunProvider;
import com.google.devtools.build.lib.analysis.RuleContext;
import com.google.devtools.build.lib.analysis.Whitelist;
import com.google.devtools.build.lib.analysis.actions.ActionConstructionContext;
import com.google.devtools.build.lib.analysis.actions.SpawnAction;
import com.google.devtools.build.lib.analysis.config.CompilationMode;
import com.google.devtools.build.lib.analysis.configuredtargets.RuleConfiguredTarget.Mode;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.packages.ImplicitOutputsFunction.SafeImplicitOutputsFunction;
import com.google.devtools.build.lib.packages.RuleErrorConsumer;
import com.google.devtools.build.lib.skylarkbuildapi.android.AndroidDataContextApi;
import com.google.devtools.build.lib.vfs.PathFragment;

/**
 * Wraps common tools and settings used for working with Android assets, resources, and manifests.
 *
 * <p>Do not create implementation classes directly - instead, get the appropriate one from {@link
 * com.google.devtools.build.lib.rules.android.AndroidSemantics}.
 *
 * <p>The {@link Label}, {@link ActionConstructionContext}, and BusyBox {@link FilesToRunProvider}
 * are needed to create virtually all actions for working with Android data, so it makes sense to
 * bundle them together. Additionally, this class includes some common tools (such as an SDK) that
 * are used in BusyBox actions.
 */
public class AndroidDataContext implements AndroidDataContextApi {

  private final Label label;
  private final RuleContext ruleContext;
  private final FilesToRunProvider busybox;
  private final AndroidSdkProvider sdk;
  private final boolean persistentBusyboxToolsEnabled;
  private final boolean throwOnProguardApplyDictionary;
  private final boolean throwOnProguardApplyMapping;
  private final boolean throwOnResourceConflict;
  private final boolean useDataBindingV2;

  public static AndroidDataContext forNative(RuleContext ruleContext) {
    return makeContext(ruleContext);
  }

  public static AndroidDataContext makeContext(RuleContext ruleContext) {
    AndroidConfiguration androidConfig =
        ruleContext.getConfiguration().getFragment(AndroidConfiguration.class);

    return new AndroidDataContext(
        ruleContext.getLabel(),
        ruleContext,
        ruleContext.getExecutablePrerequisite("$android_resources_busybox", Mode.HOST),
        androidConfig.persistentBusyboxTools(),
        AndroidSdkProvider.fromRuleContext(ruleContext),
        shouldThrowIfNotOnWhitelist(ruleContext, "allow_proguard_apply_dictionary"),
        shouldThrowIfNotOnWhitelist(ruleContext, "allow_proguard_apply_mapping"),
        shouldThrowIfNotOnWhitelist(ruleContext, "allow_resource_conflicts"),
        androidConfig.useDataBindingV2());
  }

  private static boolean shouldThrowIfNotOnWhitelist(
      RuleContext ruleContext, String whitelistName) {
    return Whitelist.hasWhitelist(ruleContext, whitelistName)
        && !Whitelist.isAvailable(ruleContext, whitelistName);
  }

  protected AndroidDataContext(
      Label label,
      RuleContext ruleContext,
      FilesToRunProvider busybox,
      boolean persistentBusyboxToolsEnabled,
      AndroidSdkProvider sdk,
      boolean throwOnProguardApplyDictionary,
      boolean throwOnProguardApplyMapping,
      boolean throwOnResourceConflict,
      boolean useDataBindingV2) {
    this.label = label;
    this.persistentBusyboxToolsEnabled = persistentBusyboxToolsEnabled;
    this.ruleContext = ruleContext;
    this.busybox = busybox;
    this.sdk = sdk;
    this.throwOnProguardApplyDictionary = throwOnProguardApplyDictionary;
    this.throwOnProguardApplyMapping = throwOnProguardApplyMapping;
    this.throwOnResourceConflict = throwOnResourceConflict;
    this.useDataBindingV2 = useDataBindingV2;
  }

  public Label getLabel() {
    return label;
  }

  public ActionConstructionContext getActionConstructionContext() {
    return ruleContext;
  }

  public RuleErrorConsumer getRuleErrorConsumer() {
    return ruleContext;
  }

  public FilesToRunProvider getBusybox() {
    return busybox;
  }

  public AndroidSdkProvider getSdk() {
    return sdk;
  }

  /*
   * Convenience methods. These are just slightly cleaner ways of doing common tasks.
   */

  /** Builds and registers a {@link SpawnAction.Builder}. */
  public void registerAction(SpawnAction.Builder spawnActionBuilder) {
    registerAction(spawnActionBuilder.build(ruleContext));
  }

  /** Registers one or more actions. */
  public void registerAction(ActionAnalysisMetadata... actions) {
    ruleContext.registerAction(actions);
  }

  public Artifact createOutputArtifact(SafeImplicitOutputsFunction function)
      throws InterruptedException {
    return ruleContext.getImplicitOutputArtifact(function);
  }

  public Artifact getUniqueDirectoryArtifact(String uniqueDirectorySuffix, String relative) {
    return ruleContext.getUniqueDirectoryArtifact(uniqueDirectorySuffix, relative);
  }

  public Artifact getUniqueDirectoryArtifact(String uniqueDirectorySuffix, PathFragment relative) {
    return ruleContext.getUniqueDirectoryArtifact(uniqueDirectorySuffix, relative);
  }

  public PathFragment getUniqueDirectory(PathFragment fragment) {
    return ruleContext.getUniqueDirectory(fragment);
  }

  public ArtifactRoot getBinOrGenfilesDirectory() {
    return ruleContext.getBinOrGenfilesDirectory();
  }

  public PathFragment getPackageDirectory() {
    return ruleContext.getPackageDirectory();
  }

  public AndroidConfiguration getAndroidConfig() {
    return ruleContext.getConfiguration().getFragment(AndroidConfiguration.class);
  }

  /** Indicates whether Busybox actions should be passed the "--debug" flag */
  public boolean useDebug() {
    return getActionConstructionContext().getConfiguration().getCompilationMode()
        != CompilationMode.OPT;
  }

  public boolean isPersistentBusyboxToolsEnabled() {
    return persistentBusyboxToolsEnabled;
  }

  public boolean throwOnProguardApplyDictionary() {
    return throwOnProguardApplyDictionary;
  }

  public boolean throwOnProguardApplyMapping() {
    return throwOnProguardApplyMapping;
  }

  public boolean throwOnResourceConflict() {
    return throwOnResourceConflict;
  }

  public boolean useDataBindingV2() {
    return useDataBindingV2;
  }
}
