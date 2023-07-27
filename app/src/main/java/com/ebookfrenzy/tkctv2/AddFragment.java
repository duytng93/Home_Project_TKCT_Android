package com.ebookfrenzy.tkctv2;

import static android.content.Context.INPUT_METHOD_SERVICE;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ebookfrenzy.tkctv2.databinding.FragmentAddBinding;
import com.google.android.material.snackbar.Snackbar;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class AddFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String currentMonthFileName;
    private String today;
    private FragmentAddBinding binding;
    private FTPClient ftpClient;
    private boolean cardsFileDownloaded = false;
    static int cardCnt = 0;
    public static String[] cardNum;
    public static String[] cardOwn;
    private boolean amtConfirmed = false;
    private static EditText commentEditText;
    private static Button button;
    public static RelativeLayout loadingPanel;
    private String purchaseToString = "";
    private boolean purSaved = false;
    View view;
    public static boolean viewNeedUpdate = false;
    public static boolean summaryNeedUpdate = false;

    public AddFragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static AddFragment newInstance(String currentMonthFileName, String today) {
        AddFragment fragment = new AddFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, currentMonthFileName);
        args.putString(ARG_PARAM2, today);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentMonthFileName = getArguments().getString(ARG_PARAM1);
            today = getArguments().getString(ARG_PARAM2);
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentAddBinding.inflate(inflater, container, false);
        view = binding.getRoot();
        //getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        view.setOnTouchListener((v, event) -> {
            InputMethodManager inputMethodManager = (InputMethodManager)getActivity().getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getApplicationWindowToken(),0);
            return false;
        });
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // create and set adapter for spinners
        String purOwnerArray[] = getResources().getStringArray(R.array.purOwner);
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(getContext(), R.layout.spinner_text_style, purOwnerArray){
            @Override
            public View getDropDownView(int position, View convertView,
                                        ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                if (position == 0) {
                    // Set the hint text color gray
                    tv.setTextColor(Color.GRAY);
                }
                return view;
            }
        };
        //ArrayAdapter<CharSequence> adapter=ArrayAdapter.createFromResource(getContext(), R.array.purOwner, R.layout.spinner_text_style);
        adapter.setDropDownViewResource(R.layout.spinner_text_style);
        binding.ownSpinner.setAdapter(adapter);
        binding.ownSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(position == 0)
                    ((TextView) parent.getChildAt(0)).setTextColor(binding.amtField.getHintTextColors());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        String storesArray[] = getResources().getStringArray(R.array.stores);
        ArrayAdapter<CharSequence> adapter2 = new ArrayAdapter<CharSequence>(getContext(), R.layout.spinner_text_style, storesArray){
            @Override
            public View getDropDownView(int position, View convertView,
                                        ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                if (position == 0) {
                    // Set the hint text color gray
                    tv.setTextColor(Color.GRAY);
                }
                return view;
            }
        };
        //ArrayAdapter<CharSequence>adapter2=ArrayAdapter.createFromResource(getContext(), R.array.stores, R.layout.spinner_text_style);
        adapter2.setDropDownViewResource(R.layout.spinner_text_style);
        binding.storeSpinner.setAdapter(adapter2);
        binding.storeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(position == 0)
                    ((TextView) parent.getChildAt(0)).setTextColor(binding.amtField.getHintTextColors());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //use in comment dialog
        commentEditText = binding.cmtEditView;
        button = binding.button;
        loadingPanel = binding.loadingPanel;

        // show loading symbol
        binding.loadingPanel.setVisibility(View.VISIBLE);
        binding.button.setEnabled(false); // button will be enabled after downloaded card file or check internet failed

        ftpClient = new FTPClient();

        if(networkConnected()){
            // download and read cards file to local storage
            downloadAndReadCardsFile();
        }
        else {
            binding.button.setEnabled(true);
            binding.loadingPanel.setVisibility(View.GONE);
            updateStatus("Internet not available. Please try to reconnect and restart the app.");
            binding.button.setText("Restart");
        }


        // set function for add purchase button
        binding.button.setOnClickListener(v -> {
            //hide virtual keyboard when button clicked
            InputMethodManager inputMethodManager = (InputMethodManager)getActivity().getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(v.getApplicationWindowToken(),0);

            updateStatus("");
            if(binding.button.getText().equals("Restart")){
                getActivity().finishAffinity();
                startActivity(getActivity().getIntent());
            }else{
                boolean amtAndCmtChecked = false; // to check in case if amt >= 50, amt need to be confirmed as well as cmt need to be input before continue the code
                boolean allGood = false; // need to be true to perform writing purchase to file

                if(!hasEnoughInfo())
                    updateStatus("Some information is missing.");
                else {  // if has enough info then check for amount confirmation, comment on large purchase
                    double amount = Double.parseDouble(binding.amtField.getText().toString());
                    if(amount < 50){  // less purchase doesn't need amount confirmation and comments
                        amtConfirmed = true;
                        amtAndCmtChecked = true;
                    }
                    else{
                        if(!amtConfirmed)   // if amount >= 50, try to confirm amt if not confirmed yet
                            showConfirmAmountAlert(amount);
                        else if("".equals(binding.cmtEditView.getText().toString()))   // try to get some comment if not comment yet
                            (new UserInputDialog()).show(getActivity().getSupportFragmentManager(),"input");
                        else // after amount confirmed and commented, amtAndCmtChecked is true
                            amtAndCmtChecked = true;
                    }

                    if(amtConfirmed && amtAndCmtChecked){  // check card number only if amount and comments are good
                        purchaseToString = purchaseToString();
                        if(purchaseToString.equals("ownNotFound"))
                            showCardNotFoundAlert();
                        else
                            allGood = true;  // if card found, then all good
                    }

                }

                if(allGood){ // if everything look good then start writing purchase input to file
                    if(networkConnected()){
                        purSaved = false;
                        uploadPurchase();
                    }
                    else{
                        updateStatus("Internet not connected");
                    }
                }
            }

        });

        binding.addCardButton.setOnClickListener((v)->{
            (new CardInputDialog()).show(getActivity().getSupportFragmentManager(),"addCard");  // show add card dialog
        });

        // set text watcher on amount field to change the amt confirmed state when the amount changed
        binding.amtField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                amtConfirmed = false;
            }  // reset confirm status after amount has changed
        });
        binding.cmtEditView.setImeActionLabel("Add\nTransaction",EditorInfo.IME_ACTION_DONE);
        binding.cmtEditView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_DONE){
                    button.performClick();
                }
                return false;
            }
        });
    }


    @Override
    public void onHiddenChanged(boolean hidden) {
        if(!hidden) {
            if(!networkConnected()){
                updateStatus("Internet not available. Please try to reconnect and restart the app.");
                binding.button.setText("Restart");
            }
        }
        super.onHiddenChanged(hidden);
    }

    public boolean networkConnected(){
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null;
    }

    public void downloadAndReadCardsFile(){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> { // a thread run this part
            try {
                ftpClient.connect(getResources().getString(R.string.server), 21);
                boolean success = ftpClient.login(getResources().getString(R.string.username), getResources().getString(R.string.password));
                if(success){
                    ftpClient.enterLocalPassiveMode();
                    ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                    String remoteCardFile = "/sda1/TKCT/cards.txt";
                    File localCardFile = new File(Environment.getExternalStorageDirectory(),"cards.txt");
                    OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(localCardFile));
                    cardsFileDownloaded = ftpClient.retrieveFile(remoteCardFile, outputStream);
                    outputStream.close();
                }

            } catch (MalformedURLException e) {
                updateError(e.toString());
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
            handler.post(this::readCardsFile);  // main thread will run this line after
        });
    }

    public void readCardsFile(){
        if(!cardsFileDownloaded){
            updateStatus("Couldn't download cards file. Restart the app might help!");
            binding.button.setText("Restart");
            binding.button.setEnabled(true);
            binding.loadingPanel.setVisibility(View.GONE);
        }
        else{
            File cardsFile = new File(Environment.getExternalStorageDirectory(), "cards.txt");
            if(cardsFile.exists()){
                //read cards file to arrays
                cardNum = new String[1000];
                cardOwn = new String[1000];
                Path filePath = Paths.get(cardsFile.getAbsolutePath());
                try {
                    Scanner readCard = new Scanner(filePath);
                    while (readCard.hasNext())  // check if there is anything to read
                    {
                        if (readCard.hasNext())  // check if next data is int
                        {
                            cardNum[cardCnt]=readCard.next();
                            cardOwn[cardCnt]= readCard.next();
                            cardCnt++;
                        }
                    }
                    binding.loadingPanel.setVisibility(View.GONE);
                    updateStatus("Ready to add transactions!");
                    readCard.close();  // close scanner
                } catch (IOException e) {
                    updateError(e.toString());
                } finally {
                    binding.button.setEnabled(true);
                }
            }
            else{
                binding.loadingPanel.setVisibility(View.GONE);
                updateStatus("Cards file not found!");
                binding.button.setEnabled(false);
            }
        }
    }

    public boolean hasEnoughInfo(){
        if ("".equals(binding.amtField.getText().toString()) || Double.parseDouble(binding.amtField.getText().toString()) == 0){
            showSimpleAlertDialog("How much was this transaction?", "Please touch the amount box and type in a positive number other than zero.");
            return false;
        }
        else if(binding.ownSpinner.getSelectedItemPosition() == 0 && "".equals(binding.cardNumberField.getText().toString())){
            showSimpleAlertDialog("Who made this transaction?", "Please choose a name from the drop down list, or type into the card field with the card numbers: last 3 digits for credit cards or last 4 digits for gift cards");
            return false;
        }
        else if(binding.storeSpinner.getSelectedItemPosition() == 0){
            showSimpleAlertDialog("Missing info", "Please choose a store, utilities, or cashback from the drop down list.");
            return false;
        }
        return true;
    }

    public void showSimpleAlertDialog(String title, String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage(message);
        builder.setTitle(title);
        builder.setCancelable(false);
        builder.setPositiveButton("OK", (dialog, which) -> {
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void showConfirmAmountAlert(double amt){  // simple alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage("Are you sure the amount was $" + amt);
        builder.setTitle("High amount transaction!");
        builder.setPositiveButton("Yes", (dialog, which) -> {
            amtConfirmed = true;
            binding.button.performClick();
        });
        builder.setNegativeButton("No", (dialog, which) -> {
            // If user click no then dialog box is canceled.
            amtConfirmed = false;
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();
    }

    public static class UserInputDialog extends DialogFragment {  // customized dialog
        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            // Get the layout inflater
            LayoutInflater inflater = requireActivity().getLayoutInflater();

            //inflate layout
            View view = inflater.inflate(R.layout.user_input, null);

            // Pass null as the parent view because its going in the dialog layout
            builder.setView(view)
                    // Add action buttons
                    .setPositiveButton("OK", (dialog, id) -> {
                        EditText comment = view.findViewById(R.id.user_input_editView);
                        commentEditText.setText(comment.getText());
                        button.performClick();
                    });
            AlertDialog dialog = builder.create();
            dialog.setCanceledOnTouchOutside(false);
            return dialog;
        }

    }

    public String purchaseToString(){
        double purAmt = Double.parseDouble(binding.amtField.getText().toString());
        String purOwn;
        if (binding.ownSpinner.getSelectedItemPosition()==0)
            purOwn = lookupOwner(binding.cardNumberField.getText().toString());
        else
            purOwn = binding.ownSpinner.getSelectedItem().toString();
        if(purOwn.equals("notFound"))
            return "ownNotFound";
        String purMer;
        switch (binding.storeSpinner.getSelectedItem().toString()) {
            case "Walmart":
                purMer = "w";
                break;
            case "Sam":
                purMer = "s";
                break;
            case "Aldi":
                purMer = "a";
                break;
            case "Costco":
                purMer = "c";
                break;
            case "Pan Asia":
                purMer = "p";
                break;
            case "Cashback":
                purMer = "cb";
                break;
            case "Sprouts":
                purMer = "sp";
                break;
            case "Utilities":
                purMer = "u";
                break;
            default:
                purMer = "o";
                break;
        }
        String purCmt;
        if("".equals(binding.cmtEditView.getText().toString()))
            purCmt = today;
        else{
            purCmt = binding.cmtEditView.getText().toString().replaceAll("\\s","_") + "_" + today;
        }

        return String.format("%7.2f %4s %2s  %s", purAmt, purOwn, purMer, purCmt);
    }

    public String lookupOwner(String owner){//(String purOwn, String cardNum[], String cardOwn[], int cardCnt) {
        boolean ownerFound = false;
        for (int i = 0 ; i < cardCnt;i++) {
            if (owner.equals(cardNum[i])) {
                owner = cardOwn[i];
                ownerFound = true;
                break;
            }
        }
        if (!ownerFound) {
            owner = "notFound";
        }
        return owner;
    }

    public void showCardNotFoundAlert(){  // simple alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage("Hint: Try to use last 3 digits for credit card and last 4 digits for gift cards. Or click add-card button add new card to file. Or select a person name instead.");
        builder.setTitle("Card owner not found!");
        builder.setPositiveButton("OK", (dialog, which) -> binding.cardNumberField.getText().clear());
        AlertDialog alertDialog = builder.create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();
    }

    public void uploadPurchase(){
        binding.loadingPanel.setVisibility(View.VISIBLE);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            String content;
            content = purchaseToString +"\n";
            try {
                ftpClient.connect(getResources().getString(R.string.server), 21);
                ftpClient.login(getResources().getString(R.string.username), getResources().getString(R.string.password));
                ftpClient.enterLocalPassiveMode();
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                String dest = "sda1/TKCT/" + currentMonthFileName;
                OutputStream out = ftpClient.appendFileStream(dest);
                out.write(content.getBytes());
                purSaved = true;
                viewNeedUpdate = true;
                summaryNeedUpdate = true;
                out.close();
            } catch(Exception e) {
                updateError(e.toString());
            }
            handler.post(() -> {
                binding.loadingPanel.setVisibility(View.GONE);
                if(purSaved){
                    updateStatus("Ready to add transactions!");
                    Snackbar.make(binding.getRoot(),"Transaction saved successfully", 4000).setAction(Html.fromHtml("<b>View</b",Html.FROM_HTML_MODE_COMPACT), v -> MainActivity.mainMenu.performIdentifierAction(MainActivity.mainMenu.getItem(1).getItemId(),0)).setActionTextColor(ContextCompat.getColor(getContext(),R.color.blue)).show();
                    binding.amtField.getText().clear();
                    binding.ownSpinner.setSelection(0);
                    binding.cardNumberField.getText().clear();
                    binding.storeSpinner.setSelection(0);
                    binding.cmtEditView.getText().clear();
                    amtConfirmed = false;
                }
                else
                    Toast.makeText(getContext(), "Failed to add transaction. Please try Add transaction again.", Toast.LENGTH_SHORT).show();
            });
        });
    }

    public static class CardInputDialog extends DialogFragment{
        Spinner spinner;
        EditText cardNumber;
        EditText cardNumber2;
        TextView helpText;
        private FTPClient ftpClient;
        String server = "ftp server name"; // ftp://betinhi.asuscomm.com don't work
        int port = 21;
        String user = "username";
        String pass = "password";

        boolean addSuccessful = false;
        Context context;

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState){
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            // Get the layout inflater
            LayoutInflater inflater = requireActivity().getLayoutInflater();

            View view = inflater.inflate(R.layout.card_input_layout, null);

            context = getActivity();
            spinner = view.findViewById(R.id.card_input_spinner);
            cardNumber = view.findViewById(R.id.card_input_editNumber);
            cardNumber2 = view.findViewById(R.id.card_input_editNumberPassword);
            button = view.findViewById(R.id.card_input_helpButton);
            helpText = view.findViewById(R.id.card_input_helpTextView);

            ArrayAdapter<CharSequence>adapter=ArrayAdapter.createFromResource(getActivity(), R.array.cardOwner, R.layout.spinner_text_style);
            adapter.setDropDownViewResource(R.layout.spinner_text_style);
            spinner.setAdapter(adapter);

            button.setOnClickListener((v)->{
                if(helpText.getVisibility() == View.GONE)
                    helpText.setVisibility(View.VISIBLE);
                else helpText.setVisibility(View.GONE);
            });

            builder.setView(view)
                    // Add action buttons
                    .setPositiveButton("Add", null)
                    .setNegativeButton("Close",null);

            AlertDialog dialog = builder.create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.setOnShowListener(dialogInterface -> {  //override positive button

                Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(view1 -> {
                    if(!validInput()){
                        Toast.makeText(context, "Add card failed. Please check your input and try again.",Toast.LENGTH_SHORT).show();
                    }
                    else{
                        boolean duplicate = false;
                        for(int i=0; i<cardCnt;i++){
                            if(cardNumber.getText().toString().equals(cardNum[i])){
                                Toast.makeText(context, "Add card failed. "+ cardNumber.getText().toString() +" has already be on the file under name: "+cardOwn[i],Toast.LENGTH_LONG).show();
                                duplicate = true;
                                break;
                            }
                        }
                        if(!duplicate){
                            loadingPanel.setVisibility(View.VISIBLE);
                            String newNum = cardNumber.getText().toString();
                            String newOwn = spinner.getSelectedItem().toString();
                            String newCard = newNum + " " + newOwn+"\n";
                            ExecutorService executor = Executors.newSingleThreadExecutor();
                            Handler handler = new Handler(Looper.getMainLooper());
                            executor.execute(() -> {
                                ftpClient = new FTPClient();
                                try {
                                    ftpClient.connect(server, port);
                                    ftpClient.login(user, pass);
                                    ftpClient.enterLocalPassiveMode();
                                    ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                                    String dest = "sda1/TKCT/cards.txt";
                                    OutputStream out = ftpClient.appendFileStream(dest);
                                    out.write(newCard.getBytes());
                                    out.close();
                                    addSuccessful = true;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                handler.post(() -> {
                                    if(addSuccessful){
                                        loadingPanel.setVisibility(View.GONE);
                                        Toast.makeText(context, "Card " + newNum +" added successfully under "+newOwn,Toast.LENGTH_SHORT).show();
                                        cardNum[cardCnt]=newNum;
                                        cardOwn[cardCnt]=newOwn;
                                        cardCnt++;
                                        spinner.setSelection(0);
                                        cardNumber.setText("");
                                        cardNumber2.setText("");
                                    }
                                    else{
                                        loadingPanel.setVisibility(View.GONE);
                                        Toast.makeText(context, "Add card failed. Some thing went wrong. Please try again later.",Toast.LENGTH_SHORT).show();
                                    }

                                });
                            });
                        }
                    }
                });
            });
            dialog.show();
            return dialog;
        }

        boolean validInput(){
            if(spinner.getSelectedItem().toString().equals("Select") || !cardNumber.getText().toString().equals(cardNumber2.getText().toString())
                    ||"".equals(cardNumber.getText().toString())||"".equals(cardNumber2.getText().toString()))
                return false;
            return true;
        }
    }

    public void updateStatus(String newStatus){
        binding.statusView.setText(newStatus);
    }
    public void updateError(String newError) {binding.errorView.setText(binding.errorView.getText() + "\n" + newError);}
}