package de.tobi1craft.repairswapper;

import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RepairSwapperClient implements ClientModInitializer {

    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("repair-swapper");

    private static KeyBinding keyBinding;

    private static boolean enabled;
    private static int swappedSlot = -1; // -1 for nothing swapped currently
    private static int swappedSlotTo;
    private static int tickCounter = 0;

    public static void doSwapping() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        int slot = getLeastDurabilitySlot(player);

        if (!needsSwap(player, slot)) return;

        if (swappedSlot != -1) swapBack(client, player);

        swappedSlot = slot;
        swappedSlotTo = 45;

        if (swappedSlot < 9) {
            player.getInventory().selectedSlot = swappedSlot;
            swappedSlot = -1;
            return;
        }
        if (RepairSwapperConfig.hand == RepairSwapperConfig.Hand.MAINHAND)
            swappedSlotTo = player.getInventory().selectedSlot + 36;

        if (!player.getInventory().getStack(swappedSlotTo).isEmpty())
            Objects.requireNonNull(client.interactionManager).clickSlot(0, swappedSlotTo, 0, SlotActionType.PICKUP, player);
        Objects.requireNonNull(client.interactionManager).clickSlot(0, swappedSlot, 0, SlotActionType.PICKUP, player);
        Objects.requireNonNull(client.interactionManager).clickSlot(0, swappedSlotTo, 0, SlotActionType.PICKUP, player);
    }

    private static void tick(MinecraftClient client) {
        while (keyBinding.wasPressed()) {
            if (enabled) disable(client);
            else enable(client, false);
        }
        if (enabled) {
            if (RepairSwapperConfig.delayToReset != 0) {
                if (tickCounter >= RepairSwapperConfig.delayToReset) {
                    disable(client);
                    tickCounter = 0;
                    return;
                } else tickCounter++;
            }
            doSwapping();
        }
    }

    public static void enable(MinecraftClient client, boolean autoTrigger) {
        tickCounter = 0;
        if (enabled || (autoTrigger && !RepairSwapperConfig.auto)) return;
        if (client.player != null && getRepairableSlots(client.player).isEmpty()) {
            if (!autoTrigger)
                client.inGameHud.setOverlayMessage(Text.translatable("hud.repair-swapper.noRepairable"), false);
            return;
        }
        swappedSlot = -1;
        enabled = true;
        client.inGameHud.setOverlayMessage(Text.translatable("hud.repair-swapper.enabled"), false);
    }

    public static void disable(MinecraftClient client) {
        client.inGameHud.setOverlayMessage(Text.translatable("hud.repair-swapper.disabled"), false);
        enabled = false;
        if (swappedSlot == -1) return;
        assert client.player != null;
        swapBack(client, client.player);
    }

    @Unique
    private static int getLeastDurabilitySlot(ClientPlayerEntity player) {
        List<Integer> repairable = getRepairableSlots(player);
        if (repairable.isEmpty()) return -1;
        int leastDurability = Integer.MAX_VALUE;
        int leastDurabilitySlot = -1;
        for (int slot : repairable) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.getMaxDamage() - stack.getDamage() < leastDurability) {
                leastDurability = stack.getMaxDamage() - stack.getDamage();
                leastDurabilitySlot = slot;
            }
        }
        return leastDurabilitySlot;
    }

    private static void swapBack(MinecraftClient client, ClientPlayerEntity player) {
        if (!player.getInventory().getStack(swappedSlot).isEmpty())
            Objects.requireNonNull(client.interactionManager).clickSlot(0, swappedSlot, 0, SlotActionType.PICKUP, player);
        Objects.requireNonNull(client.interactionManager).clickSlot(0, swappedSlotTo, 0, SlotActionType.PICKUP, player);
        Objects.requireNonNull(client.interactionManager).clickSlot(0, swappedSlot, 0, SlotActionType.PICKUP, player);
    }

    @Unique
    private static boolean needsSwap(ClientPlayerEntity player, int slot) {
        return slot != -1 && slot <= 35 && player.getInventory().selectedSlot != slot;
    }

    @Unique
    private static List<Integer> getRepairableSlots(ClientPlayerEntity player) {
        Inventory inventory = player.getInventory();
        int size = inventory.size();
        List<Integer> repairable = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            ItemStack stack = inventory.getStack(i);
            RegistryEntry<Enchantment> enchantment = player.getWorld().getRegistryManager().getWrapperOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.MENDING);
            if (stack.isDamaged() && EnchantmentHelper.getLevel(enchantment, stack) > 0) repairable.add(i);
        }
        return repairable;
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("Repair Swapper initializing");
        MidnightConfig.init("repair-swapper", RepairSwapperConfig.class);
        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.repair-swapper.toggle", GLFW.GLFW_KEY_R, "key.categories.repair-swapper"));
        ClientTickEvents.END_CLIENT_TICK.register(RepairSwapperClient::tick);
    }
}