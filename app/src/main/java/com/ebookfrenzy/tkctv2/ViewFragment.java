package com.ebookfrenzy.tkctv2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.ebookfrenzy.tkctv2.databinding.FragmentViewBinding;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ViewFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ViewFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    //private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String currentMonthFileName;
    //private String mParam2;
    private FragmentViewBinding binding;
    private int lineNum = 0;
    private FTPClient ftpClient;
    private double[] purAmtArray = new double[100];
    private String[] purOwnArray = new String[100];
    private String[] purMerArray = new String[100];
    private String[] purCmtArray = new String[100];
    float x1, x2;
    private boolean firstRun = true;
    public ViewFragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static ViewFragment newInstance(String currentMonthFileName) {
        ViewFragment fragment = new ViewFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, currentMonthFileName);
        //args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentMonthFileName = getArguments().getString(ARG_PARAM1);
            //mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentViewBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ftpClient = new FTPClient();

        binding.deleteButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMessage("Are you sure to delete the last transaction? It can't be undone.");
            builder.setTitle("Warning!");
            builder.setCancelable(false);
            builder.setPositiveButton("Yes", (dialog, which) -> {
                Toast.makeText(getContext(), "Deleting...",Toast.LENGTH_SHORT).show();
                deleteLastLine(); // delete and re-read the file
            });
            builder.setNegativeButton("No", (dialog, which) -> {
                // If user click no then dialog box is canceled.
            });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        });
    }


    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(!hidden){
            if (networkConnected()) {
                if(firstRun){
                    binding.loadingPanel.setVisibility(View.VISIBLE);
                    readCurrentMonthFile();
                    firstRun = false;
                    AddFragment.viewNeedUpdate = false;
                }else{
                    if(AddFragment.viewNeedUpdate){
                        binding.loadingPanel.setVisibility(View.VISIBLE);
                        readCurrentMonthFile();
                        AddFragment.viewNeedUpdate = false;
                    }
                }
            } else{
                binding.loadingPanel.setVisibility(View.GONE);
                updateError("Internet not available. Please reconnect and then restart the app.");
                binding.deleteButton.setEnabled(false);
            }
        }
    }

    public boolean networkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null)
            return true;
        else
            return false;
    }

    // delete last line of current month record file
    public void deleteLastLine(){
        binding.loadingPanel.setVisibility(View.VISIBLE);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                ftpClient.connect(getResources().getString(R.string.server), 21);
                boolean success = ftpClient.login(getResources().getString(R.string.username), getResources().getString(R.string.password));
                if (success) {
                    ftpClient.enterLocalPassiveMode();
                    ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                    String dest = "sda1/TKCT/" + currentMonthFileName;
                    OutputStream out = ftpClient.storeFileStream(dest);

                    for (int i = 0; i < lineNum-1; i++) {
                        String purchase = String.format("%7.2f %4s %2s  %s\n", purAmtArray[i], purOwnArray[i], purMerArray[i], purCmtArray[i]);
                        out.write(purchase.getBytes());
                    }
                    out.close();
                    //read.close();
                }
            } catch (IOException e) {
                updateError(e.toString());
            }finally {
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
                readCurrentMonthFile();
                AddFragment.summaryNeedUpdate = true;
            });
        });
    }

    // read file function
    public void readCurrentMonthFile(){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        binding.deleteButton.setEnabled(false);
        binding.loadingPanel.setVisibility(View.VISIBLE);
        executor.execute(() -> {  // background execution
            try {
                lineNum = 0;
                ftpClient.connect(getResources().getString(R.string.server), 21);
                boolean success = ftpClient.login(getResources().getString(R.string.username), getResources().getString(R.string.password));
                if (success) {
                    ftpClient.enterLocalPassiveMode();
                    ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                    String source = "/sda1/TKCT/" + currentMonthFileName;
                    Scanner scan = new Scanner(ftpClient.retrieveFileStream(source));
                    while (scan.hasNext()) {
                        purAmtArray[lineNum] = scan.nextDouble();
                        purOwnArray[lineNum] = scan.next();
                        purMerArray[lineNum] = scan.next();
                        purCmtArray[lineNum] = scan.next();
                        lineNum += 1;
                    }
                    scan.close();
                }
            } catch (IOException e) {
                updateError(e.toString());
            } catch(NullPointerException e1){
                // in case new record is not created yet
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
            handler.post(() -> { //invoked on the UI thread after the background computation finishes.
                binding.linearLayout.removeAllViews();
                if(lineNum >0){
                    for(int i=0;i<lineNum;i++){
                        addPurchasesToLinearLayout(i,purAmtArray[i],purOwnArray[i],purMerArray[i],purCmtArray[i]);
                    }
                    binding.deleteButton.setEnabled(true);
                    binding.errorView2.setText("");
                }
                else{
                    //addPurchasesToLinearLayout(0,0.0,"","Other","Nothing to view");
                    TextView noTrans = new TextView(getContext());
                    noTrans.setText("Nothing to view");
                    noTrans.setTextSize(24);
                    noTrans.setTypeface(null, Typeface.ITALIC);
                    binding.linearLayout.addView(noTrans);
                    binding.deleteButton.setEnabled(false);
                }
                binding.loadingPanel.setVisibility(View.GONE);
            });
        });
    }

    @SuppressLint("ResourceType")
    public void addPurchasesToLinearLayout(int line, double purAmt, String purOwn, String purMer, String purCmt){
        switch (purMer){
            case "w":
                purMer = "Walmart";
                break;
            case "a":
                purMer = "Aldi";
                break;
            case "s":
                purMer = "Sam's Club";
                break;
            case "p":
                purMer = "Pan Asia";
                break;
            case "c":
                purMer = "Costco";
                break;
            case "sp":
                purMer = "Sprouts";
                break;
            case "u":
                purMer = "Utilities";
                break;
            case "cb":
                purMer = "Cashback";
                break;
            default:
                purMer ="Other";
        }

        if(line == lineNum-1){
            purMer+=" (latest)";
        }
        ConstraintLayout childConstraint = new ConstraintLayout(getContext());
        childConstraint.setId(1);

        ImageButton childButton = new ImageButton(getContext());
        childButton.setImageResource(R.drawable.ic_arrow_down2);
        childButton.setBackgroundColor(Color.TRANSPARENT);
        childButton.setId(2);
        childButton.setOnClickListener(v -> {
            TextView own = (TextView) childConstraint.getChildAt(3);
            TextView cmt = (TextView) childConstraint.getChildAt(4);
            if(own.getVisibility()==View.GONE){
                own.setVisibility(View.VISIBLE);
                cmt.setVisibility(View.VISIBLE);
                childButton.setImageResource(R.drawable.ic_arrow_up2);
            }
            else{
                own.setVisibility(View.GONE);
                cmt.setVisibility(View.GONE);
                childButton.setImageResource(R.drawable.ic_arrow_down2);
            }
        });

        TextView amtTextView = new TextView(getContext());
        amtTextView.setId(3);
        amtTextView.setText("$"+purAmt);
        //amtTextView.setTextColor(Color.WHITE);

        if(purAmt>=70){
            amtTextView.setTypeface(null,Typeface.ITALIC);
            amtTextView.setTextSize(30);
            amtTextView.setTextColor(Color.RED);
        }
        else if(purAmt>=40){
            amtTextView.setTypeface(null,Typeface.ITALIC);
            amtTextView.setTextSize(25);
            amtTextView.setTextColor(Color.rgb(255,204,0));
        }
        else{
            amtTextView.setTextSize(20);
            amtTextView.setTextColor(Color.rgb(153,204,51));
        }


        TextView merTextView = new TextView(getContext());
        merTextView.setId(4);
        merTextView.setText(purMer);
        //merTextView.setTextColor(Color.WHITE);
        merTextView.setTypeface(null, Typeface.ITALIC);
        merTextView.setTextSize(14);

        TextView ownTextView = new TextView(getContext());
        ownTextView.setId(5);
        ownTextView.setText(purOwn);
        //ownTextView.setTextColor(Color.WHITE);
        ownTextView.setTypeface(null, Typeface.BOLD);
        ownTextView.setTextSize(20);


        TextView cmtTextView = new TextView(getContext());
        cmtTextView.setId(6);
        // cmtTextView.setTextColor(Color.WHITE);
        cmtTextView.setText(purCmt);


        ConstraintSet childSet = new ConstraintSet();
        childSet.constrainHeight(childConstraint.getId(),ConstraintSet.WRAP_CONTENT);
        childSet.constrainWidth(childConstraint.getId(),ConstraintSet.WRAP_CONTENT);

        childSet.constrainHeight(childButton.getId(), ConstraintSet.WRAP_CONTENT);
        childSet.constrainWidth(childButton.getId(), ConstraintSet.WRAP_CONTENT);
        childSet.connect(childButton.getId(),ConstraintSet.END,childConstraint.getId(),ConstraintSet.END,0);
        childSet.connect(childButton.getId(),ConstraintSet.TOP,merTextView.getId(),ConstraintSet.TOP,5);

        childSet.constrainHeight(amtTextView.getId(), ConstraintSet.WRAP_CONTENT);
        childSet.constrainWidth(amtTextView.getId(), ConstraintSet.WRAP_CONTENT);
        childSet.connect(amtTextView.getId(),ConstraintSet.TOP,merTextView.getId(),ConstraintSet.TOP,5);
        childSet.connect(amtTextView.getId(),ConstraintSet.END,childConstraint.getId(),ConstraintSet.END,250);

        childSet.constrainHeight(merTextView.getId(), ConstraintSet.WRAP_CONTENT);
        childSet.constrainWidth(merTextView.getId(), ConstraintSet.WRAP_CONTENT);
        childSet.connect(merTextView.getId(),ConstraintSet.START,childConstraint.getId(),ConstraintSet.START,0);
        childSet.connect(merTextView.getId(),ConstraintSet.TOP,merTextView.getId(),ConstraintSet.TOP,5);

        childSet.constrainHeight(ownTextView.getId(), ConstraintSet.WRAP_CONTENT);
        childSet.constrainWidth(ownTextView.getId(), ConstraintSet.MATCH_CONSTRAINT);
        childSet.connect(ownTextView.getId(),ConstraintSet.START,childConstraint.getId(),ConstraintSet.START,0);
        childSet.connect(ownTextView.getId(),ConstraintSet.END,childConstraint.getId(),ConstraintSet.END,0);
        childSet.connect(ownTextView.getId(),ConstraintSet.TOP,merTextView.getId(),ConstraintSet.BOTTOM,5);

        childSet.constrainHeight(cmtTextView.getId(), 70);
        childSet.constrainWidth(cmtTextView.getId(), ConstraintSet.MATCH_CONSTRAINT);
        childSet.connect(cmtTextView.getId(),ConstraintSet.START,childConstraint.getId(),ConstraintSet.START,0);
        childSet.connect(cmtTextView.getId(),ConstraintSet.END,childConstraint.getId(),ConstraintSet.END,0);
        childSet.connect(cmtTextView.getId(),ConstraintSet.TOP,ownTextView.getId(),ConstraintSet.BOTTOM,5);

        View v = new View(getContext());
        v.setId(7);
        v.setBackgroundColor(Color.parseColor("#FF039BE5"));
        childSet.constrainHeight(v.getId(),5);
        childSet.constrainWidth(v.getId(),ConstraintSet.MATCH_CONSTRAINT);
        childSet.connect(v.getId(),ConstraintSet.START,childConstraint.getId(), ConstraintSet.START,0);
        childSet.connect(v.getId(),ConstraintSet.END,childConstraint.getId(), ConstraintSet.END,0);
        childSet.connect(v.getId(),ConstraintSet.BOTTOM, childConstraint.getId(),ConstraintSet.BOTTOM,5);

        childConstraint.addView(childButton);
        childConstraint.addView(amtTextView);
        childConstraint.addView(merTextView);
        childConstraint.addView(ownTextView);
        childConstraint.addView(cmtTextView);
        childConstraint.addView(v);
        childSet.applyTo(childConstraint);
        childButton.performClick();
        binding.linearLayout.addView(childConstraint,0);

        childConstraint.setOnTouchListener((v1, event) -> {
            switch (event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    x1 = event.getX();
                    break;
                case MotionEvent.ACTION_UP:
                    x2 = event.getX();
                    if(x2-x1 <15)
                        childButton.performClick();
                    break;
            }
            return true; // consumed the event not pass it further
        });
    }

    public void updateError(String newError) {
        binding.errorView2.setText(binding.errorView2.getText() + "\n" + newError);
    }
}