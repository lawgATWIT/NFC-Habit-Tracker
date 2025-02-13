package com.example.nfc_habit_tracker;

import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.os.Bundle;
import android.util.Log;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] intentFiltersArray;

    private int scanCount = 0;
    private TableLayout nfcTable;

    private static final String CHANNEL_ID = "NFC_NOTIFICATION_CHANNEL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNotificationChannel();

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not available on this device.", Toast.LENGTH_SHORT).show();
            finish();
        }

        nfcTable = findViewById(R.id.nfcTable);

        pendingIntent = PendingIntent.getActivity(
                this,
                0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_MUTABLE
        );

        IntentFilter nfcIntentFilter = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        intentFiltersArray = new IntentFilter[]{nfcIntentFilter};
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                scanCount++;

                String tagContents = readTagData(tag);
                sendNotification(tagContents);

                addScanToTable(scanCount, tagContents);

                int red = (int) (Math.random() * 256);
                int green = (int) (Math.random() * 256);
                int blue = (int) (Math.random() * 256);
                int randomColor = Color.rgb(red, green, blue);

                getWindow().getDecorView().setBackgroundColor(randomColor);
                Toast.makeText(this, "NFC tag detected!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void addScanToTable(int scanNumber, String tagContents) {
        String currentTime = java.text.DateFormat.getDateTimeInstance().format(new Date());

        TableRow newRow = new TableRow(this);

        TextView scanNumberText = new TextView(this);
        scanNumberText.setText(String.valueOf(scanNumber));

        TextView tagContentText = new TextView(this);
        tagContentText.setText(tagContents);

        TextView scanTimeText = new TextView(this);
        scanTimeText.setText(currentTime);

        newRow.addView(scanNumberText);
        newRow.addView(tagContentText);
        newRow.addView(scanTimeText);

        nfcTable.addView(newRow);
    }

    private void createNotificationChannel() {
        CharSequence name = "NFC Scan Notifications";
        String description = "Notifications for NFC tag scans";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void sendNotification(String tagContents) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setContentTitle("NFC Scan Detected")
                .setContentText("NFC Scans: " + scanCount + "\nTag Contents: " + tagContents)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        notificationManager.notify(scanCount, builder.build());
    }
//comment!
    private String readTagData(Tag tag) {
        Ndef ndef = Ndef.get(tag);
        if (ndef != null) {
            try {
                ndef.connect();
                NdefMessage ndefMessage = ndef.getNdefMessage();
                if (ndefMessage != null) {
                    NdefRecord[] records = ndefMessage.getRecords();
                    for (NdefRecord record : records) {
                        if (record.getTnf() == NdefRecord.TNF_WELL_KNOWN &&
                                Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)) {
                            byte[] payload = record.getPayload();
                            if (payload.length > 3) {
                                Charset textCharset = StandardCharsets.UTF_8;
                                return new String(payload, 3, payload.length - 3, textCharset);
                            }
                        }
                    }
                }
                ndef.close();
            } catch (Exception e) {
                Log.e("NFC_ERROR", "Error reading NFC tag", e);
            }
        }
        return "Sorry, the tag that you scanned has no contents";
    }
}
