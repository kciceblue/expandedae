package lu.kolja.expandedae.terminal.wtlib;

import appeng.api.networking.IGridNode;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.slot.RestrictedInputSlot;
import de.mari_023.ae2wtlib.api.gui.AE2wtlibSlotSemantics;
import de.mari_023.ae2wtlib.api.terminal.ItemWUT;
import de.mari_023.ae2wtlib.api.terminal.WTMenuHost;
import lu.kolja.expandedae.Expandedae;
import lu.kolja.expandedae.terminal.ExpEncodingTerminalMenu;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class ExpWETMenu extends ExpEncodingTerminalMenu {
    public static final ResourceLocation ID = Expandedae.makeId("wireless_exp_encoding_terminal");
    public static final MenuType<ExpWETMenu> TYPE = MenuTypeBuilder
            .create(ExpWETMenu::new, ExpWETMenuHost.class)
            .buildUnregistered(ID);

    private final ExpWETMenuHost wetMenuHost;

    public ExpWETMenu(int id, final Inventory ip, final ExpWETMenuHost gui) {
        super(TYPE, id, ip, gui);
        wetMenuHost = gui;
        addSlot(new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.QE_SINGULARITY,
                wetMenuHost.getSubInventory(WTMenuHost.INV_SINGULARITY), 0), AE2wtlibSlotSemantics.SINGULARITY);
    }

    @Override
    public IGridNode getGridNode() {
        return wetMenuHost.getActionableNode();
    }

    public boolean isWUT() {
        return wetMenuHost.getItemStack().getItem() instanceof ItemWUT;
    }

    public ItemStack getTerminalItem() {
        return wetMenuHost.getItemStack();
    }
}