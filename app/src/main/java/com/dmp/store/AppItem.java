package com.dmp.store;

final class AppItem {
    final String id;
    final String title;
    final String packageName;
    final String version;
    final String filename;
    final String category;
    final String description;
    final int minSdk;

    AppItem(String id, String title, String packageName, String version, int minSdk,
            String filename, String category, String description) {
        this.id = id;
        this.title = title;
        this.packageName = packageName;
        this.version = version;
        this.minSdk = minSdk;
        this.filename = filename;
        this.category = category;
        this.description = description;
    }

    String androidRequirement() {
        if (minSdk <= 19) return "Android 4.4+";
        if (minSdk == 21) return "Android 5.0+";
        if (minSdk == 23) return "Android 6.0+";
        if (minSdk == 24) return "Android 7.0+";
        if (minSdk == 26) return "Android 8.0+";
        return "Android API " + minSdk + "+";
    }
}
