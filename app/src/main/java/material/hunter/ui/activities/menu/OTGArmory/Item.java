package material.hunter.ui.activities.menu.OTGArmory;

public class Item {

    private final String deviceName;
    private final String vendorId;
    private final String vendorName;
    private final String productId;
    private final String productName;

    public Item(String deviceName, String vendorId, String vendorName, String productId, String productName) {
        this.deviceName = deviceName;
        this.vendorId = vendorId;
        this.vendorName = vendorName;
        this.productId = productId;
        this.productName = productName;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getVendorId() {
        return vendorId;
    }

    public String getVendorName() {
        return vendorName;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }
}
