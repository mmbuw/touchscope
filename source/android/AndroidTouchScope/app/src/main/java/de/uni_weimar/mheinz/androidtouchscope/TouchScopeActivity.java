package de.uni_weimar.mheinz.androidtouchscope;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class TouchScopeActivity extends Activity
{
    private static final String TAG = "TouchScopeActivity";

    RigolScope mRigolScope = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_touch_scope);

        mRigolScope = new RigolScope(this);
    }

    @Override
    public void onDestroy()
    {
        if(mRigolScope != null)
            mRigolScope.terminate();
        super.onDestroy();
    }

    public void onTestRead(View v)
    {
        new Thread(new Runnable()
        {
            public void run()
            {
                mRigolScope.readWave();
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_touch_scope, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
