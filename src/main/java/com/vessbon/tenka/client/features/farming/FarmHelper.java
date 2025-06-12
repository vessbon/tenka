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

    private AtomicBoolean setupDone = new AtomicBoolean(false);
    private AtomicBoolean running = new AtomicBoolean(false);
    private AtomicBoolean paused = new AtomicBoolean(false);
    private AtomicReference<Set<KeyBinding>> recentKeys =
            new AtomicReference<>(new CopyOnWriteArraySet<>());

    private BlockingQueue<FarmCommand> commandQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean isExecutingTurn = new AtomicBoolean(false);

    private boolean currentFarmAxisIsX;
    private PlayerRotation.Rotation farmRotation;
    private FarmCommand turnDirection;

    private PreviousState lastState;
    private BlockPos lastFarmPos;
    private int turnTries = 0;

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
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (Keyboard.getEventKey() == 112 || Keyboard.getEventKey() == 114) return;

        if (running.get() && !paused.get()) {
            if (Keyboard.getEventKeyState() && !isMacroKey(Keyboard.getEventKey())) {
                pause();
                System.out.println("Macro paused due to manual key input.");

            } else if (!Keyboard.getEventKeyState() && isMacroKey(Keyboard.getEventKey())) {
                KeyBinding keyBinding = getKeyBindingFromCode(Keyboard.getEventKey());
                if (keyBinding == null) return;

                mc.addScheduledTask(() -> InputSimulator.setKeybindState(keyBinding, true));
            }
        }
    }

    @SubscribeEvent
    public void onMouseInput(InputEvent.MouseInputEvent event) {
        if (running.get() && !paused.get()) {
            int button = org.lwjgl.input.Mouse.getEventButton();
            boolean state = org.lwjgl.input.Mouse.getEventButtonState();

            if (state) {
                if (!paused.get()) {
                    pause();
                    System.out.println("Macro paused due to mouse click.");
                }
            }
        }
    }

    @SubscribeEvent
    public void onMouseMoveOrScroll(net.minecraftforge.client.event.MouseEvent event) {
        if (running.get() && !paused.get()) {
            if (!paused.get()) {
                pause();
                System.out.println("Macro paused due to mouse movement or scroll.");
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {

        if (event.phase != TickEvent.Phase.END || event.player != mc.thePlayer) return;

        BlockPos currentBlockPos = event.player.getPosition();
        BlockPos lastBlockPos = new BlockPos(lastX, lastY, lastZ);

        if (!currentBlockPos.equals(lastBlockPos) && running.get() && !paused.get()) {

            if (hasLeftFarm() && !paused.get()) {
                System.out.println("Left the farm");
                pause();
            }

            if (!isExecutingTurn.get() && setupDone.get()) {
                boolean shouldTurn = LayoutScanner.
                        checkTurnPointSeedCrops(mc.thePlayer, 4, currentFarmAxisIsX);

                if (!shouldTurn) {
                    MovingObjectPosition ray = mc.thePlayer.rayTrace(1.5, 1.0f);

                    if (ray != null && ray.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                        lastFarmPos = ray.getBlockPos();
                    } else {
                        lastFarmPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 0.1, mc.thePlayer.posZ);
                    }

                    BlockHighlighter.setHighlight(lastFarmPos);
                }

                if (currentBlockPos != null && shouldTurn) {
                    isExecutingTurn.set(true);

                    if (turnDirection == FarmCommand.TURN_LEFT)  {
                        sendCommand(FarmCommand.TURN_RIGHT);
                    } else if (turnDirection == FarmCommand.TURN_RIGHT) {
                        sendCommand(FarmCommand.TURN_LEFT);
                    }
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
        } else if (paused.get()) {
            System.out.println("Macro is already paused, cannot pause.");
            return;
        } else if (lastFarmPos == null || turnDirection == null) {
            System.out.println("Last farm pos or previous turn direction does not exist, cannot pause.");
            return;
        }

        paused.set(true);
        System.out.println("Pausing macro.");

        lastState = new PreviousState(lastFarmPos, turnDirection);
        mc.addScheduledTask(this::releaseAllCommonKeys);

        // sendCommand(FarmCommand.PAUSE);
    }

    public void resume() {
        if (!running.get()) {
            System.out.println("Macro not running, cannot resume.");
            return;
        } else if (!paused.get()) {
            System.out.println("Macro is already running, cannot resume.");
            return;
        }

        paused.set(false);
        System.out.println("Resuming macro.");

        restoreState(lastState);
        mc.addScheduledTask(this::repressRecentKeys);

        // sendCommand(FarmCommand.RESUME);
    }

    public void stop() {

        if (running.get()) {
            System.out.println("Stopping macro.");
            setupDone.set(false);
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

    public void prematureStop() {
        setupDone.set(false);
        running.set(false);
        paused.set(false);

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

            new PlayerRotation(blockRotation, 200L);
            TimingUtil.randomSleep(300, 500, paused);

            farmRotation = new PlayerRotation.Rotation(
                    Utils.returnNearestCardinalYaw(), Utils.getRandomFloat(3.5f, 6.5f));

            checkInterrupted();

            new PlayerRotation(farmRotation, 100L);

            // Determine farm layout axis and initial walking direction
            currentFarmAxisIsX = LayoutScanner.isRowAlongX(origin.pos, 10);
            turnDirection = LayoutScanner.initialTurnDirection(
                    origin.pos, 250, currentFarmAxisIsX);

            if (turnDirection == FarmCommand.TURN_LEFT) leftTurn();
            else if (turnDirection == FarmCommand.TURN_RIGHT) rightTurn();
            System.out.println("Executed first turn");

            checkInterrupted();

            // Hold farm button
            TimingUtil.randomSleep(100, 200, paused);
            // mc.addScheduledTask(() -> InputSimulator.holdAttack(true)); for testing enable later plz

            checkInterrupted();

            setupDone.set(true);

            while (running.get()) {

                // Poll commandQueue with timeout so we can check paused flag regularly
                FarmCommand command = commandQueue.take();

                switch(command) {
                    /* case PAUSE:
                        break;

                    case RESUME:
                        break; */

                    case TURN_LEFT:
                        TimingUtil.randomSleep(500, 750, paused);
                        if (!paused.get()) leftTurn();
                        turnDirection = FarmCommand.TURN_LEFT;
                        TimingUtil.randomSleep(1000, 1500, paused);
                        isExecutingTurn.set(false);
                        break;

                    case TURN_RIGHT:
                        TimingUtil.randomSleep(500, 750, paused);
                        if (!paused.get()) rightTurn();
                        turnDirection = FarmCommand.TURN_RIGHT;
                        TimingUtil.randomSleep(1000, 1500, paused);
                        isExecutingTurn.set(false);
                        break;

                    default:
                        System.out.println("Unexpected command.");
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
        mc.addScheduledTask(() -> {
            InputSimulator.setKeybindState(mc.gameSettings.keyBindRight, false);
            recentKeys.get().remove(mc.gameSettings.keyBindRight);

            InputSimulator.setKeybindState(mc.gameSettings.keyBindLeft, true);
            recentKeys.get().add(mc.gameSettings.keyBindLeft);
        });
    }

    private void rightTurn() {
        mc.addScheduledTask(() -> {
            InputSimulator.setKeybindState(mc.gameSettings.keyBindLeft, false);
            recentKeys.get().remove(mc.gameSettings.keyBindLeft);

            InputSimulator.setKeybindState(mc.gameSettings.keyBindRight, true);
            recentKeys.get().add(mc.gameSettings.keyBindRight);
        });
    }

    private void releaseAllCommonKeys() {
        if (mc.gameSettings != null) {
            InputSimulator.holdAttack(false);
            for (KeyBinding key : recentKeys.get()) {
                if (key != null) {
                    InputSimulator.setKeybindState(key, false);
                }
            }
        }
    }

    private void repressRecentKeys() {
        if (mc.gameSettings != null) {
            // InputSimulator.holdAttack(true); enable later plz
            System.out.println(recentKeys.get());
            for (KeyBinding key : recentKeys.get()) {
                if (key != null) {
                    InputSimulator.setKeybindState(key, true);
                }
            }
        }
    }

    public static class PreviousState {

        public BlockPos pos;
        public FarmCommand lastTurnDirection;

        public PreviousState(BlockPos lastGroundBlock, FarmCommand lastTurnDirection) {
            this.pos = lastGroundBlock.add(0, 1, 0);
            this.lastTurnDirection = lastTurnDirection;
        }
    }

    public void restoreState(PreviousState state) {
        new PlayerRotation(farmRotation, 10L);
        turnDirection = state.lastTurnDirection;
    }

    public void sendCommand(FarmCommand command) {
        commandQueue.offer(command);
    }

    public boolean isMacroKey(int keyCode) {
        Set<KeyBinding> macroKeys = recentKeys.get();
        for (KeyBinding key : macroKeys) {
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

    private boolean hasLeftFarm() {
        if (lastFarmPos == null) return false;

        double distance = mc.thePlayer.getPosition().distanceSq(lastFarmPos);
        return distance > 80;
    }

    public enum FarmCommand {
        STOP, PAUSE, RESUME, TURN_LEFT, TURN_RIGHT;
    }
}
