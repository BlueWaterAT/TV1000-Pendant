package com.bwat.pendant.activity;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.bwat.pendant.AGVConnection;
import com.bwat.pendant.ConnectionListener;
import com.bwat.pendant.R;
import com.bwat.pendant.fragment.AGVFragment;
import com.bwat.pendant.fragment.HomeFragment;
import com.bwat.pendant.fragment.PathProgrammingFragment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Socket;
import java.util.HashMap;

public class AGVMainActivity extends AGVActivity {
    Logger log = LoggerFactory.getLogger(getClass());
    String[] fragmentTitles;
    DrawerLayout drawerLayout;
    ListView drawerList;
    ActionBarDrawerToggle drawerToggle;
    HashMap<String, AGVFragment> fragmentMap = new HashMap<String, AGVFragment>();
    //	SocketConnection vehicleConnection;
    boolean connectedToWiFi = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fragmentTitles = getResources().getStringArray(R.array.mainFragments);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        drawerList = (ListView) findViewById(R.id.leftDrawer);

        drawerList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, fragmentTitles));
        drawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                replaceFragment(position);
            }
        });

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, (Toolbar) findViewById(R.id.toolbar), R.string.drawer_open, R.string.drawer_close);
        drawerLayout.setDrawerListener(drawerToggle);

        replaceFragment(0);
        getCon().setUpdateListener(new AGVConnection.UpdateListener() {
            @Override
            public void update() {
                for (AGVFragment frag : fragmentMap.values()) {
                    frag.updateAGV(getCon());
                }
            }
        });
        disconnect();
    }

    //	private void checkWifiConnected() {
    //		final WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
    //		if (!wifiManager.isWifiEnabled()) {
    //			AlertDialog.Builder alert = new AlertDialog.Builder(AGVMainActivity.this);
    //			alert.setTitle("Wifi Error");
    //			alert.setMessage("Wifi needs to be enabled for this app to work. Enable?");
    //			alert.setPositiveButton("Enable", new DialogInterface.OnClickListener() {
    //				public void onClick(DialogInterface dialog, int which) {
    //					wifiManager.setWifiEnabled(true);
    //					waitForWifiConnection();
    //				}
    //			});
    //			alert.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
    //				public void onClick(DialogInterface dialog, int which) {
    //					finish();
    //				}
    //			});
    //			alert.setCancelable(false);
    //			alert.show();
    //		} else {
    //			waitForWifiConnection();
    //		}
    //	}
    //
    //	private void waitForWifiConnection() {
    //		final ProgressDialog waiting = new ProgressDialog(AGVMainActivity.this);
    //		waiting.setMessage("Waiting for wifi connection...");
    //		waiting.setCanceledOnTouchOutside(false);
    //		waiting.setOnCancelListener(new DialogInterface.OnCancelListener() {
    //			public void onCancel(DialogInterface dialog) {
    //				finish();
    //			}
    //		});
    //		waiting.show();
    //		new Thread() {
    //			public void run() {
    //				do {
    //					try {
    //						Thread.sleep(500);
    //					} catch (InterruptedException e) {
    //					}
    //					ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    //					NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    //					connectedToWiFi = wifi.isConnected();
    //				} while (!connectedToWiFi);
    //				runOnUiThread(new Runnable() {
    //					public void run() {
    //						waiting.dismiss();
    //					}
    //				});
    //			}
    //		}.start();
    //	}


    @Override
    protected int getLayoutResource() {
        return R.layout.activity_main;
    }

    public AGVConnection getCon() {
        return AGVConnection.getInstance();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View focus = getCurrentFocus();
        if (focus == null) {
            focus = new View(this);
        }
        imm.hideSoftInputFromWindow(focus.getApplicationWindowToken(), 0);
        return true;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    public void replaceFragment(int index) {
        //Create fragment
        Fragment nextFragment = getFragment(fragmentTitles[index]);

        //display fragment
        getSupportFragmentManager().beginTransaction().replace(R.id.contentFrame, nextFragment).commit();

        //Update title and hide nav drawer
        drawerList.setItemChecked(index, true);
        getSupportActionBar().setTitle(fragmentTitles[index]);
        drawerLayout.closeDrawer(drawerList);
    }

    private Fragment getFragment(String name) {
        if (fragmentMap.containsKey(name)) {
            return fragmentMap.get(name);
        } else {
            fragmentMap.put(name, (AGVFragment) getNewFragment(name));
            return fragmentMap.get(name);
        }
    }

    private Fragment getNewFragment(String name) {
        if (name.equals(getString(R.string.fragHome))) {
            return new HomeFragment();
        } else if (name.equals(getString(R.string.fragHMIView))) {
            //			return new HMIViewFragment();
        } else if (name.equals(getString(R.string.fragPathProg))) {
            return new PathProgrammingFragment();
        }
        return null;
    }

    public void connect(final String host, final int port) {
        setConnectionStatus(getString(R.string.conStatusConnecting) + host + ":" + port);
        getCon().connect(host, port, new ConnectionListener() {
            @Override
            public void onConnect(Socket s) {
                //just show on TextView
                setConnectionStatus(getString(R.string.conStatusConnected) + host + ":" + port);
            }

            @Override
            public void onDataReceived(String data) {
                //idk do something
                log.debug("DATA RECEIVED");
                log.debug(data);
                for (AGVFragment frag : fragmentMap.values()) {
                    frag.processAGVResponse(data);
                }
            }

            @Override
            public void onDisconnect(Socket s) {
                setConnectionStatus(getString(R.string.conStatusDisconnected));
            }
        });
    }

    public void disconnect() {
        getCon().disconnect();
        setConnectionStatus(getString(R.string.conStatusDisconnected));
    }

    private void setConnectionStatus(final String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView) findViewById(R.id.connectionStatus)).setText(status);
            }
        });
    }
    //	public SocketConnection getConnection() {
    //		return vehicleConnection;
    //	}

    //	public void setConnection(SocketConnection connection) {
    //		vehicleConnection = connection;
    //	}
}
