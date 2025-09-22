import system.lib.minescript as ms
from system.lib.minescript import ItemStack
from general.lib_nbt import parse_snbt


def get_nbt(item: ItemStack) -> str:
    if isinstance(item, dict):
        return item.get("nbt")
    return getattr(item, "nbt", None)


def get_item(name: str) -> ItemStack | None:
    """
    Finds the first occurrence of an item with a specific custom name.
    Returns None if no item is found, otherwise an ItemStack object.
    """
    inventory = ms.player_inventory()

    for item in inventory:
        nbt_str = item.nbt

        if nbt_str is not None:
            nbt_data = parse_snbt(nbt_str)
            if "minecraft:custom_name" in nbt_data.get("components", {}):
                custom_name = nbt_data["components"]["minecraft:custom_name"]
                if name.lower().strip() in custom_name.lower().strip():
                    return item

    return None


def compare_items(item1: ItemStack, item2: ItemStack) -> bool:
    ms.echo(item1)
    ms.echo(item2)

    item1_nbt = parse_snbt(get_nbt(item1))
    item2_nbt = parse_snbt(get_nbt(item2))

    item1_custom_name = item1_nbt["components"]["minecraft:custom_name"]
    item2_custom_name = item2_nbt["components"]["minecraft:custom_name"]

    if item1_custom_name == item2_custom_name:
        return True
    return False