/*
 * Licence is provided in the jar as license.yml also here:
 * https://github.com/Rsl1122/Plan-PlayerAnalytics/blob/master/Plan/src/main/resources/license.yml
 */
package com.djrapitops.plan.system.webserver.webapi.bukkit;

import com.djrapitops.plan.Plan;
import com.djrapitops.plan.PlanBungee;
import com.djrapitops.plan.PlanPlugin;
import com.djrapitops.plan.api.exceptions.connection.WebException;
import com.djrapitops.plan.system.settings.ServerSpecificSettings;
import com.djrapitops.plan.system.settings.Settings;
import com.djrapitops.plan.system.settings.config.ConfigSystem;
import com.djrapitops.plan.system.webserver.response.Response;
import com.djrapitops.plan.system.webserver.webapi.WebAPI;
import com.djrapitops.plugin.api.Check;
import com.djrapitops.plugin.api.utility.log.Log;
import com.djrapitops.plugin.utilities.Verify;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Fuzzlemann
 */
public class ConfigurationWebAPI extends WebAPI {

    @Override
    public Response onRequest(PlanPlugin plugin, Map<String, String> variables) {
        if (!Check.isBukkitAvailable()) {
            Log.debug("Called a wrong server type");
            return badRequest("Called a Bungee Server");
        }
        if (Settings.BUNGEE_COPY_CONFIG.isFalse() || Settings.BUNGEE_OVERRIDE_STANDALONE_MODE.isTrue()) {
            Log.info("Bungee Config settings overridden on this server.");
            Log.debug(ConfigSystem.getConfig().getConfigNode("Plugin.Bungee-Override").getChildren().toString());
            return success();
        }
        ServerSpecificSettings.updateSettings((Plan) plugin, variables);
        return success();
    }

    @Override
    public void sendRequest(String address) throws WebException {
        throw new IllegalStateException("Wrong method call for this WebAPI, call sendRequest(String, UUID, UUID) instead.");
    }

    public void sendRequest(String address, UUID serverUUID, String accessKey) throws WebException {
        if (accessKey != null) {
            addVariable("accessKey", accessKey);
        }
        addVariable("webAddress", PlanBungee.getInstance().getWebServer().getAccessAddress());

        sendRequest(address, serverUUID);
    }

    public void sendRequest(String address, UUID serverUUID) throws WebException {
        Map<String, Object> configValues = getConfigValues(serverUUID);
        for (Map.Entry<String, Object> entry : configValues.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (!Verify.notNull(key, value)) {
                continue;
            }
            addVariable(key, value.toString());
        }
        super.sendRequest(address);
    }

    private void addConfigValue(Map<String, Object> configValues, Settings setting, Object value) {
        if (value != null) {
            configValues.put(setting.getPath(), value);
        }
    }

    private Map<String, Object> getConfigValues(UUID serverUUID) throws WebException {
        Map<String, Object> configValues = new HashMap<>();
        if (!Check.isBungeeAvailable()) {
            throw new WebException("Attempted to send config values from Bukkit to Bungee.");
        }
        addConfigValue(configValues, Settings.DB_TYPE, "mysql");
        Settings[] sameStrings = new Settings[]{
                Settings.DB_HOST, Settings.DB_USER, Settings.DB_PASS,
                Settings.DB_DATABASE, Settings.FORMAT_DECIMALS, Settings.FORMAT_SECONDS,
                Settings.FORMAT_DAY, Settings.FORMAT_DAYS, Settings.FORMAT_HOURS,
                Settings.FORMAT_MINUTES, Settings.FORMAT_MONTHS, Settings.FORMAT_MONTH,
                Settings.FORMAT_YEAR, Settings.FORMAT_YEARS, Settings.FORMAT_ZERO_SECONDS
        };
        for (Settings setting : sameStrings) {
            addConfigValue(configValues, setting, setting.toString());
        }
        addConfigValue(configValues, Settings.DB_PORT, Settings.DB_PORT.getNumber());
        addServerSpecificValues(configValues, serverUUID);

        return configValues;
    }

    private void addServerSpecificValues(Map<String, Object> configValues, UUID serverUUID) {
        ServerSpecificSettings settings = Settings.serverSpecific();

        String theme = settings.getString(serverUUID, Settings.THEME_BASE);
        Integer port = settings.getInt(serverUUID, Settings.WEBSERVER_PORT);
        String name = settings.getString(serverUUID, Settings.SERVER_NAME);

        if (!Verify.isEmpty(theme)) {
            addConfigValue(configValues, Settings.THEME_BASE, theme);
        }
        if (port != null && port != 0) {
            addConfigValue(configValues, Settings.WEBSERVER_PORT, port);
        }
        if (!Verify.isEmpty(name)) {
            addConfigValue(configValues, Settings.SERVER_NAME, name);
        }
    }
}
