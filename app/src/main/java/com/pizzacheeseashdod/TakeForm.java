package com.pizzacheeseashdod;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.pizzacheeseashdod.types.DailyEarning;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.Minutes;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;

import java.util.Calendar;
import java.util.Date;


public class TakeForm extends AppCompatActivity implements View.OnClickListener {

    final int RC_VERIFY_CASH_PAYMENT = 123;
    final int RC_VERIFY_CC_PAYMENT = 234;
    final int RC_GET_CREDIT_INFO = 126;
    TextView tvTime;
    EditText name, phone;
    Button btnChooseTime, btnFinish;
    ImageView btnCash, btnCC;
    TextView tvTotalPrice, tvDate;
    SharedPreferences sp, lastOrderSp;
    String cPhoneNumber, cName, cArrivalTime;
    Dialog d;
    private DatabaseReference earningsRef;
    private DailyEarning todayEarning;
    SharedPreferences specialProductsSp;
    private NumberFormat f;
    int width;
    private boolean anotherDay = false;
    private LocalDate pickedDate;
    private LocalTime pickedTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        f = new DecimalFormat("00");
        earningsRef = FirebaseDatabase.getInstance().getReference("Earnings");
        final DailyEarning[] temp = new DailyEarning[1];
        final Date td = Calendar.getInstance().getTime();
        earningsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    temp[0] = snapshot.getValue(DailyEarning.class);
                    //if the year, month and day are the same, this is today's earnings
                    if (temp[0] != null && (temp[0].getDate().getYear() == td.getYear() && temp[0].getDate().getMonth() == td.getMonth() && temp[0].getDate().getDate() == td.getDate()))
                        todayEarning = temp[0];
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
        specialProductsSp = getSharedPreferences("specialProducts", MODE_PRIVATE);
        pickedDate = LocalDate.now();
        pickedTime = LocalTime.now();
        setContentView(R.layout.activity_take_form);
        FirebaseApp.initializeApp(this);
        setResult(ChooseType.RS_CODE_FINISH);

        //get screen size
        final Display display = this.getWindowManager().getDefaultDisplay();
        width = display.getWidth();
        //finish

        sp = getSharedPreferences("details", MODE_PRIVATE);
        lastOrderSp = getSharedPreferences("lastOrder", MODE_PRIVATE);
        tvTime = findViewById(R.id.tvTime);
        name = findViewById(R.id.edName);
        phone = findViewById(R.id.edPhone);
        btnChooseTime = findViewById(R.id.btnChooseTime);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        btnChooseTime.setOnClickListener(this);
        btnFinish = findViewById(R.id.btnFinish);
        btnFinish.setOnClickListener(this);
        tvDate = findViewById(R.id.tvDate);

        //set the default values to lastOrder's
        if (!SplashActivity.lastOrder.getPhoneNumber().equals(""))
            Toast.makeText(this, "נטענים פרטי ההזמנה האחרונה", Toast.LENGTH_LONG).show();
        name.setText(SplashActivity.lastOrder.getcName());
        phone.setText(SplashActivity.lastOrder.getPhoneNumber());


