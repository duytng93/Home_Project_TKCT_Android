package com.ebookfrenzy.tkctv2;

public class Purchase implements Comparable<Purchase>{
    double amount;
    String owner;
    String merchant;
    String comment;
    Purchase(double amount, String owner, String merchant, String comment){
        this.amount = amount;
        this.owner = owner;
        this.merchant = merchant;
        this.comment = comment;
    }
    @Override
    public int compareTo(Purchase o) {
        // TODO Auto-generated method stub
        int result;
        if (this.amount > o.amount)
            result = 1;
        else if (this.amount < o.amount)
            result = -1;
        else
            result = 0;
        return result;
    }
    @Override
    public String toString(){
        if(merchant.equals("cb"))
            return String.format("%8.2f  %2s  %s",amount,owner,comment);
        return String.format("%8.2f  %2s  %s",amount,merchant,comment); //amount + "\t" + merchant + "\t" + comment;
    }
}
