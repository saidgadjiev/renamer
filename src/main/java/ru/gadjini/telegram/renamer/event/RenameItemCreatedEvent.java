package ru.gadjini.telegram.renamer.event;

public class RenameItemCreatedEvent {

    private int itemId;

    public RenameItemCreatedEvent(int itemId) {
        this.itemId = itemId;
    }

    public int getItemId() {
        return itemId;
    }
}
