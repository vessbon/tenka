from system.lib.minescript import BlockPos
import math

def get_direction_vectors(yaw: float) ->    tuple[tuple[int, int],
                                            tuple[int, int],
                                            tuple[int, int]]:

    """
    Returns the direction vectors for the given yaw angle.
    """

    # Convert yaw to radians
    rad = math.radians(yaw)

    # Forward vector
    forward_x = -math.sin(rad)
    forward_z = math.cos(rad)

    # Right vector
    right_x = -forward_z
    right_z = forward_x

    # Left vector
    left_x = forward_z
    left_z = -forward_x

    return ((round(forward_x), round(forward_z)),
            (round(right_x), round(right_z)),
            (round(left_x), round(left_z)))


def is_within_radius(player_pos: tuple[int, int, int],
                     target_pos: tuple[int, int, int],
                     radius: int) -> bool:

    """
    Check if the player position is within `radius` blocks of the target block.
    """

    px, py, pz = player_pos
    tx, ty, tz = target_pos

    dx = px - tx
    dy = py - ty
    dz = pz - tz

    distance = math.sqrt(dx*dx + dy*dy + dz*dz)
    return distance <= radius


def get_actual_block(block_pos: tuple[float, float, float] | list[float]) -> BlockPos | None:
    """
    Returns the actual block position, with each axis being an integer.
    """

    x, y, z = block_pos
    return math.floor(x), math.floor(y), math.floor(z)
