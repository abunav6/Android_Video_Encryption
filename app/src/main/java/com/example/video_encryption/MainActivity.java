package com.example.video_encryption;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;

import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroupOverlay;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    private static final int VIDEO_SELECT_CODE = 0;
    private static final int TEXT_SELECT_CODE = 1;
    private String file_name, key;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        (findViewById(R.id.encrypt)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {



                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("video/mp4");
                intent.addCategory(Intent.CATEGORY_OPENABLE);

                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                }
                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                }

                try {
                    startActivityForResult(
                            Intent.createChooser(intent, "Select a File to Upload"), VIDEO_SELECT_CODE
                    );
                } catch (android.content.ActivityNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Please install a file manager!", Toast.LENGTH_LONG).show();
                }
            }
        });


        (findViewById(R.id.decrypt)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("text/plain");
                intent.addCategory(Intent.CATEGORY_OPENABLE);

                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                }

                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                }

                try{
                    startActivityForResult(
                            Intent.createChooser(intent, "Select a File to upload"), TEXT_SELECT_CODE
                    );
                }catch (android.content.ActivityNotFoundException e){
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Please install a file manager!", Toast.LENGTH_LONG).show();
                }

            }
        });


    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, final @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case VIDEO_SELECT_CODE:


                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    try {
                        FileInputStream importdb = new FileInputStream(getContentResolver().openFileDescriptor(uri, "r").getFileDescriptor());

                        File file = new File(uri.getPath());
                        Log.i("input", file.getAbsolutePath());
                        try {

                            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

                            int nRead;


                            byte[] bytes = new byte[100000];

                            while ((nRead = importdb.read(bytes, 0, bytes.length)) != -1) {
                                buffer.write(bytes, 0, nRead);
                            }
                            final String encoded = new String(buffer.toByteArray(), StandardCharsets.ISO_8859_1);


                            File dir = new File("/storage/emulated/0/VideoEncryptorFiles");
                            dir.mkdir();


                            final LayoutInflater inflater = (LayoutInflater)
                                    getSystemService(LAYOUT_INFLATER_SERVICE);
                            final View popupView = inflater.inflate(R.layout.popup, null);

                            final int width = LinearLayout.LayoutParams.WRAP_CONTENT;
                            final int height = LinearLayout.LayoutParams.WRAP_CONTENT;
                            final boolean focusable = true;
                            final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);
                            ((EditText)popupView.findViewById(R.id.file_name)).setHintTextColor(Color.LTGRAY);
                            ((EditText)popupView.findViewById(R.id.file_name)).setTextColor(Color.WHITE);
                            ((EditText)popupView.findViewById(R.id.key)).setHintTextColor(Color.LTGRAY);
                            ((EditText)popupView.findViewById(R.id.key)).setTextColor(Color.WHITE);


                            popupWindow.setOutsideTouchable(false);

                            final ViewGroup root = (ViewGroup) getWindow().getDecorView().getRootView();

                            popupWindow.showAtLocation(findViewById(R.id.main_activity), Gravity.CENTER, 0, 0);
                            applyDim(root, 0.75f);


                            (popupView.findViewById(R.id.submit)).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    final long startTime = System.currentTimeMillis();
                                    file_name = ((TextView)popupView.findViewById(R.id.file_name)).getText().toString();
                                    key = ((TextView)popupView.findViewById(R.id.key)).getText().toString();


                                    try {
                                        final File file = new File(String.format("/storage/emulated/0/VideoEncryptorFiles/%s_encoded.txt", file_name));
                                        final Uri uri = FileProvider.getUriForFile(MainActivity.this, MainActivity.this.getPackageName()+".provider", file);
                                        FileOutputStream text_encoded = new FileOutputStream(file);
                                        AES aes = new AES();
                                        String encrypted = aes.encrypt(encoded, key);

                                        try{
                                            text_encoded.write(encrypted.getBytes(StandardCharsets.ISO_8859_1));
                                            long endTime = System.currentTimeMillis();
                                            long elapsed = endTime - startTime;
                                            //Toast.makeText(getApplicationContext(), "Wrote Encoded Text to File!", Toast.LENGTH_SHORT).show();
                                            LayoutInflater inf = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                                            final View pv = inf.inflate(R.layout.desc, null);
                                            final PopupWindow pw = new PopupWindow(pv, width, height, focusable);
                                            pw.setOutsideTouchable(false);
                                            pw.showAtLocation(findViewById(R.id.main_activity), Gravity.CENTER, 0,0);
                                            applyDim(root, 0.75f);

                                            ((TextView)pv.findViewById(R.id.wrote)).setTypeface(null, Typeface.BOLD);
                                            ((TextView)pv.findViewById(R.id.wrote)).setTextSize(26);
                                            ((TextView)pv.findViewById(R.id.time)).setTypeface(null, Typeface.BOLD);
                                            ((TextView)pv.findViewById(R.id.time)).setTextSize(18);

                                            ((TextView)pv.findViewById(R.id.time)).setText(String.format("It took %s milliseconds to write to %s.txt!", elapsed, file_name));

                                            pv.findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    pw.dismiss();
                                                }
                                            });

                                            pv.findViewById(R.id.send_on_WA).setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    Intent share = new Intent();
                                                    share.setType("text/plain");
                                                    share.setAction(Intent.ACTION_SEND);
                                                    share.putExtra(Intent.EXTRA_STREAM, uri);
                                                    share.setPackage("com.whatsapp");
                                                    share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                                    startActivity(share);
                                                }
                                            });



                                            pv.findViewById(R.id.view_file).setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    pw.dismiss();
                                                    LayoutInflater i = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                                                    final View p1 = i.inflate(R.layout.encoded, null);
                                                    final PopupWindow p2 = new PopupWindow(p1, width, height, focusable);
                                                    p2.setOutsideTouchable(true);
                                                    ((TextView)p1.findViewById(R.id.encoded_text)).setText(encoded);
                                                    ((TextView)p1.findViewById(R.id.title)).setText(String.format("%s_encoded.txt", file_name));
                                                    ((TextView)p1.findViewById(R.id.title)).setTextColor(Color.WHITE);
                                                    ((TextView)p1.findViewById(R.id.title)).setTypeface(null, Typeface.BOLD);
                                                    ((TextView)p1.findViewById(R.id.encoded_text)).setTextColor(Color.WHITE);
                                                    ((TextView)p1.findViewById(R.id.encoded_text)).setMovementMethod(new ScrollingMovementMethod());
                                                    p2.showAtLocation(findViewById(R.id.main_activity), Gravity.CENTER, 0,0);
                                                    applyDim(root, 0.75f);

                                                    p2.setOnDismissListener(new PopupWindow.OnDismissListener() {
                                                        @Override
                                                        public void onDismiss() {
                                                            clearDim(root);
                                                        }
                                                    });


                                                }
                                            });

                                            pw.setOnDismissListener(new PopupWindow.OnDismissListener() {
                                                @Override
                                                public void onDismiss() {
                                                    clearDim(root);
                                                }
                                            });



                                        }catch (Exception e){
                                            e.printStackTrace();
                                            Toast.makeText(getApplicationContext(), "Could not write Encoded Text to File!", Toast.LENGTH_SHORT).show();
                                        }
                                    }catch (Exception e){
                                        e.printStackTrace();
                                        Toast.makeText(getApplicationContext(), "File Could not be opened!", Toast.LENGTH_SHORT).show();

                                    }
                                    popupWindow.dismiss();
                                }
                            });


                            popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                                @Override
                                public void onDismiss() {
                                    clearDim(root);
                                }
                            });



                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(getApplicationContext(), "Input Stream not created!", Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), "Unexpected Error! Quitting", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                }
                break;


            case TEXT_SELECT_CODE:
                try {
                    LayoutInflater inflater = (LayoutInflater)
                            getSystemService(LAYOUT_INFLATER_SERVICE);
                    final View popupView = inflater.inflate(R.layout.popup2, null);

                    final int width = ViewGroup.LayoutParams.WRAP_CONTENT;
                    final int height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    final boolean focusable = true;
                    final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);
                    popupWindow.setOutsideTouchable(false);

                    final ViewGroup root = (ViewGroup) getWindow().getDecorView().getRootView();

                    popupWindow.showAtLocation(findViewById(R.id.main_activity), Gravity.CENTER, 0, 0);
                    applyDim(root, 0.75f);

                    ((EditText)popupView.findViewById(R.id.key2)).setHintTextColor(Color.LTGRAY);
                    ((EditText)popupView.findViewById(R.id.key2)).setTextColor(Color.WHITE);

                    (popupView.findViewById(R.id.submit2)).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            final long startTime = System.currentTimeMillis();
                            key = ((TextView) popupView.findViewById(R.id.key2)).getText().toString();
                            popupWindow.dismiss();


                            Uri uri = data.getData();
                            String uri_path;
                            if(uri.getPath().contains("/storage/emulated/0")){
                                uri_path = uri.getPath().split(":")[1];
                            }
                            else{
                                uri_path = uri.getPath().replace("external_files", "storage/emulated/0");
                            }
                            try {
                                File file = new File(uri_path);
                                FileInputStream fis = new FileInputStream(file);
                                file_name=file.getName().split("_")[0];
                                Log.i("File_name", file_name);


                                ByteArrayOutputStream buffer = new ByteArrayOutputStream();

                                byte[] temp = new byte[100000];
                                int nRead;

                                while ((nRead = fis.read(temp, 0, temp.length)) != -1) {
                                    buffer.write(temp, 0, nRead);
                                }

                                temp = buffer.toByteArray();
                                AES aes = new AES();
                                String to_decrypt = new String(temp, StandardCharsets.ISO_8859_1);


                                String decrypted = aes.decrypt(to_decrypt, key);
                                Log.i("Decrypted", decrypted);

                                byte[] d_bytes = decrypted.getBytes(StandardCharsets.ISO_8859_1);
                                File out;
                                final Uri vid;
                                try {
                                    out = new File(String.format("/storage/emulated/0/VideoEncryptorFiles/%s_decrypted.mp4", file_name));
                                    vid = FileProvider.getUriForFile(MainActivity.this, MainActivity.this.getPackageName()+".provider", out);
                                    Log.i("Video", "Success");
                                    Log.i("output", out.getAbsolutePath());
                                    OutputStream os = new FileOutputStream(out);
                                    os.write(d_bytes);
                                    long endTime = System.currentTimeMillis();
                                    long elapsed = endTime - startTime;
                                    LayoutInflater inf = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                                    final View pv = inf.inflate(R.layout.desc, null);
                                    final PopupWindow pw = new PopupWindow(pv, width, height, focusable);
                                    pw.setOutsideTouchable(false);
                                    pw.showAtLocation(findViewById(R.id.main_activity), Gravity.CENTER, 0,0);
                                    applyDim(root, 0.75f);


                                    ((TextView)pv.findViewById(R.id.view_file)).setText(R.string.play_video);
                                    ((TextView)pv.findViewById(R.id.wrote)).setTypeface(null, Typeface.BOLD);
                                    ((TextView)pv.findViewById(R.id.wrote)).setTextSize(26);
                                    ((TextView)pv.findViewById(R.id.time)).setTypeface(null, Typeface.BOLD);
                                    ((TextView)pv.findViewById(R.id.time)).setTextSize(18);

                                    ((TextView)pv.findViewById(R.id.time)).setText(String.format("It took %s milliseconds to decrypt and make %s_decrypted.mp4!", elapsed, file_name));

                                    pv.findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            pw.dismiss();
                                        }
                                    });


                                    pv.findViewById(R.id.view_file).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            pw.dismiss();
                                            try {
                                                final VideoView videoView = findViewById(R.id.videoView);
                                                final Uri u = Uri.parse(String.format("/storage/emulated/0/VideoEncryptorFiles/%s_decrypted.mp4", file_name));
                                                videoView.setVideoURI(u);
                                                videoView.setVisibility(View.VISIBLE);
                                                Toast.makeText(getApplicationContext(), "Playing Output Video", Toast.LENGTH_SHORT).show();
                                                (findViewById(R.id.encrypt)).setVisibility(View.INVISIBLE);
                                                (findViewById(R.id.decrypt)).setVisibility(View.INVISIBLE);
                                                videoView.start();
                                                videoView.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View view) {
                                                        videoView.stopPlayback();
                                                        videoView.setVisibility(View.INVISIBLE);
                                                        (findViewById(R.id.encrypt)).setVisibility(View.VISIBLE);
                                                        (findViewById(R.id.decrypt)).setVisibility(View.VISIBLE);
                                                    }
                                                });

                                                videoView.setOnLongClickListener(new View.OnLongClickListener() {
                                                    @Override
                                                    public boolean onLongClick(View view) {
                                                        new AlertDialog.Builder(MainActivity.this)
                                                                .setTitle("Send on Whatsapp")
                                                                .setMessage("Do you want to sent the generated Video on Whatsapp?")
                                                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                                                    @Override
                                                                    public void onClick(DialogInterface dialogInterface, int i) {
                                                                        Intent share = new Intent();
                                                                        share.setAction(Intent.ACTION_SEND);
                                                                        share.setType("video/mp4");
                                                                        share.putExtra(Intent.EXTRA_STREAM, u);
                                                                        share.setPackage("com.whatsapp");
                                                                        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                                                        startActivity(share);
                                                                    }
                                                                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                                //
                                                            }
                                                        }).setIcon(android.R.drawable.ic_dialog_alert)
                                                                .show();
                                                        return true;
                                                    }
                                                });

                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                Toast.makeText(getApplicationContext(), "Video Created. Check Storage!", Toast.LENGTH_SHORT).show();
                                            }

                                        }
                                    });

                                    pv.findViewById(R.id.send_on_WA).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            Intent share = new Intent();
                                            share.setAction(Intent.ACTION_SEND);
                                            share.setType("*/*");
                                            share.putExtra(Intent.EXTRA_STREAM, vid);
                                            share.setPackage("com.whatsapp");
                                            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                            startActivity(share);
                                        }
                                    });

                                    pw.setOnDismissListener(new PopupWindow.OnDismissListener() {
                                        @Override
                                        public void onDismiss() {
                                            clearDim(root);
                                        }
                                    });

                                    os.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Toast.makeText(getApplicationContext(), "Video Creation Failed", Toast.LENGTH_SHORT).show();
                                }
                            }catch (Exception e){
                                e.printStackTrace();
                                Toast.makeText(getApplicationContext(), "Error While Decrypting! Check the key!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });


                    popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                        @Override
                        public void onDismiss() {
                            clearDim(root);
                        }
                    });

                }catch (Exception e){
                    Toast.makeText(getApplicationContext(), "Error While Opening file!", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();

                }


                break;

        }
    }

    public static void applyDim(@NonNull ViewGroup parent, float dimAmount){
        Drawable dim = new ColorDrawable(Color.BLACK);
        dim.setBounds(0, 0, parent.getWidth(), parent.getHeight());
        dim.setAlpha((int) (255 * dimAmount));

        ViewGroupOverlay overlay = parent.getOverlay();
        overlay.add(dim);
    }

    public static void clearDim(@NonNull ViewGroup parent) {
        ViewGroupOverlay overlay = parent.getOverlay();
        overlay.clear();
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        (findViewById(R.id.encrypt)).setVisibility(View.VISIBLE);
        (findViewById(R.id.decrypt)).setVisibility(View.VISIBLE);
        (findViewById(R.id.videoView)).setVisibility(View.INVISIBLE);
    }

}
