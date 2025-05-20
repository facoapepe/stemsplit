package com.example.castapp;

import android.app.Application;
import android.content.Context;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;
import com.google.android.gms.cast.framework.CastContext;
import java.util.List;

public class MyApplication extends Application implements OptionsProvider {
    @Override
    public void onCreate() {
        super.onCreate();
        CastContext.initialize(this);
    }

    @Override
    public CastOptions getCastOptions(Context context) {
        return new CastOptions.Builder()
                .setReceiverApplicationId(context.getString(R.string.cast_app_id))
                .build();
    }

    @Override
    public List<SessionProvider> getAdditionalSessionProviders(Context context) {
        return null;
    }
}