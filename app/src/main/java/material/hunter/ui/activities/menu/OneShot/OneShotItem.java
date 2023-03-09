package material.hunter.ui.activities.menu.OneShot;

public class OneShotItem {

    private final String ESSID;
    private final String BSSID;
    private final String security;
    private final float power;
    private final boolean isWpsLocked;
    private final String wsc_device_name;
    private final String wsc_model;
    private final String wsc_modelNumber;

    public OneShotItem(String ESSID, String BSSID, String security, float power, boolean isWpsLocked, String wsc_device_name, String wsc_model, String wsc_modelNumber) {
        this.ESSID = ESSID;
        this.BSSID = BSSID;
        this.security = security;
        this.power = power;
        this.isWpsLocked = isWpsLocked;
        this.wsc_device_name = wsc_device_name;
        this.wsc_model = wsc_model;
        this.wsc_modelNumber = wsc_modelNumber;
    }

    public String getESSID() {
        return ESSID;
    }

    public String getBSSID() {
        return BSSID;
    }

    public String getSecurity() {
        return security;
    }

    public float getSignal() {
        return power;
    }

    public boolean isWpsLocked() {
        return isWpsLocked;
    }

    public String getWsc_device_name() {
        return wsc_device_name;
    }

    public String getWsc_model() {
        return wsc_model;
    }

    public String getWsc_modelNumber() {
        return wsc_modelNumber;
    }
}
