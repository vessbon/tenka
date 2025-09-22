from enum import Enum
import time
import random

class Direction(Enum):
    LEFT = 0
    RIGHT = 1
    FORWARDS = 2
    BACKWARDS = 3

class MovementHandler:
    def __init__(self, minescript):
        self.minescript = minescript
        self._initial_direction = None
        self.direction = None

    def set_initial_direction(self, direction : Direction):
        if type(direction) == Direction:
            self._initial_direction = direction
        else:
            raise TypeError("direction must be of type Direction")

    def move_right(self):
        if self.direction is None or self.direction != Direction.RIGHT:
            self.stop_moving()
            self.direction = Direction.RIGHT
            time.sleep(random.uniform(0.02, 0.1))
            self.minescript.player_press_right(True)

    def move_left(self):
        if self.direction is None or self.direction != Direction.LEFT:
            self.stop_moving()
            self.direction = Direction.LEFT
            time.sleep(random.uniform(0.02, 0.1))
            self.minescript.player_press_left(True)

    def reverse_direction(self):
        if self.direction == Direction.LEFT:
            self.move_right()
        elif self.direction == Direction.RIGHT:
            self.move_left()

    def stop_moving(self):
        if self.direction == Direction.LEFT:
            self.minescript.player_press_left(False)
        elif self.direction == Direction.RIGHT:
            self.minescript.player_press_right(False)

    def apply_initial_direction(self):
        if self._initial_direction == Direction.LEFT:
            self.move_left()
        elif self._initial_direction == Direction.RIGHT:
            self.move_right()

class IdleChecker:
    def __init__(self, max_idle_time=2.0):
        """
        max_idle_time: how long (in seconds) the player can stand still before being considered idle
        """

        self.max_idle_time = max_idle_time
        self.last_moving_time = time.time()

    def update(self, velocity: tuple[float, float, float]) -> bool:
        """
        Checks if the player has stopped moving in the horizontal direction for longer than the `max_idle_time`.
        Call this every time you want to update idle condition, more frequent updates result in more accurate results.
        """

        vx, _, vz = velocity
        speed = (vx*vx + vz*vz) ** 0.5

        if speed > 0.02: # threshold to ignore floating-point noise
            self.last_moving_time = time.time()

        return (time.time() - self.last_moving_time) >= self.max_idle_time
