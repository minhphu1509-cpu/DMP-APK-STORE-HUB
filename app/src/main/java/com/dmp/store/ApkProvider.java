package com.dmp.store;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/** Grants the system package installer temporary read access to an APK copied to our cache. */
public final class ApkProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }

    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("Read only");
        String name = uri.getLastPathSegment();
        if (name == null || name.contains("/") || name.contains("\\") || name.contains("..")) {
            throw new FileNotFoundException("Invalid APK path");
        }
        File directory = new File(getContext().getCacheDir(), "apks");
        File apk = new File(directory, name);
        try {
            if (!apk.isFile() || !apk.getCanonicalPath().startsWith(directory.getCanonicalPath() + File.separator)) {
                throw new FileNotFoundException("APK not found");
            }
            return ParcelFileDescriptor.open(apk, ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (IOException e) {
            if (e instanceof FileNotFoundException) throw (FileNotFoundException) e;
            FileNotFoundException missing = new FileNotFoundException("APK not found");
            missing.initCause(e);
            throw missing;
        }
    }

    @Override public String getType(Uri uri) { return "application/vnd.android.package-archive"; }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { throw new UnsupportedOperationException(); }
}
