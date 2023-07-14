package material.hunter.models;

public class USBArmorySwitchModel {

    private String idVendor;
    private String idProduct;
    private String manufacturer;
    private String product;
    private String serialnumber;

    public USBArmorySwitchModel() {
    }

    public String getIdVendor() {
        return idVendor;
    }

    public void setIdVendor(String idVendor) {
        this.idVendor = idVendor;
    }

    public String getIdProduct() {
        return idProduct;
    }

    public void setIdProduct(String idProduct) {
        this.idProduct = idProduct;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getSerialnumber() {
        return serialnumber;
    }

    public void setSerialNumber(String serialnumber) {
        this.serialnumber = serialnumber;
    }

    @Override
    public String toString() {
        return "USBArmoryModel{" +
                "idVendor='" + idVendor + '\'' +
                ", idProduct='" + idProduct + '\'' +
                ", manufacturer='" + manufacturer + '\'' +
                ", product='" + product + '\'' +
                ", serialnumber='" + serialnumber + '\'' +
                '}';
    }
}