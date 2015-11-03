package rabidllamastudios.com.avigate;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /** Called when the user clicks the Sensor Reading button */
    public void startSensorReadings(View view) {
        Intent intent = new Intent(this, DisplaySensorActivity.class);
        startActivity(intent);
    }

    /** Called when the user clicks the Connectivity Test button */
    public void startConnectivityTest(View view) {
        Intent intent = new Intent(this, ConnectivityTestActivity.class);
        startActivity(intent);
    }
}