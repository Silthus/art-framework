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

package net.silthus.art.api.parser;

import com.google.inject.ImplementedBy;
import lombok.NonNull;
import net.silthus.art.ART;
import net.silthus.art.DefaultArtResult;
import net.silthus.art.api.Action;
import net.silthus.art.api.Requirement;
import net.silthus.art.api.Trigger;
import net.silthus.art.api.config.ArtConfig;
import net.silthus.art.api.trigger.Target;
import net.silthus.art.api.trigger.TriggerListener;

import javax.annotation.concurrent.Immutable;
import java.util.Collection;

/**
 * The {@link ArtResult} is a core piece of the ART-Framework.
 * Use it to test for {@link Requirement}s or execute {@link Action}s.
 * <br>
 * Test if all requirements are met by calling {@link #test(Target)}.
 * You can pass additional filters by calling {@link #test(Target, Collection)}.
 * <br>
 * Execute your actions by calling {@link #execute(Target)}.
 * <br>
 * It is created from an {@link ArtConfig} and holds all loaded {@link Action}s, {@link Requirement}s and {@link Trigger}.
 * Create an {@link ArtResult} by parsing your {@link ArtConfig} with {@link ART#load(ArtConfig)}.
 * <br>
 * The {@link ArtResult} is immutable and accepts any input, including null without throwing an exception.
 */
@Immutable
@ImplementedBy(DefaultArtResult.class)
public interface ArtResult {

    /**
     * Tests if all requirements for the given target pass.
     * Will return false if any requirement or global filter fail.
     * Will return true if requirements are empty after filtering for the target type.
     * <br>
     * Global filters are always checked before checking requirements.
     * This means that persistent counted requirements will never be checked and increased.
     * <br>
     * Use the {@link #test(Target, Collection)} method if you want to apply local filters before checking requirements.
     *
     * @param target    target to check. Can be null.
     * @param <TTarget> type of the target. Any requirements not matching the target type will not be checked.
     * @return true if all requirement checks and filter pass or if the list of requirements is empty (after filtering the target type).
     * false if any filter or requirement check fails.
     * @see #test(Target, Collection)
     */
    <TTarget> boolean test(@NonNull Target<TTarget> target);

    /**
     * Wraps the given target into a {@link Target} and then calls {@link #test(Target)}.
     * Returns false if no {@link Target} wrapper was found for the given source.
     *
     * @param target    target object to wrap into a {@link Target}
     * @param <TTarget> type of the target
     * @return result of {@link #test(Target)} or false if no {@link Target} wrapper exists
     * @see #test(Target)
     */
    default <TTarget> boolean test(@NonNull TTarget target) {
        return Target.of(target).map(this::test).orElse(false);
    }

    /**
     * Tests if all requirements for the given target pass after testing if all filters pass.
     * Does the same as {@link #test(Target)}, except it first checks the provided filters.
     * Will return false as soon as any filter fails.
     *
     * @param target    target to check requirements and filter against. Can be null.
     * @param filters   list of local filters to check before anything else
     * @param <TTarget> type of the target
     * @return true if all filter checks pass and {@link #test(Target)} returns true.
     * false if any check or filter fails.
     * @see #test(Target)
     */
    <TTarget> boolean test(@NonNull Target<TTarget> target, Collection<ArtResultFilter<TTarget>> filters);

    /**
     * Wraps the given target into a {@link Target} and then calls {@link #test(Target, Collection)}.
     * Returns false if no {@link Target} wrapper was found for the given source.
     *
     * @param target    target object to wrap into a {@link Target}
     * @param filters   list of local filters to check before anything else
     * @param <TTarget> type of the target
     * @return result of {@link #test(Target, Collection)} or false if no {@link Target} wrapper exists
     * @see #test(Target, Collection)
     */
    default <TTarget> boolean test(@NonNull TTarget target, Collection<ArtResultFilter<TTarget>> filters) {
        return Target.of(target).map(tTargetTarget -> test(tTargetTarget, filters)).orElse(false);
    }

    /**
     * Executes all {@link Action}s and child actions of actions against the given target.
     * Will do nothing if the target type does not match the target type of the action.
     * <br>
     * Any {@link ArtResultFilter} and {@link Requirement}s will be checked before executing
     * the actions. No action will be executed if any filter or requirement fails.
     *
     * @param target    target to execute actions against. Can be null.
     * @param <TTarget> type of the target
     * @see #execute(Target, Collection)
     */
    <TTarget> void execute(@NonNull Target<TTarget> target);

    /**
     * Wraps the given target into a {@link Target} and then calls {@link #execute(Target)}.
     * Does nothing if no {@link Target} wrapper was found for the given source.
     *
     * @param target    target to execute actions for
     * @param <TTarget> type of the target
     */
    default <TTarget> void execute(@NonNull TTarget target) {
        Target.of(target).ifPresent(this::execute);
    }

    /**
     * Executes all {@link Action}s and child actions of actions against the given target
     * after checking the list of given filters.
     * <br>
     * Also see {@link #execute(Target)}
     *
     * @param target    target to check filters and execute actions against.
     * @param filters   list of local filters to test before executing actions
     * @param <TTarget> type of the target
     * @see #execute(Target)
     */
    <TTarget> void execute(@NonNull Target<TTarget> target, Collection<ArtResultFilter<TTarget>> filters);

    /**
     * Wraps the given target into a {@link Target} and then calls {@link #execute(Target, Collection)}.
     * Does nothing if no {@link Target} wrapper was found for the given source.
     *
     * @param target    target to execute actions for
     * @param filters   list of local filters to test before executing actions
     * @param <TTarget> type of the target
     */
    default <TTarget> void execute(@NonNull TTarget target, Collection<ArtResultFilter<TTarget>> filters) {
        Target.of(target).ifPresent(tTargetTarget -> execute(tTargetTarget, filters));
    }

    /**
     * Listens on all {@link Trigger}s in the {@link ArtResult} for the given target type.
     * You can add multiple {@link TriggerListener}s of the same target type
     * and all of them will get informed.
     * <br>
     * You will only get informed of the trigger execution after all previous
     * checks have passed and after all {@link Action}s of this {@link ArtResult}
     * have been executed.
     *
     *
     * @param targetClass class of the target you wish to listen for
     * @param triggerConsumer function to react to the trigger
     * @param <TTarget> type of the target
     */
    <TTarget> void onTrigger(Class<TTarget> targetClass, TriggerListener<TTarget> triggerConsumer);
}
