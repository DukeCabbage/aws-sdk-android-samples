/*
 *  Copyright 2013-2016 Amazon.com,
 *  Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Amazon Software License (the "License").
 *  You may not use this file except in compliance with the
 *  License. A copy of the License is located at
 *
 *      http://aws.amazon.com/asl/
 *
 *  or in the "license" file accompanying this file. This file is
 *  distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *  CONDITIONS OF ANY KIND, express or implied. See the License
 *  for the specific language governing permissions and
 *  limitations under the License.
 */

package com.amazonaws.youruserpools.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserCodeDeliveryDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.SignUpHandler;
import com.amazonaws.youruserpools.AppHelper;
import com.amazonaws.youruserpools.CognitoYourUserPoolsDemo.R;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.jakewharton.rxbinding.widget.TextViewTextChangeEvent;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

public class RegisterUser extends AppCompatActivity {

    @BindView(R.id.toolbar_Register) Toolbar mToolbar;
    @BindView(R.id.signUp_toolbar_title) TextView main_title;

    @BindView(R.id.editTextRegUserId) EditText username;
    @BindView(R.id.editTextRegUserPassword) EditText password;
    @BindView(R.id.editTextRegGivenName) EditText givenName;
    @BindView(R.id.editTextRegEmail) EditText email;
    @BindView(R.id.editTextRegPhone) EditText phone;
    CompositeSubscription compositeSubscription;
    private AlertDialog userDialog;
    private ProgressDialog waitDialog;
    private String usernameInput;
    private String passwordInput;

    SignUpHandler signUpHandler = new SignUpHandler() {
        @Override
        public void onSuccess(CognitoUser user, boolean signUpConfirmationState,
                              CognitoUserCodeDeliveryDetails cognitoUserCodeDeliveryDetails) {
            // Check signUpConfirmationState to see if the user is already confirmed
            closeWaitDialog();
            Boolean regState = signUpConfirmationState;
            if (signUpConfirmationState) {
                // User is already confirmed
                showDialogMessage("Sign up successful!", usernameInput + " has been Confirmed", true);
            } else {
                // User is not confirmed
                confirmSignUp(cognitoUserCodeDeliveryDetails);
            }
        }

        @Override
        public void onFailure(Exception exception) {
            closeWaitDialog();
            TextView label = (TextView) findViewById(R.id.textViewRegUserIdMessage);
            label.setText("Sign up failed");
            username.setBackground(getDrawable(R.drawable.text_border_error));
            showDialogMessage("Sign up failed", AppHelper.formatException(exception), false);
        }
    };


