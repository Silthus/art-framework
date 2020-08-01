/*
 * Copyright 2020 ART-Framework Contributors (https://github.com/Silthus/art-framework)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.artframework.impl;

import io.artframework.ArtContext;
import io.artframework.CombinedResult;
import io.artframework.FutureResult;
import io.artframework.Target;
import lombok.NonNull;

import java.util.*;

public final class CombinedArtContext extends DefaultArtContext implements ArtContext {

    private final Set<ArtContext> contextSet = new HashSet<>();
    private final Map<String, Object> data = new HashMap<>();

    CombinedArtContext(ArtContext context1, ArtContext context2) {
        super(context1.configuration(), context1.settings(), new ArrayList<>());
        this.data.putAll(context1.data());
        this.contextSet.add(context1);

        combine(context2);
    }

    @Override
    public @NonNull Map<String, Object> data() {
        return data;
    }

    @Override
    public final <TTarget> CombinedResult test(@NonNull Target<TTarget> target) {
        return contextSet.stream()
                .map(artContext -> artContext.test(target))
                .reduce(CombinedResult::combine)
                .orElse(CombinedResult.of(empty()));
    }

    @Override
    public final <TTarget> FutureResult execute(@NonNull Target<TTarget> target) {
        return contextSet.stream()
                .map(artContext -> artContext.execute(target))
                .reduce(FutureResult::combine)
                .orElse(FutureResult.of(empty()));
    }

    @Override
    public ArtContext combine(ArtContext context) {

        if (contextSet.contains(context)) return this;

        this.contextSet.add(context);
        this.data.putAll(context.data());

        return this;
    }

    @Override
    public void close() {
        contextSet.forEach(ArtContext::close);
    }
}