        tvTotalPrice.setText(String.format("%.2f", SplashActivity.order.getCart().getSum()) + " ₪ ");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnChooseTime:
                buildTimePickerDialog();
                break;
            case R.id.btnFinish:
                cName = name.getText().toString();
                cPhoneNumber = phone.getText().toString();
                cArrivalTime = tvTime.getText().toString() + " " + tvDate.getText().toString();
                if ((cPhoneNumber.length() < 9 || cPhoneNumber.length() > 10))
                    phone.setError("אנא הכנס מספר טלפון תקין");
                else if (!((CheckBox) findViewById(R.id.cbTerms)).isChecked()) {
                    AlertDialog.Builder agreeDialog = new AlertDialog.Builder(this).setMessage("להמשך הזמנה אנא אשר את תנאי השימוש").setPositiveButton("אישור", (dialogInterface, i) -> dialogInterface.cancel());
                    agreeDialog.show();

                } else {
                    d = new Dialog(this);
                    d.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                    d.setContentView(R.layout.shiportake_dialog);
                    d.getWindow().getAttributes().windowAnimations = R.style.fadeAnimations;
                    d.setTitle("בחר אמצעי תשלום");
                    d.setCancelable(true);
                    btnCash = d.findViewById(R.id.btnShip);
                    btnCash.setImageResource(R.drawable.cash_icon_dialog);
                    btnCash.setOnClickListener(this);
                    btnCC = d.findViewById(R.id.btnTake);
                    btnCC.setImageResource(R.drawable.credit_card_icon_dialog);
                    btnCC.setOnClickListener(this);

                    btnCash.getLayoutParams().width = width / 4;
                    btnCash.getLayoutParams().height = width / 4;
                    btnCC.getLayoutParams().width = width / 4;
                    btnCC.getLayoutParams().height = width / 4;

                    d.show();
                }
                break;
            case R.id.btnTake://credit card payment was chosen
                if (SplashActivity.myAppSettings.getCcStatus()) {
                    d.dismiss();
                    if (SplashActivity.TEST_MODE) {
                        SplashActivity.order.setCreditCardPayment(true);
                        finishOrder();
                    } else {
                        startActivityForResult(
                                AuthUI.getInstance()
                                        .createSignInIntentBuilder()
                                        .setIsSmartLockEnabled(true, false)
                                        .setAvailableProviders(
                                                Arrays.asList(new AuthUI.IdpConfig
                                                        .PhoneBuilder()
                                                        .setDefaultNumber("il", cPhoneNumber)
                                                        .build()))
                                        .build(),
                                RC_VERIFY_CC_PAYMENT);

                    }
                } else
                    Toast.makeText(this, "אפשרות זו לא זמינה כרגע", Toast.LENGTH_SHORT).show();
                break;

