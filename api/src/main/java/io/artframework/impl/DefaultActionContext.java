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

import io.artframework.*;
import io.artframework.conf.ActionConfig;
import io.artframework.conf.Constants;
import io.artframework.util.TimeUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The action context is created for every unique {@link Action} configuration.
 * It holds all relevant information to execute the action and tracks the dependencies.
 *
 * @param <TTarget> target type of the action
 */
@Log(topic = "art-framework:action")
@Accessors(fluent = true)
public final class DefaultActionContext<TTarget> extends AbstractArtObjectContext<Action<TTarget>> implements ActionContext<TTarget>, FutureTargetResultCreator {

    private final Action<TTarget> action;
    @Getter
    private final ActionConfig config;
    @Getter
    private final ActionFactory<TTarget> factory;
    @Getter
    private final ConfigMap artObjectConfig;

    @Getter
    private final List<ActionContext<?>> actions = new ArrayList<>();
    @Getter
    private final List<RequirementContext<?>> requirements = new ArrayList<>();

    public DefaultActionContext(
            @NonNull Scope scope,
            @NonNull ActionConfig config,
            @NonNull ActionFactory<TTarget> factory,
            @NonNull ConfigMap artObjectConfig) {
        super(scope, factory.meta());
        this.config = config;
        this.factory = factory;
        this.artObjectConfig = artObjectConfig;
        this.action = null;
    }

    public DefaultActionContext(@NonNull Scope scope, ArtObjectMeta<Action<TTarget>> information, Action<TTarget> action, ActionConfig config) {

        super(scope, information);
        this.action = action;
        this.config = config;
        this.factory = null;
        this.artObjectConfig = null;
    }

    public Action<TTarget> action(Target<TTarget> target, ExecutionContext<ActionContext<TTarget>> context) {

        if (action != null) {
            return action;
        } else {
            return factory().create(artObjectConfig().resolve(scope(), target, context));
        }
    }

    @Override
    public void addAction(ActionContext<?> action) {
        this.actions.add(action);
    }

    @Override
    public final void addRequirement(RequirementContext<?> requirement) {
        this.requirements.add(requirement);
    }

    @Override
    public FutureResult execute(Target<TTarget> target, ExecutionContext<ActionContext<TTarget>> context) {

        if (!isTargetType(target)) {
            log.finest(target.source().getClass().getCanonicalName() + " does not match required target type: " + targetClass().getCanonicalName());
            return empty(target, this);
        }

        FutureResult executionTest = testExecution(target);
        if (executionTest.failure()) {
            log.finest("execution test failure: " + Arrays.toString(executionTest.messages()));
            return executionTest;
        }

        CombinedResult requirementTest = testRequirements(context);
        if (requirementTest.failure()) {
            log.finest("requirements check failure: " + Arrays.toString(requirementTest.messages()));
            return of(requirementTest, target, this);
        }

        final FutureResult result = empty(target, this);

        Runnable runnable = () -> {

            long startTime = System.nanoTime();

            Action<TTarget> action = action(target, context);
            log.finest("executing " + action.getClass().getCanonicalName() + " with " + target);
            Result actionResult = action
                    .execute(target, context)
                    .with(target, this);

            store(target, Constants.Storage.LAST_EXECUTION, System.currentTimeMillis());
            long endTime = System.nanoTime();
            log.finest("executed in " + (startTime - endTime) / 1000000 + "ms: " + Arrays.toString(actionResult.messages()));

            if (!actionResult.error()) {
                log.finest("executing " + actions().size() + " nested actions");
                result.complete(actionResult.combine(executeActions(target, context)));
            }
        };

        long delay = this.config().delay();

        if (configuration().scheduler().isPresent() && delay > 0) {
            log.finest("running delayed action " + delay + "ms delay");
            configuration().scheduler().get().runTaskLater(runnable, delay);
        } else {
            runnable.run();
        }

        return result;
    }

    /**
     * Checks if the {@link DefaultActionContext} has the execute_once option
     * and already executed once for the {@link Target}.
     *
     * @param target target to check
     * @return true if action was already executed and should only execute once
     */
    public FutureResult testExecutedOnce(Target<TTarget> target) {

        if (!this.config().executeOnce()) return empty(target, this);

        if (getLastExecution(target) > 0) {
            return failure(target, this, "Action can only be executed once and was already executed.");
        } else {
            return success(target, this);
        }
    }

    /**
     * Checks if the action is on cooldown for the given {@link Target}.
     * Will always return false if no cooldown is defined (set to zero).
     *
     * @param target target to check
     * @return a successful result if the action is not on cooldown a failure otherwise
     */
    public FutureResult testCooldown(Target<TTarget> target) {
        long cooldown = this.config().cooldown();
        if (cooldown < 1) return empty(target, this);

        long lastExecution = getLastExecution(target);

        if (lastExecution < 1) return success(target, this);

        long remainingCooldown = (lastExecution + cooldown) - System.currentTimeMillis();

        if (remainingCooldown > 0) {
            return failure(target, this, "Action is still on cooldown. "
                    + TimeUtil.getAccurrateShortFormatedTime(remainingCooldown) + " are remaining.");
        } else {
            return success(target, this);
        }
    }

    private FutureResult testExecution(Target<TTarget> target) {
        return testExecutedOnce(target).combine(testCooldown(target));
    }

    private long getLastExecution(Target<TTarget> target) {
        return store(target, Constants.Storage.LAST_EXECUTION, Long.class).orElse(0L);
    }
}
