package us.talabrek.ultimateskyblock.compat.miniaturepets;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import us.talabrek.ultimateskyblock.compat.Compat;

public abstract class MiniaturePetsCompat extends Compat {

    public abstract EntityType getPetEntityType(String type);

    public abstract boolean isPetSpawningFor(Location location, EntityType type);

}
