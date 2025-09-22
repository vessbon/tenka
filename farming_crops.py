import system.lib.minescript as ms
from system.lib.minescript import EventType
from system.lib.minescript import BlockPos

from general.player import Player
import general.inventory as inventory
from general.utils import get_direction_vectors, get_actual_block
from general.utils import is_within_radius
from general.movement import *
import random
import queue
import sys
import math
import time
import threading

# All supported start blocks
SUPPORTED_CROPS = {
    "wheat": "minecraft:wheat",
    "carrots": "minecraft:carrots",
    "potatoes": "minecraft:potatoes",
    "nether_wart": "minecraft:nether_wart",
}

# All keys (as keycodes) that do not interrupt the macro when pressed
SUPPORTED_KEYS = {
    342,  # LEFT ALT
    258,  # TAB
    300, # F11
    292, # F3
    294, # F5
}

# Constants
MAX_DISTANCE = 5.0  # maximum distance (blocks) from previous logged position before interrupting
SCAN_RANGE = 255
TICK_TIME = 0.05

# Get the start block
start_block = ms.player_get_targeted_block(10)
if start_block is None or "age" not in start_block.type: raise Exception("Not looking at a valid block.")
start_block_type = start_block.type.split("[")[0]  # remove the age property
if start_block_type not in SUPPORTED_CROPS.values(): raise Exception("Not a supported crop.")

debugging = False
if len(sys.argv) > 2 and sys.argv[2]:
    debugging = True

macro_checking = False
if len(sys.argv) > 3 and sys.argv[3]:
    macro_checking = True
farming = True


# Create movement handler
movement_handler = MovementHandler(ms)
idle_checker = IdleChecker(max_idle_time=1.0)


def stop_farming(reason="Pausing macro."):
    global farming
    ms.echo(reason)

    movement_handler.stop_moving()
    time.sleep(random.uniform(0.08, 0.12))
    if not debugging:
        ms.player_press_attack(False)
    farming = False


def get_rows(start_pos: BlockPos, block_type: str) -> list:
    rows = []
    for layer_y in range(-20, 20 + 1):
        x = start_pos[0]
        y = start_pos[1] + layer_y
        z = start_pos[2]
        block = ms.getblock(x, y, z)
        if block_type in block:
            rows.append(y)

    rows.sort(reverse=True)
    return rows


def get_row_end(start_pos: BlockPos, y: int, current_yaw: float, block_type: str, direction: Direction) -> BlockPos:
    (forward_x, forward_z), (right_x, right_z), (left_x, left_z) = get_direction_vectors(current_yaw)
    step_x, step_z = (right_x, right_z) if direction == Direction.RIGHT else (left_x, left_z)

    x, _, z = map(int, start_pos)
    last_pos = (x, y, z)

    for i in range(1, SCAN_RANGE):
        nx = int(x + step_x * i)
        nz = int(z + step_z * i)
        block = ms.getblock(nx, y, nz)

        if block_type in block:
            last_pos = (nx, y, nz)
        else:
            break

    lane_x = last_pos[0] - forward_x
    lane_y = y - 2
    lane_z = last_pos[2] - forward_z
    return lane_x, lane_y, lane_z


def update_row_data(rows : list) -> tuple[int, int]:
    closest_row_y = max(rows)
    current = ALL_ROWS.index(closest_row_y) + 1
    left = TOTAL_ROWS - current
    ms.echo(f"Current row: {current}, {left} rows left below!")
    return current, left


def get_valid_rows(rows : list, player_y : float | int) -> list:
    return [r for r in rows if r <= int(player_y)]


def macro_checker():
    global farming

    while True:
        if not farming:
            break

        time.sleep(random.uniform(5, 10)) # macro check frequency

        check = math.ceil(random.random() * 5)
        if check == 1:
            ms.echo("GUI check")
            ms.player_inventory()
        elif check == 2:
            ms.echo("Check 2")
        elif check == 3:
            ms.echo("Check 3")
        elif check == 4:
            ms.echo("Check 4")
        elif check == 5:
            ms.echo("Check 5")
        else:
            ms.echo("Invalid check.")


