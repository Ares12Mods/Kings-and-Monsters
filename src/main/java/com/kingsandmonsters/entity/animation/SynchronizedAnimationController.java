package com.kingsandmonsters.entity.animation;

import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationProcessor;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.state.BoneSnapshot;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoModel;

import java.util.Map;

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
    private double elapsedAnimationTicks;

    private Map<String, double[]> footContacts = Map.of();
    private Runnable footListener;
    private String footCycleAnimation;
    private double footCycleTick;

    public SynchronizedAnimationController(T animatable, String name, int transitionTicks,
                                           AnimationStateHandler<T> handler) {
        super(animatable, name, transitionTicks, handler);
    }

    /**
     * Calls {@code onFootfall} each time a looping animation reaches one of its foot-contact times
     * (animation ticks, keyed by animation name), so step sounds land with the visible foot. Runs on
     * whichever side renders the animation, so it only ever fires on the client.
     */
    public SynchronizedAnimationController<T> withFootfalls(Map<String, double[]> contacts, Runnable onFootfall) {
        this.footContacts = contacts;
        this.footListener = onFootfall;
        return this;
    }

    @Override
    public void process(CoreGeoModel<T> model, AnimationState<T> state, Map<String, CoreGeoBone> bones,
                        Map<String, BoneSnapshot> snapshots, double seekTime, boolean crashWhenCantFindBone) {
        super.process(model, state, bones, snapshots, seekTime, crashWhenCantFindBone);
        elapsedAnimationTicks = animationSpeedModifier.apply(animatable) * Math.max(seekTime - tickOffset, 0);
        trackFootfalls(seekTime);
    }

    /** How far into the current animation the last rendered frame was, in animation ticks. */
    public double getElapsedAnimationTicks() {
        return elapsedAnimationTicks;
    }

    private void trackFootfalls(double seekTime) {
        if (footListener == null) {
            return;
        }
        AnimationProcessor.QueuedAnimation current = getCurrentAnimation();
        double[] contacts = current == null || getAnimationState() != State.RUNNING
                ? null : footContacts.get(current.animation().name());
        if (contacts == null) {
            footCycleAnimation = null;
            return;
        }

        double now = animationSpeedModifier.apply(animatable) * Math.max(seekTime - tickOffset, 0);
        if (current.animation().name().equals(footCycleAnimation)) {
            boolean wrapped = now < footCycleTick;
            for (double contact : contacts) {
                // A wrap means the loop restarted: catch contacts after the last frame or up to now.
                boolean crossed = wrapped
                        ? contact > footCycleTick || contact <= now
                        : contact > footCycleTick && contact <= now;
                if (crossed) {
                    footListener.run();
                    break;
                }
            }
        }
        footCycleAnimation = current.animation().name();
        footCycleTick = now;
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
            // Return 0.0 here, not elapsed. AnimationController.process() only installs the newly
            // requested animation when this transition-reset frame resolves to exactly 0 (its
            // adjustedTick == 0 gate); tickOffset already carries the elapsed time forward after that.
            // Returning elapsed makes GeckoLib skip the install and keep rendering the old locomotion
            // clip for the whole one-shot.
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
            AnimationDebug.log("{}: type={} elapsed={}/{} controllerState={}",
                    renderGap ? "seek after gap" : "start/seek new event", eventType, elapsed,
                    durationTicks, controller.getAnimationState());
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
