package com.example.redabenchraa.myapplication;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.uber.sdk.android.core.auth.AccessTokenManager;
import com.uber.sdk.android.core.auth.AuthenticationError;
import com.uber.sdk.android.core.auth.LoginCallback;
import com.uber.sdk.android.core.auth.LoginManager;
import com.uber.sdk.core.auth.AccessToken;
import com.uber.sdk.core.auth.Scope;
import com.uber.sdk.rides.client.Session;
import com.uber.sdk.rides.client.SessionConfiguration;
import com.uber.sdk.rides.client.UberRidesApi;
import com.uber.sdk.rides.client.error.ApiError;
import com.uber.sdk.rides.client.error.ErrorParser;
import com.uber.sdk.rides.client.model.Ride;
import com.uber.sdk.rides.client.model.RideRequestParameters;
import com.uber.sdk.rides.client.model.SandboxProductRequestParameters;
import com.uber.sdk.rides.client.model.SandboxRideRequestParameters;
import com.uber.sdk.rides.client.model.UserProfile;
import com.uber.sdk.rides.client.services.RidesService;

import java.util.Arrays;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.uber.sdk.rides.client.utils.Preconditions.checkNotNull;
import static com.uber.sdk.rides.client.utils.Preconditions.checkState;

/**
 * Created by reda-benchraa on 23/08/16.
 */
public class uberHelper {
    public Context context;

    private static final String CLIENT_ID = "XXXX"; //The app id, it can be found in uber dashboard
    private static final String REDIRECT_URI = "app://com.example.redabenchraa.myapplication"; //The android app's URI, must be inserted in uber dashboard
    private static final String LOG_UBER_HELPER = "UBER_HELPER"; // For logging

    public static final int CUSTOM_BUTTON_REQUEST_CODE = 1113;

    public AccessTokenManager accessTokenManager;
    public LoginManager loginManager;
    public SessionConfiguration configuration;
    public RidesService service;
    public Session session;
    private String requestId="null";

