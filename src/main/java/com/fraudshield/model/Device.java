package com.fraudshield.model;

import java.time.LocalDateTime;

/** A device (phone/browser) that a customer has previously used and is therefore "known". */
public class Device {
    private int id;
    private int userId;
    private String deviceId;
    private String deviceType = "MOBILE";
    private LocalDateTime firstSeen;

    public Device() {
    }

    public Device(int userId, String deviceId, String deviceType) {
        this.userId = userId;
        this.deviceId = deviceId;
        if (deviceType != null && !deviceType.isBlank()) this.deviceType = deviceType;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    public LocalDateTime getFirstSeen() { return firstSeen; }
    public void setFirstSeen(LocalDateTime firstSeen) { this.firstSeen = firstSeen; }

    public String getFirstSeenFormatted() {
        return com.fraudshield.util.DateUtil.format(firstSeen);
    }
}
