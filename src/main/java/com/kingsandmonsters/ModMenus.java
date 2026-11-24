package com.kingsandmonsters;

import com.kingsandmonsters.menu.OgreMerchantBackpackMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, KingsAndMonsters.MODID);

    public static final RegistryObject<MenuType<OgreMerchantBackpackMenu>> OGRE_MERCHANT_BACKPACK =
            MENUS.register("ogre_merchant_backpack", () -> IForgeMenuType.create(OgreMerchantBackpackMenu::new));

    public static final RegistryObject<MenuType<com.kingsandmonsters.menu.MudsnoutBoarMenu>> MUDSNOUT_BOAR =
            MENUS.register("mudsnout_boar",
                    () -> IForgeMenuType.create(com.kingsandmonsters.menu.MudsnoutBoarMenu::new));

    private ModMenus() {}
}
