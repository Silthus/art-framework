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

import io.artframework.ArtFinder;
import io.artframework.ArtFinderResult;
import io.artframework.Configuration;
import lombok.NonNull;

import java.io.File;
import java.util.function.Predicate;

public class DefaultArtFinder extends DefaultArtProvider implements ArtFinder {

    public DefaultArtFinder(@NonNull Configuration configuration) {
        super(configuration);
    }

    @Override
    public ArtFinderResult all() {
        return null;
    }

    @Override
    public ArtFinderResult allIn(File file) {
        return null;
    }

    @Override
    public ArtFinderResult allIn(File file, Predicate<File> predicate) {
        return null;
    }
}
