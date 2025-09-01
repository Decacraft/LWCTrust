package org.popcraft.lwctrust;

import com.griefcraft.lwc.LWC;
import com.griefcraft.model.Protection;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.List;
import java.util.UUID;

public class BreakListener implements Listener {

    private final LWCTrust lwcTrust;

    public BreakListener(LWCTrust lwcTrust) {
        this.lwcTrust = lwcTrust;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        Player breaker = event.getPlayer();
        Block block = event.getBlock();

        Protection prot = LWC.getInstance().findProtection(block);
        if (prot == null) return;

        // Récupère l'UUID du propriétaire (LWC peut stocker un UUID string ou parfois un nom)
        UUID owner = null;
        String ownerRaw = prot.getOwner();
        try {
            owner = UUID.fromString(ownerRaw);
        } catch (IllegalArgumentException ex) {
            // Fallback si LWC stocke un nom
            OfflinePlayer op = Bukkit.getOfflinePlayer(ownerRaw);
            if (op != null) owner = op.getUniqueId();
        }
        if (owner == null) return;

        // Si le casseur est dans la trust list du propriétaire, on autorise la destruction
        List<UUID> trusted = lwcTrust.getTrustCache().load(owner);
        if (!trusted.contains(breaker.getUniqueId())) return;

        // IMPORTANT : supprimer la protection AVANT de laisser le bloc se casser
        try {
            prot.remove(); // retire proprement du datastore LWC
        } catch (Exception ex) {
            // Si pour une raison quelconque la suppression LWC échoue, on n'autorise pas le break
            // (évite les protections orphelines)
            return;
        }

        // S'assurer que la casse n’est pas bloquée par LWC ou un autre plugin
        event.setCancelled(false);
        // Laisser le bloc se casser normalement (pas besoin de setType(AIR), Spigot le fait)
    }
}
