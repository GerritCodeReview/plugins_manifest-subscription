package com.amd.gerrit.plugins.manifestsubscription;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;

public class Config {
    static final String CONFIG_MANIFEST_PATH_PATTERN = "manifestPathPattern";

    static final String DEFAULT_MANIFEST_PATH_PATTERN = ".*\\.xml";

    @Inject
    private static PluginConfigFactory cfgFactory;

    @Inject
    @PluginName
    private static String pluginName;

    private static String manifestPathPattern;

    private static void readConfig() {
        PluginConfig cfg = cfgFactory.getFromGerritConfig(pluginName);
        manifestPathPattern = cfg.getString(CONFIG_MANIFEST_PATH_PATTERN, DEFAULT_MANIFEST_PATH_PATTERN);
    }

    public static String getManifestPathPattern() {
        if (manifestPathPattern == null && cfgFactory != null) {
            readConfig();
        }
        return manifestPathPattern;
    }
}