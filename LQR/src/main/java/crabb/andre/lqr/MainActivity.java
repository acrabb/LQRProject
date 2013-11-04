package crabb.andre.lqr;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements SensorEventListener {

    BluetoothAdapter        mBluetoothAdapter;
    private SensorManager   mSensorManager;
    private Sensor      mAccel;
    private TextView    mAccelNum;
    private TextView    mCounter;
    private TextView    mMaxLabel;
    private float       mMaxAccelRange = -1;

    int REQUEST_ENABLE_BT_CODE = 0;

    //-------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // -- VIEW SETUP ------------------------------------------------------
        mAccelNum   = (TextView) findViewById(R.id.accel_num);
        mCounter    = (TextView) findViewById(R.id.counter);
        mMaxLabel   = (TextView) findViewById(R.id.max);

        // -- BLUETOOTH SETUP -------------------------------------------------
        /*
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT_CODE);
        }
        */

        // -- ACCELEROMETER SETUP ---------------------------------------------
        setUpSensors();
        resetValues();
    }
    //-------------------------------------------------------------------------
    private void setUpSensors() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (mAccel != null)
                mMaxAccelRange = mAccel.getMaximumRange();
        } else {
            Toast.makeText(getApplicationContext(), "NO ACCELLEROMETER :(", Toast.LENGTH_LONG).show();
        }
    }

    //-- SOME VARS ------------------------------------------------------------
    long lastUpdate = 0;
    int count=0;
    float last_x, last_y, last_z, x, y, z;
    // This threshold probably have to change if using the raw accel data [~320~400]
    int SHAKE_THRESHOLD = 400;
    float maxX, maxY, maxZ;
    float max_speed;
    //-------------------------------------------------------------------------
    public void resetValues(View view) {
        resetValues();
    }
    public void resetValues() {
        maxX = maxY = maxZ = max_speed = 0;
        mCounter.setText("KICK ME!\n");
    }
    //-------------------------------------------------------------------------
    /*
        Hardcoded to map from android accelerometer range to external accelerometer range.
     */
    private int mapValueInRangeToRange(float val) {
        return (int) mapValueInRangeToRange(val, -19.8f, 19.8f, 320f, 400f);
    }
    //-------------------------------------------------------------------------
    private float mapValueInRangeToRange(float val, float a1, float a2, float b1, float b2) {
        return b1 + ((val - a1)*(b2 - b1))/(a2 - a1);
    }
    //-------------------------------------------------------------------------
    @Override
    public void onSensorChanged(SensorEvent arg0) {
        float accel[] = getLinearAcceleration(arg0.values);
//        float accel[] = arg0.values;
        mAccelNum.setText(String.format("x %f : %d\t y %f : %d\t z %f : %d\n",
                accel[0], mapValueInRangeToRange(accel[0]),
                accel[1], mapValueInRangeToRange(accel[1]),
                accel[2], mapValueInRangeToRange(accel[2])));

        // -- DO IMPORTANT THINGS ---------------------------------------------
        long curTime = System.currentTimeMillis();
        if ((curTime-lastUpdate) > 100) {
            long diffTime = (curTime - lastUpdate);
            lastUpdate = curTime;
            count = 0;

            // Map internal accelerometer range to external accelerometer range?
//            x = mapValueInRangeToRange(accel[0]);
//            y = mapValueInRangeToRange(accel[1]);
//            z = mapValueInRangeToRange(accel[2]);
            x = accel[0];
            y = accel[1];
            z = accel[2];

            maxX = (x > maxX) ? x : maxX;
            maxY = (y > maxY) ? y : maxY;
            maxZ = (z > maxZ) ? z : maxZ;


            mMaxLabel.setText(String.format("x %f\ny %f\nz %f\n",
                    maxX,
                    maxY,
                    maxZ));

            float speed = Math.abs(x+y+z - last_x - last_y - last_z) / diffTime * 10000;

            if (speed > SHAKE_THRESHOLD) {
                Toast.makeText(this, "shake detected w/ speed: " + speed, Toast.LENGTH_SHORT).show();
                if (speed > max_speed) {
                    max_speed = speed;
                    mCounter.setText(String.format("KICKED: %f\n", speed));
                }
            }
            last_x = x;
            last_y = y;
            last_z = z;
        }
        count++;
    }
    //-------------------------------------------------------------------------
    float gravity[] = {0,0,0};
    private float[] getLinearAcceleration(float[] values) {
        // alpha is calculated as t/(t+dT)
        //  with t, the low-pass filter's time-constant
        //  and dT, the event delivery rate

        final float alpha = 0.8f;
        gravity[0] = alpha * gravity[0] + (1-alpha) * values[0];
        gravity[1] = alpha * gravity[1] + (1-alpha) * values[1];
        gravity[2] = alpha * gravity[2] + (1-alpha) * values[2];

        float linear[] = {0,0,0};
        linear[0] = values[0] - gravity[0];
        linear[1] = values[1] - gravity[1];
        linear[2] = values[2] - gravity[2];

        return linear;
    }
    //-------------------------------------------------------------------------
    @Override
         public void onAccuracyChanged(Sensor arg0, int arg1) {
    }


    //-------------------------------------------------------------------------
    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_NORMAL);
    }
    //-------------------------------------------------------------------------
    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }
    //-------------------------------------------------------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    //-------------------------------------------------------------------------
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
