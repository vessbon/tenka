"""Safety checks and failsafe mechanisms for farming macros."""

import math
import time
from typing import Tuple, Optional
from dataclasses import dataclass


@dataclass
class FailsafeState:
    logged_position: Tuple[float, float, float]
    last_log_time: float
    start_item: object
    start_yaw: float
    start_pitch: float


class FailsafeManager:
    def __init__(self, minescript, player, config, state):
        self._ms = minescript
        self._player = player
        self._config = config
        self._state = state

    def check_failsafes(self) -> Tuple[bool, Optional[str]]:
        """
        Run all failsafe checks. Returns (is_safe, error_message).
        """
        checks = [
            self._check_item_swap,
            self._check_orientation,
            self._check_gui_open,
            self._check_movement_distance
        ]

        for check in checks:
            is_safe, message = check()
            if not is_safe:
                return False, message

        return True, None

    def update_position_log(self):
        """
        Update logged position at regular intervals.
        """
        if time.time() - self._state.last_log_time >= self._config.log_interval:
            self._state.logged_position = self._player.position
            self._state.last_log_time = time.time()

    def reset_position_after_teleport(self, new_position: Tuple[int, int, int]):
        """
        Reset logged position after teleportation to prevent false alarms.
        """
        self._state.logged_position = new_position
        self._state.last_log_time = time.time()


    def _check_item_swap(self) -> Tuple[bool, Optional[str]]:
        """
        Check if main hand item changed unexpectedly.
        """
        current_item = self._ms.player_hand_items().main_hand
        if self._state.start_item != current_item:
            return False, "Main hand item changed. Possible macro check."
        return True, None

    def _check_orientation(self) -> Tuple[bool, Optional[str]]:
        """
        Check if player orientation changed too much.
        """
        yaw_diff = abs(self._player.yaw - self._state.start_yaw)
        pitch_diff = abs(self._player.pitch - self._state.start_pitch)

        orientation_tolerance = self._config.orientation_tolerance
        if yaw_diff > orientation_tolerance or pitch_diff > orientation_tolerance:
            return False, "Orientation changes too much."
        return True, None

    def _check_gui_open(self) -> Tuple[bool, Optional[str]]:
        """
        Check if any GUI is open.
        """
        if self._ms.screen_name() is not None:
            return False, "GUI opened unexpectedly. Possible macro check."
        return True, None

    def _check_movement_distance(self) -> Tuple[bool, Optional[str]]:
        """
        Check if player moved too far from logged position.
        """
        dx = self._player.x - self._state.logged_position[0]
        dy = self._player.y - self._state.logged_position[1]
        dz = self._player.z - self._state.logged_position[2]
        distance = math.sqrt(dx*dx + dy*dy + dz*dz)

        if distance > self._config.max_distance:
            return False, f"Moved too far from previous position ({distance:.1f} blocks)."
        return True, None
