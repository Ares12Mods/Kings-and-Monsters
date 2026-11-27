package com.kingsandmonsters;

import com.kingsandmonsters.entity.OgreRallyEvents;
import com.kingsandmonsters.item.MudsnoutRecallTracker;
import com.kingsandmonsters.item.OgreHookbladeEvents;
import com.kingsandmonsters.item.OgreKingsClubEvents;
import com.kingsandmonsters.item.OgrebloodTotemEvents;
import com.kingsandmonsters.item.RattleboneRingEvents;
import com.kingsandmonsters.tribute.BogIronArmorEffects;
import com.kingsandmonsters.tribute.BogIronProgression;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Static trackers outlive a world: leaving a single-player world and opening another keeps the JVM
 * (and these maps) alive. Game-time stamps from the old world would block cooldowns in the new one,
 * and queued effects would reference levels that no longer exist.
 */
public final class ServerStateReset {
    private ServerStateReset() {
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        MudsnoutRecallTracker.resetState();
        OgreKingsClubEvents.resetState();
        OgrebloodTotemEvents.resetState();
        RattleboneRingEvents.resetState();
        BogIronArmorEffects.resetState();
        BogIronProgression.resetState();
        OgreRallyEvents.resetState();
        OgreHookbladeEvents.resetState();
        com.kingsandmonsters.tribute.OutpostEvents.resetState();
    }
}
