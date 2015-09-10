package at.grabner.circleview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import at.grabner.circleprogress.CircleProgressView;

public class MainActivity extends AppCompatActivity {

    CircleProgressView mCircleView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCircleView = (CircleProgressView) findViewById(R.id.circleView);

        mCircleView.setMaxValue(100);
        mCircleView.setCenterImage(R.drawable.tier_reg_r);
        mCircleView.setBarColor(getResources().getColor(R.color.primary_color));
    }

    @Override
    protected void onStart() {
        super.onStart();
        mCircleView.setValue(0);
        mCircleView.setValueAnimated(42, 1500);
    }

}

