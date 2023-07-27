package com.ebookfrenzy.tkctv2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
//import androidx.fragment.app.FragmentTransaction;

import android.app.Dialog;
import android.content.Context;
//import android.content.DialogInterface;
//import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
//import android.os.PersistableBundle;
import android.provider.Settings;
import android.util.Log;
//import android.view.LayoutInflater;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
//import android.view.inputmethod.InputMethodManager;
//import android.widget.TextView;
//import android.widget.LinearLayout;
import android.widget.Toast;

import com.ebookfrenzy.tkctv2.databinding.ActivityMainBinding;
import com.ebookfrenzy.tkctv2.databinding.LoadingMessageBinding;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
//import java.net.MalformedURLException;
//import java.net.SocketException;
import java.time.LocalDate;
import java.time.Month;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    String latest_version = "";

    String current_version = "2.8.5";  //
    /*
    * Auto check for update
    * dialog show there is a new update
    * view and summary only run when purchases record change
    * add pie chart in summary, add total dollar, pie chart animation
    * delete this month of last year record
    * write last month total to total_history file
    * add bar chart for totals
    * add detail zoom in out button, show balance
    * add downloading dialog when download update
    * check connection -> check last month total -> write last month total -> check for update -> initialize fragments
    * combine exit dialog, connecting dialog, downloading dialog to loading dialog
    * add help feature
    * fix left right button in summary
    * reduce space between action bar items
    * show exit icon in sub menu
    * use text input layout in add fragment
    * modify color of first spinner item
    * snack bar, transaction color
    * manifest android:windowSoftInputMode="stateVisible|adjustResize"
    * add function to enter button when click on comment view
    * add floating detail, zoom buttons
    * auto scroll when expand or collapse details
    * */

    /* crash when switch between dark and light*/

    FTPClient ftpClient;
    boolean apkDownloaded = false;
    private String today, currentMonthFileName, lastMonthFileName, lastMonth;

    int monthNum, year;
    public static Menu mainMenu;
    private FragmentManager fragmentManager;

    public static AddFragment addFragment; // to call and show it inside other fragment class code
    public static ViewFragment viewFragment;
    public static SummaryFragment summaryFragment;

    public Fragment[] fragments;

    int curPos=0;
    float x1, x2;
    static final int min_distance = 100;
    View view;

    boolean swipeLeft;
    boolean noSwipe = true;

    private ActivityMainBinding binding;

    private long pressedTime;

    private double purTotal;

    private boolean serverConnected = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        view = binding.getRoot();
        setContentView(view);
        fragmentManager = getSupportFragmentManager();
        ftpClient = new FTPClient();
        // get manage storage permission
//        Thread permissionThread = new Thread(() -> {
            if (Build.VERSION.SDK_INT >= 30){
                if (!Environment.isExternalStorageManager()){
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("This application needs full access of the device storage.");
                    builder.setTitle("Storage permission required!");
                    builder.setPositiveButton("OK", (dialog, which) -> {
                        Intent getPermission = new Intent();
                        getPermission.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                        startActivity(getPermission);
                    });
                    builder.setNegativeButton("Cancel", (dialog, which) -> {
                        MainActivity.this.finishAffinity();
                        System.exit(0);
                    });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.setCanceledOnTouchOutside(false);
                    alertDialog.show();

                }
            }
