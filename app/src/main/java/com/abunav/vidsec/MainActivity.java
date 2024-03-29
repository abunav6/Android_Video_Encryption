package com.abunav.vidsec;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroupOverlay;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final int VIDEO_SELECT_CODE = 0;
    private static final int TEXT_SELECT_CODE = 1;
    private static final int DECRYPT_FROM_JSON = 2;
    private static final int KEY_SELECT_CODE = 3;
    private String file_name, key;
    private String email;
    private String downloadURL;
    private String RSAEncryptedAESKey;
    private ArrayList<String> users;
    private ArrayList<String> public_keys;
    private View progressView;
    private PopupWindow progressWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();
        Window w = getWindow();
        w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);




        findViewById(R.id.logout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Toast.makeText(getApplicationContext(), "You have successfully logged out!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            email = user.getEmail();
            findViewById(R.id.signedInAs).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.signedInAs)).setText(email);
        }


        File dir = new File("/storage/emulated/0/VideoEncryptorFiles");
        dir.mkdirs();


        (findViewById(R.id.encrypt)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("video/mp4");
                intent.addCategory(Intent.CATEGORY_OPENABLE);

                checkReadAndWritePermissions();

                try {
                    startActivityForResult(
                            Intent.createChooser(intent, "Select a File to Upload"), VIDEO_SELECT_CODE
                    );
                } catch (android.content.ActivityNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });


        (findViewById(R.id.decrypt)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("text/plain");
                intent.addCategory(Intent.CATEGORY_OPENABLE);

                checkReadAndWritePermissions();

                try {
                    startActivityForResult(
                            Intent.createChooser(intent, "Select a File to upload"), TEXT_SELECT_CODE
                    );
                } catch (android.content.ActivityNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }

            }
        });

        findViewById(R.id.decryptFromJSON).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("application/octet-stream");
                intent.addCategory(Intent.CATEGORY_OPENABLE);

                checkReadAndWritePermissions();
                try {
                    startActivityForResult(
                            Intent.createChooser(intent, "Choose the JSON File you want to decrypt"), DECRYPT_FROM_JSON
                    );
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
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
                encryptVideoFile(data, resultCode);
                break;


            case TEXT_SELECT_CODE:
                decryptFromTextFile(data);
                break;


            case DECRYPT_FROM_JSON:
                decryptFromJSON(data);
                break;

            case KEY_SELECT_CODE:
                getPrivateKeyAndDecrypt(data);
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
        (findViewById(R.id.encrypt)).setVisibility(View.VISIBLE);
        (findViewById(R.id.decrypt)).setVisibility(View.VISIBLE);
        (findViewById(R.id.videoView)).setVisibility(View.INVISIBLE);
    }

    public void getDecryptedKey(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/plain");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        checkReadAndWritePermissions();

        try{
            startActivityForResult(
                    Intent.createChooser(intent, "Select your Private Key File"), KEY_SELECT_CODE
            );
        }catch (android.content.ActivityNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void decryptFromTextFile(Intent data){
        try {
            LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
            final View popupView = inflater.inflate(R.layout.decrypt, null);

            final int width = ViewGroup.LayoutParams.WRAP_CONTENT;
            final int height = ViewGroup.LayoutParams.WRAP_CONTENT;
            final boolean focusable = true;
            final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);
            popupWindow.setOutsideTouchable(false);

            final ViewGroup root = (ViewGroup) getWindow().getDecorView().getRootView();

            popupWindow.showAtLocation(findViewById(R.id.main_activity), Gravity.CENTER, 0, 0);
            applyDim(root, 0.75f);

            ((EditText) popupView.findViewById(R.id.key2)).setHintTextColor(Color.LTGRAY);
            ((EditText) popupView.findViewById(R.id.key2)).setTextColor(Color.WHITE);

            (popupView.findViewById(R.id.submit2)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final long startTime = System.currentTimeMillis();
                    key = ((TextView) popupView.findViewById(R.id.key2)).getText().toString();
                    popupWindow.dismiss();
                    Uri uri = data.getData();
                    String uri_path = "storage/emulated/0/".concat(uri.getPath().split(":")[1]);
                    int code = decryptAndSave(uri_path, key);
                    if (code == 0) giveDecryptionStats(root, startTime);
                }
            });


            popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    clearDim(root);
                }
            });

        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();

        }


    }

    public void encryptVideoFile(Intent data, int resultCode){

        if (resultCode == RESULT_OK) {
            Uri uri = data.getData();
            Log.i("Size", String.valueOf(new File(uri.getPath()).length()));
            boolean f = true;
            FileInputStream importdb;
            File file;
            try {
                importdb = new FileInputStream(getContentResolver().openFileDescriptor(uri, "r").getFileDescriptor());
                file = new File(uri.getPath());
            } catch (Exception e) {
                e.printStackTrace();
                f = false;
                file = null;
                importdb = null;
                Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
            if (f) {
                ByteArrayOutputStream buffer;
                Log.i("input", file.getAbsolutePath());
                buffer = new ByteArrayOutputStream();
                int nRead;
                f = true;
                try {

                    byte[] bytes = new byte[100000];
                    while ((nRead = importdb.read(bytes, 0, bytes.length)) != -1) {
                        buffer.write(bytes, 0, nRead);
                    }
                    //byte[] bytes = FileUtils.readFileToByteArray(file);
                } catch (Exception e) {
                    e.printStackTrace();
                    f = false;
                    Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
                if (f) {
                    final String encoded = new String(buffer.toByteArray(), StandardCharsets.ISO_8859_1);
                    try {
                        buffer.close();
                    } catch (IOException e) {
                        Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }


                    final LayoutInflater inflater = (LayoutInflater)
                            getSystemService(LAYOUT_INFLATER_SERVICE);
                    final View popupView = inflater.inflate(R.layout.save_encrypted, null);

                    final int width = LinearLayout.LayoutParams.WRAP_CONTENT;
                    final int height = LinearLayout.LayoutParams.WRAP_CONTENT;
                    final boolean focusable = true;
                    final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);
                    ((EditText) popupView.findViewById(R.id.file_name)).setHintTextColor(Color.LTGRAY);
                    ((EditText) popupView.findViewById(R.id.file_name)).setTextColor(Color.WHITE);
                    ((EditText) popupView.findViewById(R.id.key)).setHintTextColor(Color.LTGRAY);
                    ((EditText) popupView.findViewById(R.id.key)).setTextColor(Color.WHITE);


                    popupWindow.setOutsideTouchable(false);

                    final ViewGroup root = (ViewGroup) getWindow().getDecorView().getRootView();

                    popupWindow.showAtLocation(findViewById(R.id.main_activity), Gravity.CENTER, 0, 0);
                    applyDim(root, 0.75f);


                    (popupView.findViewById(R.id.submit)).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            final long startTime = System.currentTimeMillis();
                            file_name = ((TextView) popupView.findViewById(R.id.file_name)).getText().toString();
                            key = ((TextView) popupView.findViewById(R.id.key)).getText().toString();

                            final File file;
                            boolean flag = true;
                            FileOutputStream text_encoded;
                            try {
                                file = new File(String.format("/storage/emulated/0/VideoEncryptorFiles/%s_encoded.txt", file_name));
                                FileProvider.getUriForFile(MainActivity.this, MainActivity.this.getPackageName() + ".provider", file);
                                text_encoded = new FileOutputStream(file);
                            } catch (Exception e) {
                                e.printStackTrace();
                                flag = false;
                                text_encoded = null;
                                Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            }

                            String encrypted = AES.encrypt(encoded, key);
                            if (flag) {
                                boolean temp = true;
                                try {
                                    text_encoded.write(encrypted != null ? encrypted.getBytes(StandardCharsets.ISO_8859_1) : new byte[0]);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    temp = false;
                                    Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                }
                                if (temp) {
                                    long endTime = System.currentTimeMillis();
                                    long elapsed = endTime - startTime;

                                    LayoutInflater inf = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                                    final View pv = inf.inflate(R.layout.decrypted_desc, null);
                                    final PopupWindow pw = new PopupWindow(pv, width, height, focusable);
                                    pw.setOutsideTouchable(false);
                                    pw.showAtLocation(findViewById(R.id.main_activity), Gravity.CENTER, 0, 0);
                                    applyDim(root, 0.75f);

                                    ((TextView) pv.findViewById(R.id.wrote)).setText(R.string.encoded_wrote);
                                    ((TextView) pv.findViewById(R.id.wrote)).setTypeface(null, Typeface.BOLD);
                                    ((TextView) pv.findViewById(R.id.wrote)).setTextSize(26);
                                    ((TextView) pv.findViewById(R.id.time)).setTypeface(null, Typeface.BOLD);
                                    ((TextView) pv.findViewById(R.id.time)).setTextSize(18);

                                    ((TextView) pv.findViewById(R.id.time)).setText(String.format("It took %s milliseconds to write to %s.txt!", elapsed, file_name));

                                    pv.findViewById(R.id.close).setOnClickListener(view12 -> pw.dismiss());


                                    pv.findViewById(R.id.give_access).setOnClickListener(v -> {
                                        try {

                                            users = new ArrayList<>();
                                            public_keys = new ArrayList<>();

                                            Toast.makeText(getApplicationContext(), "Please wait while the file is uploaded", Toast.LENGTH_LONG).show();
                                            String file_path = String.format("storage/emulated/0/VideoEncryptorFiles/%s_encoded.txt", file_name);
                                            StorageReference storageReference = FirebaseStorage.getInstance().getReference();
                                            StorageReference encryptedTextFile = storageReference.child(String.format("%s/%s", email, file_name));
                                            final UploadTask uploadTask = encryptedTextFile.putFile(Uri.fromFile(new File(file_path)));


                                            LayoutInflater inflater1 = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                                            progressView = inflater1.inflate(R.layout.progress, null);
                                            progressWindow = new PopupWindow(progressView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true);
                                            progressWindow.setOutsideTouchable(true);
                                            progressWindow.showAtLocation(findViewById(R.id.main_activity), Gravity.CENTER, 0, 0);

                                            uploadTask.addOnFailureListener(e -> Toast.makeText(getApplicationContext(), "Could not upload: " + e.getMessage(), Toast.LENGTH_LONG).show()).addOnSuccessListener(taskSnapshot -> Toast.makeText(getApplicationContext(), "Successfully uploaded File!", Toast.LENGTH_SHORT).show()
                                            ).addOnCompleteListener(task -> {
                                                if (task.isSuccessful()) {
                                                    encryptedTextFile.getDownloadUrl().addOnSuccessListener(uri1 -> {
                                                        pw.dismiss();
                                                        downloadURL = uri1.toString();
                                                        progressView.setVisibility(View.INVISIBLE);
                                                        progressWindow.dismiss();
                                                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Users");
                                                        LayoutInflater inf1 = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                                                        final View view1 = inf1.inflate(R.layout.user_list, null);
                                                        final PopupWindow popupWindow1 = new PopupWindow(view1, width, height, focusable);
                                                        popupWindow1.setOutsideTouchable(true);
                                                        popupWindow1.showAtLocation(findViewById(R.id.main_activity), Gravity.CENTER, 0, 0);
                                                        applyDim(root, 0.75f);
                                                        ListView list_of_users = view1.findViewById(R.id.list_of_users);
                                                        popupWindow1.setOnDismissListener(() -> clearDim(root));

                                                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.white_list_text, users);
                                                        list_of_users.setAdapter(adapter);

                                                        reference.addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot snapshot) {

                                                                for (DataSnapshot snap : snapshot.getChildren()) {
                                                                    String user = snap.getKey();
                                                                    if (user.replace(",", ".").equals(email)) {
                                                                        continue;
                                                                    }
                                                                    users.add(user.replace(",", "."));
                                                                    public_keys.add(String.valueOf(snap.getValue()));
                                                                    Log.i("Added User", user);
                                                                }
                                                                Toast.makeText(getApplicationContext(), "Choose one user from this list to which the JSON File will be sent", Toast.LENGTH_LONG).show();
                                                                adapter.notifyDataSetChanged();

                                                                list_of_users.setOnItemClickListener((parent, view2, position, id) -> {
                                                                    writeJSONFile(key, position);
                                                                });

                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError error) {

                                                            }
                                                        });

                                                    });
                                                }
                                            }).addOnProgressListener(taskSnapshot -> {
                                                Long uploaded = taskSnapshot.getBytesTransferred();
                                                Long total = taskSnapshot.getTotalByteCount();
                                                Log.i("Uploaded", String.valueOf(uploaded));
                                                Log.i("Total", String.valueOf(total));

                                                showProgress(taskSnapshot, root, popupWindow);
                                            });

                                        } catch (Exception e) {
                                            Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                                            e.printStackTrace();
                                        }

                                    });


                                    pv.findViewById(R.id.view_file).setOnClickListener(view13 -> {
                                        pw.dismiss();
                                        LayoutInflater i = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                                        final View p1 = i.inflate(R.layout.encoded, null);
                                        final PopupWindow p2 = new PopupWindow(p1, width, height, focusable);
                                        p2.setOutsideTouchable(true);
                                        Toast.makeText(getApplicationContext(), "Wait while the encoded text is loaded, this may take a while", Toast.LENGTH_LONG).show();
                                        ((TextView) p1.findViewById(R.id.encoded_text)).setText(encoded);
                                        ((TextView) p1.findViewById(R.id.title)).setText(String.format("%s_encoded.txt", file_name));
                                        ((TextView) p1.findViewById(R.id.title)).setTextColor(Color.WHITE);
                                        ((TextView) p1.findViewById(R.id.title)).setTypeface(null, Typeface.BOLD);
                                        ((TextView) p1.findViewById(R.id.encoded_text)).setTextColor(Color.WHITE);
                                        ((TextView) p1.findViewById(R.id.encoded_text)).setMovementMethod(new ScrollingMovementMethod());
                                        p2.showAtLocation(findViewById(R.id.main_activity), Gravity.CENTER, 0, 0);
                                        applyDim(root, 0.75f);

                                        p2.setOnDismissListener(() -> clearDim(root));


                                    });

                                    pw.setOnDismissListener(() -> clearDim(root));
                                }
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

                }

            }
        }
    }

    public int decryptAndSave(String uri_path, String decryptKey){
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        FileInputStream fis;
        try {
            File file = new File(uri_path);
            fis = new FileInputStream(file);
            file_name = file.getName().split("_")[0];
            Log.i("File_name", file_name);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            fis = null;
            e.printStackTrace();
        }
        if (fis != null) {
            byte[] temp = new byte[100000];
            int nRead;
            try {
                while ((nRead = fis.read(temp, 0, temp.length)) != -1) {
                    buffer.write(temp, 0, nRead);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                return 1;
            }
            temp = buffer.toByteArray();
            AES aes = new AES();
            String to_decrypt = new String(temp, StandardCharsets.ISO_8859_1);
            String decrypted;
            try {
                decrypted = aes.decrypt(to_decrypt, decryptKey);
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                return 1;
            }
            byte[] d_bytes = decrypted.getBytes(StandardCharsets.ISO_8859_1);
            File out;
            OutputStream os;
            try {
                out = new File(String.format("/storage/emulated/0/VideoEncryptorFiles/%s_decrypted.mp4", file_name));
                FileProvider.getUriForFile(MainActivity.this, MainActivity.this.getPackageName() + ".provider", out);
                Log.i("Video", "Success");
                Log.i("output", out.getAbsolutePath());
                os = new FileOutputStream(out);
                os.write(d_bytes);
                Toast.makeText(getApplicationContext(), "Successfully Saved the decrypted Video file", Toast.LENGTH_LONG).show();
                os.close();
                return 0;
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                return 1;
            }
        }
        return 1;
    }

    public void checkReadAndWritePermissions(){
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

    }

    public void giveDecryptionStats(ViewGroup root, long startTime){
        long endTime = System.currentTimeMillis();
        long elapsed = endTime - startTime;
        LayoutInflater inf = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        final View pv = inf.inflate(R.layout.decrypted_desc, null);
        final PopupWindow pw = new PopupWindow(pv, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true);
        pw.setOutsideTouchable(false);
        pw.showAtLocation(findViewById(R.id.main_activity), Gravity.CENTER, 0, 0);
        applyDim(root, 0.75f);


        ((TextView) pv.findViewById(R.id.view_file)).setText(R.string.play_video);
        ((TextView) pv.findViewById(R.id.wrote)).setText(R.string.wrote_vid);
        ((TextView) pv.findViewById(R.id.wrote)).setTypeface(null, Typeface.BOLD);
        ((TextView) pv.findViewById(R.id.wrote)).setTextSize(26);
        ((TextView) pv.findViewById(R.id.time)).setTypeface(null, Typeface.BOLD);
        ((TextView) pv.findViewById(R.id.time)).setTextSize(18);

        ((TextView) pv.findViewById(R.id.time)).setText(String.format("It took %s milliseconds to decrypt and make %s_decrypted.mp4!", elapsed, file_name));

        pv.findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pw.dismiss();
            }
        });

        pv.findViewById(R.id.give_access).setVisibility(View.INVISIBLE);


        pv.findViewById(R.id.view_file).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pw.dismiss();
                videoPlay(file_name);
            }
        });


        pw.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                clearDim(root);
            }
        });

    }

    public void showProgress(UploadTask.TaskSnapshot taskSnapshot, ViewGroup root, PopupWindow popupWindow){
        /*
            shows the progress while UPLOADING the text file to Firebase Storage
        */

        Log.i("Uploaded", String.valueOf(taskSnapshot.getBytesTransferred()));
        Log.i("Total", String.valueOf(taskSnapshot.getTotalByteCount()));

        long uploaded = 0L;
        long total = taskSnapshot.getTotalByteCount();

        double perc;


        applyDim(root, 0.75f);

        uploaded = taskSnapshot.getBytesTransferred();
        perc = Math.round(100.0* (100.0 * uploaded/total))/100.0;
        ((TextView) progressView.findViewById(R.id.upload_status)).setText(String.format("Upload Status : %s%%", perc));
        ((ProgressBar) progressView.findViewById(R.id.progressBar)).setProgress((int) Math.round(perc));

        popupWindow.setOnDismissListener(() -> clearDim(root));
    }

    public void showProgress(FileDownloadTask.TaskSnapshot taskSnapshot, ViewGroup root, PopupWindow popupWindow){

        /*
            Gives the progress Status while downloading the text file from firebase storage
        */


        Log.i("Uploaded", String.valueOf(taskSnapshot.getBytesTransferred()));
        Log.i("Total", String.valueOf(taskSnapshot.getTotalByteCount()));

        applyDim(root, 0.75f);

        long uploaded;
        long total = taskSnapshot.getTotalByteCount();
        double perc;

        uploaded = taskSnapshot.getBytesTransferred();
        perc = Math.round(100.0* (100.0 * uploaded/total))/100.0;
        Log.i("Percentage ", String.valueOf(perc)+"%");
        ((TextView) progressView.findViewById(R.id.upload_status)).setText(String.format("Download Status : %s%%", perc));
        ((ProgressBar) progressView.findViewById(R.id.progressBar)).setProgress((int) Math.round(perc));
        popupWindow.setOnDismissListener(() -> clearDim(root));
    }


    public void writeJSONFile(String key, int position){
        try {
            String senderEmailID = email;
            String encryptedAESKey = Base64.getEncoder().encodeToString(RSA.encrypt(key, public_keys.get(position)));
            Log.i("Receiver's Public Key ", public_keys.get(position));
            Log.i("RSA Encrypted AES Key", encryptedAESKey);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("download-url", downloadURL);
            jsonObject.put("sender-emailID", senderEmailID);
            jsonObject.put("encrypted-AES-key", encryptedAESKey);
            jsonObject.put("comment", "In order to decrypt this, simply choose this JSON file under the \"Decrypt from JSON\" option on the app's main screen");
            try {
                String jsonFilePath = String.format("storage/emulated/0/VideoEncryptorFiles/JSON/%s/", email);
                File dir = new File(jsonFilePath);
                if (!dir.exists())
                    dir.mkdirs();
                FileWriter file1 = new FileWriter(jsonFilePath + file_name + ".json");
                file1.write(jsonObject.toString());
                Toast.makeText(getApplicationContext(), "Successfully Wrote JSON File", Toast.LENGTH_SHORT).show();
                file1.close();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "Could not write the JSON file data: "+e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Could not Encrypt the AES Key :"+e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void decryptFromJSON(Intent data){
        Uri jsonFileURI = data.getData();
        String uri_path = "storage/emulated/0/".concat(jsonFileURI.getPath().split(":")[1]);
        Log.i("Path", uri_path);
        JsonParser parser = new JsonParser();

        try (FileReader reader = new FileReader(uri_path)) {
            Object obj = parser.parse(reader);
            JsonObject receivedEncryptedVideo = (JsonObject) obj;
            downloadURL = receivedEncryptedVideo.get("download-url").toString();
            RSAEncryptedAESKey = receivedEncryptedVideo.get("encrypted-AES-key").toString();
            String senderEmailID = receivedEncryptedVideo.get("sender-emailID").toString();
            Toast.makeText(getApplicationContext(), "Choose your private key from VideoEncryptorFiles folder", Toast.LENGTH_LONG).show();
            getDecryptedKey();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public String getFileName(String url){
        final String regex = "2F(.*)\\?";
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(url);
        String temp;
        if(matcher.find()){
            temp = matcher.group(0);
            return temp.substring(2,temp.length()-1);
        }
        return null;
    }

    public void getPrivateKeyAndDecrypt(Intent data){
        Uri privateKeyFile = null;
        try {
            privateKeyFile = data.getData();
        }catch (Exception e){
            Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
        String privateKeyPath = "storage/emulated/0/".concat(privateKeyFile.getPath().split(":")[1]);
        Log.i("pvt key path", privateKeyPath);
        try {
            BufferedReader br = new BufferedReader(new FileReader(privateKeyPath));
            StringBuilder stringBuilder = new StringBuilder();
            String line = null;
            String ls = System.getProperty("line.separator");
            while((line = br.readLine()) != null){
                stringBuilder.append(line);
                stringBuilder.append(ls);
            }
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            br.close();

            String fileContents = stringBuilder.toString();



            LayoutInflater i = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
            final View getKeyView = i.inflate(R.layout.get_pvt_key, null);
            final PopupWindow getKeyPopup = new PopupWindow(getKeyView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true);
            getKeyPopup.setOutsideTouchable(true);
            ViewGroup root = (ViewGroup) getWindow().getDecorView().getRootView();
            getKeyPopup.showAtLocation(findViewById(R.id.main_activity), Gravity.CENTER, 0, 0);
            applyDim(root, 0.75f);

            getKeyPopup.setOnDismissListener(() -> clearDim(root));

            getKeyView.findViewById(R.id.getPvtKey).setOnClickListener(v -> {
                String password = ((EditText)getKeyView.findViewById(R.id.loginpassword)).getText().toString();
                Log.i("PWD", password);
                Log.i("Encrypted PVT Key", fileContents);
                try{
                    String decryptedPrivateKey = AES.decrypt(fileContents, password);
                    Log.i("Used Private Key", decryptedPrivateKey);
                    String AESKey = RSA.decrypt(RSAEncryptedAESKey, decryptedPrivateKey);
                    Log.i("Download URL", downloadURL);
                    Log.i("AES Key", AESKey);

                    StorageReference storage = FirebaseStorage.getInstance().getReferenceFromUrl(downloadURL.substring(1,downloadURL.length()-1));
                    String fileName = getFileName(downloadURL);

                    String savePath = String.format("storage/emulated/0/VideoEncryptorFiles/%s_fromJSON.txt", fileName);
                    File downloadedTextFile = new File(savePath);

                    LayoutInflater inflater1 = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                    progressView = inflater1.inflate(R.layout.progress, null);
                    progressWindow = new PopupWindow(progressView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true);
                    progressWindow.setOutsideTouchable(true);
                    progressWindow.showAtLocation(findViewById(R.id.main_activity), Gravity.CENTER, 0, 0);


                    storage.getFile(downloadedTextFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                            Toast.makeText(getApplicationContext(), "The file has been downloaded, decryption will begin now", Toast.LENGTH_LONG).show();
                            int code = decryptAndSave(savePath, AESKey);
                            if (code==0){
                                progressView.setVisibility(View.INVISIBLE);
                                progressWindow.dismiss();
                                getKeyPopup.dismiss();
                                videoPlay(fileName);
                            }
                        }
                    }).addOnFailureListener(e -> {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), "Could not download File: "+e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }).addOnProgressListener(taskSnapshot -> {
                        showProgress(taskSnapshot, root, getKeyPopup);
                    });

                }catch (Exception e){
                    Toast.makeText(getApplicationContext(),e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }

    }

    public void videoPlay(String file_name){
        final VideoView videoView = findViewById(R.id.videoView);
        Uri u = null;
        boolean f = true;

        try {
            u = Uri.parse(String.format("/storage/emulated/0/VideoEncryptorFiles/%s_decrypted.mp4", file_name));
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            f = false;
        }
        if (f) {
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

        }
    }
}