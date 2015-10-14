package com.zoromatic.flashlight;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.zoromatic.flashlight.R;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

@SuppressWarnings("deprecation")
public class FlashlightActivity extends ThemeActionBarActivity {
	
	private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private ListView leftDrawerList;
    private List<RowItem> rowItems;
    
	private Camera camera;
	private ToggleButton button;
	private SeekBar seekBar;
	private final Context context = this;
	private TextView valueFrequency;
	private static final int ACTIVITY_SETTINGS = 0;
	
	private Handler mHandler = new Handler();

    private boolean mActive = false;
    private boolean mSwap = true;   
    private int mDelay = 0;      
    public static final String ALERTOPEN = "alertopen";
    public static final String ALERTID = "alertid";
    private boolean mAlertDialogOpen = false;
    private int mAlertDialogID = -1;
    private String log = "";
    public static final int CAMERANOTSUPPORTED = 0;
    public static final int FLASHNOTSUPPORTED = 1;
    public static final int FLASHNOTAVAILABLE = 2;
    public static final String MYCAMERA = "mycamera";
    private boolean mCameraOpen = false;
    public static final String DRAWEROPEN = "draweropen";
    private boolean mDrawerOpen = false;
    
    public static final String TOGGLEON = "toggleon";
    public static final String FREQUENCY = "frequency";
    private boolean mToggleOn = false;
    private int mStrobeFrequency = 0;
    
    Bundle savedState = null;
    
    private static Notification notification;
	private static NotificationManager notificationManager;
	
	private String initialTheme = "dark";
	private int initialColorScheme = 9;
	private String initialOrientation = "sens";
	private String initialLanguage = "en";
    
    private final Runnable mRunnable = new Runnable() {

        public void run() {         
            if (mActive) {
            	try {
	            	if (camera == null) {
	            		return;
	            	}
	            	
	            	final Parameters p = camera.getParameters();
	            	camera.startPreview();
					
	                if (mSwap) {                    
	                    p.setFlashMode(Parameters.FLASH_MODE_TORCH);
						camera.setParameters(p);
						//camera.startPreview();					
						
	                    mSwap = false;
	                    mHandler.postDelayed(mRunnable, mDelay);
	                } else {
	                    p.setFlashMode(Parameters.FLASH_MODE_OFF);
						camera.setParameters(p);
						//camera.stopPreview();					
						
	                    mSwap = true;
	                    mHandler.postDelayed(mRunnable, mDelay);
	                }  
            	} catch (Exception e) {
    	        	e.printStackTrace();
    	        	button = (ToggleButton) findViewById(R.id.togglebutton);
    	    		button.setChecked(false);
    	    		seekBar.setEnabled(false);
    	    		
    	    		mToggleOn = false;
    	        }
            }           
        }
    };
    
    @SuppressWarnings("unused")
	private final Thread mThread = new Thread() {
        public void run() {
        	if (mActive) {
            	try {
	            	if (camera == null) {
	            		return;
	            	}
	            	
	            	final Parameters p = camera.getParameters();
						                
	                while (mActive) {
	                	p.setFlashMode(Parameters.FLASH_MODE_TORCH);
						camera.setParameters(p);
						camera.startPreview();
						
						sleep(mDelay);
						
						p.setFlashMode(Parameters.FLASH_MODE_OFF);
						camera.setParameters(p);
						camera.stopPreview();
						
						sleep(mDelay);
	                }
            	} catch (Exception e) {
    	        	e.printStackTrace();
    	        	button = (ToggleButton) findViewById(R.id.togglebutton);
    	    		button.setChecked(false);
    	    		seekBar.setEnabled(false);
    	    		
    	    		mToggleOn = false;
    	        }
            } 
        }
    };
    
    private void startStrobe(int frequency) {        
        if (frequency == 0) {
        	stopStrobe();
        	return;
    	}
        
    	mActive = true;
        //mDelay = (20 - frequency + 1) * 20;
    	
    	if (frequency == 0)
    		mDelay = 0;
    	else
    		mDelay=(int) ((1/(float)frequency)*1000/2);    	
    	
        //valueFrequency.setText(Integer.toString(frequency), TextView.BufferType.NORMAL);
        
    	mHandler.post(mRunnable);
        
        //mThread.start();
    }
    