    @OnClick(R.id.signUp)
    void signUp() {
        // Read user data and register
        CognitoUserAttributes userAttributes = new CognitoUserAttributes();

        usernameInput = username.getText().toString();
        if (usernameInput.isEmpty()) {
            username.setError(username.getHint() + " cannot be empty");
            return;
        }

        passwordInput = password.getText().toString();
        if (passwordInput.isEmpty()) {
            password.setError(password.getHint() + " cannot be empty");
            return;
        }

        String userInput = givenName.getText().toString();
        if (userInput.length() > 0) {
            userAttributes.addAttribute(AppHelper.getSignUpFieldsC2O().get(givenName.getHint()), userInput);
        }


        userInput = email.getText().toString();
        if (userInput.length() > 0) {
            userAttributes.addAttribute(AppHelper.getSignUpFieldsC2O().get(email.getHint()), userInput);
        }


        userInput = phone.getText().toString();
        if (userInput.length() > 0) {
            userAttributes.addAttribute(AppHelper.getSignUpFieldsC2O().get(phone.getHint()), userInput);
        }

        showWaitDialog("Signing up...");

        AppHelper.getPool().signUpInBackground(usernameInput, passwordInput, userAttributes, null, signUpHandler);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_user);
        ButterKnife.bind(this);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            // get back to main screen
            String value = extras.getString("TODO");
            if (value.equals("exit")) {
                onBackPressed();
            }
        }

        mToolbar.setTitle("");
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        main_title.setText("Sign up");

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (compositeSubscription == null || compositeSubscription.isUnsubscribed()) {
            compositeSubscription = new CompositeSubscription();
        }

        compositeSubscription.add(RxTextView.textChangeEvents(username)
                .subscribe(new Action1<TextViewTextChangeEvent>() {
                    @Override
                    public void call(TextViewTextChangeEvent textViewTextChangeEvent) {
                        String str = textViewTextChangeEvent.text().toString();
                        TextView label = (TextView) findViewById(R.id.textViewRegUserIdLabel);
                        if (str.isEmpty()) {
                            label.setText("");
                        } else {
                            label.setText(username.getHint());
                            username.setBackground(getDrawable(R.drawable.text_border_selector));
                        }

                        ((TextView) findViewById(R.id.textViewRegUserIdMessage)).setText("");
                    }
                }));

        compositeSubscription.add(RxTextView.textChangeEvents(password)
                .subscribe(new Action1<TextViewTextChangeEvent>() {
                    @Override
                    public void call(TextViewTextChangeEvent textViewTextChangeEvent) {
                        String str = textViewTextChangeEvent.text().toString();
                        TextView label = (TextView) findViewById(R.id.textViewRegUserPasswordLabel);
                        if (str.isEmpty()) {
                            label.setText("");
                        } else {
                            label.setText(password.getHint());
                            password.setBackground(getDrawable(R.drawable.text_border_selector));
                        }

                        ((TextView) findViewById(R.id.textViewUserRegPasswordMessage)).setText("");
                    }
                }));

        compositeSubscription.add(RxTextView.textChangeEvents(givenName)
                .subscribe(new Action1<TextViewTextChangeEvent>() {
                    @Override
                    public void call(TextViewTextChangeEvent textViewTextChangeEvent) {
                        String str = textViewTextChangeEvent.text().toString();
                        TextView label = (TextView) findViewById(R.id.textViewRegGivenNameLabel);
                        if (str.isEmpty()) {
                            label.setText("");
                        } else {
                            label.setText(givenName.getHint());
                            givenName.setBackground(getDrawable(R.drawable.text_border_selector));
                        }

                        ((TextView) findViewById(R.id.textViewRegGivenNameMessage)).setText("");
                    }
                }));

        compositeSubscription.add(RxTextView.textChangeEvents(email)
                .subscribe(new Action1<TextViewTextChangeEvent>() {
                    @Override
                    public void call(TextViewTextChangeEvent textViewTextChangeEvent) {
                        String str = textViewTextChangeEvent.text().toString();
                        TextView label = (TextView) findViewById(R.id.textViewRegEmailLabel);
                        if (str.isEmpty()) {
                            label.setText("");
                        } else {
                            label.setText(email.getHint());
                            email.setBackground(getDrawable(R.drawable.text_border_selector));
                        }

                        ((TextView) findViewById(R.id.textViewRegEmailMessage)).setText("");
                    }
                }));

        compositeSubscription.add(RxTextView.textChangeEvents(phone)
                .subscribe(new Action1<TextViewTextChangeEvent>() {
                    @Override
                    public void call(TextViewTextChangeEvent textViewTextChangeEvent) {
                        String str = textViewTextChangeEvent.text().toString();
                        TextView label = (TextView) findViewById(R.id.textViewRegPhoneLabel);
                        if (str.isEmpty()) {
                            label.setText("");
                        } else {
                            label.setText(phone.getHint());
                            phone.setBackground(getDrawable(R.drawable.text_border_selector));
                        }

                        ((TextView) findViewById(R.id.textViewRegPhoneMessage)).setText("");
                    }
                }));
    }

    @Override
    public void onStop() {
        super.onStop();
        if (compositeSubscription != null && !compositeSubscription.isUnsubscribed()) {
            compositeSubscription.unsubscribe();
        }
    }

    private void confirmSignUp(CognitoUserCodeDeliveryDetails cognitoUserCodeDeliveryDetails) {
        Intent intent = new Intent(this, SignUpConfirm.class);
        intent.putExtra("source", "signup");
        intent.putExtra("name", usernameInput);
        intent.putExtra("destination", cognitoUserCodeDeliveryDetails.getDestination());
        intent.putExtra("deliveryMed", cognitoUserCodeDeliveryDetails.getDeliveryMedium());
        intent.putExtra("attribute", cognitoUserCodeDeliveryDetails.getAttributeName());
        startActivityForResult(intent, 10);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10) {
            if (resultCode == RESULT_OK) {
                String name = null;
                if (data.hasExtra("name")) {
                    name = data.getStringExtra("name");
                }
                exit(name, passwordInput);
            }
        }
    }

    private void showDialogMessage(String title, String body, final boolean exit) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title).setMessage(body).setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    userDialog.dismiss();
                    if (exit) {
                        exit(usernameInput);
                    }
                } catch (Exception e) {
                    if (exit) {
                        exit(usernameInput);
                    }
                }
            }
        });
        userDialog = builder.create();
        userDialog.show();
    }

    private void showWaitDialog(String message) {
        closeWaitDialog();
        waitDialog = new ProgressDialog(this);
        waitDialog.setTitle(message);
        waitDialog.show();
    }

    private void closeWaitDialog() {
        try {
            waitDialog.dismiss();
        } catch (Exception e) {
            //
        }
    }

    private void exit(String uname) {
        exit(uname, null);
    }

    private void exit(String uname, String password) {
        Intent intent = new Intent();
        if (uname == null) {
            uname = "";
        }
        if (password == null) {
            password = "";
        }
        intent.putExtra("name", uname);
        intent.putExtra("password", password);
        setResult(RESULT_OK, intent);
        finish();
    }
}
