package lu.kolja.expandedae.terminal;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableItem;
import appeng.core.definitions.AEItems;
import appeng.helpers.IPatternTerminalMenuHost;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.util.ConfigInventory;
import lu.kolja.expandedae.definition.ExpItems;
import lu.kolja.expandedae.definition.ExpMenus;
import lu.kolja.expandedae.mixin.accessor.AccessorPatternEncodingTermMenu;
import lu.kolja.expandedae.terminal.wtlib.ExpWETMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class ExpEncodingTerminalMenu extends PatternEncodingTermMenu {
    private static final String ACTION_MODIFY_PATTERN = "modifyPattern";
    private static final String ACTION_MOVE_PATTERN = "movePattern";

    public ExpEncodingTerminalMenu(int id, Inventory ip, IPatternTerminalMenuHost host) {
        this(ExpMenus.EXP_ENCODING_TERMINAL.get(), id, ip, host);
    }
    public ExpEncodingTerminalMenu(MenuType<?> menuType, int id, Inventory ip, IPatternTerminalMenuHost host) {
        super(menuType, id, ip, host, true);
        registerClientAction(ACTION_MODIFY_PATTERN, Integer.class, this::modifyPattern);
        registerClientAction(ACTION_MOVE_PATTERN, Boolean.class, this::movePattern);
    }

    @Override
    public void encode() {
        super.encode();
        refillBlankPatterns();
    }

    /**
     * Pattern Refiller Card used to read Shift via client-only {@code Minecraft}
     * during {@code encode()}, which crashes dedicated servers and aborts the refill.
     * Shift handling now lives in {@code MixinPatternEncodingTermScreen} + {@link #movePattern}.
     * This only restocks blank patterns from the ME network.
     */
    private void refillBlankPatterns() {
        if (!(this instanceof ExpWETMenu wetMenu)) {
            return;
        }

        var terminalItem = wetMenu.getTerminalItem();
        if (terminalItem.isEmpty() || !(terminalItem.getItem() instanceof IUpgradeableItem upgradeableItem)) {
            return;
        }

        IUpgradeInventory inventory = upgradeableItem.getUpgrades(terminalItem);
        if (!inventory.isInstalled(ExpItems.PATTERN_REFILLER_CARD)) {
            return;
        }

        var encodedPatternSlot = ((AccessorPatternEncodingTermMenu) this).getEncodedPatternSlot();
        if (encodedPatternSlot.getItem().isEmpty()) {
            return;
        }

        var node = this.getGridNode();
        if (node == null || node.getGrid() == null) {
            return;
        }

        var blankPatternSlot = ((AccessorPatternEncodingTermMenu) this).getBlankPatternSlot();
        var blankPatternSlotCount = blankPatternSlot.getItem().getCount();
        int needed = 64 - blankPatternSlotCount;
        if (needed <= 0) {
            return;
        }

        int changed = (int) node.getGrid().getStorageService().getInventory().extract(
                AEItemKey.of(AEItems.BLANK_PATTERN),
                needed,
                Actionable.MODULATE,
                this.getActionSource()
        );
        if (changed <= 0) {
            return;
        }

        blankPatternSlot.set(new ItemStack(AEItems.BLANK_PATTERN, blankPatternSlotCount + changed));
        blankPatternSlot.setChanged();
    }

    public void modifyPattern(Integer data) {
        if (isClientSide()) {
            sendClientAction(ACTION_MODIFY_PATTERN, data);
        } else {
            var encodedInputsInv = ((AccessorPatternEncodingTermMenu) this).getEncodedInputsInv();
            var encodedOutputsInv = ((AccessorPatternEncodingTermMenu) this).getEncodedOutputsInv();
            var output = isValid(encodedOutputsInv, data);
            if (output == null) {
                return;
            }
            var input = isValid(encodedInputsInv, data);
            if (input == null) {
                return;
            }
            for (int slot = 0; slot < output.length; ++slot) {
                if (output[slot] != null) {
                    encodedOutputsInv.setStack(slot, output[slot]);
                }
            }
            for (int slot = 0; slot < input.length; ++slot) {
                if (input[slot] != null) {
                    encodedInputsInv.setStack(slot, input[slot]);
                }
            }
        }
    }

    private static GenericStack[] isValid(ConfigInventory inv, int data) {
        boolean flag = data > 0;
        if (!flag) {
            data = -data;
        }
        GenericStack[] result = new GenericStack[inv.size()];
        for (int slot = 0; slot < inv.size(); ++slot) {
            GenericStack stack = inv.getStack(slot);
            if (stack != null) {
                if (flag) {
                    if (data * stack.amount() > Integer.MAX_VALUE) {
                        return null;
                    } else {
                        result[slot] = new GenericStack(stack.what(), data * stack.amount());
                    }
                } else {
                    if (stack.amount() % data != 0) {
                        return null;
                    } else {
                        result[slot] = new GenericStack(stack.what(), stack.amount() / data);
                    }
                }
            }
        }
        return result;
    }

    public void movePattern(Boolean data) {
        if (isClientSide()) {
            sendClientAction(ACTION_MOVE_PATTERN, data);
        } else {
            if (!data) return;
            var player = this.getPlayer();
            // Need to do this check first because #addItem ignores that there are no free slots if the player is in creative mode
            if (player.getInventory().getFreeSlot() > 0) {
                player.addItem(encodedPatternSlot.getItem());
                encodedPatternSlot.setChanged();
            }
        }
    }
}
