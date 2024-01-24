package net.fireturtle.dragonmaiden;

    
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;

public class DragonmaidenScreenHandler extends ScreenHandler {
   private final Inventory inventory;
   private final AbstractDragonmaidenEntity entity;

   public DragonmaidenScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, AbstractDragonmaidenEntity entity) {
      super((ScreenHandlerType)null, syncId);
      this.inventory = inventory;
      this.entity = entity;
      int i = (int)true;
      inventory.onOpen(playerInventory.player);
      int j = true;
      this.addSlot(new 1(this, inventory, 0, 8, 18, entity));
      this.addSlot(new 2(this, inventory, 1, 8, 36, entity));
      int k;
      int l;
      if (this.hasChest(entity)) {
         for(k = 0; k < 3; ++k) {
            for(l = 0; l < ((AbstractDonkeyEntity)entity).getInventoryColumns(); ++l) {
               this.addSlot(new Slot(inventory, 2 + l + k * ((AbstractDonkeyEntity)entity).getInventoryColumns(), 80 + l * 18, 18 + k * 18));
            }
         }
      }

      for(k = 0; k < 3; ++k) {
         for(l = 0; l < 9; ++l) {
            this.addSlot(new Slot(playerInventory, l + k * 9 + 9, 8 + l * 18, 102 + k * 18 + -18));
         }
      }

      for(k = 0; k < 9; ++k) {
         this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
      }

   }

   public boolean canUse(PlayerEntity player) {
      return !this.entity.areInventoriesDifferent(this.inventory) && this.inventory.canPlayerUse(player) && this.entity.isAlive() && this.entity.distanceTo(player) < 8.0F;
   }

   private boolean hasChest(AbstractDragonmaidenEntity dragonmaiden) {
      return false;
   //   return dragonmaiden.hasChest();
   }

   public ItemStack quickMove(PlayerEntity player, int slot) {
      ItemStack itemStack = ItemStack.EMPTY;
      Slot slot2 = (Slot)this.slots.get(slot);
      if (slot2 != null && slot2.hasStack()) {
         ItemStack itemStack2 = slot2.getStack();
         itemStack = itemStack2.copy();
         int i = this.inventory.size();
         if (slot < i) {
            if (!this.insertItem(itemStack2, i, this.slots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (this.getSlot(1).canInsert(itemStack2) && !this.getSlot(1).hasStack()) {
            if (!this.insertItem(itemStack2, 1, 2, false)) {
               return ItemStack.EMPTY;
            }
         } else if (this.getSlot(0).canInsert(itemStack2)) {
            if (!this.insertItem(itemStack2, 0, 1, false)) {
               return ItemStack.EMPTY;
            }
         } else if (i <= 2 || !this.insertItem(itemStack2, 2, i, false)) {
            int k = i + 27;
            int m = k + 9;
            if (slot >= k && slot < m) {
               if (!this.insertItem(itemStack2, i, k, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (slot >= i && slot < k) {
               if (!this.insertItem(itemStack2, k, m, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (!this.insertItem(itemStack2, k, k, false)) {
               return ItemStack.EMPTY;
            }

            return ItemStack.EMPTY;
         }

         if (itemStack2.isEmpty()) {
            slot2.setStack(ItemStack.EMPTY);
         } else {
            slot2.markDirty();
         }
      }

      return itemStack;
   }

   public void onClosed(PlayerEntity player) {
      super.onClosed(player);
      this.inventory.onClose(player);
   }
}

