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

package io.artframework.annotations;

import io.artframework.ArtObject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Every art object and module needs an ART annotation to provide the identifier and mark it as an art object.
 * <p>
 * Make sure that the provided identifier is unique across all ART. To do this it is highly recommended
 * to prefix your ART with a groupId (java) like prefix, e.g. <code>io.art-framework:my-action</code>.
 * <p>
 * Use this annotation to provide additional aliases and descriptions for your {@link ArtObject} and {@link ArtModule}s.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ART {

    /**
     * An identifier for your actions, requirements and trigger is always needed.
     * Use this annotation to provide a unique identifier.
     * <p>
     * Make sure that the provided identifier is unique across all ART. To do this it is highly recommended
     * to prefix your ART with a groupId (java) like prefix, e.g. <code>io.art-framework:my-action</code>.
     *
     * @return the unique identifier of this art object
     */
    String value();

    /**
     * You can provide a list of alias names for your actions, requirements and trigger.
     * ART aliases are only registered if there is no existing identifier.
     *
     * @return a list of aliases for this art object
     */
    String[] alias() default {};

    /**
     * Optionally provide a detailed description about what your ArtObject does.
     * This helps users of your ART when selecting an appropriate object.
     *
     * @return detailed description
     */
    String[] description() default {};

    /**
     * The tagged art-object will be automatically discovered and registered if this is true.
     * <p>
     * Set it to false if you need custom constructor parameters or want to register your
     * art manually through the art-provider.
     *
     * @return true if this art-object should be automatically registered with the provider
     */
    boolean autoRegister() default true;
}
