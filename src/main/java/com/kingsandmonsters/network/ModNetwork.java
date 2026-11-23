package com.kingsandmonsters.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(ScreenShakePayload.TYPE, ScreenShakePayload.STREAM_CODEC, ScreenShakePayload::handle);
        registrar.playToClient(GroundRipplePayload.TYPE, GroundRipplePayload.STREAM_CODEC, GroundRipplePayload::handle);
        registrar.playToClient(AngerHudPayload.TYPE, AngerHudPayload.STREAM_CODEC, AngerHudPayload::handle);
        registrar.playToClient(OgreOverlayPayload.TYPE, OgreOverlayPayload.STREAM_CODEC, OgreOverlayPayload::handle);
        registrar.playToClient(OgrebloodTotemActivationPayload.TYPE,
                OgrebloodTotemActivationPayload.STREAM_CODEC, OgrebloodTotemActivationPayload::handle);
        registrar.playToClient(BogEyeTargetPayload.TYPE, BogEyeTargetPayload.STREAM_CODEC, BogEyeTargetPayload::handle);
        registrar.playToClient(BogrotSyncPayload.TYPE, BogrotSyncPayload.STREAM_CODEC, BogrotSyncPayload::handle);
        registrar.playToClient(CombatEffectSyncPayload.TYPE, CombatEffectSyncPayload.STREAM_CODEC,
                CombatEffectSyncPayload::handle);
        registrar.playToClient(KingDialoguePayload.TYPE, KingDialoguePayload.STREAM_CODEC,
                KingDialoguePayload::handle);
        registrar.playToServer(KingDialogueActionPayload.TYPE, KingDialogueActionPayload.STREAM_CODEC,
                KingDialogueActionPayload::handle);
        registrar.playToClient(QuestDialoguePayload.TYPE, QuestDialoguePayload.STREAM_CODEC,
                QuestDialoguePayload::handle);
        registrar.playToClient(QuestTrackerPayload.TYPE, QuestTrackerPayload.STREAM_CODEC,
                QuestTrackerPayload::handle);
        registrar.playToServer(QuestActionPayload.TYPE, QuestActionPayload.STREAM_CODEC,
                QuestActionPayload::handle);
        registrar.playToServer(OpenBackpackPayload.TYPE, OpenBackpackPayload.STREAM_CODEC, OpenBackpackPayload::handle);
        registrar.playToServer(BoarHeadbuttPayload.TYPE, BoarHeadbuttPayload.STREAM_CODEC, BoarHeadbuttPayload::handle);
        registrar.playToServer(BoarChargePayload.TYPE, BoarChargePayload.STREAM_CODEC, BoarChargePayload::handle);
        registrar.playToServer(SummonArmyPayload.TYPE, SummonArmyPayload.STREAM_CODEC, SummonArmyPayload::handle);
    }
}
