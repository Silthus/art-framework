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

import io.artframework.*;
import io.artframework.annotations.ConfigOption;
import io.artframework.conf.ActionConfig;
import io.artframework.conf.ContextConfig;
import io.artframework.util.ConfigUtil;
import lombok.Data;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SuppressWarnings("ALL")
class ArtObjectContextParserTest {

    private ActionProvider actions;
    private ActionLineParser parser;
    private ActionFactory<?> factory;
    private ArtObjectMeta artObjectMeta;

    @BeforeEach
    @SneakyThrows
    void beforeEach() {
        actions = mock(ActionProvider.class);
        factory = mock(ActionFactory.class);
        artObjectMeta = mock(ArtObjectMeta.class);
        when(factory.meta()).thenReturn(artObjectMeta);
        when(factory.createContext(any())).thenReturn(mock(ActionContext.class));
        when(artObjectMeta.configMap()).thenReturn(ConfigUtil.getConfigFields(TestConfig.class));
        when(actions.get(anyString())).thenAnswer(invocation -> Optional.of(factory));

        this.parser = new ActionLineParser(Arrays.asList("").iterator(), Scope.of(configurationBuilder -> configurationBuilder.actions(actions)));
    }

    private <TConfig> TConfig extractArtConfig(TConfig config) {
        ArgumentCaptor<ContextConfig> argument = ArgumentCaptor.forClass(ContextConfig.class);
        verify(factory).createContext(argument.capture());

        argument.getValue().contextConfig().applyTo(config);

        return config;
    }

    private <TConfig> TConfig extractIndividualConfig(TConfig config) {
        ArgumentCaptor<ContextConfig> argument = ArgumentCaptor.forClass(ContextConfig.class);
        verify(factory).createContext(argument.capture());

        argument.getValue().artObjectConfig().applyTo(config);

        return config;
    }

    @Test
    @DisplayName("should return false if accept(String) is null")
    void shouldNotAcceptNullStrings() {

        assertThat(parser.accept(null)).isFalse();
    }

    @Test
    @DisplayName("should return false if accept(String) is empty")
    void shouldNotAcceptEmptyStrings() {

        assertThat(parser.accept("  ")).isFalse();
    }

    @Nested
    @DisplayName("parse()")
    class parse {

        @Test
        @SneakyThrows
        @DisplayName("should parse action config inside [...]")
        void shouldParseActionConfig() {

            assertThat(parser.accept("!foobar[cooldown:5s, delay=10s]")).isTrue();

            assertThatCode(() -> parser.parse()).doesNotThrowAnyException();

            assertThat(extractArtConfig(new ActionConfig()))
                    .extracting(ActionConfig::cooldown, ActionConfig::delay)
                    .contains(5000L, 10000L);
        }

        @Test
        @SneakyThrows
        @DisplayName("should parse action config inside (...)")
        void shouldParseActionConfigInBrackets() {

            assertThat(parser.accept("!foobar(delay=10s, cooldown:5s)")).isTrue();

            assertThatCode(() -> parser.parse()).doesNotThrowAnyException();

            assertThat(extractArtConfig(new ActionConfig()))
                    .extracting(ActionConfig::cooldown, ActionConfig::delay)
                    .contains(5000L, 10000L);
        }

        @Test
        @SneakyThrows
        @DisplayName("should parse art object config")
        void shouldParseCustomConfig() {

            assertThat(parser.accept("!foobar foo number=2")).isTrue();

            assertThatCode(() -> parser.parse()).doesNotThrowAnyException();

            TestConfig config = extractIndividualConfig(new TestConfig());

            assertThat(config)
                    .extracting(TestConfig::getName, TestConfig::getNumber)
                    .contains("foo", 2);
        }

        @Test
        @SneakyThrows
        @DisplayName("should parse art object config and options")
        void shouldParseCustomConfigAndOptions() {

            assertThat(parser.accept("!foobar(delay=10) name=foo;number:2")).isTrue();


            assertThatCode(() -> parser.parse()).doesNotThrowAnyException();

            assertThat(extractArtConfig(new ActionConfig()))
                    .extracting(ActionConfig::delay)
                    .isEqualTo(10L);

            assertThat(extractIndividualConfig(new TestConfig()))
                    .extracting(TestConfig::getName, TestConfig::getNumber)
                    .contains("foo", 2);
        }

        @Test
        @SneakyThrows
        @DisplayName("should throw if no matching factory is found")
        void shouldThrowIfNoIdentifierMatches() {

            when(actions.get(anyString())).thenReturn(Optional.empty());

            assertThat(parser.accept("!foobar")).isTrue();
            assertThatExceptionOfType(ParseException.class)
                    .isThrownBy(() -> parser.parse())
                    .withMessageContaining("No action with identifier \"foobar\" found");
        }
    }

    @Data
    static class TestConfig {
        @ConfigOption(position = 0)
        private String name;
        @ConfigOption
        private int number;
    }

    static class WrongConfigClass {
        private final String foobar;

        WrongConfigClass(String foobar) {
            this.foobar = foobar;
        }
    }
}