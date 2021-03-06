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

package io.artframework.util;

import com.google.common.base.Strings;
import io.artframework.Target;
import lombok.NonNull;
import lombok.extern.java.Log;

import javax.annotation.Nullable;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log(topic = "art-framework")
public final class ReflectionUtil {

    // always edit the regexr link and update the link below!
    // the regexr link and the regex should always match
    // https://regexr.com/59dgv
    private static final Pattern QUOTED_STRING_ARRAY = Pattern.compile("^(\"(?<quoted>.*?)\")?(?<value>.*?)?,(?<rest>.*)$");

    @SuppressWarnings("rawtypes")
    public static Class getTypeArgument(@NonNull Object object, int position) {
        Type genericSuperclass = object instanceof Class ? ((Class) object).getGenericSuperclass() : object.getClass().getGenericSuperclass();
        return ((Class) ((ParameterizedType) genericSuperclass).getActualTypeArguments()[position]);
    }

    public static Object toObject(Class<?> fieldType, String value) {

        if (fieldType.isArray()) {
            return toArray(fieldType.getComponentType(), value);
        }

        if (Boolean.class == fieldType || Boolean.TYPE == fieldType) return Boolean.parseBoolean(value);
        if (Byte.class == fieldType || Byte.TYPE == fieldType) return Byte.parseByte(value);
        if (Short.class == fieldType || Short.TYPE == fieldType) return Short.parseShort(value);
        if (Integer.class == fieldType || Integer.TYPE == fieldType) return Integer.parseInt(value);
        if (Long.class == fieldType || Long.TYPE == fieldType) return Long.parseLong(value);
        if (Float.class == fieldType || Float.TYPE == fieldType) return Float.parseFloat(value);
        if (Double.class == fieldType || Double.TYPE == fieldType) return Double.parseDouble(value);

        return value;
    }

    public static Object toArray(Class<?> arrayType, String input) {
        ArrayList<String> strings = new ArrayList<>();

        Matcher matcher = QUOTED_STRING_ARRAY.matcher(input);

        while (matcher.matches()) {
            String quoted = matcher.group("quoted");
            String value = matcher.group("value");
            input = matcher.group("rest");
            if (quoted != null) {
                strings.add(quoted);
            } else {
                strings.add(value);
            }

            if (!Strings.isNullOrEmpty(input)) {
                matcher = QUOTED_STRING_ARRAY.matcher(input);
            } else {
                break;
            }
        }
        strings.add(input);

        String[] result = strings.toArray(new String[0]);
        Object array = Array.newInstance(arrayType, result.length);
        for (int i = 0; i < result.length; i++) {
            Array.set(array, i, toObject(arrayType, result[i].trim()));
        }

        return array;
    }

    /**
     * Takes the given map and target and tries to extract the nearest possible type match of the target.
     *
     * @param target    target to find match for
     * @param map       map to find match in
     * @param <TTarget> target type
     * @param <TResult> result type of the map
     * @return extracted map value if the target type matched and was found
     */
    public static <TTarget, TResult> Optional<TResult> getEntryForTarget(@NonNull TTarget target, @NonNull Map<Class<?>, TResult> map) {

        if (target instanceof Target) {
            return getEntryForTarget(((Target<?>) target).source(), map);
        } else if (target instanceof Optional) {
            if (((Optional<?>) target).isEmpty()) {
                return Optional.empty();
            } else {
                return getEntryForTarget(((Optional<?>) target).get(), map);
            }
        }

        return getEntryForTargetClass(target.getClass(), map);
    }

    /**
     * Takes the given map and class and tries to extract the nearest possible type match for the class in the map.
     *
     * @param targetClass the target class and matching key of the map
     * @param map the map to extract
     * @param <TResult> the type of the result
     * @return the matched target result if found
     */
    public static <TResult> Optional<TResult> getEntryForTargetClass(@NonNull Class<?> targetClass, @NonNull Map<Class<?>, TResult> map) {

        if (map.containsKey(targetClass)) {
            return Optional.ofNullable(map.get(targetClass));
        }

        Class<?> currentTargetClass = null;
        TResult result = null;
        for (Map.Entry<Class<?>, TResult> entry : map.entrySet()) {
            if (entry.getKey().isAssignableFrom(targetClass)) {
                // pick the nearest possible result we can find
                if (currentTargetClass == null || currentTargetClass.isAssignableFrom(entry.getKey())) {
                    currentTargetClass = entry.getKey();
                    result = entry.getValue();
                }
            }
        }

        return Optional.ofNullable(result);
    }