if macro_checking:
    thread = threading.Thread(target=macro_checker, daemon=True)
    thread.start()


with ms.EventQueue() as event_queue:
    event_queue.register_key_listener()
    event_queue.register_mouse_listener()

    # Setup player class
    player = Player(ms, ms.get_player())

    # Set starting orientation and original yaw/pitch values to check against
    ms.player_set_orientation(180, 5)
    player.update(ms.get_player())

    start_yaw = player.yaw
    start_pitch = player.pitch


    # --- LOOP VARIABLE SETUP ---
    # Set start position as initial logged position
    logged_position = player.position
    log_time_seconds = 1.0
    last_log_time = time.time()

    # Initial direction
    initial_direction = None

    # Farm layout variables
    last_farm_block = None
    is_at_last_block = False

    # Fall logging
    falling = False
    landing_cooldown = 1
    fall_start_time = None
    landing_start_time = None
    previous_y = player.y


    # Set initial row
    ALL_ROWS = get_rows(start_block.position, start_block_type)
    TOTAL_ROWS = len(ALL_ROWS)
    current_row = ALL_ROWS.index(start_block.position[1]) + 1
    rows_left = TOTAL_ROWS - current_row
    ms.echo(f"Starting at row: {current_row}, {rows_left} rows left below!")


    # Initial movement direction
    if len(sys.argv) > 1 and sys.argv[1].lower().strip() == "left":
        movement_handler.move_left()
    elif len(sys.argv) > 1 and sys.argv[1].lower().strip() == "right":
        movement_handler.move_right()
    else:
        raise Exception("You must specify 'left' or 'right' as an argument.")
    movement_handler.set_initial_direction(movement_handler.direction)


    # Select appropriate equipment for selected crop
    item = None
    if start_block_type == SUPPORTED_CROPS['wheat']:
        item = inventory.get_item("Wheat")
    elif start_block_type == SUPPORTED_CROPS['carrots']:
        item = inventory.get_item("Carrot")
    elif start_block_type == SUPPORTED_CROPS['potatoes']:
        item = inventory.get_item("Potato")
    elif start_block_type == SUPPORTED_CROPS['nether_wart']:
        item = inventory.get_item("Nether Wart")


    # Select the tool from the hotbar only
    if item is not None and 0 <= item.slot <= 8:
        ms.player_inventory_select_slot(item.slot)
    else:
        stop_farming("No valid item found in the hotbar. Cancelling macro.")
        ms.player_inventory_select_slot(0)

    main_hand_item = ms.player_hand_items().main_hand

    if not debugging:
        ms.player_press_attack(True)

    #ms.execute("setspawn")


    # ----- PLAYER FARM LOGIC TICK LOOP -----
    while farming:
        try:
            event = event_queue.get(block=False)
            if (event.type == EventType.KEY and event.key not in SUPPORTED_KEYS) or event.type == EventType.MOUSE:
                stop_farming("Interrupted macro by manual input. Pausing macro.")
        except queue.Empty:
            pass


        # Update player info
        player.update(ms.get_player())


        # -- PLAYER ITEM SWAP FAILSAFE --
        current_item = ms.player_hand_items().main_hand
        if main_hand_item != current_item:
            stop_farming("Main hand item changed. Pausing macro."
                         " If this was not you, it might be a macro check. Pausing macro.")

        # -- PLAYER ROTATE FAILSAFE --
        if abs(player.yaw - start_yaw) > 3 or abs(player.pitch - start_pitch) > 3:
            stop_farming("Macro interrupted by changed yaw or pitch."
                         " If this was not you, it might be a macro check. Pausing macro.")

        # -- OPENED GUI FAILSAFE --
        if ms.screen_name() is not None:
            stop_farming("Macro interrupted by opened GUI."
                         " If this was not you, it might be a macro check. Pausing macro.")


        # Log position every log_time_seconds
        if time.time() - last_log_time >= log_time_seconds:
            logged_position = (player.x, player.y, player.z)
            last_log_time = time.time()

        # Get distance from last logged position
        dx = player.x - logged_position[0]
        dy = player.y - logged_position[1]
        dz = player.z - logged_position[2]
        distance = math.sqrt(dx*dx + dy*dy + dz*dz)

        # -- MOVED TOO FAST / UNINTENTIONAL TP FAILSAFE --
        if distance > MAX_DISTANCE:
            ms.echo(f"Logged position at fail: {logged_position}")
            stop_farming(f"You moved too far from previous position ({distance} blocks from previous position)."
                         " If this was not you, it might be a macro check. Pausing macro.")


        # Attempt to retrieve the last block
        if rows_left == 0 and last_farm_block is None and not falling and fall_start_time is None:
            current_row_y = ALL_ROWS[current_row - 1]
            ms.echo(movement_handler.direction)

            last_farm_block = get_row_end(
                start_block.position,
                current_row_y,
                start_yaw,
                start_block_type,
                movement_handler.direction
            )

            if last_farm_block is not None:
                ms.echo(f"End of farm detected at: {last_farm_block}")
            else:
                stop_farming("End of farm couldn't be detected. Pausing macro.")


        # ---- FALL LOGIC ----
        # Detect fall
        if player.y < previous_y - 1 and not falling:  # 1 block fall minimum
            falling = True
            fall_start_time = time.time()

        # Cooldown between falling and reversing direction
        if fall_start_time is not None and time.time() - fall_start_time > random.uniform(0.5, 2):
            # Reverse movement
            movement_handler.reverse_direction()
            fall_start_time = None

        # -- MOVEMENT STOPPED UNINTENTIONALLY FAILSAFE --
        if not falling and fall_start_time is None:
            idle = idle_checker.update(player.velocity)

            if idle and not is_at_last_block:
                if last_farm_block is not None:
                    block_below_player = player.block_pos_below()
                    is_at_last_block = is_within_radius(block_below_player, last_farm_block, 5)

                    # -- REACHED FARM END LOGIC --
                    if last_farm_block is not None and is_at_last_block:
                        ms.echo("Reached the end of the farm, teleporting to start.")
                        ms.execute("tp Spoonsky -124.7 49 -112.7")
                        #ms.execute("warp garden")

                        # Reset distance logging so no failsafe triggers after teleport
                        start_time = time.time()
                        while True:
                            player.update(ms.get_player())
                            current_block = get_actual_block(ms.player_position())

                            # Check if the block below player has changed, for this farm, only y value is important
                            if current_block[1] - 1 != block_below_player[1]:
                                break
                            if time.time() - start_time > 2:
                                stop_farming("Teleport failed. Pausing macro.")
                                break
                            time.sleep(TICK_TIME)

                        player.update(ms.get_player())
                        logged_position = player.position
                        last_log_time = time.time()

                        # Move in the initial direction, aka farm start direction
                        is_at_last_block = False
                        movement_handler.apply_initial_direction()

                        # Reset row data
                        valid_rows = get_valid_rows(ALL_ROWS, logged_position[1] + 1)
                        if debugging:
                            ms.echo(f"Valid rows: {valid_rows}")
                        current_row, rows_left = update_row_data(valid_rows)

                        previous_y = player.y

                else:
                    stop_farming("Macro interrupted by idling."
                                 " If this was not you, it might be a macro check. Pausing macro.")

        # Detect landing (block under player)
        if falling:
            block_below = player.block_type_below()

            # Detect landing
            if block_below not in ["minecraft:air", "minecraft:water"] and abs(player.vy) < 0.05:
                # Starts immediately after falling
                if landing_start_time is None:
                    landing_start_time = time.time()

                    valid_rows = get_valid_rows(ALL_ROWS, player.y + 2)
                    if valid_rows:
                        current_row, rows_left = update_row_data(valid_rows)

                    else:
                        stop_farming("Tried turning when there are no valid rows. Pausing macro.")

                # `landing_cooldown` seconds after landing
                elif time.time() - landing_start_time >= landing_cooldown:
                    idle_checker.update(player.velocity)
                    falling = False
                    previous_y = player.y
                    landing_start_time = None

        time.sleep(TICK_TIME)  # roughly one minecraft tick

ms.echo("Farm ended! Run script again to farm.")
