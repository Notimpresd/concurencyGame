package org.example;

public class Room {
    private String name;
    private String ip;
    private boolean online;

    public Room(String name, String ip) {
        this.name = name;
        this.ip = ip;
        this.online = false;
    }

    public String getName() {
        return name;
    }

    public String getIp() {
        return ip;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    @Override
    public String toString() {
        return name + " (" + (online ? "Online" : "Offline") + ")";
    }
}
