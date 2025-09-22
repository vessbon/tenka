import system.lib.java as java

Minecraft = java.JavaClass("net.minecraft.client.Minecraft")
mc = Minecraft.getInstance()


class Client:
    PACKET_CATEGORIES = {
        "mojang": "game",
        "yarn": "play"
    }

    @classmethod
    def resolve_packet_class(cls, name: str, category: str = ""):
        """
        Resolve a packet class name into its fully qualified path.
        Supports Mojang, Intermediary, and Yarn mappings.
        """
        # Intermediary: raw 'class_...' names
        if name.startswith("class"):
            return f"net.minecraft.{name}"

        # Yarn: contains 'c2s'
        if "c2s" in name:
            if not category:
                category = cls.PACKET_CATEGORIES["yarn"]
            return f"net.minecraft.network.packet.c2s.{category}.{name}"

        # Mojang mapping
        if not category:
            category = cls.PACKET_CATEGORIES["mojang"]
        return f"net.minecraft.network.protocol.{category}.{name}"

    @classmethod
    def send_packet(cls, packet: str, *args, category: str = ""):
        """
        Create and send a packet instance to the server.
        """
        full_class = cls.resolve_packet_class(packet, category)
        packet_class = java.JavaClass(full_class)
        mc.player.connection.getConnection().send(packet_class(*args))


class Screen:
    @staticmethod
    def close_screen():
        screen = mc.screen
        if screen is None:
            return

        menu = screen.getMenu()
        if menu is not None:
            container_id = menu.containerId
            mc.setScreen(None)
            Client.send_packet("ServerboundContainerClosePacket", container_id)
