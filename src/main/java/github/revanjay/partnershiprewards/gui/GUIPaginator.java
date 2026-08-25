package github.revanjay.partnershiprewards.gui;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public class GUIPaginator<T> {

    private final List<T> items;
    private final int pageSize;
    private int currentPage;

    public GUIPaginator(List<T> items, int pageSize) {
        this.items = items != null ? items : Collections.emptyList();
        this.pageSize = Math.max(1, pageSize);
        this.currentPage = 0;
    }

    public int getTotalPages() {
        if (items.isEmpty()) return 1;
        return (int) Math.ceil((double) items.size() / pageSize);
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public boolean hasNextPage() {
        return currentPage < getTotalPages() - 1;
    }

    public boolean hasPreviousPage() {
        return currentPage > 0;
    }

    public void nextPage() {
        if (hasNextPage()) {
            currentPage++;
        }
    }

    public void previousPage() {
        if (hasPreviousPage()) {
            currentPage--;
        }
    }

    public void setPage(int page) {
        if (page >= 0 && page < getTotalPages()) {
            this.currentPage = page;
        }
    }

    public List<T> getCurrentPageItems() {
        if (items.isEmpty()) return Collections.emptyList();
        int fromIndex = currentPage * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, items.size());
        if (fromIndex >= items.size()) return Collections.emptyList();
        return items.subList(fromIndex, toIndex);
    }

    public void applyNavigationButtons(Inventory inv, int prevSlot, int nextSlot, int infoSlot) {
        if (hasPreviousPage()) {
            inv.setItem(prevSlot, EnhancedGUI.createGradientItem(Material.ARROW, "&e« Previous Page (" + currentPage + ")", List.of("&7Click to view previous page")));
        } else {
            inv.setItem(prevSlot, EnhancedGUI.createGlassPane(Material.GRAY_STAINED_GLASS_PANE, "&7No Previous Page"));
        }

        if (infoSlot >= 0 && infoSlot < inv.getSize()) {
            inv.setItem(infoSlot, EnhancedGUI.createGradientItem(Material.PAPER, "&6Page " + (currentPage + 1) + " / " + getTotalPages(), List.of("&7Total items: " + items.size())));
        }

        if (hasNextPage()) {
            inv.setItem(nextSlot, EnhancedGUI.createGradientItem(Material.ARROW, "&eNext Page (" + (currentPage + 2) + ") »", List.of("&7Click to view next page")));
        } else {
            inv.setItem(nextSlot, EnhancedGUI.createGlassPane(Material.GRAY_STAINED_GLASS_PANE, "&7No Next Page"));
        }
    }
}
