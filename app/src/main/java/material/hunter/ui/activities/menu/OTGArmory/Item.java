package material.hunter.ui.activities.menu.OTGArmory;

public class Item {

    private final String deviceName;
    private final int vendorId;
    private final String vendorName;
    private final int productId;
    private final String productName;

    public Item(String deviceName, int vendorId, String vendorName, int productId, String productName) {
        this.deviceName = deviceName;
        this.vendorId = vendorId;
        this.vendorName = vendorName;
        this.productId = productId;
        this.productName = productName;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public int getVendorId() {
        return vendorId;
    }

    public String getVendorName() {
        return vendorName;
    }

    public int getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }
}
