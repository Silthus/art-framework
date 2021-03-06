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

package io.artframework.parser.flow;

import io.artframework.ConfigMap;
import io.artframework.Scope;
import io.artframework.TriggerFactory;
import io.artframework.conf.TriggerConfig;

import java.util.Iterator;
import java.util.Optional;

class TriggerLineParser extends ArtObjectContextLineParser<TriggerFactory> {

    public TriggerLineParser(Iterator<String> iterator, Scope scope) {
        super(scope, iterator, new FlowType("trigger", "@"));
    }

    @Override
    protected Optional<TriggerFactory> factory(String identifier) {
        return this.configuration().trigger().get(identifier);
    }

    @Override
    protected ConfigMap configMap() {
        return TriggerConfig.configMap();
    }
}
