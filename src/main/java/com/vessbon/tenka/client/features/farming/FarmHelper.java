package com.vessbon.tenka.client.features.farming;

import com.vessbon.tenka.client.utils.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


public class FarmHelper  {

    private static FarmHelper instance;
    private Thread farmingThread;

    private static final Minecraft mc = Minecraft.getMinecraft();

    private AtomicBoolean running = new AtomicBoolean(false);
    private AtomicBoolean paused = new AtomicBoolean(false);
    private AtomicReference<Set<KeyBinding>> recentKeys =
            new AtomicReference<>(new CopyOnWriteArraySet<>());

    private BlockingQueue<String> turnQueue = new LinkedBlockingQueue<>();
    private AtomicBoolean isExecutingTurn = new AtomicBoolean(false);

    private PlayerRotation currentRotation;
    private TurnDirection turnDirection;

    private double lastX, lastY, lastZ;


    public FarmHelper() {
        instance = this;
    }

    public static FarmHelper getInstance() {
        if (instance == null) {
            instance = new FarmHelper();
        }
        return instance;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {

        if (event.phase != TickEvent.Phase.END || event.player != mc.thePlayer) return;

        BlockPos currentBlockPos = event.player.getPosition();
        BlockPos lastBlockPos = new BlockPos(lastX, lastY, lastZ);

        if (!currentBlockPos.equals(lastBlockPos) && running.get() && !paused.get() && !isExecutingTurn.get()) {

            boolean shouldTurn = LayoutScanner.
                    checkTurnPointSeedCrops(mc.thePlayer, 4);

            if (currentBlockPos != null && shouldTurn && !isExecutingTurn.get()) {
                if (turnDirection == TurnDirection.LEFT)  {
                    updateTurnDirection(TurnDirection.RIGHT);
                } else if (turnDirection == TurnDirection.RIGHT) {
                    updateTurnDirection(TurnDirection.LEFT);
                }
            }
        }

        // Update last position for the next tick
        lastX = event.player.posX;
        lastY = event.player.posY;
        lastZ = event.player.posZ;
    }

    public void start() {

        if (!running.get()) {
            System.out.println("Activating macro.");
            running.set(true);
            paused.set(false);

            farmingThread = new Thread(this::runLogic, "FarmingThread");
            farmingThread.start();
        } else {
            System.out.println("Macro is already running.");
        }
    }

    public void pause() {

        if (!running.get()) {
            System.out.println("Macro not running, cannot pause.");
            return;
        }

        if (!paused.get()) {
            System.out.println("Pausing macro.");
            paused.set(true);

            mc.addScheduledTask(this::releaseAllCommonKeys);

        } else {
            System.out.println("Resuming macro.");
            paused.set(false);

            mc.addScheduledTask(this::repressRecentKeys);
        }
    }

    public void stop() {

        if (running.get()) {
            System.out.println("Stopping macro.");
            running.set(false);
            paused.set(false);

            if (farmingThread != null && farmingThread.isAlive()) {
                farmingThread.interrupt();
            }

            if (mc.thePlayer != null && mc.gameSettings != null) {
                mc.addScheduledTask(() -> {
                    releaseAllCommonKeys();
                    recentKeys.get().clear();
                });
            }
        } else {
            System.out.println("Macro not running, no need to stop.");
        }
    }

    private void checkInterrupted() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
    }

    private void runLogic() {
        try {
            if (!running.get()) return;

            // Find starting block
            BlockMatch origin = LayoutScanner.findNearestCropPos(5);
            if (origin == null) return;

            // Highlight starting block
            BlockHighlighter.setHighlight(origin.pos);

            // Rotate player to starting block
            PlayerRotation.Rotation rotation = Utils.blockPosToYawPitch(
                    origin.pos, mc.thePlayer.getPositionVector());

            new PlayerRotation(new PlayerRotation.Rotation(rotation.yaw, rotation.pitch), 15L);
            TimingUtil.randomSleep(300, 500, paused);
            Utils.faceNearestCardinal();

            // Determine farm layout axis and initial walking direction
            boolean isRowAlongX = LayoutScanner.isRowAlongX(origin.pos, 10);
            turnDirection = LayoutScanner.initialTurnDirection(
                    origin.pos, 250, isRowAlongX);

            if (turnDirection == TurnDirection.LEFT) leftTurn();
            else if (turnDirection == TurnDirection.RIGHT) rightTurn();
            System.out.println("Executed first turn");

            checkInterrupted();

            // Hold farm button
            TimingUtil.randomSleep(100, 200, paused);
            mc.addScheduledTask(() -> InputSimulator.holdAttack(true));

            while (running.get()) {
                while (paused.get()) {
                    TimingUtil.randomSleep(300, 500, paused);
                    checkInterrupted();
                }

                // Wait for a turn signal
                String turn = turnQueue.take(); // This blocks until "LEFT" or "RIGHT"

                if ("LEFT".equals(turn)) leftTurn();
                else if ("RIGHT".equals(turn)) rightTurn();
                else {
                    System.out.println("Unexpected turn type");
                }

                checkInterrupted();
            }

        } catch (InterruptedException e) {
            System.err.println("Macro was completely stopped: " + e);
            farmingThread = null;
        } catch (Exception e) {
            System.err.println("Error in macro logic: " + e);
            farmingThread = null;
        }
    }

    private void leftTurn() {
        isExecutingTurn.set(true);
        TimingUtil.randomSleep(500, 750, paused);

        mc.addScheduledTask(() -> {
            InputSimulator.setKeybindState(mc.gameSettings.keyBindRight, false);
            recentKeys.get().remove(mc.gameSettings.keyBindRight);

            InputSimulator.setKeybindState(mc.gameSettings.keyBindLeft, true);
            recentKeys.get().add(mc.gameSettings.keyBindLeft);
        });

        TimingUtil.randomSleep(1000, 1500, paused);
        isExecutingTurn.set(false);
    }

    private void rightTurn() {
        isExecutingTurn.set(true);
        TimingUtil.randomSleep(500, 750, paused);

        mc.addScheduledTask(() -> {
            InputSimulator.setKeybindState(mc.gameSettings.keyBindLeft, false);
            recentKeys.get().remove(mc.gameSettings.keyBindLeft);

            InputSimulator.setKeybindState(mc.gameSettings.keyBindRight, true);
            recentKeys.get().add(mc.gameSettings.keyBindRight);
        });

        TimingUtil.randomSleep(1000, 1500, paused);
        isExecutingTurn.set(false);
    }

    private void releaseAllCommonKeys() {
        if (mc.gameSettings != null) {
            InputSimulator.holdAttack(false);
            for (KeyBinding key : recentKeys.get()) {
                if (key != null) {
                    InputSimulator.setKeybindState(key, false);
                    TimingUtil.randomSleep(10, 30, paused);
                }
            }
        }
    }

    private void repressRecentKeys() {
        if (mc.gameSettings != null) {
            InputSimulator.holdAttack(true);
            System.out.println(recentKeys.get());
            for (KeyBinding key : recentKeys.get()) {
                if (key != null) {
                    InputSimulator.setKeybindState(key, true);
                    TimingUtil.randomSleep(10, 30, paused);
                }
            }
        }
    }

    public void updateTurnDirection(TurnDirection turnDirection) {
        if (turnDirection == TurnDirection.LEFT) {
            turnQueue.offer("LEFT");
        } else if (turnDirection == TurnDirection.RIGHT) {
            turnQueue.offer("RIGHT");
        }

        this.turnDirection = turnDirection;
    }

    public enum TurnDirection {
        LEFT, RIGHT;
    }
}
