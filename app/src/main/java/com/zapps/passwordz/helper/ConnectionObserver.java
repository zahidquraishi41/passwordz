package com.zapps.passwordz.helper;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

public class ConnectionObserver implements LifecycleObserver {
    private static final String TAG = "ZQ-ConnectionObserver";
    private final ConnectivityManager connectivityManager;
    private final ConnectivityManager.NetworkCallback networkCallback;
    private final NetworkRequest networkRequest;
    private final LifecycleOwner lifecycleOwner;
    private boolean wasDisconnected;

    public interface ConnectionChange {
        void onConnected();
    }


    public static boolean isConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network activeNetwork = connectivityManager.getActiveNetwork();
        NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        return (networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET));
    }

    public ConnectionObserver(Context context, LifecycleOwner lifecycleOwner, ConnectionChange connectionChange) {
        wasDisconnected = false;
        this.lifecycleOwner = lifecycleOwner;
        lifecycleOwner.getLifecycle().addObserver(this);
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        networkRequest = new NetworkRequest.Builder().build();
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onUnavailable() {
                super.onUnavailable();
                CToast.error(context, Messages.NO_INTERNET);
                wasDisconnected = true;
            }

            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                if (wasDisconnected) {
                    wasDisconnected = false;
                    CToast.info(context, "Back online");
                }
                connectionChange.onConnected();
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                CToast.error(context, Messages.NO_INTERNET);
                wasDisconnected = true;
            }
        };
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private void startListening() {
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private void stopListening() {
        connectivityManager.unregisterNetworkCallback(networkCallback);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private void removeObserver() {
        lifecycleOwner.getLifecycle().removeObserver(this);
    }

}
