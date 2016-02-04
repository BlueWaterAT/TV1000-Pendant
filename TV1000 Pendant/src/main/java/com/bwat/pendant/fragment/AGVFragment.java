package com.bwat.pendant.fragment;

import android.support.v4.app.Fragment;
import com.bwat.pendant.AGVConnection;
import com.bwat.pendant.activity.AGVMainActivity;

/**
 * @author Kareem ElFaramawi
 */
public abstract class AGVFragment extends Fragment {
    protected AGVMainActivity getAGVActivity() {
        return (AGVMainActivity) getActivity();
    }

    public abstract void updateAGV(AGVConnection con);

    public abstract void processAGVResponse(String message);
}
