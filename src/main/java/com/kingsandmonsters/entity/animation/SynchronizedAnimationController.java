package com.kingsandmonsters.entity.animation;

import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationController;

/**
 * GeckoLib controller that can begin a newly selected one-shot at an authoritative elapsed tick.
 * GeckoLib normally resets a newly observed animation to tick zero; this adjusts that reset's
 * clock origin so entities first rendered mid-action do not replay history from the beginning.
 */
public final class SynchronizedAnimationController<T extends GeoAnimatable> extends AnimationController<T> {
    private double pendingElapsedTick = -1.0;
    private double pendingGapElapsedTick = -1.0;
    private long consumedEventStartTick = Long.MIN_VALUE;
    private int consumedEventType = Integer.MIN_VALUE;
    private long lastEvaluationGameTick = Long.MIN_VALUE;

    public SynchronizedAnimationController(T animatable, String name, int transitionTicks,
                                           AnimationStateHandler<T> handler) {
        super(animatable, name, transitionTicks, handler);
    }

    private boolean consumeAndSeek(int eventType, long eventStartTick, double elapsedTick) {
        if (eventType == consumedEventType && eventStartTick == consumedEventStartTick) {
            return false;
        }
        consumedEventType = eventType;
        consumedEventStartTick = eventStartTick;
        pendingGapElapsedTick = -1.0;
        pendingElapsedTick = Math.max(0.0, elapsedTick);
        forceAnimationReset();
        return true;
    }

    @Override
    protected double adjustTick(double tick) {
        if (pendingGapElapsedTick >= 0.0) {
            double elapsed = pendingGapElapsedTick;
            pendingGapElapsedTick = -1.0;
            double speed = Math.max(0.0001, animationSpeedModifier.apply(animatable));
            tickOffset = tick - elapsed / speed;
            return elapsed;
        }

        if (pendingElapsedTick >= 0.0 && shouldResetTick) {
            double elapsed = pendingElapsedTick;
            pendingElapsedTick = -1.0;
            super.adjustTick(tick);
            double speed = Math.max(0.0001, animationSpeedModifier.apply(animatable));
            tickOffset = tick - elapsed / speed;
            // Must return 0.0 here, not elapsed. AnimationController.process() only dequeues/installs
            // the newly requested animation when this transition-reset frame resolves to exactly 0
            // (its adjustedTick == 0 gate) — tickOffset above already carries the authoritative elapsed
            // time forward for every frame after this one. Returning elapsed instead of 0.0 makes
            // GeckoLib skip that install and keep rendering the previous locomotion clip for the rest
            // of the one-shot, even though the synchronized attack state is already correct.
            return 0.0;
        }

        return super.adjustTick(tick);
    }

    /**
     * Reconciles a retained one-shot against absolute world time. Continuous evaluations leave
     * GeckoLib alone; the first evaluation after a render gap seeks an active event forward once,
     * while an expired event is discarded once so locomotion can take over immediately.
     */
    public static boolean revalidateEvent(AnimationController<?> controller, int eventType,
                                          long eventStartTick, int durationTicks,
                                          long currentGameTick) {
        if (eventStartTick == Long.MIN_VALUE) return false;
        double elapsed = Math.max(0L, currentGameTick - eventStartTick);
        if (!(controller instanceof SynchronizedAnimationController<?> synchronizedController)) {
            return elapsed < durationTicks;
        }

        boolean sameEvent = eventType == synchronizedController.consumedEventType
                && eventStartTick == synchronizedController.consumedEventStartTick;
        boolean renderGap = sameEvent
                && synchronizedController.lastEvaluationGameTick != Long.MIN_VALUE
                && currentGameTick - synchronizedController.lastEvaluationGameTick > 2L;
        synchronizedController.lastEvaluationGameTick = currentGameTick;

        if (elapsed >= durationTicks) {
            if (sameEvent) {
                synchronizedController.pendingElapsedTick = -1.0;
                synchronizedController.pendingGapElapsedTick = -1.0;
            }
            return false;
        }

        if (!sameEvent || renderGap) {
            if (renderGap) {
                synchronizedController.seekActiveEventOnce(elapsed);
            } else {
                synchronizedController.consumeAndSeek(eventType, eventStartTick, elapsed);
            }
        }
        return true;
    }

    private void seekActiveEventOnce(double elapsedTick) {
        // This is still the same running event. Rebase its clock for one evaluation only; resetting
        // the identical clip here can leave GeckoLib's transition state waiting indefinitely.
        pendingGapElapsedTick = Math.max(0.0, elapsedTick);
    }
}
