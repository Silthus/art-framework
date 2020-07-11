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

package net.silthus.art.util;

import com.google.common.base.Strings;
import net.silthus.art.annotations.ConfigOption;
import net.silthus.art.annotations.Ignore;
import net.silthus.art.api.config.ArtConfigException;
import net.silthus.art.api.config.ConfigFieldInformation;
import net.silthus.art.api.config.FieldNameFormatter;
import net.silthus.art.api.config.FieldNameFormatters;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public final class ConfigUtil {

    public static Map<String, ConfigFieldInformation> getConfigFields(Class<?> configClass, FieldNameFormatter formatter) throws ArtConfigException {
        try {
            Constructor<?> constructor = configClass.getConstructor();
            constructor.setAccessible(true);
            return getConfigFields("", configClass, constructor.newInstance(), formatter);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new ArtConfigException(e);
        }
    }

    public static Map<String, ConfigFieldInformation> getConfigFields(Class<?> configClass) throws ArtConfigException {
        return getConfigFields(configClass, FieldNameFormatters.LOWER_UNDERSCORE);
    }

    private static Map<String, ConfigFieldInformation> getConfigFields(String basePath, Class<?> configClass, Object configInstance, FieldNameFormatter formatter) throws ArtConfigException {
        Map<String, ConfigFieldInformation> fields = new HashMap<>();

        try {
            Field[] allFields = FieldUtils.getAllFields(configClass);
            for (Field field : allFields) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                if (field.isAnnotationPresent(Ignore.class)) continue;

                Optional<ConfigOption> configOption = getConfigOption(field);

                String identifier = basePath + configOption.map(ConfigOption::value)
                        .filter(s -> !Strings.isNullOrEmpty(s))
                        .orElse(formatter.apply(field.getName()));
                ConfigFieldInformation configInformation = new ConfigFieldInformation(identifier, field.getName(), field.getType());

                if (field.getType().isPrimitive() || field.getType().equals(String.class)) {

                    configInformation.setDescription(configOption.map(ConfigOption::description).orElse(configInformation.getDescription()));
                    configInformation.setRequired(configOption.map(ConfigOption::required).orElse(configInformation.isRequired()));
                    configInformation.setPosition(configOption.map(ConfigOption::position).orElse(configInformation.getPosition()));

                    field.setAccessible(true);
                    configInformation.setDefaultValue(field.get(configInstance));

                    fields.put(identifier, configInformation);
                } else {
                    fields.putAll(getConfigFields(identifier + ".", field.getType(), field.getType().getConstructor().newInstance(), formatter));
                }
            }

            List<ConfigFieldInformation> sameFieldPosition = fields.values().stream().filter(field1 -> fields.values().stream().anyMatch(
                    field2 -> field1 != field2
                            && field1.getPosition() > -1
                            && field2.getPosition() > -1
                            && field1.getPosition() == field2.getPosition()
            )).collect(Collectors.toList());
            if (!sameFieldPosition.isEmpty()) {
                throw new ArtConfigException("found same position " + sameFieldPosition.get(0).getPosition() + " on the following fields: "
                        + sameFieldPosition.stream().map(ConfigFieldInformation::getIdentifier).collect(Collectors.joining(",")));
            }

        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | InstantiationException e) {
            throw new ArtConfigException(e);
        }

        return fields;
    }

    public static Optional<ConfigOption> getConfigOption(Field field) {

        if (field.isAnnotationPresent(ConfigOption.class)) {
            return Optional.of(field.getAnnotation(ConfigOption.class));
        }
        return Optional.empty();
    }

    /**
     * Tries to find the config file containing the given id.
     *
     * @param id id of the ART config
     * @return null if no config file containing the id was found.
     *          the absolute path to the config file if found.
     */
    public static Optional<String> getFileName(String id) {

        try {
            return Files.walk(new File("").toPath())
                    .filter(Files::isRegularFile)
                    .filter(file -> containsString(file.toFile(), id))
                    .map(Path::toFile)
                    .map(File::getAbsolutePath)
                    .findFirst();
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private static boolean containsString(File file, String string) {
        try {
            Scanner scanner = new Scanner(file);

            //now read the file line by line...
            int lineNum = 0;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                lineNum++;
                if(line.contains(string)) {
                    return true;
                }
            }
        } catch(FileNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void setConfigFields(Object config, Map<ConfigFieldInformation, Object> fieldValueMap) {
        fieldValueMap.forEach((configFieldInformation, o) -> setConfigField(config, configFieldInformation, o));
    }

    public static void setConfigField(Object config, ConfigFieldInformation fieldInformation, Object value) {

        try {
            if (fieldInformation.getIdentifier().contains(".")) {
                // handle nested config objects
                String nestedIdentifier = StringUtils.substringBefore(fieldInformation.getIdentifier(), ".");
                Field parentField = config.getClass().getDeclaredField(nestedIdentifier);
                parentField.setAccessible(true);
                Object nestedConfigObject = parentField.get(config);
                setConfigField(nestedConfigObject, fieldInformation.copyOf(nestedIdentifier), value);
            } else {
                Field field = config.getClass().getDeclaredField(fieldInformation.getName());
                field.setAccessible(true);
                field.set(config, value);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
