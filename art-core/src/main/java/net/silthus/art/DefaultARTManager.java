package net.silthus.art;

import com.google.inject.Inject;
import lombok.*;
import net.silthus.art.api.ARTFactory;
import net.silthus.art.api.ARTManager;
import net.silthus.art.api.ARTRegistrationException;
import net.silthus.art.api.ARTType;
import net.silthus.art.api.actions.Action;
import net.silthus.art.api.trigger.TriggerContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;

@Data
public class DefaultARTManager implements ARTManager {

    @Inject
    private Logger logger;
    private final Map<String, ARTBuilder> registeredPlugins = new HashMap<>();

    @Setter(AccessLevel.PACKAGE)
    private boolean loaded = false;

    @Override
    public void load() {

        setLoaded(true);
    }

    @Override
    public void unload() {

        setLoaded(false);
    }

    @Override
    public void reload() {

        unload();
        load();
    }

    @Override
    public void register(String pluginName, Consumer<ARTBuilder> builder) throws ARTRegistrationException {

        if (isLoaded()) {
            throw new ARTRegistrationException("ART already initialized! Make sure you register your ART directly after your plugin was loaded.");
        }

        ARTBuilder artBuilder;
        if (registeredPlugins.containsKey(pluginName)) {
            artBuilder = registeredPlugins.get(pluginName);
        } else {
            artBuilder = new ARTBuilder(pluginName);
        }

        builder.andThen(art -> registeredPlugins.put(pluginName, art))
                .andThen(art -> {
                    getLogger().info(pluginName + " registered their ART:");

                    Map<ARTType, Map<String, ARTFactory>> createdART = art.build();
                    for (ARTType artType : createdART.keySet()) {
                        getLogger().info("    - " + artType.name() + ": " + createdART.get(artType).size());
                    }
                })
                .accept(artBuilder);
    }

    @Override
    public <TTarget, TConfig> void trigger(String identifier, TTarget target, Predicate<TriggerContext<TTarget, TConfig>> context) {

    }

    @Override
    public List<Action<?, ?>> createActions(Object config) {
        return null;
    }

    @Override
    public <TTarget> List<Action<TTarget, ?>> createActions(Class<TTarget> targetClass, Object config) {
        return null;
    }
}
