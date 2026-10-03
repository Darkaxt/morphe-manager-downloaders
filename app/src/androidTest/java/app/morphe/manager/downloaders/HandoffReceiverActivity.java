package app.morphe.manager.downloaders;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;

/** Runs in the separate test APK without depending on the target APK's libraries. */
public class HandoffReceiverActivity extends Activity {
    public static final String RESULT = "app.morphe.manager.downloaders.test.HANDOFF_RESULT";
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent response = new Intent(RESULT).setPackage("app.morphe.manager.downloaders");
        try {
            Uri uri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (InputStream input = getContentResolver().openInputStream(uri)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            }
            String name;
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                cursor.moveToFirst();
                name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
            }
            byte[] bytes = output.toByteArray();
            StringBuilder hash = new StringBuilder();
            for (byte b : MessageDigest.getInstance("SHA-256").digest(bytes)) hash.append(String.format("%02x", b));
            response.putExtra("name", name).putExtra("mime", getIntent().getType())
                .putExtra("size", bytes.length).putExtra("sha256", hash.toString());
        } catch (Exception error) { response.putExtra("error", error.toString()); }
        sendBroadcast(response);
        finish();
    }
}
