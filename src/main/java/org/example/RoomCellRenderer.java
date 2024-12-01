package org.example;

import javax.swing.*;
import java.awt.*;

public class RoomCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof Room) {
            Room room = (Room) value;
            setText(room.toString());
            setForeground(room.isOnline() ? Color.GREEN.darker() : Color.RED);
        }
        return component;
    }
}
