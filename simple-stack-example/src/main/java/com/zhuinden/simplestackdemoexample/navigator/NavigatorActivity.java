package com.zhuinden.simplestackdemoexample.navigator;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.RelativeLayout;

import com.zhuinden.simplestack.Backstack;
import com.zhuinden.simplestack.HistoryBuilder;
import com.zhuinden.simplestack.StateChanger;
import com.zhuinden.simplestack.navigator.DefaultStateChanger;
import com.zhuinden.simplestack.navigator.Navigator;
import com.zhuinden.simplestackdemoexample.R;
import com.zhuinden.simplestackdemoexample.common.BackstackService;
import com.zhuinden.simplestackdemoexample.common.FirstKey;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Owner on 2017. 04. 07..
 */

public class NavigatorActivity
        extends AppCompatActivity {
    public static final String TAG = "BackstackDelegateActivity";

    @BindView(R.id.root)
    RelativeLayout root;

    Backstack backstack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        Navigator.install(this, root, HistoryBuilder.single(new FirstKey()));
    }

    @Override
    public void onBackPressed() {
        if(!Navigator.onBackPressed(this)) {
            super.onBackPressed();
        }
    }

    @Override
    public Object getSystemService(String name) {
        if(name.equals(BackstackService.TAG)) {
            return Navigator.getBackstack(this);
        }
        return super.getSystemService(name);
    }
}
