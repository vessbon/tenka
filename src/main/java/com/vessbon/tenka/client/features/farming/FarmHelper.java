package com.vessbon.tenka.client.features.farming;

import com.vessbon.tenka.client.utils.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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

    private final AtomicBoolean setupDone = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final AtomicReference<Set<KeyBinding>> recentKeys =
            new AtomicReference<>(new CopyOnWriteArraySet<>());

    private final BlockingQueue<FarmCommand> commandQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean isExecutingTurn = new AtomicBoolean(false);

    private boolean currentFarmAxisIsX;
    private PlayerRotation.Rotation farmRotation;
    private FarmCommand turnDirection;

    private PreviousState lastState;
    private BlockPos lastFarmPos;

    private List<DelayedMainThreadTask> delayedTasks = new ArrayList<>();
    private long currentTickCounter = 0; // To track ticks for precise scheduling


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
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        int keyCode = Keyboard.getEventKey();
        if (keyCode == 59 || keyCode == 61) return;

        if (running.get() && !paused.get()) {
            if (Keyboard.getEventKeyState() && !isHelperKey(keyCode)) {
                pause();
                System.out.println("Helper paused due to manual key input.");

            } else if (!Keyboard.getEventKeyState() && isHelperKey(keyCode)) {
                KeyBinding keyBinding = getKeyBindingFromCode(keyCode);
                if (keyBinding == null) return;

                mc.addScheduledTask(() -> InputSimulator.setKeybindState(keyBinding, true));
            }
        }
    }

    @SubscribeEvent
    public void onMouseInput(InputEvent.MouseInputEvent event) {
        if (running.get() && !paused.get()) {
            boolean state = org.lwjgl.input.Mouse.getEventButtonState();

            if (state) {
                if (!paused.get()) {
                    pause();
                    System.out.println("Helper paused due to mouse click.");
                }
            }
        }
    }

    @SubscribeEvent
    public void onMouseMoveOrScroll(net.minecraftforge.client.event.MouseEvent event) {
        if (running.get() && !paused.get()) {
            if (!paused.get()) {
                pause();
                System.out.println("Helper paused due to mouse movement or scroll.");
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {

        if (event.phase != TickEvent.Phase.END || event.player != mc.thePlayer) return;

        BlockPos currentBlockPos = event.player.getPosition();

        if (!currentBlockPos.equals(lastFarmPos) && running.get() && !paused.get()) {
            if (setupDone.get()) {
                if (hasLeftFarm() && !paused.get()) {
                    System.out.println("You left the farm, pausing.");
                    pause();
                }

                boolean shouldTurn = LayoutScanner.
                        checkTurnPointSeedCrops(mc.thePlayer, 4, currentFarmAxisIsX);

                if (!shouldTurn && isExecutingTurn.get()) {
                    isExecutingTurn.set(false);
                }

                if (!isExecutingTurn.get()) {

                    if (shouldTurn) {
                        isExecutingTurn.set(true);

                        if (turnDirection == FarmCommand.TURN_LEFT) {
                            sendCommand(FarmCommand.TURN_RIGHT);
                        } else if (turnDirection == FarmCommand.TURN_RIGHT) {
                            sendCommand(FarmCommand.TURN_LEFT);
                        }
                    } else {
                        getLastFarmPos();
                        BlockHighlighter.setHighlight(lastFarmPos);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            currentTickCounter++; // Increment tick counter

            // Use an iterator to safely remove elements during iteration
            Iterator<DelayedMainThreadTask> iterator = delayedTasks.iterator();
            while (iterator.hasNext()) {
                DelayedMainThreadTask task = iterator.next();
                if (currentTickCounter >= task.targetTick) {
                    mc.addScheduledTask(task.runnable);
                    iterator.remove(); // Remove the task after scheduling for execution
                }
            }
        }
    }

    public void start() {

        if (!running.get()) {
            System.out.println("Activating helper.");
            paused.set(false);

            farmingThread = new Thread(this::runLogic, "FarmingThread");
            farmingThread.start();
        } else {
            System.out.println("Helper is already running.");
        }
    }

    public void pause() {

        if (!running.get()) {
            System.out.println("Helper not running, cannot pause.");
            return;
        } else if (paused.get()) {
            System.out.println("Helper is already paused, cannot pause.");
            return;
        } else if (lastFarmPos == null || turnDirection == null) {
            System.out.println("Last farm pos or previous turn direction does not exist, cannot pause.");
            return;
        }

        paused.set(true);
        System.out.println("Pausing helper.");

        lastState = new PreviousState(lastFarmPos, turnDirection);
        releaseAllCommonKeys();

        // sendCommand(FarmCommand.PAUSE);
    }

    public void resume() {
        if (!running.get()) {
            System.out.println("Helper not running, cannot resume.");
            return;
        } else if (!paused.get()) {
            System.out.println("Helper is already running, cannot resume.");
            return;
        }

        paused.set(false);
        System.out.println("Resuming helper.");

        restoreState(lastState);
        repressRecentKeys();

        // sendCommand(FarmCommand.RESUME);
    }

    public void stop() {

        if (running.get()) {

            System.out.println("Stopping helper.");
            cleanup();

            if (farmingThread != null && farmingThread.isAlive()) {
                farmingThread.interrupt();
            }

            if (mc.thePlayer != null && mc.gameSettings != null) {
                releaseAllCommonKeys();
                recentKeys.get().clear();
            }

            commandQueue.offer(FarmCommand.STOP);

        } else {
            System.out.println("Helper not running, no need to stop.");
        }
    }

    public void prematureStop() {

        cleanup();

        if (farmingThread != null && farmingThread.isAlive()) {
            farmingThread.interrupt();
        }
    }

    private void checkInterrupted() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
    }

    private void runLogic() {
        try {

            // Find starting block
            BlockMatch origin = LayoutScanner.getHoveredCrop();

            if (origin == null) {
                System.out.println("Please aim at a crop block.");
                prematureStop();
                return;
            }

            running.set(true);

            // Highlight starting block
            BlockHighlighter.setHighlight(origin.pos);

            // Rotate player to starting block
            PlayerRotation.Rotation blockRotation = Utils.blockPosToYawPitch(
                    origin.pos, mc.thePlayer.getPositionVector());

            new PlayerRotation(blockRotation, 400L);
            TimingUtil.randomSleep(800, 900, paused);

            // Rotate the player to the proper yaw and pitch for farming
            float requiredYaw = Utils.returnNearestCardinalYaw();
            float requiredPitch = Utils.getRandomFloat(3.5f, 6.5f);

            System.out.println(requiredYaw);

            farmRotation = new PlayerRotation.Rotation(requiredYaw, requiredPitch);
            new PlayerRotation(farmRotation, 800L);

            TimingUtil.randomSleep(500, 800, paused);
            mc.addScheduledTask(() -> InputSimulator.setKeybindState(mc.gameSettings.keyBindForward, true));
            TimingUtil.randomSleep(70, 115, paused);
            mc.addScheduledTask(() -> InputSimulator.setKeybindState(mc.gameSettings.keyBindForward, false));

            checkInterrupted();

            // Determine farm layout axis and initial walking direction
            currentFarmAxisIsX = LayoutScanner.isRowAlongX(origin.pos, 10);
            turnDirection = LayoutScanner.initialTurnDirection(
                    origin.pos, 250);

            if (turnDirection == FarmCommand.TURN_LEFT) leftTurn();
            else if (turnDirection == FarmCommand.TURN_RIGHT) rightTurn();
            System.out.println("Executed first turn");

            checkInterrupted();

            // Hold farm button
            TimingUtil.randomSleep(100, 200, paused);
            // mc.addScheduledTask(() -> InputSimulator.holdAttack(true)); for testing enable later plz

            checkInterrupted();

            getLastFarmPos();
            setupDone.set(true);

            while (running.get()) {

                // Poll commandQueue with timeout so we can check paused flag regularly
                FarmCommand command = commandQueue.take();

                switch(command) {
                    /* case PAUSE:
                        break;

                    case RESUME:
                        break; */

                    case STOP:
                        checkInterrupted();
                        break;

                    case TURN_LEFT:
                        System.out.println("Turning left");
                        TimingUtil.randomSleep(200, 400, paused);
                        if (!paused.get()) leftTurn();
                        turnDirection = FarmCommand.TURN_LEFT;
                        break;

                    case TURN_RIGHT:
                        System.out.println("Turning right");
                        TimingUtil.randomSleep(200, 400, paused);
                        if (!paused.get()) rightTurn();
                        turnDirection = FarmCommand.TURN_RIGHT;
                        break;

                    default:
                        System.out.println("Unexpected command.");
                }

                checkInterrupted();
            }

        } catch (InterruptedException e) {
            System.err.println("Helper was completely stopped: " + e);
            farmingThread = null;
        } catch (Exception e) {
            System.err.println("Error in helper logic: " + e);
            farmingThread = null;
        }
    }

    private void leftTurn() {
        scheduleTaskOnMainThread(() -> {
            InputSimulator.setKeybindState(mc.gameSettings.keyBindRight, false);
            recentKeys.get().remove(mc.gameSettings.keyBindRight);
        }, 0);

        scheduleTaskOnMainThread(() -> {
            InputSimulator.setKeybindState(mc.gameSettings.keyBindLeft, true);
            recentKeys.get().add(mc.gameSettings.keyBindLeft);
        }, Utils.getRandomLong(5L, 15L));
    }

    private void rightTurn() {
        scheduleTaskOnMainThread(() -> {
            InputSimulator.setKeybindState(mc.gameSettings.keyBindLeft, false);
            recentKeys.get().remove(mc.gameSettings.keyBindLeft);
        }, 0);

        scheduleTaskOnMainThread(() -> {
            InputSimulator.setKeybindState(mc.gameSettings.keyBindRight, true);
            recentKeys.get().add(mc.gameSettings.keyBindRight);
        }, Utils.getRandomLong(5L, 15L));
    }

    private void releaseAllCommonKeys() {
        if (mc.gameSettings == null) return;
        // scheduleTaskOnMainThread(() -> InputSimulator.holdAttack(false), 0);
        KeyBinding[] keysToRelease = recentKeys.get().toArray(new KeyBinding[0]);
        for (int i = 0; i < keysToRelease.length; i++) {
            final KeyBinding key = keysToRelease[i];
            if (key != null) {
                scheduleTaskOnMainThread(() -> {
                    InputSimulator.setKeybindState(key, false);
                }, 1L * (i + 1));
            }
        }
    }

    private void repressRecentKeys() {
        if (mc.gameSettings == null) return;
        // scheduleTaskOnMainThread(() -> InputSimulator.holdAttack(true), 0);
        KeyBinding[] keysToRepress = recentKeys.get().toArray(new KeyBinding[0]);
        for (int i = 0; i < keysToRepress.length; i++) {
            final KeyBinding key = keysToRepress[i];
            if (key != null) {
                scheduleTaskOnMainThread(() -> {
                    InputSimulator.setKeybindState(key, true);
                }, 1L * (i + 1));
            }
        }
    }

    private void scheduleTaskOnMainThread(Runnable task, long ticksDelay) {
        long targetTick = currentTickCounter + ticksDelay;
        synchronized (delayedTasks) { // Synchronize access to delayedTasks to prevent concurrent modification
            delayedTasks.add(new DelayedMainThreadTask(task, targetTick));
        }
    }

    public void restoreState(PreviousState state) {
        new PlayerRotation(farmRotation, 800L);
        turnDirection = state.lastTurnDirection;
    }

    public void sendCommand(FarmCommand command) {
        commandQueue.offer(command);
    }

    public boolean isHelperKey(int keyCode) {
        Set<KeyBinding> helperKeys = recentKeys.get();
        for (KeyBinding key : helperKeys) {
            if (key != null && key.getKeyCode() == keyCode) {
                return true;
            }
        }
        return false;
    }

    private KeyBinding getKeyBindingFromCode(int keyCode) {
        // Iterate through all registered key bindings in Minecraft's game settings
        for (KeyBinding key : mc.gameSettings.keyBindings) {
            if (key != null && key.getKeyCode() == keyCode) {
                return key;
            }
        }
        return null;
    }

    private void getLastFarmPos() {
        MovingObjectPosition ray = mc.thePlayer.rayTrace(1.5, 1.0f);

        if (ray != null && ray.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            lastFarmPos = ray.getBlockPos();
        } else {
            lastFarmPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 0.1, mc.thePlayer.posZ);
        }
    }

    private boolean hasLeftFarm() {
        if (lastFarmPos == null) return false;

        double distance = mc.thePlayer.getPosition().distanceSq(lastFarmPos);
        return distance > 80;
    }

    private void cleanup() {
        lastFarmPos = null;
        setupDone.set(false);
        running.set(false);
        paused.set(false);
        isExecutingTurn.set(false);

        synchronized (delayedTasks) {
            delayedTasks.clear();
        }

        currentTickCounter = 0;
    }

    public enum FarmCommand {
        STOP, PAUSE, RESUME, TURN_LEFT, TURN_RIGHT
    }

    public static class PreviousState {

        public BlockPos pos;
        public FarmCommand lastTurnDirection;

        public PreviousState(BlockPos lastGroundBlock, FarmCommand lastTurnDirection) {
            this.pos = lastGroundBlock.add(0, 1, 0);
            this.lastTurnDirection = lastTurnDirection;
        }
    }

    private static class DelayedMainThreadTask {
        Runnable runnable;
        long targetTick;

        public DelayedMainThreadTask(Runnable runnable, long targetTick) {
            this.runnable = runnable;
            this.targetTick = targetTick;
        }
    }
}
