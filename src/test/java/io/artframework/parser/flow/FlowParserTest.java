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

import io.artframework.ArtContext;
import io.artframework.ArtObjectContext;
import io.artframework.ArtObjectMeta;
import io.artframework.ConfigMap;
import io.artframework.Configuration;
import io.artframework.Factory;
import io.artframework.ParseException;
import io.artframework.conf.ActionConfig;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("ALL")
@DisplayName("FlowParser")
class FlowParserTest {

    private FlowParser parser;
    private ArtObjectContextParser<?> flowParser;

    @BeforeEach
    @SneakyThrows
    void beforeEach() {

        Configuration configuration = Configuration.getDefault();

        flowParser = spy(new ArtObjectContextParser<Factory<?, ?>>(configuration, new FlowType("test", ".")) {
            @Override
            protected Optional<Factory<?, ?>> factory(String identifier) {
                Factory<ArtObjectContext<?>, ?> factory = mock(Factory.class);
                ArtObjectMeta artObjectMeta = mock(ArtObjectMeta.class);
                when(artObjectMeta.configMap()).thenReturn(new HashMap());
                when(factory.meta()).thenReturn(artObjectMeta);
                ArtObjectContext context = mock(ArtObjectContext.class);
                when(factory.create(anyMap())).thenReturn(context);
                return Optional.of(factory);
            }

            @Override
            protected ConfigMap configMap() {
                return ActionConfig.configMap();
            }
        });

        configuration.parser().clear();
        configuration.parser().add(flowParser);

        parser = new FlowParser(configuration);
    }

    @Nested
    @DisplayName("parse(ArtConfig)")
    class parse {

        @Test
        @DisplayName("should throw if config object is null")
        public void shouldThrowIfObjectIsNull() {
            assertThatExceptionOfType(NullPointerException.class)
                    .isThrownBy(() -> parser.parse(null));
        }

        @Test
        @SneakyThrows
        @DisplayName("should call parser.parse() for every line")
        void shouldCallParseForEveryLine() {

            parser.parse(Arrays.asList(
                    "!foobar",
                    "!foo",
                    "?requirement",
                    "and more",
                    "foo",
                    "---"
            ));
            verify(flowParser, times(6)).parse();
        }

        @Test
        @SneakyThrows
        @DisplayName("should return all parsed contexts in the result")
        void shouldAddAllParsedContextsToTheResult() {

            assertThat(parser.parse(Arrays.asList(
                    "!foobar",
                    "!foo",
                    "?requirement",
                    "and more",
                    "foo",
                    "---"
            ))).extracting(ArtContext::getArtContexts)
                    .asList().hasSize(6);
        }

        @Test
        @SneakyThrows
        @DisplayName("should throw and display line number if parsing fails")
        void shouldThrowIfParsingALineFails() {

            ParseException exception = new ParseException("TEST ERROR");
            doAnswer((Answer<ArtObjectContext<?>>) invocation -> {
                ArtObjectContextParser<?> parser = (ArtObjectContextParser<?>) invocation.getMock();
                if ("ERROR".equals(parser.input())) {
                    throw exception;
                }
                return mock(ArtObjectContext.class);
            }).when(flowParser).parse();

            assertThatExceptionOfType(ParseException.class)
                    .isThrownBy(() -> parser.parse(Arrays.asList(
                            "!foobar",
                            "?req",
                            "ERROR",
                            "!foo",
                            "bar"
                    )))
                    .withMessage("TEST ERROR on ART line 3/5")
                    .withCause(exception);
        }

        @Test
        @DisplayName("should throw if line matches no parser")
        void shouldThrowIfLineHasNoMatchingParser() {

            doReturn(false).when(flowParser).accept("no-match");

            assertThatExceptionOfType(ParseException.class)
                    .isThrownBy(() -> parser.parse(Arrays.asList(
                            "!foobar",
                            "foo",
                            "no-match"
                    )))
                    .withMessage("Unable to find matching FlowParser for \"no-match\" on line 3/3");
        }
    }
}