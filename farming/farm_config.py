"""Farm configuration and crop definitions."""

from dataclasses import dataclass
from typing import Set

@dataclass
class CropConfig:
    minecraft_id: str
    tool_custom_name: str

@dataclass
class FarmSettings:
    scan_range: int = 255
    max_distance: float = 5.0
    tick_time: float = 0.05
    max_idle_time: float = 1.0
    landing_cooldown: float = 1.0
    log_interval: float = 1.0
    orientation_tolerance: float = 3.0


# Crop definitions
CROPS = {
    "wheat": CropConfig("minecraft:wheat", "Wheat"),
    "carrots": CropConfig("minecraft:carrots", "Carrot"),
    "potatoes": CropConfig("minecraft:potatoes", "Potato"),
    "nether_wart": CropConfig("minecraft:nether_wart", "Wart")
}

# Non-interrupting keys
ALLOWED_KEYS: Set[int] = {
    342,  # LEFT ALT
    258,  # TAB
    300,  # F11
    294,  # F5
    292  # F3
}


# Farm types for future expansion
class FarmType:
    CIRCULAR_CROP = "circular_crop"
