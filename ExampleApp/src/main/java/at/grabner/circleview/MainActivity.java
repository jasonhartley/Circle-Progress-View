package at.grabner.circleview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import at.grabner.circleprogress.TierBadgeView;

public class MainActivity extends AppCompatActivity {

    TierBadgeView mTierBadgeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTierBadgeView = (TierBadgeView) findViewById(R.id.tierBadgeView);

        mTierBadgeView.setMaxValue(100);
        mTierBadgeView.setCenterImage(R.drawable.tier_reg_r);
        mTierBadgeView.setBarColor(getResources().getColor(R.color.primary_color));
    }

    @Override
    protected void onStart() {
        super.onStart();
        mTierBadgeView.setValue(0);
        mTierBadgeView.setValueAnimated(42, 1500);
    }

}

