package material.hunter.models;

public class LicenseModel {

    private final String title;
    private final String license;

    public LicenseModel(String title, String license) {
        this.title = title;
        this.license = license;
    }

    public String getTitle() {
        return title;
    }

    public String getLicense() {
        return license;
    }
}