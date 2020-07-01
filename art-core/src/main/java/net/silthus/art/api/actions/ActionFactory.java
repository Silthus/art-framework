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

package net.silthus.art.api.actions;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import net.silthus.art.api.Action;
import net.silthus.art.api.factory.ArtFactory;
import net.silthus.art.api.scheduler.Scheduler;

import javax.annotation.Nullable;

/**
 * The {@link ActionFactory} creates a fresh {@link ActionContext} for each unique
 * configuration of the registered {@link Action}s.
 * <br>
 * One {@link ActionFactory} is created per target type and {@link Action}.
 *
 * @param <TTarget> target type this factory accepts.
 * @param <TConfig> custom action config type used when creating the {@link ActionContext}.
 */
public class ActionFactory<TTarget, TConfig> extends ArtFactory<TTarget, TConfig, Action<TTarget, TConfig>, ActionConfig<TConfig>> {

    private final Scheduler scheduler;

    @Inject
    ActionFactory(@Assisted Class<TTarget> targetClass, @Assisted Action<TTarget, TConfig> action, @Nullable Scheduler scheduler) {
        super(targetClass, action);
        this.scheduler = scheduler;
    }

    @Override
    public ActionContext<TTarget, TConfig> create(ActionConfig<TConfig> config) {
        return new ActionContext<>(getTargetClass(), getArtObject(), config, scheduler);
    }
}
