package fi.dy.masa.simplematica.mixin.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import fi.dy.masa.simplematica.util.invoker.IEntityInvoker;
import net.minecraft.world.entity.Entity;

@Mixin(Entity.class)
public abstract class MixinEntity implements IEntityInvoker
{
    @Shadow protected boolean wasTouchingWater;

    public MixinEntity()
    {
        super();
    }

    @Override
    public void litematica$toggleTouchingWater(boolean toggle)
    {
        if (toggle != this.wasTouchingWater)
        {
            this.wasTouchingWater = toggle;
        }
    }
}
