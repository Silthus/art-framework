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

package io.artframework.events;

public abstract class RegisteredEvent {

    protected final EventListener listener;
    protected final EventExecutor executor;
    protected final EventHandler info;

    public RegisteredEvent(final EventListener listener, final EventExecutor executor, EventHandler info) {

        this.listener = listener;
        this.executor = executor;
        this.info = info;
    }

    /**
     * Gets the listener for this registration
     *
     * @return Registered Listener
     */
    public EventListener getListener() {

        return listener;
    }

    public EventPriority getPriority() {

        return info.priority();
    }

    /**
     * Calls the event executor
     *
     * @param trigger The event
     */
    public void callTrigger(final Event trigger) throws EventException {

        if (trigger instanceof Cancellable && !isIgnoringCancelled() && ((Cancellable) trigger).isCancelled()) {
            return;
        }
        call(trigger);
    }

    public boolean isIgnoringCancelled() {

        return info.ignoreCancelled();
    }

    protected abstract void call(final Event trigger) throws EventException;
}
