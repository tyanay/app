package com.example.jodrew.heartratebandapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.app.Activity;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;

//Band References
import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.BandIOException;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.BandAccelerometerEvent;
import com.microsoft.band.sensors.BandAccelerometerEventListener;
import com.microsoft.band.sensors.BandGsrEvent;
import com.microsoft.band.sensors.BandGsrEventListener;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.BandRRIntervalEvent;
import com.microsoft.band.sensors.BandRRIntervalEventListener;
import com.microsoft.band.sensors.BandSkinTemperatureEvent;
import com.microsoft.band.sensors.BandSkinTemperatureEventListener;
import com.microsoft.band.sensors.HeartRateConsentListener;
import com.microsoft.band.sensors.HeartRateQuality;
import com.microsoft.band.sensors.SampleRate;

import static com.example.jodrew.heartratebandapp.R.id.buttonStop;

public class MainActivity extends AppCompatActivity {

    private BandClient client = null;
    private Button buttonStart, btnConsent, buttonStop;
    private TextView txtStatus, txtStatus2, txtStatus3;
    private Boolean consentCliecked;



    // Heart Rate Listener
    private BandHeartRateEventListener bandHeartRateEventListener =
            new BandHeartRateEventListener() {
                @Override
                public void onBandHeartRateChanged(final BandHeartRateEvent bandHeartRateEvent) {
                    try {
                        if (bandHeartRateEvent != null) {

                            // Get data
                            int heartRate = bandHeartRateEvent.getHeartRate();
                            HeartRateQuality quality = bandHeartRateEvent.getQuality();
                            long currentTimeMillis = System.currentTimeMillis();
                            appendToUI(String.format("Heart Rate = %d beats per minute\n"
                                        + "Quality = %s\n", bandHeartRateEvent.getHeartRate(), bandHeartRateEvent.getQuality()));

                        } else {
                            throw new Exception("bandHeartRateEvent == null");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        finish();
                    }
                }
            };

    // Skin Temp Listener
    private BandSkinTemperatureEventListener bandSkinTemperatureEventListener =
            new BandSkinTemperatureEventListener() {
                @Override
                public void onBandSkinTemperatureChanged(
                        BandSkinTemperatureEvent bandSkinTemperatureEvent) {
                    try {

                        // Get data
                        float temperature = bandSkinTemperatureEvent.getTemperature();
                        long currentTimeMillis = System.currentTimeMillis();

                    } catch (Exception e) {
                        e.printStackTrace();
                        finish();
                    }
                }
            };

    // GSR Listener
    private BandGsrEventListener bandGsrEventListener =
            new BandGsrEventListener() {

                @Override
                public void onBandGsrChanged(BandGsrEvent bandGsrEvent) {
                    try {

                        // Get data
                        int resistance = bandGsrEvent.getResistance();
                        long currentTimeMillis = System.currentTimeMillis();
                        appendToUI3(String.format("GSR = %d Units\n"
                                + "Time = %s\n", resistance, currentTimeMillis));

                    } catch (Exception e) {
                        e.printStackTrace();
                        finish();
                    }
                }
            };

    // HRV ( RR Interval ) Listener
    private BandRRIntervalEventListener bandHrvEventListener =
            new BandRRIntervalEventListener() {
                @Override
                public void onBandRRIntervalChanged(BandRRIntervalEvent bandHrvEvent) {
                    try {

                        // Get data
                        long currentTimeMillis = System.currentTimeMillis();
                        double interval = bandHrvEvent.getInterval();

                    } catch (Exception e) {
                        e.printStackTrace();
                        finish();
                    }
                }
            };

    // Accelerometer Listener
    private BandAccelerometerEventListener bandAccelerometerEventListener =
            new BandAccelerometerEventListener() {

                @Override
                public void onBandAccelerometerChanged(BandAccelerometerEvent bandAccelerometerEvent) {
                    try {

                        // Get data
                        float x_axe = bandAccelerometerEvent.getAccelerationX();
                        float y_axe = bandAccelerometerEvent.getAccelerationY();
                        float z_axe = bandAccelerometerEvent.getAccelerationZ();

                    } catch (Exception e) {
                        e.printStackTrace();
                        finish();
                    }
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Text heart rate
        txtStatus = (TextView) findViewById(R.id.txtStatus);
        //Text Gsr
        txtStatus2 = (TextView) findViewById(R.id.txtStatus2);
        //Text Temperature
        txtStatus3 = (TextView) findViewById(R.id.txtStatus3);

        //set consent
        btnConsent = (Button) findViewById(R.id.btnConsent);

        //set start button
        buttonStart = (Button) findViewById(R.id.buttonStart);

        //set stop button
        buttonStop = (Button) findViewById(R.id.buttonStop);

        final WeakReference<Activity> reference = new WeakReference<Activity>(this);

        btnConsent.setOnClickListener(new OnClickListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void onClick(View v) {
                consentCliecked = true;
                new HeartRateConsentTask().execute(reference);
            }
        });

        // Start measurements button listener
        buttonStart.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        appendToUI("you pressed start");
                        startInteractionWithBand();
                    }
                });
        // Stop measurements button listener
        buttonStop.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                         stopMeasurementsAndExperiment();
                    }
                });




    }// END onCreate
    private void startInteractionWithBand() throws RuntimeException {
        //check if the user clicked on the consent button
        try {
            if (consentCliecked) {
                //check if the client allowed measure heart rate
                if (client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {
                    //start reading from sensors
                    new SensorsSubscriptionTask().execute();
                    //CHECK
                    long lastArriveTimeHeartRate = System.currentTimeMillis();
                    long lastArriveTimeSkinTemp = System.currentTimeMillis();
                    long lastArriveTimeGsr = System.currentTimeMillis();
                    long lastArriveTimeHrv = System.currentTimeMillis();
                } else {
                    throw new RuntimeException("Need to consent before start");
                }
            } else {
                throw new RuntimeException("Click Consent before start");
            }
        } catch (Exception e) {
            final WeakReference<Activity> reference = new WeakReference<Activity>(this);
            new HeartRateConsentTask().execute(reference);
        }
    }
    //Kick off the sensors rate reading registration
    private class SensorsSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    if (client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {
                        appendToUI("register sensors");
                        client.getSensorManager().registerHeartRateEventListener(bandHeartRateEventListener); // HR
                        client.getSensorManager().registerSkinTemperatureEventListener(bandSkinTemperatureEventListener); // Skin Temp
                        client.getSensorManager().registerGsrEventListener(bandGsrEventListener); // GSR
                        client.getSensorManager().registerRRIntervalEventListener(bandHrvEventListener); // HRV
                        client.getSensorManager().registerAccelerometerEventListener(bandAccelerometerEventListener, SampleRate.MS128); //Accelerometer
                    } else {
                        appendToUI("You have not given this application consent to access heart rate data yet."
                                + " Please press the Heart Rate Consent button.\n");
                    }
                } else {
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                appendToUI(exceptionMessage);

            } catch (Exception e) {
                appendToUI(e.getMessage());
            }
            return null;
        }
    }

    private void stopMeasurementsAndExperiment() {
        try {
            // stop the experiment
            boolean needToStop = true;

            if (client != null) {
                //unregister sensors
                appendToUI("unregister sensors");
                client.getSensorManager().unregisterHeartRateEventListener(bandHeartRateEventListener);
                client.getSensorManager().unregisterSkinTemperatureEventListener(bandSkinTemperatureEventListener);
                client.getSensorManager().unregisterGsrEventListener(bandGsrEventListener);
                client.getSensorManager().unregisterRRIntervalEventListener(bandHrvEventListener);
                client.getSensorManager().unregisterAccelerometerEventListener(bandAccelerometerEventListener);
            }

        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
    }

    //Need to get user consent
    private class HeartRateConsentTask extends AsyncTask<WeakReference<Activity>, Void, Void> {
        @Override
        protected Void doInBackground(WeakReference<Activity>... params) {
            try {
                if (getConnectedBandClient()) {

                    if (params[0].get() != null) {
                        client.getSensorManager().requestHeartRateConsent(params[0].get(), new HeartRateConsentListener() {
                            @Override
                            public void userAccepted(boolean consentGiven) {
                            }
                        });
                    }
                } else {
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                appendToUI(exceptionMessage);

            } catch (Exception e) {
                appendToUI(e.getMessage());
            }
            return null;
        }
    }


    //Get connection to band
    private boolean getConnectedBandClient() throws InterruptedException, BandException {

        if (client == null) {
            //Find paired bands
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                //No bands found...message to user
                return false;
            }
            //need to set client if there are devices
            client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
        } else if(ConnectionState.CONNECTED == client.getConnectionState()) {
            return true;
        }

        //need to return connected status
        return ConnectionState.CONNECTED == client.connect().await();
    }

    @Override
    protected void onResume() {
        super.onResume();
        txtStatus.setText("");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (client != null) {
            try {
                client.getSensorManager().unregisterHeartRateEventListener(bandHeartRateEventListener);
            } catch (BandIOException e) {
                appendToUI(e.getMessage());
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (client != null) {
            try {
                client.disconnect().await();
            } catch (InterruptedException e) {
                // Do nothing as this is happening during destroy
            } catch (BandException e) {
                // Do nothing as this is happening during destroy
            }
        }
        super.onDestroy();
    }

    private void appendToUI(final String string) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtStatus.setText(string);
            }
        });
    }

    private void appendToUI2(final String string) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtStatus2.setText(string);
            }
        });
    }

    private void appendToUI3(final String string) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtStatus3.setText(string);
            }
        });
    }

}
