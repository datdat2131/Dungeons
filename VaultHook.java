package com.astrax.dungeon.integration;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/** Chi duoc nap khi Vault co mat (kiem tra o AstraXDungeon truoc khi goi). */
public final class VaultHook {
    private final Economy eco;

    private VaultHook(Economy eco) { this.eco = eco; }

    public static VaultHook create() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        return rsp == null ? null : new VaultHook(rsp.getProvider());
    }

    public void give(OfflinePlayer p, double amount) { eco.depositPlayer(p, amount); }
}
