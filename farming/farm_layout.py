"""Farm layout detection and management"""

from abc import ABC, abstractmethod
from typing import List
from system.lib.minescript import BlockPos
from general.utils import get_direction_vectors
from general.movement import Direction


class FarmLayout(ABC):
    """
    Abstract base class for different farm layouts.
    """

    @abstractmethod
    def get_rows(self, start_pos: BlockPos, block_type: str) -> List[int]:
        pass

    @abstractmethod
    def get_row_end(self, start_pos: BlockPos, row_y: int,
                    current_yaw: float, block_type: str, direction: Direction) -> BlockPos:
        pass

    @abstractmethod
    def get_teleport_command(self) -> str:
        pass


class CircularCropFarm(FarmLayout):
    """
    Circular farm layout for crops (wheat, carrots, potatoes and nether wart).
    A circular farm layout has multiple rows stacked on top of each other,
    and drops down one row each time it reaches the end of a row.
    """

    def __init__(self, minescript, scan_range: int = 255):
        self._ms = minescript
        self._scan_range = scan_range

    def get_rows(self, start_pos: BlockPos, block_type: str) -> List[int]:
        """
        Find all farm rows by scanning vertically
        """
        rows = []
        for layer_y in range(-20, 21):
            x, y, z = start_pos
            test_y = y + layer_y
            block = self._ms.getblock(x, test_y, z)
            if block_type in block:
                rows.append(test_y)

        rows.sort(reverse=True)  # top to bottom
        return rows

    def get_row_end(self, start_pos: BlockPos, row_y: int,
                    current_yaw: float, block_type: str, direction: Direction) -> BlockPos:
        """
        Find the end position of a row in the given direction.
        """
        (forward_x, forward_z), (right_x, right_z), (left_x, left_z) = get_direction_vectors(current_yaw)
        step_x, step_z = (right_x, right_z) if direction == direction.RIGHT else (left_x, left_z)

        x, _, z = map(int, start_pos)
        last_pos = (x, row_y, z)

        for i in range(1, self._scan_range):
            nx = int(x + step_x * i)
            nz = int(z + step_z * i)
            block = self._ms.getblock(nx, row_y, nz)

            if block_type in block:
                last_pos = (nx, row_y, nz)
            else:
                break

        # Return lane position (one block back and down, below the player)
        lane_x = last_pos[0] - forward_x
        lane_y = row_y - 2
        lane_z = last_pos[2] - forward_z
        return lane_x, lane_y, lane_z