    public uberHelper(Context context){
        this.context = context;
        accessTokenManager = new AccessTokenManager(context);
        configuration = new SessionConfiguration.Builder()
                .setRedirectUri(REDIRECT_URI)
                .setClientId(CLIENT_ID)
                .setScopes(Arrays.asList(Scope.PROFILE,Scope.REQUEST,Scope.REQUEST_RECEIPT)) // Setting scopes for our app, check the scopes on dashboard
                .setEnvironment(SessionConfiguration.Environment.SANDBOX)//For production the envirement must be changed to Environment.PRODUCTION
                .build();
        //Validation function
        validateConfiguration(configuration);
        loginManager = new LoginManager(accessTokenManager,new LoginCall(),configuration,CUSTOM_BUTTON_REQUEST_CODE);

        //Checking if the user is already authenticated. If so create a sessin and a service so we could use his account of his behalf.
        if(checkAuthentification()){
            session = loginManager.getSession();
            service = UberRidesApi.with(session).build().createService();
        }
    }
    public boolean checkAuthentification(){
        return loginManager.isAuthenticated();
    }
    //The login function
    public void login(){
        loginManager.login((Activity) context);
    }
    //The request ride funtion
    public void requestRide(@NonNull String pickupName,@NonNull String pickupAddresse, float lat, float lng) {
        if(checkAuthentification()){
            RideRequestParameters ride = new RideRequestParameters.Builder()
                    .setPickupCoordinates(lat,lng)
                    .setPickupNickname(pickupName)
                    .setPickupAddress(pickupAddresse) // Destination can be set after this
                    .build();
            service.requestRide(ride)
                    .enqueue(new Callback<Ride>() {
                        @Override
                        public void onResponse(Call<Ride> call, Response<Ride> response) {
                            if (response.isSuccessful()) {
                                requestId = response.body().getRideId();
                                Log.v(LOG_UBER_HELPER,"Request ID :"+requestId);
                                Toast.makeText(context,response.body().getStatus(),Toast.LENGTH_SHORT).show();
                            } else {
                                ApiError error = ErrorParser.parseError(response);
                                Log.v(LOG_UBER_HELPER,error.getClientErrors().get(0).getTitle());
                                Toast.makeText(context,error.getClientErrors().get(0).getTitle(),Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<Ride> call, Throwable t) {
                            Toast.makeText(context,"Connection Failure",Toast.LENGTH_SHORT).show();
                        }
                    });
        }else{
            Log.v(LOG_UBER_HELPER,"Please login");
            Toast.makeText(context,"Please login",Toast.LENGTH_SHORT).show();
        }

    }
    public void cancelRide() {
        if(checkAuthentification()){
            if(!requestId.equals("null")) {
                service.cancelCurrentRide()
                        .enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(context, "Ride cancelled", Toast.LENGTH_SHORT).show();
                                } else {
                                    ApiError error = ErrorParser.parseError(response);
                                    Log.v(LOG_UBER_HELPER, error.getClientErrors().get(0).getTitle());
                                }
                            }

                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                                Toast.makeText(context, "Connection Failure", Toast.LENGTH_SHORT).show();
                            }
                        });
            }else{
                Log.v(LOG_UBER_HELPER,"Please request a ride");
                Toast.makeText(context,"Please request a ride",Toast.LENGTH_SHORT).show();
            }
        }else{
            Log.v(LOG_UBER_HELPER,"Please login");
            Toast.makeText(context,"Please login",Toast.LENGTH_SHORT).show();
        }

    }
    public void getRequestStatus() {
        if(checkAuthentification()) {
            service.getRideDetails(requestId).enqueue(new Callback<Ride>() {
                @Override
                public void onResponse(Call<Ride> call, Response<Ride> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(context, response.body().getStatus(), Toast.LENGTH_SHORT).show();
                    } else {
                        ApiError error = ErrorParser.parseError(response);
                        Log.v(LOG_UBER_HELPER, error.getClientErrors().get(0).getTitle());
                    }
                }

                @Override
                public void onFailure(Call<Ride> call, Throwable t) {
                    Toast.makeText(context, "Connection Failure", Toast.LENGTH_SHORT).show();
                }
            });
        }else{
            Log.v(LOG_UBER_HELPER,"Please login");
            Toast.makeText(context,"Please login",Toast.LENGTH_SHORT).show();
        }
    }
    public void setRideSandBox(rideStatus status) {
        if(checkAuthentification()) {
            if(!requestId.equals("null")) {
                service.updateSandboxRide(requestId, new SandboxRideRequestParameters.Builder().setStatus(status.toString()).build())
                        .enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(context, "Change accepted", Toast.LENGTH_SHORT).show();
                                } else {
                                    ApiError error = ErrorParser.parseError(response);
                                    Log.v(LOG_UBER_HELPER, error.getClientErrors().get(0).getTitle());
                                }
                            }
                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                                Toast.makeText(context, "Connection Failure", Toast.LENGTH_SHORT).show();

                            }
                        });
            }else{
                Log.v(LOG_UBER_HELPER,"Please request a ride");
                Toast.makeText(context,"Please request a ride",Toast.LENGTH_SHORT).show();
            }
        }else{
            Log.v(LOG_UBER_HELPER,"Please login");
            Toast.makeText(context,"Please login",Toast.LENGTH_SHORT).show();
        }
    }
    public void setDriverSandBox(boolean state,String productId){
        if(checkAuthentification()) {
            if(!requestId.equals("null")) {
                service.updateSandboxProduct(productId, new SandboxProductRequestParameters.Builder().setDriversAvailable(state).build()).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(context, "Change accepted", Toast.LENGTH_SHORT).show();
                        } else {
                            ApiError error = ErrorParser.parseError(response);
                            Log.v(LOG_UBER_HELPER, error.getClientErrors().get(0).getTitle());
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(context, "Connection Failure", Toast.LENGTH_SHORT).show();
                    }
                });
            }else{
                Log.v(LOG_UBER_HELPER,"Please request a ride");
                Toast.makeText(context,"Please request a ride",Toast.LENGTH_SHORT).show();
                }
        }else{
            Log.v(LOG_UBER_HELPER,"Please login");
            Toast.makeText(context,"Please login",Toast.LENGTH_SHORT).show();
            }
        }
    public void setSurgeMultiplierSandBox(float value){
        if(checkAuthentification()) {
            if(!requestId.equals("null")) {
                service.updateSandboxProduct(requestId, new SandboxProductRequestParameters.Builder().setSurgeMultiplier(value).build()).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(context, "Change accepted", Toast.LENGTH_SHORT).show();
                        } else {
                            ApiError error = ErrorParser.parseError(response);
                            Log.v(LOG_UBER_HELPER, error.getClientErrors().get(0).getTitle());
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(context, "Connection Failure", Toast.LENGTH_SHORT).show();
                    }
                });
            }else{
                Log.v(LOG_UBER_HELPER,"Please request a ride");
                Toast.makeText(context,"Please request a ride",Toast.LENGTH_SHORT).show();
                }
        }else {
            Log.v(LOG_UBER_HELPER,"Please login");
            Toast.makeText(context,"Please login",Toast.LENGTH_SHORT).show();
            }
        }
    public String getRequestId(){
        if(checkAuthentification()) {
            if(!requestId.equals("null")) {
                return requestId;
            }else{
                Log.v(LOG_UBER_HELPER,"Please request a ride");
                Toast.makeText(context,"Please request a ride",Toast.LENGTH_SHORT).show();
            }
        }else{
            Log.v(LOG_UBER_HELPER,"Please login");
            Toast.makeText(context,"Please login",Toast.LENGTH_SHORT).show();
        }
        return "null";
    }
    private void validateConfiguration(SessionConfiguration configuration) {
        String nullError = "%s must not be null";
        String sampleError = "Please update your %s in the gradle.properties of the project before " +
                "using the Uber SDK Sample app. For a more secure storage location, " +
                "please investigate storing in your user home gradle.properties ";
        checkNotNull(configuration, String.format(nullError, "SessionConfiguration"));
        checkNotNull(configuration.getClientId(), String.format(nullError, "Client ID"));
        checkNotNull(configuration.getRedirectUri(), String.format(nullError, "Redirect URI"));
        checkState(!configuration.getClientId().equals("insert_your_client_id_here"),String.format(sampleError, "Client ID"));
        checkState(!configuration.getRedirectUri().equals("insert_your_redirect_uri_here"),String.format(sampleError, "Redirect URI"));
    }
    public class LoginCall implements LoginCallback {
        @Override
        public void onLoginCancel() {
            Toast.makeText(context, R.string.user_cancels_message, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onLoginError(@NonNull AuthenticationError error) {
            Log.v(LOG_UBER_HELPER,error.name());
        }

        @Override
        public void onLoginSuccess(@NonNull AccessToken accessToken) {
            loadProfileInfo();
        }

        @Override
        public void onAuthorizationCodeReceived(@NonNull String authorizationCode) {
            Toast.makeText(context, context.getString(R.string.authorization_code_message, authorizationCode),
                    Toast.LENGTH_LONG)
                    .show();
        }
        private void loadProfileInfo() {
            session = loginManager.getSession();
            service = UberRidesApi.with(session).build().createService();
            service.getUserProfile()
                    .enqueue(new Callback<UserProfile>() {
                        @Override
                        public void onResponse(Call<UserProfile> call, Response<UserProfile> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(context, context.getString(R.string.greeting, response.body().getFirstName()), Toast.LENGTH_LONG).show();
                            } else {
                                ApiError error = ErrorParser.parseError(response);
                                Toast.makeText(context, error.getClientErrors().get(0).getTitle(), Toast.LENGTH_LONG).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<UserProfile> call, Throwable t) {
                            Toast.makeText(context,"Connection Failure",Toast.LENGTH_SHORT).show();

                        }
                    });
        }
    }

    //ride status for sandbox, these are the different status of a request
    public enum rideStatus{
        PROCESSING ("processing"),
        NO_DRIVERS_AVAILABLE("no_drivers_available"),
        ACCEPTED ("accepted"),
        ARRIVING ("arriving"),
        IN_PROGRESS("in_progress"),
        DRIVER_CANCELED("driver_canceled"),
        RIDER_CANCELED("rider_canceled"),
        COMPLETED("completed");

        private final String name;
        private rideStatus(String s) {
            name = s;
        }
        public String toString() {
            return this.name;
        }
        public static boolean contains(String test) {
            for (rideStatus c : rideStatus.values()) {
                if (c.name().equals(test)) {
                    return true;
                }
            }
            return false;
        }
    }
}