    private void stopStrobe() {        
        mActive = false;
        
        //valueFrequency.setText(Integer.toString(0), TextView.BufferType.NORMAL);
        
        mHandler.post(mRunnable);
        mHandler.removeCallbacks(mRunnable);
        
        //mThread.interrupt();            
    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
		
		String lang = Preferences.getLanguageOptions(this);

		if (lang.equals("")) {
			String langDef = Locale.getDefault().getLanguage();

			if (!langDef.equals(""))
				lang = langDef;
			else
				lang = "en";

			Preferences.setLanguageOptions(this, lang);                
		}

		// Change locale settings in the application
		Resources res = getApplicationContext().getResources();
		DisplayMetrics dm = res.getDisplayMetrics();
		android.content.res.Configuration conf = res.getConfiguration();
		conf.locale = new Locale(lang.toLowerCase());
		res.updateConfiguration(conf, dm);
	    
	    savedState = savedInstanceState;
	    
	    initialTheme = Preferences.getTheme(getApplicationContext());
	    initialColorScheme = Preferences.getColorScheme(getApplicationContext());
    	initialOrientation = Preferences.getOrientation(getApplicationContext());
    	initialLanguage = Preferences.getLanguageOptions(getApplicationContext());
    	
    	if (initialOrientation.equals("port")) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		} else if (initialOrientation.equals("land")) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		}
    	    
		setContentView(R.layout.activity_flashlight);
		
		initView();
		toolbar = (Toolbar) findViewById(R.id.toolbar);
	    setSupportActionBar(toolbar);
	    initDrawer();
	    
	    TypedValue outValue = new TypedValue();
		getTheme().resolveAttribute(R.attr.colorPrimary,
				outValue,
				true);
		int primaryColor = outValue.resourceId;

		setStatusBarColor(findViewById(R.id.statusBarBackground), 
				getResources().getColor(primaryColor));
		
		ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.app_name);
        }
	    
	    notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notification = new Notification(R.drawable.ic_launcher, getResources().getText(R.string.app_name),
				System.currentTimeMillis());
		
		notification.flags |= Notification.FLAG_ONGOING_EVENT 
				| Notification.FLAG_NO_CLEAR;
		notification.when = 0;

		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
					getIntent(), PendingIntent.FLAG_UPDATE_CURRENT);
		
		notification.setLatestEventInfo(context, (String)getResources().getText(R.string.app_name), 
				(String)getResources().getText(R.string.tapopen), pendingIntent);
		
		valueFrequency = (TextView) findViewById(R.id.valueFrequency);
		valueFrequency.setText(Integer.toString(0), TextView.BufferType.NORMAL);
		
		button = (ToggleButton) findViewById(R.id.togglebutton);
		button.setChecked(false);
		
		seekBar = (SeekBar) findViewById(R.id.seekBarStrobe);
		seekBar.setEnabled(false);
		seekBar.setProgress(0);
		
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {       

		    @Override       
		    public void onStopTrackingTouch(SeekBar seekBar) {      
      
		    }       

		    @Override       
		    public void onStartTrackingTouch(SeekBar seekBar) {     
      
		    }       

		    @Override       
		    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		    	mStrobeFrequency = progress;
		    	
		    	if (progress > 0) {
		    		stopStrobe();
		    		startStrobe(progress);
		    	} else {
		    		stopStrobe();
		    		
		    		if (camera != null) {		    		
			    		boolean on = ((ToggleButton) button).isChecked();
			    		
			    		if (on) {			    			
		    				final Parameters p = camera.getParameters();
		    				
	    					p.setFlashMode(Parameters.FLASH_MODE_TORCH);
	    					camera.setParameters(p);
	    					camera.startPreview();	    					
	
			    			seekBar.setEnabled(true);		 
			    			
			    			mToggleOn = true;
			    		} else {
			    			mToggleOn = false;
			    		}
		    		}
		    	}
		    }       
		});

		try {
			final PackageManager pm = context.getPackageManager();
			
			if (!isCameraSupported(pm)){
				mAlertDialogOpen = true;
				mAlertDialogID = CAMERANOTSUPPORTED;
				openAlertDialog(mAlertDialogID);
			}
			else {
				if (!isFlashSupported(pm)) {
					mAlertDialogOpen = true;
					mAlertDialogID = FLASHNOTSUPPORTED;
					openAlertDialog(mAlertDialogID);								
				} else {
					
					if (savedInstanceState == null) {
						boolean turnon = Preferences.getTurnOnOnOpen(context);
						
						if (turnon) {
							mToggleOn = true;
							
							button.setChecked(mToggleOn);						
							seekBar.setEnabled(mToggleOn);								
						}
						
						boolean keepstrobe = Preferences.getKeepStrobeFrequency(context);
						
						if (keepstrobe) {
							mStrobeFrequency = Preferences.getStrobeFrequency(context);								
							seekBar.setProgress(mStrobeFrequency);																						
						}
					} else {
						Intent localIntent = getIntent();					
						Bundle extras = localIntent.getExtras();
	
						if (extras != null) {
							mToggleOn = extras.getBoolean(TOGGLEON);
							mStrobeFrequency = extras.getInt(FREQUENCY);
							
							button.setChecked(mToggleOn);						
							seekBar.setEnabled(mToggleOn);
							seekBar.setProgress(mStrobeFrequency);
						} 
					}
					
					if (camera == null)
						new CameraTask().execute();	
				}
			}					
		} catch (Exception e) {
        	e.printStackTrace();        	
    		button = (ToggleButton) findViewById(R.id.togglebutton);
    		button.setChecked(false);
    		seekBar.setEnabled(false);
    		
    		mToggleOn = false;        		
        }	
	}
    
    public int getActionBarHeight() {
		int actionBarHeight = 0;
		TypedValue tv = new TypedValue();
		if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
		{
			actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
		}
		return actionBarHeight;
	}

	public int getStatusBarHeight() {
		int result = 0;
		int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}
    
    @SuppressLint("InlinedApi")
	public void setStatusBarColor(View statusBar,int color) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			Window w = getWindow();
			w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			//status bar height
			//int actionBarHeight = getActionBarHeight();
			int statusBarHeight = getStatusBarHeight();
			//action bar height
			statusBar.getLayoutParams().height = /*actionBarHeight + */statusBarHeight;
			statusBar.setBackgroundColor(color);
		} else {
			statusBar.setVisibility(View.GONE);
		}
	}
    
    @Override
	protected void onDestroy() {
		super.onDestroy();
		
		try {
			boolean keep = Preferences.getKeepActive(context);
			
			if (!keep && camera != null) {
				stopStrobe();
				
				camera.stopPreview();
				camera.release();
				camera = null;
			}
		} catch (Exception e) {
        	e.printStackTrace();
        	button = (ToggleButton) findViewById(R.id.togglebutton);
    		button.setChecked(false);
    		seekBar.setEnabled(false);
    		
    		mToggleOn = false;
        }
	}
    
    @Override
    protected void onStart() {
    	super.onStart();
    	
		if (camera != null) {
	    	try {
				final Parameters p = camera.getParameters();
				
				boolean on = ((ToggleButton) button).isChecked();
				
				if (on) {
					Log.i("FlashlightActivity", "Flashlight is turned on!");
					p.setFlashMode(Parameters.FLASH_MODE_TORCH);
					camera.setParameters(p);
					camera.startPreview();						

					seekBar.setEnabled(true);

					if (seekBar.getProgress() > 0)
						startStrobe(seekBar.getProgress());	
					
					startNotification(true);
					
					mToggleOn = true;
					
				} else {
					Log.i("FlashlightActivity", "Flashlight is turned off!");
					p.setFlashMode(Parameters.FLASH_MODE_OFF);
					camera.setParameters(p);
					camera.stopPreview();						
					
					stopStrobe();
					seekBar.setEnabled(false);	
					
					startNotification(false);
					
					mToggleOn = false;
				}				
			} catch (Exception e) {
	        	e.printStackTrace();
	        	button = (ToggleButton) findViewById(R.id.togglebutton);
	    		button.setChecked(false);
	    		seekBar.setEnabled(false);
	    		
	    		mToggleOn = false;
	        }
		}
    };
    
    @Override
	protected void onStop() {
		super.onStop();
		
		try {
			boolean keep = Preferences.getKeepActive(context);
			
			if (!keep && camera != null) {
				stopStrobe();
				
				camera.stopPreview();
				camera.release();
				camera = null;
			}
		} catch (Exception e) {
        	e.printStackTrace();   
        	
        	button = (ToggleButton) findViewById(R.id.togglebutton);
    		button.setChecked(false);
    		seekBar.setEnabled(false);
    		
    		mToggleOn = false;
        }
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		try {
			boolean keep = Preferences.getKeepActive(context);
			
			if (!keep && camera != null) {
				stopStrobe();
				
				camera.stopPreview();
				camera.release();
				camera = null;
			}
		} catch (Exception e) {
        	e.printStackTrace();      
        	
        	button = (ToggleButton) findViewById(R.id.togglebutton);
    		button.setChecked(false);
    		seekBar.setEnabled(false);
    		
    		mToggleOn = false;
        }
	}
	
	@Override
	protected void onResume() {
		super.onResume();		
		
		if (camera != null) {
			try {				
				final Parameters p = camera.getParameters();
				
				boolean on = ((ToggleButton) button).isChecked();
				
				if (on) {
					Log.i("FlashlightActivity", "Flashlight is turned on!");
					p.setFlashMode(Parameters.FLASH_MODE_TORCH);
					camera.setParameters(p);
					camera.startPreview();
					
					seekBar.setEnabled(true);

					if (seekBar.getProgress() > 0)
						startStrobe(seekBar.getProgress());
					
					startNotification(true);
					
					mToggleOn = true;
					
				} else {
					Log.i("FlashlightActivity", "Flashlight is turned off!");
					p.setFlashMode(Parameters.FLASH_MODE_OFF);
					camera.setParameters(p);
					camera.stopPreview();
					
					stopStrobe();
					seekBar.setEnabled(false);
					
					startNotification(false);
					
					mToggleOn = false;
				}				
			} catch (Exception e) {
	        	e.printStackTrace();
	        	button = (ToggleButton) findViewById(R.id.togglebutton);
	    		button.setChecked(false);
	    		seekBar.setEnabled(false);
	    		
	    		mToggleOn = false;
	        }
		} else {
			new CameraTask().execute();
		}
	}
	
	private void initView() {
        leftDrawerList = (ListView) findViewById(R.id.left_drawer);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        //navigationDrawerAdapter = new ArrayAdapter<String>( this, R.layout.simple_list_item_1, leftSliderData);        
        
        rowItems = new ArrayList<RowItem>();
        String theme = Preferences.getTheme(this);
		
        RowItem item = new RowItem(theme.compareToIgnoreCase("light") == 0?R.drawable.ic_settings_black_48dp:R.drawable.ic_settings_white_48dp, 
				(String) getResources().getText(R.string.action_settings));
		rowItems.add(item);
        
        ItemAdapter adapter = new ItemAdapter(this, rowItems);
        leftDrawerList.setAdapter(adapter);
        //leftDrawerList.setAdapter(navigationDrawerAdapter);
        
        leftDrawerList.setOnItemClickListener(new DrawerItemClickListener());        
		
		if (theme.compareToIgnoreCase("light") == 0)
			leftDrawerList.setBackgroundColor(getResources().getColor(android.R.color.background_light));
		else 
			leftDrawerList.setBackgroundColor(getResources().getColor(android.R.color.background_dark));
    }

    private void initDrawer() {

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close) {

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                mDrawerOpen = false;
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                mDrawerOpen = true;
            }
        };
        drawerLayout.setDrawerListener(drawerToggle);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }
    
    /* The click listener for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private void selectItem(int position) {
    	leftDrawerList.setItemChecked(position, true);
        drawerLayout.closeDrawers(); 
        mDrawerOpen = false;
        
    	if (position == 0) { // open Settings
    		Intent settingsIntent = null;
	    	
	    	//if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
	    	//	settingsIntent = new Intent(getApplicationContext(), ZoromaticFlashlightPreferenceActivity.class);
	    	//} else {
	    		settingsIntent = new Intent(getApplicationContext(), FlashlightPreferenceActivity.class);
	    	//}
	    	
			startActivityForResult(settingsIntent, ACTIVITY_SETTINGS);
        }    	      
    }
	
	@SuppressLint("RtlHardcoded")
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		
		setContentView(R.layout.activity_flashlight);	
		
		initView();
		toolbar = (Toolbar) findViewById(R.id.toolbar);
	    setSupportActionBar(toolbar);
	    initDrawer();
	    
	    /*ActionBar actionBar = getSupportActionBar();
	    actionBar.setDisplayShowHomeEnabled(true);
	    actionBar.setIcon(R.drawable.ic_launcher);*/
	    
	    drawerToggle.onConfigurationChanged(newConfig);
	    
	    if (mDrawerOpen) {
	    	drawerLayout.openDrawer(Gravity.LEFT);
	    	mDrawerOpen = true;
	    } else {
	    	drawerLayout.closeDrawers();
	    	mDrawerOpen = false;
	    }
	    
	    drawerToggle.syncState();
		
		button = (ToggleButton) findViewById(R.id.togglebutton);
		button.setChecked(mToggleOn);
		
		seekBar = (SeekBar) findViewById(R.id.seekBarStrobe);
		seekBar.setEnabled(mToggleOn);
		seekBar.setProgress(mStrobeFrequency);
		
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {       

		    @Override       
		    public void onStopTrackingTouch(SeekBar seekBar) {      
      
		    }       

		    @Override       
		    public void onStartTrackingTouch(SeekBar seekBar) {     
      
		    }       

		    @Override       
		    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		    	mStrobeFrequency = progress;
		    	
		    	if (progress > 0) {
		    		stopStrobe();
		    		startStrobe(progress);
		    	} else {
		    		stopStrobe();
		    		
		    		if (camera != null) {		    		
			    		boolean on = ((ToggleButton) button).isChecked();
			    		
			    		if (on) {			    			
		    				final Parameters p = camera.getParameters();
		    				
	    					p.setFlashMode(Parameters.FLASH_MODE_TORCH);
	    					camera.setParameters(p);
	    					camera.startPreview();	    					
	
			    			seekBar.setEnabled(true);		 
			    			
			    			mToggleOn = true;
			    		} else {
			    			mToggleOn = false;
			    		}
		    		}
		    	}
		    }       
		});	
	};
	
	@Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(ALERTOPEN, mAlertDialogOpen);
        savedInstanceState.putInt(ALERTID, mAlertDialogID);
        savedInstanceState.putBoolean(MYCAMERA, mCameraOpen);
        savedInstanceState.putBoolean(TOGGLEON, mToggleOn);
        savedInstanceState.putInt(FREQUENCY, mStrobeFrequency);
        savedInstanceState.putBoolean(DRAWEROPEN, mDrawerOpen);
        
        savedState = savedInstanceState;
        
        super.onSaveInstanceState(savedInstanceState);
    }
	
	@SuppressLint("RtlHardcoded")
	@Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
		// Always call the superclass so it can restore the view hierarchy
	    super.onRestoreInstanceState(savedInstanceState);
	    
	    mAlertDialogOpen = savedInstanceState.getBoolean(ALERTOPEN);
		mAlertDialogID = savedInstanceState.getInt(ALERTID);	
		mCameraOpen = savedInstanceState.getBoolean(MYCAMERA);
		mToggleOn = savedInstanceState.getBoolean(TOGGLEON);
		mStrobeFrequency = savedInstanceState.getInt(FREQUENCY);
		mDrawerOpen = savedInstanceState.getBoolean(DRAWEROPEN);
		
		if (mAlertDialogOpen) {
			openAlertDialog(mAlertDialogID);
			return;
		}
		
		if (mDrawerOpen) {
			if (drawerLayout != null) {
				drawerLayout.openDrawer(Gravity.LEFT);
				mDrawerOpen = true;
				drawerToggle.syncState();
			}		
		}
		
		button.setChecked(mToggleOn);						
		seekBar.setEnabled(mToggleOn);
		seekBar.setProgress(mStrobeFrequency);		
	}

	private void startNotification(boolean start) {
		
		if (start) {
			notificationManager.notify(R.string.app_name, notification);
		} else {
			notificationManager.cancel(R.string.app_name);
		}
	}

	private void openAlertDialog(int alertDialogID) {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
		String title = "", message = "";
		
		if (alertDialogID == 0) {
			title = getResources().getString(R.string.noflash);
			message = getResources().getString(R.string.flashnotsupported);
			log = getResources().getString(R.string.flashnotsupported);
		} else if (alertDialogID == 1) {
			title = getResources().getString(R.string.nocamera);
			message = getResources().getString(R.string.cameranotsupported);
			log = getResources().getString(R.string.cameranotsupported);
		} else {
			title = getResources().getString(R.string.noflash);
			message = getResources().getString(R.string.flashnotavailable);
			log = getResources().getString(R.string.flashnotavailable);
		}
		
		alertDialog
		.setTitle(title)
		.setMessage(message)
		.setOnKeyListener(new Dialog.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode,
                    KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                	try {
                		if (camera != null) {
                			stopStrobe();
                			
                			camera.stopPreview();
                			camera.release();
                			camera = null;
                		}
                    	
                    	dialog.dismiss();
                    	
                    	startNotification(false);
        				finish();
        				
        				android.os.Process.killProcess(android.os.Process.myPid());
					} catch (Exception e) {
						e.printStackTrace();	
					}                	
                }
                
                return true;
            }
        })
		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialog, final int which) { 
				Log.e("FlashlightActivity", log);
				try {
					if (camera != null) {
						stopStrobe();
						
						camera.stopPreview();
						camera.release();
						camera = null;
					}
					
					dialog.dismiss();
					
					startNotification(false);
					finish();
					
					android.os.Process.killProcess(android.os.Process.myPid());
				} catch (Exception e) {
					e.printStackTrace();	
				} 
			}
		});
		
		alertDialog.show();		
	}
	
	public class CameraTask extends AsyncTask<Void, Void, OpenCamera>{

    	Runnable uiRunnable = new Runnable() {
    	    public void run() {
    	    	    	    	
    	    }
    	};
    	
    	@Override
		protected void onPreExecute() {
    		  		
        }
    	
    	@Override
		protected OpenCamera doInBackground(Void... params) {			
			OpenCamera openResult = null;
			
			try {
	    		if (camera == null)
					camera = Camera.open();
	    		
	    		if (camera != null)
	    			openResult = new OpenCamera(true, false);
	    		else
	    			openResult = new OpenCamera(false, false);
	            
	        } catch (Exception e) {
	            e.printStackTrace();
	            openResult = new OpenCamera(false, true);
	        }
			
			return openResult;
		}
    	
    	@Override
		protected void onPostExecute(OpenCamera found) {
            
            try {
	            if (found != null && found.cameraResult) {
		            
	            	boolean on = ((ToggleButton) button).isChecked();
	            	
	            	if (on) {
		            	final Parameters p = camera.getParameters();
						
						p.setFlashMode(Parameters.FLASH_MODE_TORCH);
						camera.setParameters(p);
						camera.startPreview();	    					
			
						seekBar.setEnabled(true);
						
						if (seekBar.getProgress() > 0)
							startStrobe(seekBar.getProgress());	
						
						startNotification(true);
						
						mToggleOn = true;
	            	} else {
	            		mToggleOn = false;
	            	}
					
					mCameraOpen = true;
	            } else {
	            	if (found != null && found.cameraException && !mCameraOpen) {
		            	mAlertDialogOpen = true;
						mAlertDialogID = FLASHNOTAVAILABLE;
						openAlertDialog(mAlertDialogID);
	            	}
	            	
	            	mCameraOpen = false;
	            }
    		} catch (Exception e) {
	            e.printStackTrace();	            
	        }
        }		
	}	
	
	private static class OpenCamera {
        boolean cameraResult = false;
        boolean cameraException = false;
        
        public OpenCamera(boolean result, boolean exception) {
        	cameraResult = result;
        	cameraException = exception;
        }        
	}

	public void onToggleClicked(View view) {
		
		if (camera == null)
			return;
		
		try {
			PackageManager pm = context.getPackageManager();
			
			if (isFlashSupported(pm)){
				final Parameters p = camera.getParameters();
				
				boolean on = ((ToggleButton) view).isChecked();
				
				if (on) {
					Log.i("FlashlightActivity", "Flashlight is turned on!");
					p.setFlashMode(Parameters.FLASH_MODE_TORCH);
					camera.setParameters(p);
					camera.startPreview();					

					seekBar.setEnabled(true);

					if (seekBar.getProgress() > 0)
						startStrobe(seekBar.getProgress());		
					
					startNotification(true);
					
					mToggleOn = true;
					
				} else {
					Log.i("FlashlightActivity", "Flashlight is turned off!");
					p.setFlashMode(Parameters.FLASH_MODE_OFF);
					camera.setParameters(p);
					camera.stopPreview();					
					
					stopStrobe();
					seekBar.setEnabled(false);
					
					startNotification(false);
					
					mToggleOn = false;
				}
			} else {
				button.setChecked(false);
				seekBar.setEnabled(false);
				
				mToggleOn = false;
				
				mAlertDialogOpen = true;
				mAlertDialogID = FLASHNOTSUPPORTED;
				openAlertDialog(mAlertDialogID);
			}
		} catch (Exception e) {
        	e.printStackTrace();
        	button = (ToggleButton) findViewById(R.id.togglebutton);
    		button.setChecked(false);
    		seekBar.setEnabled(false);
    		
    		mToggleOn = false;
        }
	}
	
	private boolean isFlashSupported(PackageManager packageManager){ 
		if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
			return true;
		} 
		return false;
	}

	private boolean isCameraSupported(PackageManager packageManager){
		if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
			return true;
		} 
		return false;
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
	    if (keyCode == KeyEvent.KEYCODE_MENU) {
	    	
	    	drawerLayout.closeDrawers();
	    	mDrawerOpen = false;	    		        
	    }
	    
	    return super.onKeyUp(keyCode, event);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu_flashlight, menu);
	    
	    return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		 if (drawerToggle.onOptionsItemSelected(item)) {
			 return true;
	     }
		 
		switch (item.getItemId()) {
	    case R.id.action_settings:
	    	drawerLayout.closeDrawers();
	    	mDrawerOpen = false;
	    	
	    	Intent settingsIntent = null;
	    	
	    	settingsIntent = new Intent(getApplicationContext(), FlashlightPreferenceActivity.class);
	    	
			startActivityForResult(settingsIntent, ACTIVITY_SETTINGS);
	    default:
	    	return super.onOptionsItemSelected(item);	         
		}
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	super.onActivityResult(requestCode, resultCode, intent);
        
    	String theme = Preferences.getTheme(getApplicationContext());
    	int colorScheme = Preferences.getColorScheme(getApplicationContext());
    	String orient = Preferences.getOrientation(getApplicationContext());
    	String lang = Preferences.getLanguageOptions(getApplicationContext());
    	
    	if (!theme.equals(initialTheme)) {
    		initialTheme = theme;
    		
    		startNotification(false);
    		
    		if (camera != null) {
    			stopStrobe();
    			
    			camera.stopPreview();
    			camera.release();
    			camera = null;
    		}
    		
    		Intent localIntent = getIntent();
    		finish();
    		localIntent.putExtra(TOGGLEON, mToggleOn);
    		localIntent.putExtra(FREQUENCY, mStrobeFrequency);
    		startActivity(localIntent);    		    		   		   
    	}
    	
    	if (colorScheme != initialColorScheme) {
    		initialColorScheme = colorScheme;
    		
    		startNotification(false);
    		
    		if (camera != null) {
    			stopStrobe();
    			
    			camera.stopPreview();
    			camera.release();
    			camera = null;
    		}
    		
    		Intent localIntent = getIntent();
    		finish();
    		localIntent.putExtra(TOGGLEON, mToggleOn);
    		localIntent.putExtra(FREQUENCY, mStrobeFrequency);
    		startActivity(localIntent);    		    		   		   
    	}
    	
    	if (!lang.equals(initialLanguage)) {
    		initialLanguage = lang;
    		
    		startNotification(false);
    		
    		if (camera != null) {
    			stopStrobe();
    			
    			camera.stopPreview();
    			camera.release();
    			camera = null;
    		}
    		
    		Intent localIntent = getIntent();
    		finish();
    		localIntent.putExtra(TOGGLEON, mToggleOn);
    		localIntent.putExtra(FREQUENCY, mStrobeFrequency);
    		startActivity(localIntent);    		    		   		   
    	}
    	
    	if (!orient.equals(initialOrientation)) {
    		initialOrientation = orient;
    		
    		if (orient.equals("port")) {
    			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    		} else if (orient.equals("land")) {
    			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    		} else {
    			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    		}    	    	
    	}
    }

	@Override
    public void onBackPressed() {
		if (!mDrawerOpen) {
			super.onBackPressed();
			startNotification(false);
			
			if (camera != null) {
				stopStrobe();
				
				camera.stopPreview();
				camera.release();
				camera = null;
			}
			
			Preferences.setStrobeFrequency(context, mStrobeFrequency);
			
			finish();
		} else {
			if (drawerLayout != null)
				drawerLayout.closeDrawers();
				mDrawerOpen = false;
		}		
	}
}
