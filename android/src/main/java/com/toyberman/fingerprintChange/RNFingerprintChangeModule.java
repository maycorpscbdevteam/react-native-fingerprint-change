package com.toyberman.fingerprintChange;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.preference.PreferenceManager;

import androidx.annotation.RequiresApi;
import androidx.core.hardware.fingerprint.FingerprintManagerCompat;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class RNFingerprintChangeModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
    private final String LAST_KEY_ID = "LAST_KEY_ID";
    private SharedPreferences spref;

    public RNFingerprintChangeModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;

        spref = PreferenceManager.getDefaultSharedPreferences(reactContext);
    }

    /**
     * Using reflection to access the fingerprint internal api.
     *
     * @param context
     * @return
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws ClassNotFoundException
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private String getFingerprintInfo(Context context) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        FingerprintManager fingerprintManager = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
        Method method = FingerprintManager.class.getDeclaredMethod("getEnrolledFingerprints");
        Object obj = method.invoke(fingerprintManager);

        String fingerprintsId = "";
        if (obj != null) {
            Class<?> clazz = Class.forName("android.hardware.fingerprint.Fingerprint");
            Method getFingerId = clazz.getDeclaredMethod("getFingerId");

            for (int i = 0; i < ((List) obj).size(); i++) {
                Object item = ((List) obj).get(i);
                if (item != null) {
                    fingerprintsId += "#" + getFingerId.invoke(item);
                }
            }

        }

        return fingerprintsId;

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @ReactMethod
    public void hasFingerPrintChanged(Callback errorCallback, Callback successCallback) {

        // if no fingerprint hardware return false
        if (!hasFingerprintHardware(this.reactContext)) {
            successCallback.invoke(false);
            return;
        }

        try {
            // get current fingers id sum
            String fingersId = getFingerprintInfo(this.reactContext);
            // last saved key
            String lastKeyId = spref.getString(LAST_KEY_ID, null);

            if (lastKeyId != null && !lastKeyId.equals(fingersId)) {
                successCallback.invoke(true);
            } else {
                successCallback.invoke(false);
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
            successCallback.invoke(false);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @ReactMethod
    public void init(Callback errorCallback) {
        try {
            // get current fingers id sum
            String fingersId = getFingerprintInfo(this.reactContext);
            // last saved key
            spref.edit().putString(LAST_KEY_ID, fingersId).apply();
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
            errorCallback.invoke(true);
        }
    }

    private boolean hasFingerprintHardware(Context mContext) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Fingerprint API only available on from Android 6.0 (M)
            FingerprintManager fingerprintManager = (FingerprintManager) mContext.getSystemService(Context.FINGERPRINT_SERVICE);

            if (fingerprintManager == null) {
                return false;
            }

            return fingerprintManager.isHardwareDetected();
        } else {
            // Supporting devices with SDK < 23
            FingerprintManagerCompat fingerprintManager = FingerprintManagerCompat.from(getReactApplicationContext());

            if (fingerprintManager == null) {
                return false;
            }

            return fingerprintManager.isHardwareDetected();
        }

    }

    @Override
    public String getName() {
        return "RNFingerprintChange";
    }

}