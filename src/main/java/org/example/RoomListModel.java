package org.example;

import javax.swing.*;

public class RoomListModel extends DefaultListModel<Room> {
    public void refresh() {
        // Notify listeners that the contents have changed
        fireContentsChanged(this, 0, getSize() - 1);
    }
}
