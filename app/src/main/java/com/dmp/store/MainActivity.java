package com.dmp.store;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MainActivity extends Activity {
    private static final String BRAND_NAME = "DMP APK STORE HUB";
    private static final int SETTINGS_INSTALL_SOURCE = 407;
    private static final int INSTALL_APK_REQUEST = 408;
    private static final int BG = Color.rgb(9, 12, 19);
    private static final int PANEL = Color.rgb(20, 25, 37);
    private static final int PANEL_FOCUS = Color.rgb(37, 47, 68);
    private static final int MUTED = Color.rgb(157, 169, 190);
    private static final int WHITE = Color.rgb(245, 247, 252);
    private static final int BLUE = Color.rgb(86, 125, 255);

    private final List<AppItem> apps = new ArrayList<>();
    private final Map<String, Bitmap> appIcons = new HashMap<>();
    private LinearLayout root;
    private LinearLayout pageContent;
    private View firstAppCard;
    private TextView appCount;
    private boolean showingDetails;
    private boolean showingInstallSuccess;
    private View successOpenButton;
    private View successReturnButton;
    private AppItem currentDetailApp;
    private String selectedCategory = "Tất cả";
    private String query = "";
    private AppItem pendingApp;
    private AppItem installingApp;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(BG);
            getWindow().setNavigationBarColor(BG);
        }
        loadCatalog();
        showHome();
    }

    private void loadCatalog() {
        try {
            InputStream in = getAssets().open("catalog.json");
            byte[] bytes = new byte[in.available()];
            int count = in.read(bytes);
            in.close();
            JSONArray list = new JSONArray(new String(bytes, 0, count, "UTF-8"));
            for (int i = 0; i < list.length(); i++) {
                JSONObject item = list.getJSONObject(i);
                apps.add(new AppItem(item.getString("id"), item.getString("title"),
                        item.getString("package"), item.getString("version"), item.getInt("minSdk"),
                        item.getString("file"), item.getString("category"), item.getString("description")));
            }
        } catch (Exception e) {
            Toast.makeText(this, "Không đọc được danh mục ứng dụng", Toast.LENGTH_LONG).show();
        }
    }

    private void showHome() {
        showingDetails = false;
        showingInstallSuccess = false;
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setFocusableInTouchMode(true);
        setContentView(root);
        root.addView(buildHeader(), new LinearLayout.LayoutParams(-1, dp(76)));

        ScrollView page = new ScrollView(this);
        page.setFillViewport(false);
        pageContent = new LinearLayout(this);
        pageContent.setOrientation(LinearLayout.VERTICAL);
        pageContent.setPadding(dp(48), dp(16), dp(48), dp(42));
        page.addView(pageContent);
        root.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
        firstAppCard = null;
        populateHome();
        root.post(new Runnable() {
            @Override public void run() {
                root.requestFocus();
                if (firstAppCard != null) {
                    firstAppCard.setFocusableInTouchMode(true);
                    if (!firstAppCard.requestFocus()) firstAppCard.requestFocusFromTouch();
                }
            }
        });
    }

    @Override protected void onResume() {
        super.onResume();
        if (showingDetails && currentDetailApp != null && root != null) {
            root.post(new Runnable() {
                @Override public void run() {
                    if (showingDetails && currentDetailApp != null) showDetails(currentDetailApp);
                }
            });
        }
    }

    private void populateHome() {
        if (pageContent == null) return;
        pageContent.removeAllViews();
        LinearLayout content = pageContent;
        content.addView(buildHero(), new LinearLayout.LayoutParams(-1, dp(205)));
        addCategoryBar(content);

        List<AppItem> filtered = filteredApps();
        if (appCount != null) {
            boolean narrowed = !query.trim().isEmpty() || !selectedCategory.equals("Tất cả");
            appCount.setText(narrowed ? filtered.size() + " / " + apps.size() + " ứng dụng" : apps.size() + " ứng dụng");
        }
        if (!query.trim().isEmpty() || !selectedCategory.equals("Tất cả")) {
            addSection(content, query.trim().isEmpty() ? selectedCategory : "Kết quả tìm kiếm", filtered);
        } else {
            addSection(content, "Nổi bật cho TV", filtered.subList(0, Math.min(5, filtered.size())));
            addSection(content, "Giải trí", filterCategory(filtered, "Giải trí"));
            addSection(content, "Video", filterCategory(filtered, "Video"));
            addSection(content, "Giao diện TV & tiện ích", merge(filterCategory(filtered, "Giao diện TV"), filterCategory(filtered, "Tiện ích")));
        }
        if (filtered.isEmpty()) {
            TextView empty = label("Không tìm thấy ứng dụng phù hợp.", 22, MUTED, false);
            empty.setPadding(0, dp(44), 0, dp(44));
            content.addView(empty);
        }
        TextView foot = label("APK được lưu sẵn trong thư viện · Người dùng xác nhận cài đặt qua Android", 13, MUTED, false);
        foot.setPadding(0, dp(24), 0, 0);
        content.addView(foot);
    }

    private View buildHeader() {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(48), 0, dp(48), 0);
        bar.setBackgroundColor(Color.rgb(12, 15, 23));

        LinearLayout logo = new LinearLayout(this);
        logo.setGravity(Gravity.CENTER_VERTICAL);
        logo.addView(brandMark(), new LinearLayout.LayoutParams(dp(42), dp(42)));
        TextView brand = label(BRAND_NAME, 22, WHITE, true);
        LinearLayout.LayoutParams brandParams = new LinearLayout.LayoutParams(-2, -1);
        brandParams.leftMargin = dp(10);
        logo.addView(brand, brandParams);
        bar.addView(logo, new LinearLayout.LayoutParams(dp(360), -2));

        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(16);
        search.setTextColor(WHITE);
        search.setHintTextColor(MUTED);
        search.setHint("Tìm ứng dụng");
        search.setPadding(dp(16), 0, dp(16), 0);
        search.setBackground(round(PANEL, dp(18), Color.TRANSPARENT, 0));
        search.setFocusable(true);
        search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        search.setText(query);
        search.clearFocus();
        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                query = s.toString();
                populateHome();
            }
            @Override public void afterTextChanged(android.text.Editable s) { }
        });
        search.setTag("search-field");
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(dp(360), dp(48));
        searchParams.leftMargin = dp(18);
        bar.addView(search, searchParams);

        appCount = label(apps.size() + " ứng dụng", 15, MUTED, false);
        appCount.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        bar.addView(appCount, new LinearLayout.LayoutParams(0, -1, 1));
        return bar;
    }

    private View buildHero() {
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setPadding(dp(32), dp(22), dp(32), dp(22));
        hero.setBackground(roundGradient(Color.rgb(35, 48, 87), Color.rgb(23, 32, 59), dp(22)));
        TextView eyebrow = label("DÀNH RIÊNG CHO MÀN HÌNH LỚN", 13, Color.rgb(166, 188, 255), true);
        hero.addView(eyebrow);
        TextView heading = label("Khám phá ứng dụng cho TV", 30, WHITE, true);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-2, -2);
        hp.topMargin = dp(8);
        hero.addView(heading, hp);
        TextView sub = label("Chọn ứng dụng yêu thích · Xem phiên bản · Cài đặt bằng remote", 16, Color.rgb(205, 213, 231), false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-2, -2);
        sp.topMargin = dp(7);
        hero.addView(sub, sp);
        return hero;
    }

    private void addCategoryBar(LinearLayout content) {
        String[] categories = {"Tất cả", "Giải trí", "Video", "Giao diện TV", "Tiện ích"};
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(this);
        chips.setGravity(Gravity.CENTER_VERTICAL);
        for (String category : categories) {
            final String value = category;
            TextView chip = label(category, 15, selectedCategory.equals(category) ? WHITE : MUTED, selectedCategory.equals(category));
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(17), 0, dp(17), 0);
            chip.setBackground(round(selectedCategory.equals(category) ? BLUE : PANEL, dp(22), Color.TRANSPARENT, 0));
            chip.setFocusable(true);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { selectedCategory = value; showHome(); }
            });
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2, dp(42));
            cp.rightMargin = dp(10);
            chips.addView(chip, cp);
        }
        scroll.addView(chips);
        LinearLayout.LayoutParams bar = new LinearLayout.LayoutParams(-1, dp(58));
        bar.topMargin = dp(12);
        content.addView(scroll, bar);
    }

    private void addSection(LinearLayout content, String heading, List<AppItem> rows) {
        if (rows.isEmpty()) return;
        TextView title = label(heading, 21, WHITE, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(18);
        titleParams.bottomMargin = dp(12);
        content.addView(title, titleParams);

        HorizontalScrollView horizontal = new HorizontalScrollView(this);
        horizontal.setHorizontalScrollBarEnabled(false);
        LinearLayout cards = new LinearLayout(this);
        for (AppItem app : rows) {
            cards.addView(appCard(app), new LinearLayout.LayoutParams(dp(224), dp(158)));
            View gap = new View(this);
            cards.addView(gap, new LinearLayout.LayoutParams(dp(14), 1));
        }
        horizontal.addView(cards);
        content.addView(horizontal, new LinearLayout.LayoutParams(-1, dp(165)));
    }

    private View appCard(AppItem app) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(17), dp(15), dp(17), dp(13));
        card.setBackground(focusBackground());
        card.setFocusable(true);
        card.setClickable(true);
        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showDetails(app); }
        });
        if (firstAppCard == null) firstAppCard = card;

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon = appLogo(app);
        icon.setBackground(roundGradient(iconColor(app), blend(iconColor(app), Color.BLACK, 0.25f), dp(16)));
        icon.setPadding(dp(2), dp(2), dp(2), dp(2));
        top.addView(icon, new LinearLayout.LayoutParams(dp(46), dp(46)));
        TextView category = label(app.category, 12, MUTED, false);
        category.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams catp = new LinearLayout.LayoutParams(0, dp(46), 1);
        catp.leftMargin = dp(8);
        top.addView(category, catp);
        card.addView(top);

        TextView name = label(app.title, 18, WHITE, true);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.topMargin = dp(13);
        card.addView(name, np);
        TextView version = label("Phiên bản " + app.version, 13, MUTED, false);
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(-1, -2);
        vp.topMargin = dp(4);
        card.addView(version, vp);
        return card;
    }

    private void showDetails(AppItem app) {
        showingDetails = true;
        showingInstallSuccess = false;
        currentDetailApp = app;
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(52), dp(34), dp(52), dp(34));
        root.setBackgroundColor(BG);
        setContentView(root);

        LinearLayout brand = new LinearLayout(this);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        brand.addView(brandMark(), new LinearLayout.LayoutParams(dp(36), dp(36)));
        TextView brandName = label(BRAND_NAME, 17, BLUE, true);
        LinearLayout.LayoutParams brandNameParams = new LinearLayout.LayoutParams(-2, -1);
        brandNameParams.leftMargin = dp(8);
        brand.addView(brandName, brandNameParams);
        root.addView(brand);
        LinearLayout body = new LinearLayout(this);
        body.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, 0, 1);
        root.addView(body, bp);

        ImageView icon = appLogo(app);
        icon.setBackground(roundGradient(iconColor(app), blend(iconColor(app), Color.BLACK, 0.28f), dp(30)));
        icon.setPadding(dp(6), dp(6), dp(6), dp(6));
        body.addView(icon, new LinearLayout.LayoutParams(dp(150), dp(150)));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0, -2, 1);
        ip.leftMargin = dp(36);
        body.addView(info, ip);
        info.addView(label(app.category.toUpperCase(Locale.ROOT) + "  ·  PHIÊN BẢN " + app.version, 14, Color.rgb(166, 188, 255), true));
        TextView title = label(app.title, 36, WHITE, true);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2);
        tp.topMargin = dp(12);
        info.addView(title, tp);
        TextView description = label(app.description, 18, MUTED, false);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1, -2);
        dp.topMargin = dp(13);
        info.addView(description, dp);
        TextView requirement = label(app.androidRequirement() + "     ·     Gói APK có sẵn trong thư viện", 15, MUTED, false);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
        rp.topMargin = dp(16);
        info.addView(requirement, rp);
        TextView installed = label(isInstalled(app) ? "ĐÃ CÀI ĐẶT" : "SẴN SÀNG CÀI ĐẶT", 13,
                isInstalled(app) ? Color.rgb(117, 222, 173) : MUTED, true);
        LinearLayout.LayoutParams stp = new LinearLayout.LayoutParams(-1, -2);
        stp.topMargin = dp(15);
        info.addView(installed, stp);

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        TextView install = actionButton(isInstalled(app) ? "Cài lại ứng dụng" : "Cài đặt APK", true);
        install.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { beginInstall(app); }
        });
        actions.addView(install, new LinearLayout.LayoutParams(dp(220), dp(54)));
        TextView back = actionButton("Quay lại", false);
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showHome(); }
        });
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(dp(180), dp(54));
        backParams.leftMargin = dp(14);
        actions.addView(back, backParams);
        root.addView(actions);
        install.requestFocus();
    }

    private void beginInstall(AppItem app) {
        if (Build.VERSION.SDK_INT < app.minSdk) {
            new AlertDialog.Builder(this).setTitle("Android chưa tương thích")
                    .setMessage(app.title + " yêu cầu " + app.androidRequirement() + ". Thiết bị hiện tại đang chạy Android " + Build.VERSION.RELEASE + ".")
                    .setPositiveButton("Đóng", null).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
            pendingApp = app;
            new AlertDialog.Builder(this)
                    .setTitle("Cho phép cài ứng dụng")
                    .setMessage("Android cần bạn cho phép DMP APK STORE HUB cài APK. Bật quyền “Cho phép cài ứng dụng” cho DMP APK STORE HUB, sau đó quay lại.")
                    .setNegativeButton("Để sau", new android.content.DialogInterface.OnClickListener() {
                        @Override public void onClick(android.content.DialogInterface d, int which) { pendingApp = null; }
                    })
                    .setPositiveButton("Mở cài đặt", new android.content.DialogInterface.OnClickListener() {
                        @Override public void onClick(android.content.DialogInterface d, int which) {
                            Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                    Uri.parse("package:" + getPackageName()));
                            startActivityForResult(settings, SETTINGS_INSTALL_SOURCE);
                        }
                    }).show();
            return;
        }
        copyAndInstall(app);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == INSTALL_APK_REQUEST) {
            AppItem app = installingApp;
            installingApp = null;
            if (app != null && isInstalled(app)) showInstallSuccess(app);
            return;
        }
        if (requestCode == SETTINGS_INSTALL_SOURCE && pendingApp != null) {
            if (Build.VERSION.SDK_INT < 26 || getPackageManager().canRequestPackageInstalls()) {
                AppItem app = pendingApp;
                pendingApp = null;
                copyAndInstall(app);
            } else {
                Toast.makeText(this, "Chưa bật quyền cài APK cho DMP APK STORE HUB", Toast.LENGTH_LONG).show();
                pendingApp = null;
            }
        }
    }

    private void copyAndInstall(AppItem app) {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Đang chuẩn bị APK…");
        progress.setCancelable(false);
        progress.show();
        new Thread(new Runnable() {
            @Override public void run() {
                File apk = null;
                Exception failure = null;
                try {
                    File directory = new File(getCacheDir(), "apks");
                    if (!directory.exists() && !directory.mkdirs()) throw new IllegalStateException("Không tạo được thư mục tạm");
                    apk = new File(directory, app.filename);
                    if (!apk.isFile() || apk.length() == 0) {
                        String assetPath = findApkAssetPath(app.filename);
                        InputStream input = getAssets().open(assetPath);
                        FileOutputStream output = new FileOutputStream(apk);
                        byte[] buffer = new byte[64 * 1024];
                        int count;
                        while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                        output.flush();
                        output.close();
                        input.close();
                    }
                } catch (Exception e) { failure = e; }
                final File ready = apk;
                final Exception error = failure;
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        progress.dismiss();
                        if (error != null || ready == null || !ready.isFile()) {
                            if (error != null) android.util.Log.e("DMPStore", "Could not prepare APK: " + app.filename, error);
                            String reason = error == null ? "Không tìm thấy tệp sau khi sao chép." : error.getClass().getSimpleName() + ": " + error.getMessage();
                            new AlertDialog.Builder(MainActivity.this).setTitle("Không chuẩn bị được APK")
                                    .setMessage("Tệp: " + app.filename + "\n" + reason)
                                    .setPositiveButton("Đóng", null).show();
                            return;
                        }
                        installFile(app, ready);
                    }
                });
            }
        }, "apk-copy").start();
    }

    private void installFile(AppItem app, File apk) {
        try {
            installingApp = app;
            Uri uri = Uri.parse("content://com.dmp.store.apkprovider/" + Uri.encode(apk.getName()));
            Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (android.os.Build.VERSION.SDK_INT >= 16) {
                intent.setClipData(android.content.ClipData.newRawUri(apk.getName(), uri));
            }
            try {
                startActivityForResult(intent, INSTALL_APK_REQUEST);
            } catch (android.content.ActivityNotFoundException unsupportedAction) {
                // Several Android TV 6 package installers only register ACTION_VIEW.
                Intent legacyIntent = new Intent(Intent.ACTION_VIEW);
                legacyIntent.setDataAndType(uri, "application/vnd.android.package-archive");
                legacyIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                if (android.os.Build.VERSION.SDK_INT >= 16) {
                    legacyIntent.setClipData(android.content.ClipData.newRawUri(apk.getName(), uri));
                }
                // Android 6 TV installers often filter APK intents to file:// only.
                // Resolve that legacy filter, then explicitly route the content:// URI
                // so the installer can still read our private, grant-protected file.
                Intent lookup = new Intent(Intent.ACTION_VIEW);
                lookup.setDataAndType(Uri.fromFile(apk), "application/vnd.android.package-archive");
                android.content.pm.ResolveInfo handler = getPackageManager().resolveActivity(lookup, 0);
                if (handler != null && handler.activityInfo != null) {
                    legacyIntent.setComponent(new android.content.ComponentName(
                            handler.activityInfo.packageName, handler.activityInfo.name));
                }
                startActivityForResult(legacyIntent, INSTALL_APK_REQUEST);
            }
        } catch (Exception e) {
            installingApp = null;
            android.util.Log.e("DMPStore", "Could not launch installer for " + app.filename, e);
            new AlertDialog.Builder(this).setTitle("Không mở được trình cài đặt")
                    .setMessage("Tệp: " + app.filename + "\n" + e.getClass().getSimpleName() + ": " + e.getMessage())
                    .setPositiveButton("Đóng", null).show();
        }
    }

    private void showInstallSuccess(final AppItem app) {
        showingDetails = false;
        showingInstallSuccess = true;
        currentDetailApp = app;
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(52), dp(34), dp(52), dp(34));
        root.setBackgroundColor(BG);
        setContentView(root);

        LinearLayout brand = new LinearLayout(this);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        brand.addView(brandMark(), new LinearLayout.LayoutParams(dp(36), dp(36)));
        TextView brandName = label(BRAND_NAME, 17, BLUE, true);
        LinearLayout.LayoutParams brandNameParams = new LinearLayout.LayoutParams(-2, -1);
        brandNameParams.leftMargin = dp(8);
        brand.addView(brandName, brandNameParams);
        root.addView(brand);

        LinearLayout center = new LinearLayout(this);
        center.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams centerParams = new LinearLayout.LayoutParams(-1, 0, 1);
        root.addView(center, centerParams);
        ImageView icon = appLogo(app);
        center.addView(icon, new LinearLayout.LayoutParams(dp(132), dp(132)));
        LinearLayout message = new LinearLayout(this);
        message.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(0, -2, 1);
        messageParams.leftMargin = dp(30);
        center.addView(message, messageParams);
        message.addView(label("CÀI ĐẶT THÀNH CÔNG", 16, Color.rgb(117, 222, 173), true));
        TextView title = label(app.title, 34, WHITE, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(10);
        message.addView(title, titleParams);
        message.addView(label("Bạn có thể mở ứng dụng ngay hoặc quay lại thư viện.", 18, MUTED, false));

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        TextView open = actionButton("Mở ứng dụng", true);
        successOpenButton = open;
        open.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openInstalledApp(app); }
        });
        actions.addView(open, new LinearLayout.LayoutParams(dp(220), dp(54)));
        TextView back = actionButton("Quay lại DMP APK STORE HUB", false);
        successReturnButton = back;
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showHome(); }
        });
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(dp(250), dp(54));
        backParams.leftMargin = dp(14);
        actions.addView(back, backParams);
        root.addView(actions);
        open.requestFocus();
    }

    private void openInstalledApp(AppItem app) {
        Intent launch = getPackageManager().getLaunchIntentForPackage(app.packageName);
        if (launch == null) {
            Toast.makeText(this, "Ứng dụng đã cài nhưng không có màn hình để mở.", Toast.LENGTH_LONG).show();
            showHome();
            return;
        }
        try {
            startActivity(launch);
        } catch (Exception e) {
            android.util.Log.e("DMPStore", "Could not open installed app " + app.packageName, e);
            Toast.makeText(this, "Không mở được ứng dụng vừa cài.", Toast.LENGTH_LONG).show();
        }
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (showingInstallSuccess) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && successReturnButton != null) {
                successReturnButton.requestFocus();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && successOpenButton != null) {
                successOpenButton.requestFocus();
                return true;
            }
            if ((keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)
                    && getCurrentFocus() != null) {
                getCurrentFocus().performClick();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private boolean isInstalled(AppItem app) {
        try {
            PackageInfo ignored = getPackageManager().getPackageInfo(app.packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) { return false; }
    }

    private String findApkAssetPath(String filename) throws IOException {
        String[] names = getAssets().list("");
        for (String name : names) {
            if (name.equals(filename)) return name;
        }
        for (String name : names) {
            if (name.equalsIgnoreCase(filename)) return name;
        }
        throw new java.io.FileNotFoundException("Không thấy " + filename + " trong assets (" + java.util.Arrays.toString(names) + ")");
    }

    private List<AppItem> filteredApps() {
        List<AppItem> result = new ArrayList<>();
        String needle = normalizeSearch(query).trim();
        for (AppItem app : apps) {
            boolean categoryMatch = selectedCategory.equals("Tất cả") || app.category.equals(selectedCategory);
            boolean queryMatch = needle.isEmpty() || matchesSearch(app, needle);
            if (categoryMatch && queryMatch) result.add(app);
        }
        return result;
    }

    private boolean matchesSearch(AppItem app, String normalizedQuery) {
        String searchable = normalizeSearch(app.title + " " + app.category + " " + app.description + " "
                + app.packageName + " " + app.version + " " + app.filename + " " + app.id);
        String[] terms = normalizedQuery.split("\\s+");
        for (String term : terms) {
            if (!term.isEmpty() && !searchable.contains(term)) return false;
        }
        return true;
    }

    private String normalizeSearch(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
        return normalized.replace('đ', 'd');
    }

    private List<AppItem> filterCategory(List<AppItem> source, String category) {
        List<AppItem> result = new ArrayList<>();
        for (AppItem app : source) if (app.category.equals(category)) result.add(app);
        return result;
    }

    private List<AppItem> merge(List<AppItem> first, List<AppItem> second) {
        List<AppItem> result = new ArrayList<>(first);
        result.addAll(second);
        return result;
    }

    private TextView actionButton(String text, boolean primary) {
        TextView button = label(text, 17, WHITE, true);
        button.setGravity(Gravity.CENTER);
        button.setFocusable(true);
        button.setClickable(true);
        button.setBackground(focusBackground(primary ? BLUE : PANEL));
        return button;
    }

    private android.graphics.drawable.Drawable focusBackground() {
        return focusBackground(PANEL);
    }

    private android.graphics.drawable.Drawable focusBackground(int normal) {
        android.graphics.drawable.StateListDrawable states = new android.graphics.drawable.StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused}, round(PANEL_FOCUS, dp(18), BLUE, dp(2)));
        states.addState(new int[]{android.R.attr.state_pressed}, round(BLUE, dp(18), BLUE, dp(2)));
        states.addState(new int[]{}, round(normal, dp(18), Color.rgb(37, 44, 59), dp(1)));
        return states;
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private ImageView brandMark() {
        ImageView mark = new ImageView(this);
        int resource = getResources().getIdentifier("ic_launcher", "mipmap", getPackageName());
        if (resource != 0) mark.setImageResource(resource);
        mark.setContentDescription(BRAND_NAME);
        mark.setScaleType(ImageView.ScaleType.FIT_CENTER);
        return mark;
    }

    private ImageView appLogo(AppItem app) {
        ImageView image = new ImageView(this);
        Bitmap bitmap = appIcons.get(app.id);
        if (bitmap == null) {
            try {
                InputStream input = getAssets().open("icon-" + app.id + ".png");
                bitmap = BitmapFactory.decodeStream(input);
                input.close();
                if (bitmap != null) appIcons.put(app.id, bitmap);
            } catch (IOException e) {
                android.util.Log.w("DMPStore", "Missing app icon for " + app.id, e);
            }
        }
        if (bitmap != null) image.setImageBitmap(bitmap);
        else image.setImageDrawable(roundGradient(iconColor(app), blend(iconColor(app), Color.BLACK, 0.25f), dp(16)));
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setContentDescription(app.title);
        return image;
    }

    private android.graphics.drawable.Drawable round(int color, int radius, int stroke, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeWidth > 0) drawable.setStroke(strokeWidth, stroke);
        return drawable;
    }

    private android.graphics.drawable.Drawable roundGradient(int first, int second, int radius) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{first, second});
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private String initials(String title) {
        String[] words = title.split(" ");
        if (words.length == 1) return title.substring(0, Math.min(2, title.length())).toUpperCase(Locale.ROOT);
        return (words[0].substring(0, 1) + words[1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    private int iconColor(AppItem app) {
        if (app.category.equals("Video")) return Color.rgb(198, 52, 67);
        if (app.category.equals("Giao diện TV")) return Color.rgb(68, 107, 192);
        if (app.category.equals("Tiện ích")) return Color.rgb(103, 76, 191);
        return Color.rgb(27 + Math.abs(app.id.hashCode() % 80), 105 + Math.abs(app.id.hashCode() % 70), 139 + Math.abs(app.id.hashCode() % 70));
    }

    private int blend(int color, int other, float ratio) {
        return Color.rgb((int) (Color.red(color) * (1 - ratio) + Color.red(other) * ratio),
                (int) (Color.green(color) * (1 - ratio) + Color.green(other) * ratio),
                (int) (Color.blue(color) * (1 - ratio) + Color.blue(other) * ratio));
    }

    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    @Override public void onBackPressed() {
        if (showingDetails) {
            showHome();
            return;
        }
        super.onBackPressed();
    }
}
