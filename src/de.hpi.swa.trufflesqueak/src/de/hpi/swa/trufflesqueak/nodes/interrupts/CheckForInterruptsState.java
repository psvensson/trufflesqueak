/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.interrupts;

import java.util.ArrayDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.trufflesqueak.util.LogUtils;
import de.hpi.swa.trufflesqueak.util.MiscUtils;

public final class CheckForInterruptsState {
    private static final String CHECK_FOR_INTERRUPTS_THREAD_NAME = "TruffleSqueakCheckForInterrupts";

    private static final int INTERRUPT_CHECKS_EVERY_N_MILLISECONDS = 20;

    private final SqueakImageContext image;
    private ScheduledExecutorService executor;
    private final ArrayDeque<Integer> semaphoresToSignal = new ArrayDeque<>();

    private boolean isActive = true;
    protected long nextWakeupTick;
    protected boolean interruptPending;
    private boolean pendingFinalizationSignals;

    /**
     * `shouldTrigger` is set to `true` by a dedicated thread. To guarantee atomicity, it would be
     * necessary to mark this field as `volatile` or use an `AtomicBoolean`. However, such a field
     * cannot be moved by the Graal compiler during compilation. Since atomicity is not needed for
     * the interrupt handler mechanism, we can use a standard boolean here for better compilation.
     */
    private boolean shouldTrigger;
    private boolean shouldTriggerNoTimer;

    @CompilationFinal private PointersObject interruptSemaphore;
    private PointersObject timerSemaphore;
    private ScheduledFuture<?> interruptChecks;

    public CheckForInterruptsState(final SqueakImageContext image) {
        this.image = image;
        if (image.options.disableInterruptHandler) {
            image.printToStdOut("Interrupt handler disabled...");
        }
    }

    @TruffleBoundary
    public void start() {
        if (image.options.disableInterruptHandler) {
            return;
        }
        final Object interruptSema = image.getSpecialObject(SPECIAL_OBJECT.THE_INTERRUPT_SEMAPHORE);
        if (interruptSema instanceof PointersObject) {
            setInterruptSemaphore((PointersObject) interruptSema);
        } else {
            assert interruptSema == NilObject.SINGLETON;
        }
        final Object timerSema = image.getSpecialObject(SPECIAL_OBJECT.THE_TIMER_SEMAPHORE);
        if (timerSema instanceof PointersObject) {
            setTimerSemaphore((PointersObject) timerSema);
        } else {
            assert timerSema == NilObject.SINGLETON;
        }
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            final Thread t = new Thread(r, CHECK_FOR_INTERRUPTS_THREAD_NAME);
            t.setDaemon(true);
            return t;
        });
        interruptChecks = executor.scheduleWithFixedDelay(() -> {
            if (!shouldTrigger) {
                shouldTrigger = isActive && (nextWakeUpTickTrigger() || pendingFinalizationSignals() || hasSemaphoresToSignal());
                shouldTriggerNoTimer = isActive && (pendingFinalizationSignals() || hasSemaphoresToSignal());
            }
        }, INTERRUPT_CHECKS_EVERY_N_MILLISECONDS, INTERRUPT_CHECKS_EVERY_N_MILLISECONDS, TimeUnit.MILLISECONDS);
    }

    @TruffleBoundary
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    public void setInterruptPending() {
        interruptPending = true;
        shouldTrigger = isActive;
        shouldTriggerNoTimer = isActive;
    }

    public void setNextWakeupTick(final long msTime) {
        LogUtils.INTERRUPTS.finer(() -> {
            if (nextWakeupTick != 0) {
                return (msTime != 0 ? "Changing nextWakeupTick to " + msTime + " from " : "Resetting nextWakeupTick from ") + nextWakeupTick;
            } else {
                return msTime != 0 ? "Setting nextWakeupTick to " + msTime : "Resetting nextWakeupTick when it was already 0";
            }
        });
        nextWakeupTick = msTime;
    }

    public long getNextWakeupTick() {
        return nextWakeupTick;
    }

    public boolean isActive() {
        return isActive;
    }

    public void activate() {
        isActive = true;
    }

    public void deactivate() {
        isActive = false;
        resetTriggers();
    }

    protected boolean interruptPending() {
        return interruptPending;
    }

    protected boolean nextWakeUpTickTrigger() {
        if (nextWakeupTick != 0) {
            final long time = MiscUtils.currentTimeMillis();
            if (time >= nextWakeupTick) {
                LogUtils.INTERRUPTS.finer(() -> "Reached nextWakeupTick: " + nextWakeupTick);
                return true;
            }
        }
        return false;
    }

    public void setPendingFinalizations(final boolean value) {
        pendingFinalizationSignals = value;
    }

    protected boolean pendingFinalizationSignals() {
        return pendingFinalizationSignals;
    }

    protected boolean hasSemaphoresToSignal() {
        return !semaphoresToSignal.isEmpty();
    }

    protected Integer nextSemaphoreToSignal() {
        return semaphoresToSignal.pollFirst();
    }

    public static int getInterruptChecksEveryNms() {
        return INTERRUPT_CHECKS_EVERY_N_MILLISECONDS;
    }

    @TruffleBoundary
    public void signalSemaphoreWithIndex(final int index) {
        semaphoresToSignal.addLast(index);
    }

    public boolean shouldTrigger() {
        return shouldTrigger;
    }

    public boolean shouldTriggerNoTimer() {
        return shouldTriggerNoTimer;
    }

    private void resetTriggers() {
        shouldTrigger = false;
        shouldTriggerNoTimer = false;
    }

    public PointersObject getInterruptSemaphore() {
        return interruptSemaphore;
    }

    public void setInterruptSemaphore(final PointersObject interruptSemaphore) {
        this.interruptSemaphore = interruptSemaphore;
    }

    public PointersObject getTimerSemaphore() {
        return timerSemaphore;
    }

    public void setTimerSemaphore(final PointersObject timerSemaphore) {
        this.timerSemaphore = timerSemaphore;
    }

    /*
     * TESTING
     */

    public void reset() {
        CompilerAsserts.neverPartOfCompilation("Resetting interrupt handler only supported for testing purposes");
        isActive = true;
        nextWakeupTick = 0;
        if (interruptChecks != null) {
            interruptChecks.cancel(true);
        }
        shutdown();
        interruptPending = false;
        pendingFinalizationSignals = false;
        clearWeakPointersQueue();
        semaphoresToSignal.clear();
    }

    private void clearWeakPointersQueue() {
        while (image.weakPointersQueue.poll() != null) {
            // Poll until empty.
        }
    }
}
