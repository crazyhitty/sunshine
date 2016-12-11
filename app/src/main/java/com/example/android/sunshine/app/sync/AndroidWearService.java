package com.example.android.sunshine.app.sync;

import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.WearableListenerService;

public class AndroidWearService extends WearableListenerService {
    private static final String TAG = "MyService";

    private static final String ARG_WEATHER_MESSAGES_PATH = "/messages";
    private static final String ARG_HIGH_TEMP = "HIGH_TEMP";
    private static final String ARG_LOW_TEMP = "LOW_TEMP";
    private static final String ARG_WEATHER_ID = "WEATHER_ID";

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        super.onDataChanged(dataEventBuffer);
        /*for (DataEvent dataEvent : dataEventBuffer) {
            if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
                String path = dataEvent.getDataItem().getUri().getPath();
                if (TextUtils.equals(path, ARG_WEATHER_MESSAGES_PATH)) {

                    Log.e(TAG, "onDataChanged: message: received from wear");
                    Toast.makeText(getApplicationContext(), "Ping received from wear", Toast.LENGTH_SHORT).show();
                }
            }
        }*/
    }
}