            case R.id.btnShip://cash payment was chosen
                if (SplashActivity.TEST_MODE) {
                    SplashActivity.order.setCreditCardPayment(false);
                    finishOrder();
                } else {
                    d.dismiss();
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(true, false)
                                    .setAvailableProviders(
                                            Arrays.asList(new AuthUI.IdpConfig
                                                    .PhoneBuilder()
                                                    .setDefaultNumber("il", cPhoneNumber)
                                                    .build()))
                                    .build(),
                            RC_VERIFY_CASH_PAYMENT);
                }
                break;

            case R.id.tvTerms:
                AlertDialog.Builder temrsDialog = new AlertDialog.Builder(this).setTitle("תנאי שימוש").setMessage(R.string.terms)
                        .setPositiveButton("הבנתי,ואני מאשר את תנאי השימוש.", (dialogInterface, i) -> {
                            ((CheckBox) findViewById(R.id.cbTerms)).setChecked(true);
                            dialogInterface.cancel();
                        }).setNegativeButton("ביטול", (dialogInterface, i) -> dialogInterface.cancel());
                temrsDialog.show();
                break;
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_VERIFY_CASH_PAYMENT && resultCode == RESULT_OK) {//after cash payment was chosen and verified
            SplashActivity.order.setCreditCardPayment(false);
            finishOrder();
        } else if ((requestCode == RC_VERIFY_CC_PAYMENT && resultCode == RESULT_OK)) {//after credit card payment was chosen and verified
            SplashActivity.order.setShipment(false);
            Intent intent = new Intent(this, CreditCardForm.class);
            startActivityForResult(intent, RC_GET_CREDIT_INFO);
        } else if (requestCode == RC_GET_CREDIT_INFO && resultCode == RESULT_OK) {//after credit card payment was filled up
            SplashActivity.order.setCreditCardPayment(true);
            finishOrder();
        }
    }

    public boolean moreThanTen(LocalTime chosenTime) {
        LocalTime now = LocalTime.now();
        int minutesDiff = Minutes.minutesBetween(now, chosenTime).getMinutes();
        return (minutesDiff >= 9);
    }

    public void buildTimePickerDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.timepicker_dialog);
        dialog.setTitle("בחר שעה");
        Button btnFinish = (Button) dialog.findViewById(R.id.btnFinish);
        Button btnPickDate = dialog.findViewById(R.id.pickDate);

        //building the datepicker dialog listener
        final DatePickerDialog.OnDateSetListener dateSetListener = (view, year, monthOfYear, dayOfMonth) -> {
            pickedDate = new LocalDate(year, monthOfYear + 1, dayOfMonth);
            String timeString, dateString = "";
            if (pickedDate.isEqual(LocalDate.now())) {
                anotherDay = false;
                if (moreThanTen(pickedTime))
                    timeString = f.format(pickedTime.getHourOfDay()) + ":" + f.format(pickedTime.getMinuteOfHour());
                else
                    timeString = "בתוך 10 דקות";
            } else {
                timeString = f.format(pickedTime.getHourOfDay()) + ":" + f.format(pickedTime.getMinuteOfHour());
                dateString = "(" + pickedDate.getDayOfMonth() + "/" + (pickedDate.getMonthOfYear()) + ")";
                anotherDay = true;
            }
            tvTime.setText(timeString);
            tvDate.setText(dateString);
        };
        //building the datePicker itself and asserting the listener to it
        btnPickDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(TakeForm.this, dateSetListener, pickedDate.getYear(), pickedDate.getMonthOfYear(), pickedDate.getDayOfMonth());
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis() + (24 * 60 * 60 * 1000) * 7);
            datePickerDialog.updateDate(pickedDate.getYear(), pickedDate.getMonthOfYear(), pickedDate.getDayOfMonth());
            datePickerDialog.show();
        });

        btnFinish.setOnClickListener(v -> dialog.hide());

        TimePicker tp = dialog.findViewById(R.id.timePicker);

        tp.setIs24HourView(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            tp.setHour(pickedTime.getHourOfDay());
            tp.setMinute(pickedTime.getMinuteOfHour());
        } else {
            tp.setCurrentHour(pickedTime.getHourOfDay());
            tp.setCurrentMinute(pickedTime.getMinuteOfHour());
        }

        tp.setOnTimeChangedListener((view, hourOfDay, minute) -> {
            String s;
            //if the chosen time is NOT in the past or it is in another day, apply it
            pickedTime = new LocalTime(hourOfDay, minute);
            if (moreThanTen(pickedTime) || anotherDay)
                s = f.format(hourOfDay) + ":" + f.format(minute);
            else
                s = "בתוך 10 דקות";
            tvTime.setText(s);
        });
        dialog.show();
    }

    //finishes the order
    public void finishOrder() {
        //update the order with the parameters
        SplashActivity.order.setShipment(false);
        SplashActivity.order.setcName(cName);
        SplashActivity.order.setPhoneNumber(cPhoneNumber);
        SplashActivity.order.setArrivalTime(cArrivalTime);

        if (!SplashActivity.TEST_MODE)
            saveThisEarning();

        backToMain();
        overridePendingTransition(0, 0);
    }


    public void saveThisEarning() {
        //update the daily earnings in the database
        //if there were no earnings today, create a new earning row
        if (todayEarning == null) {
            todayEarning = new DailyEarning(Calendar.getInstance().getTime());
            todayEarning.setKey(earningsRef.push().getKey());
        }
        if (SplashActivity.order.isCreditCardPayment())
            todayEarning.setCcSum(todayEarning.getCcSum() + SplashActivity.order.getCart().getSum());
        else
            todayEarning.setCashSum(todayEarning.getCashSum() + SplashActivity.order.getCart().getSum());
        earningsRef.child(todayEarning.getKey()).setValue(todayEarning);
    }

    private void backToMain() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        MainActivity.finished = true;
        startActivity(i);


    }
}