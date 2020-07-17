package net.silthus.art.conf;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.silthus.art.Action;
import net.silthus.art.ArtContext;
import net.silthus.art.Trigger;
import net.silthus.art.TriggerListener;

@Data
@EqualsAndHashCode(callSuper = true)
public class ArtContextSettings extends SettingsBase {

    public static final ArtContextSettings DEFAULT = new ArtContextSettings();

    public static ArtContextSettings of(ArtContextSettings settings) {
        return new ArtContextSettings(settings);
    }

    ArtContextSettings() {
    }

    ArtContextSettings(ArtContextSettings settings) {
        this.autoTrigger = settings.autoTrigger;
        this.executeActions = settings.executeActions;
    }

    /**
     * Set to true if you automatically want to trigger the {@link Action}s
     * defined in this {@link ArtContext} event if there are no listeners
     * subscribed to this result.
     * Defaults to true.
     * <br>
     * As an alternative you can subscribe to this {@link ArtContext} by using the
     * {@link ArtContext#onTrigger(Class, TriggerListener)} method. Then all actions defined in the
     * config will be executed, unless {@link #isExecuteActions()} is false.
     *
     * @see #isExecuteActions()
     */
    private boolean autoTrigger = true;

    /**
     * Set to false if you want to prevent the {@link ArtContext} from executing
     * any {@link Action}s. This only affects actions that would be automatically
     * executed when a {@link Trigger} fires and a {@link TriggerListener} is attached
     * or {@link #isAutoTrigger()} is set to true.
     * Defaults to true.
     * <br>
     * You can always bypass this by directly calling one of the {@link ArtContext} methods.
     */
    private boolean executeActions = true;
}