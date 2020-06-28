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

package net.silthus.art;

import com.google.inject.Binder;
import kr.entree.spigradle.Plugin;
import lombok.Getter;
import net.silthus.art.api.ArtManager;
import net.silthus.art.api.scheduler.Scheduler;
import net.silthus.art.parser.flow.FlowParserModule;
import net.silthus.art.scheduler.BukkitScheduler;
import net.silthus.slib.bukkit.BasePlugin;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.ServicePriority;

import javax.inject.Inject;

@Plugin
public class ArtPlugin extends BasePlugin {

    private static final int BSTATS_ID = 7981;

    @Inject
    @Getter
    private ArtManager artManager;

    @Override
    public void enable() {

        Metrics metrics = new Metrics(this, BSTATS_ID);

        ART.setInstance(artManager);
        ART.load();

        ART.register(new ArtBukkitDescription(this), artBuilder ->
                artBuilder.target(Entity.class).filter(new EntityWorldFilter()));

        Bukkit.getServicesManager().register(ArtManager.class, artManager, this, ServicePriority.Normal);
    }

    @Override
    public void disable() {

        ART.getInstance().ifPresent(ArtManager::unload);

        Bukkit.getServicesManager().unregisterAll(this);
    }

    @Override
    public void configure(Binder binder) {

        binder.install(new ArtGuiceModule());
        binder.install(new FlowParserModule());
        binder.bind(Scheduler.class).to(BukkitScheduler.class);
    }
}