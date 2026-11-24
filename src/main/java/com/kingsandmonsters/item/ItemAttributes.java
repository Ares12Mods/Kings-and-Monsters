package com.kingsandmonsters.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.RegistryObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Exact attribute values for the held weapons and tools of the mod.
 *
 * <p>Vanilla 1.20.1 only takes whole-number damage on swords, so the held stats are declared here and
 * swapped in for whatever the item class would have given it.</p>
 */
public final class ItemAttributes {
    public static final UUID BASE_ATTACK_DAMAGE = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    public static final UUID BASE_ATTACK_SPEED = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");

    private record Entry(Supplier<Attribute> attribute, AttributeModifier modifier) {
    }

    private static final List<Map.Entry<Supplier<? extends Item>, List<Entry>>> PENDING = new ArrayList<>();
    private static Map<Item, List<Entry>> resolved;

    private ItemAttributes() {
    }

    /** A held weapon: attack damage and attack speed modifiers, not counting the player's base 1 and 4. */
    public static Builder mainHand(RegistryObject<? extends Item> item, double damage, double speed) {
        Builder builder = new Builder(item);
        builder.add(() -> Attributes.ATTACK_DAMAGE,
                new AttributeModifier(BASE_ATTACK_DAMAGE, "Weapon modifier", damage, AttributeModifier.Operation.ADDITION));
        builder.add(() -> Attributes.ATTACK_SPEED,
                new AttributeModifier(BASE_ATTACK_SPEED, "Weapon modifier", speed, AttributeModifier.Operation.ADDITION));
        return builder;
    }

    /** Adds the modifier, replacing any earlier one with the same id. */
    public static void addOrUpdate(net.minecraft.world.entity.ai.attributes.AttributeInstance instance, AttributeModifier modifier) {
        instance.removeModifier(modifier.getId());
        instance.addTransientModifier(modifier);
    }

    public static UUID uuid(String name) {
        return UUID.nameUUIDFromBytes(("kingsandmonsters:" + name).getBytes(StandardCharsets.UTF_8));
    }

    public static AttributeModifier modifier(String name, double amount, AttributeModifier.Operation operation) {
        return new AttributeModifier(uuid(name), name, amount, operation);
    }

    public static final class Builder {
        private final List<Entry> entries = new ArrayList<>();

        private Builder(RegistryObject<? extends Item> item) {
            PENDING.add(Map.entry(item, entries));
        }

        public Builder add(Supplier<Attribute> attribute, AttributeModifier modifier) {
            entries.add(new Entry(attribute, modifier));
            return this;
        }
    }

    /** The declared held-item attributes as an immutable multimap. */
    public static Multimap<Attribute, AttributeModifier> asMultimap(Item item) {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        List<Entry> entries = table().get(item);
        if (entries != null) {
            entries.forEach(e -> builder.put(e.attribute().get(), e.modifier()));
        }
        return builder.build();
    }

    private static synchronized Map<Item, List<Entry>> table() {
        if (resolved == null) {
            Map<Item, List<Entry>> map = new IdentityHashMap<>();
            for (Map.Entry<Supplier<? extends Item>, List<Entry>> pending : PENDING) {
                map.put(pending.getKey().get(), pending.getValue());
            }
            resolved = map;
        }
        return resolved;
    }

    @SubscribeEvent
    public static void onItemAttributes(ItemAttributeModifierEvent event) {
        if (event.getSlotType() != EquipmentSlot.MAINHAND) {
            return;
        }
        List<Entry> entries = table().get(event.getItemStack().getItem());
        if (entries == null) {
            return;
        }
        event.clearModifiers();
        for (Entry entry : entries) {
            event.addModifier(entry.attribute().get(), entry.modifier());
        }
    }
}
