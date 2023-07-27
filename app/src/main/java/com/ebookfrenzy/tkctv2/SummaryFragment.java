package com.ebookfrenzy.tkctv2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ebookfrenzy.tkctv2.databinding.FragmentSummaryBinding;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.eazegraph.lib.models.BarModel;
import org.eazegraph.lib.models.PieModel;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collections;
import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SummaryFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SummaryFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

    // TODO: Rename and change types of parameters
    private String selectedMonthFileName ;
    //private String mParam2;
    private FragmentSummaryBinding binding;
    private String summaryContent="";

    private int monthNum;

    private FTPClient ftpClient;

    private boolean firstRun = true;

    float ratio = 1.0f;

    double totalAldi, totalSam, totalSprouts, totalWalmart, totalCostco, totalOther, totalUtilities, totalPanAsia, Total, beBalance, tiBalance, nhiBalance;
    public SummaryFragment() {
        // Required empty public constructor
    }


    // TODO: Rename and change types and number of parameters
    public static SummaryFragment newInstance() {
        SummaryFragment fragment = new SummaryFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSummaryBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ftpClient = new FTPClient();
        // set horizontal scroll for textview
        binding.sumTextView.setMovementMethod(new ScrollingMovementMethod());
        binding.sumTextView.setHorizontallyScrolling(true);
        binding.sumTextView.setScrollbarFadingEnabled(false);
        binding.sumTextView.setTextSize(ratio + 14);

        // get date information
        LocalDate currentDate = LocalDate.now();
        Month month = currentDate.getMonth();
        monthNum = month.getValue();
        binding.textView6.setText("/ "+currentDate.getYear());

        // setup spinner
        ArrayAdapter<CharSequence> adapter=ArrayAdapter.createFromResource(getContext(), R.array.months, R.layout.spinner_text_style);
        adapter.setDropDownViewResource(R.layout.spinner_text_style);
        binding.monthSpinner.setAdapter(adapter);
        binding.monthSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                calculateSum(position, binding.textView6.getText().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        binding.leftButton.setOnClickListener((v)->{
            if(binding.monthSpinner.getSelectedItemPosition() == 0){
                int year = Integer.parseInt(binding.textView6.getText().toString().substring(2));
                binding.textView6.setText("/ "+ (year - 1));
                binding.monthSpinner.setSelection(11);
            }
            else if(binding.monthSpinner.getSelectedItemPosition()-1>=0)
                binding.monthSpinner.setSelection(binding.monthSpinner.getSelectedItemPosition()-1);
        });

        binding.rightButton.setOnClickListener((v)->{
            if(binding.monthSpinner.getSelectedItemPosition() == 11){
                int year = Integer.parseInt(binding.textView6.getText().toString().substring(2));
                binding.textView6.setText("/ "+ (year + 1));
                binding.monthSpinner.setSelection(0);
            }
            else if(binding.monthSpinner.getSelectedItemPosition()+1<=11)
            binding.monthSpinner.setSelection(binding.monthSpinner.getSelectedItemPosition()+1);
        });

        binding.zoomInButtonFloat.setOnClickListener((v)->{
            float textSize = binding.sumTextView.getTextSize(); // textSize in pixel(px)
            float textSizeSp = textSize / getResources().getDisplayMetrics().scaledDensity; //convert px to sp
            binding.sumTextView.setTextSize(textSizeSp+2);
        });

        binding.zoomOutButtonFloat.setOnClickListener((v)->{
            float textSizeSp = binding.sumTextView.getTextSize() / getResources().getDisplayMetrics().scaledDensity; //convert px to sp
            binding.sumTextView.setTextSize(textSizeSp-2);
        });

        binding.detailButtonFloat.setOnClickListener((v)->{
            Resources res = getResources();
            Drawable arrow_up = ResourcesCompat.getDrawable(res, R.drawable.ic_arrow_up2, null);
            Drawable arrow_down = ResourcesCompat.getDrawable(res, R.drawable.ic_arrow_down2, null);
            if(binding.sumTextView.getVisibility()==View.GONE) {
                binding.zoomOutButtonFloat.show();
                binding.zoomInButtonFloat.show();
                binding.sumTextView.setVisibility(View.VISIBLE);
                binding.detailButtonFloat.setIcon(arrow_up);
                //binding.sumTextView.setText(summaryContent);
                binding.scrollView.post(() -> {   // need to use post. or else it won't go down after make sumTextView Visible from Gone
                    binding.scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                });

            }
            else
            {
                binding.zoomOutButtonFloat.hide();
                binding.zoomInButtonFloat.hide();
                binding.sumTextView.setVisibility(View.GONE);
                binding.detailButtonFloat.setIcon(arrow_down);
            }
        });
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(!hidden){
            if(firstRun) {
                binding.monthSpinner.setSelection(monthNum - 1);
                firstRun = false;
                AddFragment.summaryNeedUpdate = false;
            }else{
                int selectedItem = Integer.parseInt(binding.monthSpinner.getSelectedItem().toString());
                if(selectedItem == monthNum && AddFragment.summaryNeedUpdate){
                    calculateSum(selectedItem-1, binding.textView6.getText().toString()); // update any changes of current month after resume. if selected item is not current month, no need to update
                    AddFragment.summaryNeedUpdate = false;
                }
            }
        }
    }

    private void calculateSum(int position, String yearStr){
        int year = Integer.parseInt(yearStr.substring(4));
        selectedMonthFileName = (position + 1) +"_"+year+".txt";
        //display summary
        if(networkConnected()){
            binding.loadingPanel.setVisibility(View.VISIBLE);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(() -> {
                summaryContent ="";
                calculateSummary(selectedMonthFileName);
                handler.post(() -> {
                    binding.errorView3.setText("");
                    binding.loadingPanel.setVisibility(View.GONE);
                    binding.sumTextView.setText(summaryContent);
                    binding.aldi.setText(String.format("Aldi: $%.2f",totalAldi));
                    binding.walmart.setText(String.format("Walmart: $%.2f",totalWalmart));
                    binding.sam.setText(String.format("Sam's Club: $%.2f",totalSam));
                    binding.costco.setText(String.format("Costco: $%.2f",totalCostco));
                    binding.sprouts.setText(String.format("Sprouts: $%.2f",totalSprouts));
                    binding.utilities.setText(String.format("Utilities: $%.2f",totalUtilities));
                    binding.other.setText(String.format("Other: $%.2f",totalOther));
                    binding.panAsia.setText(String.format("Pan Asia: $%.2f",totalPanAsia));
                    binding.piechart.startAnimation();
                    drawBarChart(selectedMonthFileName);
                    setBalance();
                });
            });
        }
        else
            updateError("Internet not available. Please reconnect and then restart the app.");
    }

    private void drawBarChart(String monthFileName){
        binding.barchart.clearChart();
        String moNum,yr;
        moNum = monthFileName.substring(0,monthFileName.indexOf('_'));
        yr = monthFileName.substring(monthFileName.indexOf('_')+1,monthFileName.indexOf('.'));

        final String firstMonth = moNum+"/"+yr;
        String secondMonth = previousMonth(firstMonth);
        String thirdMonth = previousMonth(secondMonth);
        String fourthMonth = previousMonth(thirdMonth);
        String fifthMonth = previousMonth(fourthMonth);
        String sixthMonth = previousMonth(fifthMonth);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        FTPClient ftpClient1 = new FTPClient();
        executor.execute(() -> {
            try {
                ftpClient1.connect(getResources().getString(R.string.server), 21);
                boolean success = ftpClient1.login(getResources().getString(R.string.username), getResources().getString(R.string.password));
                if (success) {
                    ftpClient1.enterLocalPassiveMode();
                    ftpClient1.setFileType(FTP.BINARY_FILE_TYPE);
                    String source = "/sda1/TKCT/total_history.txt";
                    Scanner scan = new Scanner(ftpClient1.retrieveFileStream(source));
                    while (scan.hasNext()) {
                        String value = scan.next();
                        if(value.equals(sixthMonth))
                            binding.barchart.addBar(new BarModel(sixthMonth, scan.nextFloat(), 0xFFA9A9A9));
                        else if(value.equals(fifthMonth))
                            binding.barchart.addBar(new BarModel(fifthMonth,scan.nextFloat(), 0xFFA9A9A9));
                        else if(value.equals(fourthMonth))
                            binding.barchart.addBar(new BarModel(fourthMonth,scan.nextFloat(), 0xFFA9A9A9));
                        else if(value.equals(thirdMonth))
                            binding.barchart.addBar(new BarModel(thirdMonth,scan.nextFloat(), 0xFFA9A9A9));
                        else if(value.equals(secondMonth))
                            binding.barchart.addBar(new BarModel(secondMonth,scan.nextFloat(), 0xFFA9A9A9));
                        else if(value.equals(firstMonth))
                            binding.barchart.addBar(new BarModel(firstMonth,scan.nextFloat(), 0xFFA9A9A9));
                    }
                    scan.close();
                }
            } catch (IOException | NullPointerException e) {
                Log.d("drawBarChart:", e.toString());
            } finally {
                try {
                    if (ftpClient1.isConnected()) {
                        ftpClient1.logout();
                        ftpClient1.disconnect();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            handler.post(()->{
                LocalDate currentDate = LocalDate.now();
                Month month = currentDate.getMonth();
                if(Integer.parseInt(moNum) == month.getValue())
                    binding.barchart.addBar(new BarModel(firstMonth,(float)Total, 0xFF039BE5));
                binding.barchart.startAnimation();
            });
        });
    }

    private String previousMonth(String curMonthYear){
        String curMonth = curMonthYear.substring(0,curMonthYear.indexOf('/'));
        String curYear = curMonthYear.substring(curMonthYear.indexOf('/')+1);
        if(Integer.parseInt(curMonth)==1)
            return "12/"+(Integer.parseInt(curYear)-1);
        else
            return (Integer.parseInt(curMonth) - 1) +"/"+curYear;
    }

    private void setBalance(){
        binding.beBalance.setText(String.format("$ %.2f",beBalance));
        setBalanceColor(beBalance,binding.beBalance);
        binding.tiBalance.setText(String.format("$ %.2f",tiBalance));
        setBalanceColor(tiBalance,binding.tiBalance);
        binding.nhiBalance.setText(String.format("$ %.2f",nhiBalance));
        setBalanceColor(nhiBalance,binding.nhiBalance);
    }

    private void setBalanceColor(double amt, TextView t){
        if(amt>=0)
            t.setTextColor(Color.RED);
        else t.setTextColor(Color.GREEN);
    }

    public boolean networkConnected(){
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) {
            return true;
        } else {
            return false;
        }
    }

    @SuppressLint("DefaultLocale")
    public void calculateSummary(String selectedMonthFileName){
        ArrayList<Purchase> bePurchase = new ArrayList<>();
        ArrayList<Purchase> tiPurchase = new ArrayList<>();
        ArrayList<Purchase> nhiPurchase = new ArrayList<>();
        ArrayList<Purchase> hieuPurchase = new ArrayList<>();
        ArrayList<Purchase> baPurchase = new ArrayList<>();
        ArrayList<Purchase> cashBack = new ArrayList<>();
        double purAmt;
        String purOwn, purMer, purCmt;
        int peoNum = 5;
        double avg;
        double total = 0, cashback = 0, totalN = 0, totalT = 0, totalB = 0, totalH = 0, totalBa = 0,
                totalW=0, totalC=0, totalS=0, totalA=0, totalO=0, totalP=0, totalSp=0, totalU=0;

        try {
            ftpClient.connect(getResources().getString(R.string.server), 21);
            boolean success = ftpClient.login(getResources().getString(R.string.username), getResources().getString(R.string.password));
            if(success){
                ftpClient.enterLocalPassiveMode();
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                String source = "/sda1/TKCT/" + selectedMonthFileName;
                Scanner scanner = new Scanner(ftpClient.retrieveFileStream(source));
                while (scanner.hasNext())  // check if there is anything to read
                {
                    purAmt = scanner.nextDouble();
                    purOwn = scanner.next();
                    purMer = scanner.next();
                    purCmt = scanner.next();
                    if (!purMer.equals("cb"))
                    {
                        total += purAmt;
                        switch(purOwn)
                        {
                            case "nhi":
                                totalN += purAmt;
                                nhiPurchase.add(new Purchase(purAmt,purOwn,purMer,purCmt));
                                break;
                            case "ti":
                                totalT += purAmt;
                                tiPurchase.add(new Purchase(purAmt,purOwn,purMer,purCmt));
                                break;
                            case "be":
                                totalB += purAmt;
                                bePurchase.add(new Purchase(purAmt,purOwn,purMer,purCmt));
                                break;
                            case "ba":
                                totalBa += purAmt;
                                baPurchase.add(new Purchase(purAmt,purOwn,purMer,purCmt));
                                break;
                            case "hieu":
                                totalH += purAmt;
                                hieuPurchase.add(new Purchase(purAmt,purOwn,purMer,purCmt));
                                break;
                        }
                    }
                    else
                    {
                        cashback += purAmt;
                        switch(purOwn)
                        {
                            case "nhi":
                                totalN -= purAmt;
                                cashBack.add(new Purchase(purAmt,purOwn,purMer,purCmt));
                                break;
                            case "ti":
                                totalT -= purAmt;
                                cashBack.add(new Purchase(purAmt,purOwn,purMer,purCmt));
                                break;
                            case "be":
                                totalB -= purAmt;
                                cashBack.add(new Purchase(purAmt,purOwn,purMer,purCmt));
                                break;
                            case "ba":
                                totalBa -= purAmt;
                                cashBack.add(new Purchase(purAmt,purOwn,purMer,purCmt));
                                break;
                            case "hieu":
                                totalH -= purAmt;
                                cashBack.add(new Purchase(purAmt,purOwn,purMer,purCmt));
                                break;
                        }
                    }

                    switch(purMer)
                    {
                        case "w":
                            totalW += purAmt;
                            break;
                        case "c":
                            totalC += purAmt;
                            break;
                        case "s":
                            totalS += purAmt;
                            break;
                        case "o":
                            totalO += purAmt;
                            break;
                        case "a":
                            totalA += purAmt;
                            break;
                        case "sp":
                            totalSp += purAmt;
                            break;
                        case "u":
                            totalU += purAmt;
                            break;
                        case "p":
                            totalP += purAmt;
                            break;
                    }
                }
                scanner.close();  // close scanner
            }

        } catch (IOException e) {
            updateError(e.toString());
        } catch (InputMismatchException e1){
            // to stop scanner if the next data is not double
        } catch (NullPointerException e2){
            summaryContent = "There is no transaction file \n for the selected month.\n\n";
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

        Collections.sort(tiPurchase,Collections.reverseOrder());
        Collections.sort(nhiPurchase,Collections.reverseOrder());
        Collections.sort(bePurchase,Collections.reverseOrder());
        Collections.sort(baPurchase,Collections.reverseOrder());
        Collections.sort(hieuPurchase,Collections.reverseOrder());
        Collections.sort(cashBack,Collections.reverseOrder());
        if (!bePurchase.isEmpty())
        {
            summaryContent +="     Be:\n";
            for (Purchase i: bePurchase)
                summaryContent +="    "+ i +"\n";
        }
        if(!hieuPurchase.isEmpty())
        {
            summaryContent +="   Hieu:\n";
            for (Purchase i: hieuPurchase)
                summaryContent +="    "+ i +"\n";
        }
        if(!tiPurchase.isEmpty())
        {
            summaryContent +="     Ti:\n";
            for (Purchase i: tiPurchase)
                summaryContent +="    "+ i +"\n";
        }
        if(!nhiPurchase.isEmpty())
        {
            summaryContent +="    Nhi:\n";
            for (Purchase i: nhiPurchase)
                summaryContent +="    "+ i +"\n";
        }
        if(!baPurchase.isEmpty())
        {
            summaryContent +="     Ba:\n";
            for (Purchase i: baPurchase)
                summaryContent +="    "+ i +"\n";
        }
        if(!cashBack.isEmpty()){
            summaryContent +="Cashback:\n";
            for (Purchase i: cashBack)
                summaryContent +="    "+ i +"\n";
        }
        summaryContent += "\n----------------------------------------------------------------------------\n\n";
        summaryContent += String.format("Total expenses:   %8.2f (Aldi %.2f%%, Pan Asia %.2f%%, Costco %.2f%%, Sprouts %.2f%%)%n", total, totalA/total*100, totalP/total*100, totalC/total*100,totalSp/total*100);
        summaryContent += String.format(" House monthly: +   750.00 (Walmart %.2f%%, Sam %.2f%%, Utilities %.2f%%, Other %.2f%%)%n",totalW/total*100,totalS/total*100,totalU/total*100,totalO/total*100);
        summaryContent += String.format("     Cash back: - %8.2f%n", cashback);
        summaryContent +=               "                ---------\n";
        summaryContent += String.format("         Total:   %8.2f%n", total + 750 - cashback);
        avg = (total + 750 - cashback) / peoNum;
        summaryContent += String.format("   AVG for one:   %8.2f%n",avg);
        summaryContent +=               "=====================================================================\n";
        summaryContent +=               "           PAID     DEBT\n";

        beBalance = avg * 2 - totalB - totalH - 750 + (avg - totalBa) / 3;
        tiBalance = avg - totalT + (avg - totalBa) / 3;
        nhiBalance = avg - totalN + (avg - totalBa) / 3;

        summaryContent += String.format("Hieu+Be:%8.2f &%8.2f (= %.2f x 2 - %.2f + %.2f)%n",totalB + totalH + 750,beBalance,avg,totalB + 750, (avg - totalBa) / 3);
        summaryContent+=" --------------------------------------------------------------------\n";
        summaryContent += String.format("     Ti:%8.2f &%8.2f (= %.2f - %.2f + %.2f)%n",totalT,tiBalance,avg,totalT, (avg - totalBa) / 3);
        summaryContent+=" --------------------------------------------------------------------\n";
        summaryContent += String.format("    Nhi:%8.2f &%8.2f (= %.2f - %.2f + %.2f)%n",totalN,nhiBalance,avg,totalN, (avg - totalBa) / 3);
        summaryContent+=" --------------------------------------------------------------------\n";
        summaryContent += String.format("     Ba:%8.2f &%8.2f (= %.2f - %.2f) (/3 = %.2f)%n",totalBa,avg - totalBa,avg,totalBa, (avg - totalBa) / 3);

        binding.piechart.clearChart();
        binding.piechart.addPieSlice(new PieModel("Aldi", (float) ((float) totalA*100/total), Color.parseColor("#FC0008")));
        binding.piechart.addPieSlice(new PieModel("Walmart", (float) ((float) totalW*100/total),Color.parseColor("#FDD400")));
        binding.piechart.addPieSlice(new PieModel("Sam's Club", (float) ((float) totalS*100/total),Color.parseColor("#FB7D3A")));
        binding.piechart.addPieSlice(new PieModel("Costco", (float) ((float) totalC*100/total),Color.parseColor("#1FBF02")));
        binding.piechart.addPieSlice(new PieModel("Pan Asia", (float) ((float) totalP*100/total),Color.parseColor("#3A72FF")));
        binding.piechart.addPieSlice(new PieModel("Sprouts", (float) ((float) totalSp*100/total),Color.parseColor("#28F4B4")));
        binding.piechart.addPieSlice(new PieModel("Utilities", (float) ((float) totalU*100/total),Color.parseColor("#C32F99")));
        binding.piechart.addPieSlice(new PieModel("Other", (float) ((float) totalO*100/total),Color.parseColor("#6B2BFF")));

        totalAldi = totalA;
        totalWalmart = totalW;
        totalCostco = totalC;
        totalSprouts = totalSp;
        totalOther = totalO;
        totalSam = totalS;
        totalUtilities = totalU;
        totalPanAsia = totalP;
        Total = total;
    }



    public void updateError(String newError) {binding.errorView3.setText(binding.errorView3.getText() + "\n" + newError);}
}