    /**
     * Tries to extract the generic type provided by the interface implementation of the class.
     * <p>This does not work if the implementing class uses a generic to provide the interface type.
     * <pre>{@code
     * public class MyResolver implements Resolver<Foobar> {}
     *
     * // returns the type Foobar.class
     * getInterfaceTypeArgument(MyResolver.class, Resolver.class, 0);
     * }</pre>
     * @param clazz the class that should have the type of an implementing interface extracted
     * @param interfaceType the type of the interface to extract
     * @param position the generic argument position in the interface. starting at index 0.
     * @return the extracted type if found.
     */
    public static Optional<Class<?>> getInterfaceTypeArgument(Class<?> clazz, Class<?> interfaceType, int position) {

        Class<?> foundClass = null;
        Type[] genericInterfaces = clazz.getGenericInterfaces();

        for (Type genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType && ((ParameterizedType) genericInterface).getRawType().equals(interfaceType)) {
                Type[] genericTypes = ((ParameterizedType) genericInterface).getActualTypeArguments();
                if (genericTypes.length > position) {
                    try {
                        foundClass = (Class<?>) genericTypes[position];
                    } catch (ClassCastException ignored) {
                    }
                    break;
                }
            }
        }

        if (foundClass == null &&
                clazz.getGenericSuperclass() instanceof ParameterizedType
                && interfaceType.isAssignableFrom((Class<?>) ((ParameterizedType) clazz.getGenericSuperclass()).getRawType())) {
            final ParameterizedType type = (ParameterizedType) clazz.getGenericSuperclass();
            Type[] genericTypes = type.getActualTypeArguments();
            if (genericTypes.length > position) {
                try {
                    foundClass = (Class<?>) genericTypes[position];
                } catch (ClassCastException ignored) {
                }
            }
        }

        if (foundClass == null && clazz.getSuperclass() != null) {
            return getInterfaceTypeArgument(clazz.getSuperclass(), interfaceType, position);
        }

        return Optional.ofNullable(foundClass);
    }

    public static boolean isLambda(Class<?> clazz) {
        return clazz.getSimpleName().contains("$$Lambda$");
    }

    /**
     * Checks if the target type matches the given object.
     * <p>
     * The target can be of type {@link Target} which will extract the actual target from it first.
     * False will be returned if the target is null.
     *
     * @param target the target to check against this context
     * @return true if the type matches or false of the object is null
     * or does not extend the target type
     */
    public static <TTarget> boolean isTargetType(@NonNull Class<?> targetClass, @Nullable TTarget target) {
        if (target == null) return false;

        if (target instanceof Target) {
            return isTargetType(targetClass, ((Target<?>) target).source());
        }
        return targetClass.isInstance(target);
    }

    /**
     * Recursively searches the given class and all of its super classes for a field with the given name.
     *
     * @param type the class that should be searched
     * @param name the name of the field
     * @return the field or an empty optional
     */
    public static Optional<Field> getDeclaredField(Class<?> type, String name) {

        return getAllFields(type, new ArrayList<>()).stream()
                .filter(field -> field.getName().equals(name))
                .findFirst();
    }

    /**
     * Recursively gets all fields from the given class and its superclasses.
     *
     * @param type the class that should be searched
     * @param fields the list to store the fields in. provide an empty array list to start.
     * @return the list of fields from the given class and its superclasses
     */
    public static List<Field> getAllFields(Class<?> type, List<Field> fields) {

        fields.addAll(Arrays.asList(type.getDeclaredFields()));

        if (type.getSuperclass() != null) {
            getAllFields(type.getSuperclass(), fields);
        }

        return fields;
    }

    /**
     * Recursively gets all methods from the given class and its superclass.
     *
     * @param type the class that should be searched
     * @param methods the list to store the result in. provide an empty list to start with.
     * @return a list of all methods of the class and its superclasses
     */
    public static List<Method> getAllMethods(Class<?> type, List<Method> methods) {

        methods.addAll(Arrays.asList(type.getDeclaredMethods()));

        if (type.getSuperclass() != null) {
            getAllMethods(type.getSuperclass(), methods);
        }

        return methods;
    }
}
