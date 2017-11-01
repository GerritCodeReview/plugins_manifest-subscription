package com.amd.gerrit.plugins.manifestsubscription;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;

public class ManifestSubscriptionConfig {
    static final String CONFIG_MAX_BRANCHES_PER_REPO = "maxBranchesPerRepo";

    static final int DEFAULT_MAX_BRANCHES_PER_REPO = 1000;

    @Inject
    private static PluginConfigFactory cfgFactory;

    @Inject
    @PluginName
    private static String pluginName;

    private static int maxBranchesPerRepo;

    public static void readConfig() {
        PluginConfig cfg = cfgFactory.getFromGerritConfig(pluginName);
        maxBranchesPerRepo = cfg.getInt(CONFIG_MAX_BRANCHES_PER_REPO,
                DEFAULT_MAX_BRANCHES_PER_REPO);
    }

    public static int getMaxBranchesPerRepo() {
        return maxBranchesPerRepo;
    }
}