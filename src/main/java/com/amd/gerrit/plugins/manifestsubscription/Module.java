// Copyright (C) 2015 Advanced Micro Devices, Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.amd.gerrit.plugins.manifestsubscription;

import com.google.gerrit.extensions.events.GitReferenceUpdatedListener;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

class Module extends AbstractModule {

  @Override
  protected void configure() {
    requestStaticInjection(ManifestSubscriptionConfig.class);

    bind(ManifestSubscription.class).in(Scopes.SINGLETON);

    DynamicSet.bind(binder(), LifecycleListener.class)
        .to(ManifestSubscription.class);
    DynamicSet.bind(binder(), GitReferenceUpdatedListener.class)
        .to(ManifestSubscription.class);
  }
}
