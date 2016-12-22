package com.legendfarmer.android.rdfdcalculator;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.legendfarmer.android.rdfdcalculator.util.InputFilterMinMax;

public class MainActivity extends AppCompatActivity
{
    private RadioGroup frequency, calcType;
    private EditText principalTxt, rateOfInterestTxt, tYY, tMM;
    private TextView amountTxt, interest;
    private Button reset;
    private ShareActionProvider mShareActionProvider;
    private String PACKAGE_NAME;
    private Tracker mTracker;
    private static final String TAG = "MainActivity";
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Obtain the shared Tracker instance.
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        PACKAGE_NAME = getApplicationContext().getPackageName();

        //Initiate elements from view to controller
        initViews();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
        sendScreenImageName();
    }

    public void initViews()
    {
        rateOfInterestTxt = (EditText) findViewById(R.id.roi);
        principalTxt = (EditText) findViewById(R.id.principal);
        tYY = (EditText) findViewById(R.id.tYY);
        tMM = (EditText) findViewById(R.id.tMM);
        amountTxt = (TextView) findViewById(R.id.amount);
        interest = (TextView) findViewById(R.id.interest);
        frequency = (RadioGroup) findViewById(R.id.frequency);
        calcType = (RadioGroup) findViewById(R.id.calcType);
        reset = (Button) findViewById(R.id.reset);

        //Input field Validations (limit values in a range)
        tMM.setFilters(new InputFilter[]{new InputFilterMinMax(0, 11), new InputFilter.LengthFilter(2)});
        tYY.setFilters(new InputFilter[]{new InputFilterMinMax(0, 99), new InputFilter.LengthFilter(2)});
        principalTxt.setFilters(new InputFilter[]{new InputFilterMinMax(0, 999999999)});

        //Assign methods to events
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                resetAllFields(principalTxt, rateOfInterestTxt, tYY, tMM);
            }
        });
        setOnchangeListenerForRG(frequency, calcType);
        setOnChangeListenerForET(principalTxt, tMM, tYY, rateOfInterestTxt);
    }

    public void setOnChangeListenerForET(EditText... args) {
        for (EditText et : args)
        {
            etTxtChangeListener(et);
        }
    }

    public void setOnchangeListenerForRG(RadioGroup... args) {
        for (RadioGroup rg : args) {
            rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                    validateDoCalculation();
                    hideKeyPad();
                }
            });
        }
    }

    public void etTxtChangeListener(EditText et) {
        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable)
            {
                validateDoCalculation();
            }
        });
    }

    public void validateDoCalculation()
    {
        int selFreq = frequency.getCheckedRadioButtonId(), selType = calcType.getCheckedRadioButtonId(), typ = 0;
        Double n = 1d;

        switch (selFreq)
        {
            case R.id.halfYearly:
                n = 2d;break;
            case R.id.yearly:
                n = 1d; break;
            case R.id.quarterly:
                n = 4d;  break;
            case R.id.monthly:
                n = 12d;break;
        }

        switch (selType)
        {
            case R.id.rd:
                typ = 0;
                principalTxt.setHint(getString(R.string.monthlyDep));
                break;
            case R.id.fd:
                principalTxt.setHint(getString(R.string.totDep));
                typ = 1;
                break;
        }


        Double p = Double.parseDouble(getNumETAsString(principalTxt));
        Double t = Double.parseDouble(getNumETAsString(tYY)) + Double.parseDouble(getNumETAsString(tMM)) / 12d;
        Double r = 0d;
        try
        {
            r = Double.parseDouble(getNumETAsString(rateOfInterestTxt));
        }
        catch (Exception ex)
        {
            r = 0d;
        }
        r = r / 100d;

        float amount = 0f, deposit = 0f, intAmount = 0f;
        deposit = typ == 0 ? p.floatValue() * t.floatValue() * 12 : p.floatValue();
        amount = deposit;

        if (!isAnyElementEmpty())
        {
            amount = fdCalc(p, t, r, n, typ);
            intAmount = amount == 0 ? 0 : amount - deposit;
        }

        //set result to views
        amountTxt.setText(bNotation(amount));
        interest.setText(bNotation(intAmount));
    }

    void trackEvent(String event)
    {
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Action")
                .setAction(event)
                .build());
    }

    float fdCalc(Double p, Double t, Double r, Double n, int typ)
    {
        float res = 0f;
        switch (typ)
        {
            case 0:                         //RD
                res = findRd(p, t, r, n);
                break;
            case 1:                        //FD
                res = findFd(p, t, r, n);
                break;
        }
        return res;
    }

    public float findRd(Double p, double t, double r, double n)
    {
        trackEvent(getString(R.string.rd));

        Double monthsAftIntCred = 12d / n;
        Double timePeriod = t * 12d;
        Float res = 0f;
        double nTimes = t * n;
        while (timePeriod > 0)
        {
            res = res + findFd(p, timePeriod / 12d, r, n);
            timePeriod--;
        }
        return res;
    }

    public float findFd(Double p, double t, double r, double n)
    {
        trackEvent(getString(R.string.fixed));

        Double amount = p * Math.pow((1d + (r / n)), n * t);
        return amount.floatValue();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem item = menu.findItem(R.id.menu_item_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == R.id.action_exit)
        {
            finish();
            System.exit(0);
            return true;
        }
        else if (id == R.id.menu_item_share)
        {
            trackEvent(getString(R.string.shareRes));
            setShareIntent();
        }
        else if (id == R.id.rate_us)
        {
            Toast.makeText(getApplicationContext(),getString(R.string.redirectToast),Toast.LENGTH_SHORT).show();
            rateUs();
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean isETEmpty(EditText et)
    {
        return et.getText().toString().trim().length() == 0;
    }

    public boolean isTVEmpty(TextView tv)
    {
        return tv.getText().toString().trim().length() == 0;
    }

    public String getNumETAsString(EditText et)
    {
        if (isETEmpty(et)) {
            return "0";
        } else {
            return et.getText().toString();
        }
    }

    public String getNumTVAsString(TextView tv)
    {
        if (isTVEmpty(tv))
        {
            return "0";
        } else {
            return tv.getText().toString();
        }
    }

    public void resetAllFields(EditText... ets)
    {
        for (EditText e : ets) {
            e.setText("");
        }

        frequency.check(R.id.yearly);
        calcType.check(R.id.fd);
        principalTxt.requestFocus(0);
    }

    public boolean isAnyElementEmpty()
    {
        return isETEmpty(principalTxt) || isETEmpty(rateOfInterestTxt) || (isETEmpty(tYY) && isETEmpty(tMM));
    }

    // Call to update the share intent
    private void setShareIntent()
    {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, R.string.app_name);
        shareIntent.setType(getString(R.string.textplain));
        StringBuilder buildContent = new StringBuilder();//
        buildContent.append(getString(R.string.app_name)).append("\n\n");
        buildContent.append(getString(R.string.typeOfInvestment)).append(" : ").append(getSelectedInvType()).append("\n");
        buildContent.append(principalTxt.getHint()).append(" : ").append(getNumETAsString(principalTxt)).append("\n");
        buildContent.append(getString(R.string.roi)).append(" : ").append(getNumETAsString(rateOfInterestTxt)).append("\n");
        buildContent.append(getString(R.string.time)).append(" : ").append(getNumETAsString(tYY) + " "+ getString(R.string.yy) + " ").append(getNumETAsString(tMM) + " " + getString(R.string.mm)).append("\n");
        buildContent.append(getString(R.string.cfreq)).append(" : ").append(getSelectedCompFreq()).append("\n\n\n");
        buildContent.append(getString(R.string.maturity)).append(" : ").append(getNumTVAsString(amountTxt)).append("\n");
        buildContent.append(getString(R.string.interest)).append(" : ").append(getNumTVAsString(interest)).append("\n");
        String shareContent = getString(R.string.shareApp);
        buildContent.append(shareContent);
        shareIntent.putExtra(Intent.EXTRA_TEXT, buildContent.toString());
        startActivity(Intent.createChooser(shareIntent, getString(R.string.shareVia)));
        {
            if (mShareActionProvider != null)
            {
                mShareActionProvider.setShareIntent(shareIntent);
            }
        }
    }

    private String bNotation(Float num) {
        String res = Float.toString(num);
        if (num > 10000000000000000000000f) {
            res = getString(R.string.infinity);
        } else if (num > 1000000000000f) {
            num = num / 1000000000000f;
            res = Float.toString(num) + " " + getString(R.string.trillion);
        } else if (num > 100000000) {
            num = num / 100000000;
            res = Float.toString(num) + " " + getString(R.string.billion);
        } else if (num > 10000000) {
            num = num / 10000000;
            res = Float.toString(num) + " " + getString(R.string.crore);
        } else if (num > 1000000) {
            num = num / 100000;
            res = Float.toString(num) + " " + getString(R.string.lakh);
        }
        return res;
    }

    void rateUs()
    {
        Uri uri = Uri.parse("market://details?id=" + PACKAGE_NAME);
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        // To count with Play market backstack, After pressing back button,
        // to taken back to our application, we need to add following flags to intent.
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            trackEvent(getString(R.string.rateApp));
            startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + PACKAGE_NAME)));
        }
    }

    void hideKeyPad() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction()
    {
        Thing object = new Thing.Builder()
                .setName("FD Calculator") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://fd-calculator.in"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart()
    {
        super.onStart();
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop()
    {
        super.onStop();
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    public String getSelectedInvType()
    {
        int selFreq = calcType.getCheckedRadioButtonId();
        switch (selFreq)
        {
            case R.id.fd:
                return getString(R.string.fixed) +  " " + getString(R.string.deposit);
            case R.id.rd:
                return getString(R.string.rd) + " " + getString(R.string.deposit);
            default:
                return "";
        }
    }

    public String getSelectedCompFreq()
    {
        int selFreq = frequency.getCheckedRadioButtonId();
        switch (selFreq)
        {
            case R.id.halfYearly:
                return getString(R.string.halfyear);
            case R.id.yearly:
                return getString(R.string.year);
            case R.id.quarterly:
                return getString(R.string.quarter);
            case R.id.monthly:
                return getString(R.string.monthly);
        }
        return "";
    }

    private void sendScreenImageName()
    {
        Log.i(TAG, "Setting screen name: " + "HomeScreen");
        mTracker.setScreenName(getString(R.string.app_name));
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

}