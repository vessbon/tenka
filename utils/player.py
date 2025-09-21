from system.lib.minescript import EntityData
from system.lib.minescript import BlockPos
import math

class Player:
    def __init__(self, minescript, entity_data: EntityData):
        self._minescript = minescript
        self._apply_entity_data(entity_data)

    def _apply_entity_data(self, entity_data: EntityData):

        self.yaw = entity_data.yaw
        self.pitch = entity_data.pitch

        self.x = math.floor(entity_data.position[0])
        self.y = math.ceil(entity_data.position[1])
        self.z = math.floor(entity_data.position[2])
        self.position = (self.x, self.y, self.z)

        self.vx = entity_data.velocity[0]
        self.vy = entity_data.velocity[1]
        self.vz = entity_data.velocity[2]
        self.velocity = (self.vx, self.vy, self.vz)

    def update(self, entity_data: EntityData):
        self._apply_entity_data(entity_data)

    def block_pos_below(self) -> BlockPos | None:
        """
        Returns the block position below player position.
        """

        bx, by, bz = self.position
        return bx, by - 1, bz

    def block_type_below(self):
        """
        Returns the block type below player position.
        """

        block_pos = self.block_pos_below()
        bx, by, bz = block_pos
        return self._minescript.getblock(bx, by, bz)

    def __str__(self):
        return (f"Position: {self.position}\n"
                f"Velocity: {self.velocity}\n"
                f"Pitch: {self.pitch}\n"
                f"Yaw: {self.yaw}")