package net.silthus.art.parser.flow.parser;

import lombok.SneakyThrows;
import net.silthus.art.ActionContext;
import net.silthus.art.api.actions.ActionFactory;
import net.silthus.art.api.actions.ActionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ActionParser")
class ActionParserTest {

    private ActionParser parser;
    private ActionManager actionManager;
    private ActionFactory<?, ?> factory;

    @BeforeEach
    void beforeEach() {
        this.factory = mock(ActionFactory.class);
        this.actionManager = mock(ActionManager.class);
        when(actionManager.getFactory(anyString())).thenReturn(Optional.of(factory));
        when(factory.create(any())).thenAnswer(invocation -> new ActionContext<>(null, null, invocation.getArgument(0)));

        this.parser = new ActionParser(actionManager);
    }

    @Nested
    @DisplayName("parse()")
    class parse {

        @Test
        @SneakyThrows
        @DisplayName("should match action identifier '!'")
        void shouldMatchActionIdentifier() {

            assertThat(parser.accept("!foobar")).isTrue();
            assertThat(parser.parse()).extracting(ActionContext::getConfig)
                    .isEqualTo(Optional.empty());
        }

        @ParameterizedTest
        @SneakyThrows
        @DisplayName("should not match other identifier: ")
        @ValueSource(chars = {'?', '@', ':', '~', '#', '-', '+', '*', '_', '<', '>', '|'})
        void shouldNotMatchOtherIdentifiers(char identifier) {

            assertThat(parser.accept(identifier + "foobar")).isFalse();
        }
    }
}