//        });
//        permissionThread.start();
//        try {
//            permissionThread.join();
//        } catch (InterruptedException e) {
//            Log.d("Error", e.toString());
//        }



        //get current month file name
        LocalDate currentDate = LocalDate.now();
        int day = currentDate.getDayOfMonth();
        Month month = currentDate.getMonth();
        monthNum = month.getValue();
        year = currentDate.getYear() - 2000;
        today = monthNum + "/" + day + "/" + year; //today is use as default comment
        currentMonthFileName = monthNum + "_" +year +".txt";

        //get last month file name
        if(monthNum==1)
            lastMonth = "12_"+(year-1);
        else
            lastMonth = (monthNum-1)+"_"+year;
        lastMonthFileName = lastMonth+".txt";
        lastMonth = lastMonth.replace("_","/");

        //set up tool bar
        setTitle(month + " " + currentDate.getYear());
        ColorDrawable colorDrawable
                = new ColorDrawable(Color.parseColor("#FF039BE5"));
        getSupportActionBar().setBackgroundDrawable(colorDrawable);

        binding.restart.setOnClickListener((v)->{
            MainActivity.this.finishAffinity();
            startActivity(MainActivity.this.getIntent());
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if(Environment.isExternalStorageManager()){
                LoadingDialog connectingDialog = new LoadingDialog("  Connecting to the server...", 20);
                connectingDialog.show(getSupportFragmentManager(),"Connecting");

                ExecutorService executor = Executors.newSingleThreadExecutor();
                Handler handler = new Handler(Looper.getMainLooper());
                executor.execute(() -> {
                    boolean success = checkLastMonthTotal();
                    if(serverConnected){  // serverConnected is checked in side checkLastMonthTotal()
                        connectingDialog.dismiss();
                        if(!success) // if last month total not found in the total history file then
                            handler.post(this::writeLastMonthTotal); // calculate last month total and write to the total history file (check for update included inside)
                        else handler.post(this::checkNewUpdate); // if last month total found then only check for update. If no update found, then fragments will be initialized
                    }else handler.post(()->{
                        connectingDialog.dismiss();
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setMessage("Couldn't connect to the server. Please check your connection or wait a few second then click Restart.");
                        builder.setTitle("Connection Failed!");
                        builder.setPositiveButton("Restart", (dialog, which) -> {
                            MainActivity.this.finishAffinity();
                            startActivity(MainActivity.this.getIntent());
                        });
                        AlertDialog alertDialog = builder.create();
                        alertDialog.setCanceledOnTouchOutside(false);
                        alertDialog.show();

                    });
                });
            }
            else{
                binding.loadingPanel.setVisibility(View.GONE);
                binding.restart.setVisibility(View.VISIBLE);
            }
        }


    }

    private boolean checkLastMonthTotal(){
        boolean totalExist= false;
        try {
            ftpClient.connect(getResources().getString(R.string.server), 21);
            boolean success = ftpClient.login(getResources().getString(R.string.username), getResources().getString(R.string.password));
            if (success) {
                serverConnected = true;
                ftpClient.enterLocalPassiveMode();
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                String source = "/sda1/TKCT/total_history.txt";

                // delete this month record of last year
                ftpClient.deleteFile("/sda1/TKCT/"+monthNum+"_"+(year-1)+".txt");

                InputStream inputStream = ftpClient.retrieveFileStream(source);
                Scanner scan = new Scanner(inputStream);

                while (scan.hasNext()) {
                    if(scan.next().equals(lastMonth)) {
                        totalExist = true;
                        break;
                    }
                }
                scan.close();
            }
            else {
                serverConnected = false;
            }
        } catch (IOException e) {
            Log.d("checkLastMonthTotal:", e.toString());
        }
        finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return totalExist;
    }

    private void writeLastMonthTotal(){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            try {
                // read and calculate total
                ftpClient.connect(getResources().getString(R.string.server), 21);
                boolean success = ftpClient.login(getResources().getString(R.string.username), getResources().getString(R.string.password));
                if (success) {
                    ftpClient.enterLocalPassiveMode();
                    ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                    String source = "/sda1/TKCT/"+lastMonthFileName;
                    Scanner scan = new Scanner(ftpClient.retrieveFileStream(source));
                    while(scan.hasNext()){
                        double amt = scan.nextDouble();
                        scan.next();
                        if(!scan.next().equals("cb"))
                            purTotal += amt;
                        scan.next();
                    }
                    scan.close();
                }

                // write total to total_history file
                FTPClient ftpClient1 = new FTPClient();
                ftpClient1.connect(getResources().getString(R.string.server),21);
                if (ftpClient1.login(getResources().getString(R.string.username), getResources().getString(R.string.password))) {
                    ftpClient1.enterLocalPassiveMode();
                    ftpClient1.setFileType(FTP.BINARY_FILE_TYPE);
                    String source = "/sda1/TKCT/total_history.txt";
                    String content = String.format("%s %.2f%n",lastMonth,purTotal);
                    OutputStream out = ftpClient1.appendFileStream(source);
                    out.write(content.getBytes());
                    out.close();
                }
                if(ftpClient1.isConnected())
                {
                    ftpClient1.logout();
                    ftpClient1.disconnect();
                }
            } catch (IOException e) {
                Log.d("writeLastMonthTotal:", e.toString());
            } catch(InputMismatchException e1){// when the asterisk exist, the read loop will run into mismatch exception and prevent writing total
            }
            finally {
                try {
                    if (ftpClient.isConnected()) {
                        ftpClient.logout();
                        ftpClient.disconnect();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            handler.post(this::checkNewUpdate);
        });
    }

    private void checkNewUpdate(){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {  // background execution
            try {
                ftpClient.connect(getResources().getString(R.string.server), 21);
                boolean success = ftpClient.login(getResources().getString(R.string.username), getResources().getString(R.string.password));
                if (success) {
                    ftpClient.enterLocalPassiveMode();
                    ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                    String source = "/sda1/TKCT/latest_version.txt";
                    Scanner scan = new Scanner(ftpClient.retrieveFileStream(source));
                    while (scan.hasNext()) {
                        latest_version = scan.next();
                    }
                    scan.close();
                }
            } catch (IOException | NullPointerException e) {
                Log.d("checkNewUpdate:", e.toString());
            } finally {
                try {
                    if (ftpClient.isConnected()) {
                        ftpClient.logout();
                        ftpClient.disconnect();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            handler.post(()->{
                if(!latest_version.equals("") && !current_version.equals(latest_version)){
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("Click OK to download and install new update.");
                    builder.setTitle("Update needed!");
                    builder.setPositiveButton("OK", (dialog, which) -> downloadAndInstallUpdate());
                    AlertDialog alertDialog = builder.create();
                    alertDialog.setCanceledOnTouchOutside(false);
                    alertDialog.show();
                }
                else{
                    // initialize fragments
                    addFragment = AddFragment.newInstance(currentMonthFileName,today);
                    viewFragment = ViewFragment.newInstance(currentMonthFileName);
                    summaryFragment = SummaryFragment.newInstance();

                    fragments = new Fragment[]{addFragment, viewFragment, summaryFragment};
                    binding.container.removeAllViews();
                    fragmentManager.beginTransaction()
                            .add(R.id.container, addFragment).add(R.id.container, viewFragment).add(R.id.container,summaryFragment)
                            .hide(viewFragment).hide(summaryFragment)
                            .commitNow();

                }
            });
        });
    }

    private void downloadAndInstallUpdate(){
       // (new ExitDialog()).show(getSupportFragmentManager(),"Exit");
        LoadingDialog downloadingDialog = new LoadingDialog("  Downloading...",28);
        downloadingDialog.show(getSupportFragmentManager(),"Downloading");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> { // a thread run this part
            try {
                ftpClient.connect(getResources().getString(R.string.server), 21);
                boolean success = ftpClient.login(getResources().getString(R.string.username), getResources().getString(R.string.password));
                if(success){
                    ftpClient.enterLocalPassiveMode();
                    ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                    String apkFile = "/sda1/TKCT/app-debug.apk";
                    File downloadedApkFile = new File(Environment.getExternalStorageDirectory(),"app-debug.apk");
                    OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(downloadedApkFile));
                    apkDownloaded = ftpClient.retrieveFile(apkFile, outputStream);
                    outputStream.close();
                }

            } catch (IOException e) {
                Log.d("downloadAndInstallUpdate", e.toString());
            } finally {
                try {
                    if (ftpClient.isConnected()) {
                        ftpClient.logout();
                        ftpClient.disconnect();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            handler.post(() -> {
                downloadingDialog.dismiss();
                if(apkDownloaded){
                    installUpdate();
                }else{
                    Toast.makeText(MainActivity.this, "Couldn't download apk file. Try again later.", Toast.LENGTH_SHORT).show();
                }
            });  // main thread will run this line after
        });
    }

    private void installUpdate() {
        try{
            Context context = MainActivity.this;
            File downloadedAPK = new File(Environment.getExternalStorageDirectory(),"app-debug.apk");
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri downloaded_apk = FileProvider.getUriForFile(context, getApplicationContext().getPackageName()+".provider",downloadedAPK);
            intent.setDataAndType(downloaded_apk, "application/vnd.android.package-archive");
            List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                context.grantUriPermission(context.getApplicationContext().getPackageName() + ".provider", downloaded_apk, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        }catch(Exception e){
            Log.d("Install update error:", e.toString());
        }

    }

    // attach menu to toolbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        mainMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.add){
            mainMenu.findItem(R.id.view).setIcon(R.drawable.ic_view);
            mainMenu.findItem(R.id.summary).setIcon(R.drawable.ic_summary);
            item.setIcon(R.drawable.ic_add_clicked);
            curPos = 0;
            if(noSwipe){
                fragmentManager.beginTransaction().setCustomAnimations(R.anim.left_enter, R.anim.right_exit, R.anim.pop_enter, R.anim.pop_exit)
                        .show(addFragment).hide(viewFragment).hide(summaryFragment)
                        .commitNow();
            }else{
                if(!swipeLeft){
                    fragmentManager.beginTransaction().setCustomAnimations(R.anim.left_enter, R.anim.right_exit, R.anim.pop_enter, R.anim.pop_exit)
                            .show(addFragment).hide(viewFragment).hide(summaryFragment)
                            .commitNow();
                }
                else{
                    fragmentManager.beginTransaction().setCustomAnimations(R.anim.right_enter, R.anim.left_exit, R.anim.pop_enter, R.anim.pop_exit)
                            .show(addFragment).hide(viewFragment).hide(summaryFragment)
                            .commitNow();
                }
            }

            return true;
        }
        else if(item.getItemId() == R.id.view){
            item.setIcon(R.drawable.ic_view_clicked);
            mainMenu.findItem(R.id.add).setIcon(R.drawable.ic_add);
            mainMenu.findItem(R.id.summary).setIcon(R.drawable.ic_summary);
            curPos = 1;
            if(addFragment.isVisible()){
                fragmentManager.beginTransaction().setCustomAnimations(R.anim.right_enter, R.anim.left_exit, R.anim.pop_enter, R.anim.pop_exit)
                        .show(viewFragment).hide(addFragment).hide(summaryFragment)
                        .commitNow();
            }
            else{
                fragmentManager.beginTransaction().setCustomAnimations(R.anim.left_enter, R.anim.right_exit, R.anim.pop_enter, R.anim.pop_exit)
                        .show(viewFragment).hide(addFragment).hide(summaryFragment)
                        .commitNow();
            }
            return true;
        }
        else if(item.getItemId() == R.id.summary){
            item.setIcon(R.drawable.ic_summary_clicked);
            mainMenu.findItem(R.id.add).setIcon(R.drawable.ic_add);
            mainMenu.findItem(R.id.view).setIcon(R.drawable.ic_view);
            curPos = 2;
            if(noSwipe){
                fragmentManager.beginTransaction().setCustomAnimations(R.anim.right_enter, R.anim.left_exit, R.anim.pop_enter, R.anim.pop_exit)
                        .show(summaryFragment).hide(addFragment).hide(viewFragment)
                        .commitNow();
            }else{
                if(swipeLeft){
                    fragmentManager.beginTransaction().setCustomAnimations(R.anim.right_enter, R.anim.left_exit, R.anim.pop_enter, R.anim.pop_exit)
                            .show(summaryFragment).hide(addFragment).hide(viewFragment)
                            .commitNow();
                }
                else{
                    fragmentManager.beginTransaction().setCustomAnimations(R.anim.left_enter, R.anim.right_exit, R.anim.pop_enter, R.anim.pop_exit)
                            .show(summaryFragment).hide(addFragment).hide(viewFragment)
                            .commitNow();
                }
            }
            return true;
        }
        else if(item.getItemId() == R.id.exit){
            //(new ExitDialog()).show(getSupportFragmentManager(),"Exit");
            LoadingDialog exitingDialog = new LoadingDialog("   Exiting....", 28);
            exitingDialog.show(getSupportFragmentManager(), "Exit");
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());
            executor.execute(()->{
                try {
                    Thread.sleep(600);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                handler.post(()->{
                    MainActivity.this.finishAffinity();
                    System.exit(0);
                });
            });
            //(new HelpDialog("add")).show(getSupportFragmentManager(),"Add_help");
            return true;
        }
        else if(item.getItemId() == R.id.help){
            if(curPos == 0)
                (new HelpDialog("add")).show(getSupportFragmentManager(),"Add_help");
            else if(curPos == 1)
                (new HelpDialog("view")).show(getSupportFragmentManager(),"View_help");
            else if(curPos == 2)
                (new HelpDialog("summary")).show(getSupportFragmentManager(),"Summary_help");
            return true;
        }
        else
            return super.onOptionsItemSelected(item);
    }

    public static class HelpDialog extends DialogFragment {

        private String layout;

        public HelpDialog(String layout){
            this.layout = layout;
        }
        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            // Get the layout inflater
            LayoutInflater inflater = requireActivity().getLayoutInflater();

            if(layout.equals("add"))
                builder.setView(inflater.inflate(R.layout.add_help, null));
            else if(layout.equals("view"))
                builder.setView(inflater.inflate(R.layout.view_help, null));
            else if(layout.equals("summary"))
                builder.setView(inflater.inflate(R.layout.summary_help, null));

            builder.setNegativeButton("OK", (dialog, which) -> {
                //close dialog
            });
            AlertDialog dialog = builder.create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
            return dialog;
        }
    }

    public static class LoadingDialog extends DialogFragment {

        private final String message;
        private final int size;
        public LoadingDialog(String msg, int size){
            message = msg;
            this.size = size;
        }

        LoadingMessageBinding binding;

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            // Get the layout inflater
            binding = LoadingMessageBinding.inflate(getLayoutInflater());
            builder.setView(binding.getRoot());
            binding.message.setText(message);
            binding.message.setTextSize(size);
            AlertDialog dialog = builder.create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
            return dialog;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        //if(event.getY()>= maxY*0.85){
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                x2 = event.getX();
                float deltaX = Math.abs(x2 - x1);
                if(x2 > x1 && deltaX >= min_distance){ // swipe right
                    noSwipe = false;
                    if(curPos == 0)
                        curPos = 3;
                    swipeLeft = false;
                    mainMenu.performIdentifierAction(mainMenu.getItem(curPos-1).getItemId(),0);
                    noSwipe = true;
                }
                if( x1 > x2 && deltaX >=min_distance){ // swipe left
                    noSwipe = false;
                    if(curPos == 2)
                        curPos = -1;
                    swipeLeft= true;
                    mainMenu.performIdentifierAction(mainMenu.getItem(curPos+1).getItemId(),0);
                    noSwipe = true;
                }

                break;
        }
        //  }

        return super.onTouchEvent(event);
    }

    @Override
    public void onBackPressed() {
        if(pressedTime+2000>System.currentTimeMillis()){
            mainMenu.performIdentifierAction(R.id.exit,0);
        }else{
            Toast.makeText(getBaseContext(), "Press back again to exit", Toast.LENGTH_SHORT).show();
        }
        pressedTime = System.currentTimeMillis();
    }

}