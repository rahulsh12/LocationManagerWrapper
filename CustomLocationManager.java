import javax.security.auth.PrivateCredentialPermission;

import android.R.integer;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.provider.SyncStateContract.Constants;


public class CustomLocationManager{
	
	public static long MILLIS_PER_SECOND = 1000;
	public static long ONE_MINUTES = 1 * MILLIS_PER_SECOND;
	
	private LocationManager mLocationManager;
	private LocationListener mLocationListener;
	private OnLocationChangeListener mLcc;
	private OnConnectionChangeCallback mCc;
	private OnConnectionFailedCallback mCfc;
	
	private Location mOldLocation;
	private Location mNewLocation;
	
	private boolean mIsNetworkLocationAvailable = false;
	private boolean mIsGPSLocationAvailable = false;
	
	private int mNWProviderStatus;
	private int mGPSProviderStatus;
	
	/** Interface for receiving call backs inside the class which implements the Interface */
	public interface OnLocationChangeListener{
		
		public void onLocationChange(Location location); 
	}
	
	public interface OnConnectionChangeCallback{
		
		public void onConnected();
		public void onDisconnected();
	}
	
	public interface OnConnectionFailedCallback{
		
		public void onConnectionFailed();
	}
	
	/** Constructor for CustomLocationManager
	 *  @param context The context of the service/activity instantiating the class
	 *  @param cb The context of the service/activity implementing the Interface
	 *  @param cc The context of the class implementing the OnConnectedCallback Interface
	 *  @param cfc The context of the class implementing the OnConnectionFailedCallback Interface
	 */
	public CustomLocationManager(Context context
								, OnLocationChangeListener cb
								, OnConnectionChangeCallback cc
								, OnConnectionFailedCallback cfc){
		
		mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		
		mLcc = cb;
		mCc = cc;
		mCfc = cfc;
		
		mOldLocation = null;
		mNewLocation = null;
		
		
		// Register for new location updates from the LocationManager
		mLocationListener = new LocationListener() {
			
		    public void onLocationChanged(Location location) {
		    	
		    	mNewLocation = location;
		    	mLcc.onLocationChange(location);
		    	mOldLocation = mNewLocation;
		    }

		    public void onStatusChanged(String provider, int status, Bundle extras) {
		    	
		    	if(provider.equals(LocationManager.GPS_PROVIDER))
		    		mGPSProviderStatus = status;

		    	if(provider.equals(LocationManager.NETWORK_PROVIDER))
		    		mNWProviderStatus = status;
		    }

		    public void onProviderEnabled(String provider) {
		    	if(provider.equals(LocationManager.GPS_PROVIDER))
		    		mIsGPSLocationAvailable = true;
		    	else if(provider.equals(LocationManager.NETWORK_PROVIDER))
		    		mIsNetworkLocationAvailable = true;
		    }

		    public void onProviderDisabled(String provider) {
		    	if(provider.equals(LocationManager.GPS_PROVIDER))
		    		mIsGPSLocationAvailable = false;
		    	else if(provider.equals(LocationManager.NETWORK_PROVIDER))
		    		mIsNetworkLocationAvailable = false;
		    	
		    	if(!mIsGPSLocationAvailable && !mIsNetworkLocationAvailable)
		    		mCc.onDisconnected();
		    }

		  };
	}
	
	public boolean isProviderEnabled(){
		if(mIsNetworkLocationAvailable || mIsGPSLocationAvailable)
			return true;
		else 
			return false;
	}
	
	/**
	 * Tries to connect to location services
	 */
	public void connect(){
		  // If any of the location provider is available, invoke onConnected
		  // else invoke onConnectionFailed
		  mIsGPSLocationAvailable = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		  mIsNetworkLocationAvailable = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		  if( mIsGPSLocationAvailable || mIsNetworkLocationAvailable )
			  mCc.onConnected();
		  else
			  mCfc.onConnectionFailed();
	}
	
	/**
	 *  Remove location updates from the Location Manager
	 */
	public void disconnect(){
		
		mLocationManager.removeUpdates(mLocationListener);
		mLocationManager = null;
		mLocationListener = null;
	}
	
	/** Gives the best location after making a comparison between 
	 *  Last location of N/W and that of GPS
	 */
	public Location getLastKnownLocation(){
		
		Location lastLocationNW = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		Location lastLocationGPS = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if(lastLocationGPS != null || lastLocationNW !=null ){
			mOldLocation = isBetterLocation(lastLocationNW,lastLocationGPS)? lastLocationNW : lastLocationGPS;
		}
		return mOldLocation; 
	}
	
	/** Starts the request for location updates from the LocationManager instance */
	public void requestLocation(CustomLocationRequest requestParams){
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER
												, 0
												, 0
												, mLocationListener);		
		mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER
				, 0
				, 0
				, mLocationListener);
		
		mLocationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER
				, 0
				, 0
				, mLocationListener);
	
	}
	/** Stops the updates from the LocationManager instance */
	public void stopUpdates(){
		mLocationManager.removeUpdates(mLocationListener);
	}
	
	public boolean isConnected(){
		return mIsNetworkLocationAvailable || mIsGPSLocationAvailable ? true : false; 
	}
	
	/** Determines whether one Location reading is better than the current Location fix
	  * @param location  The new Location that you want to evaluate
	  * @param currentBestLocation  The current Location fix, to which you want to compare the new one
	  */
	protected boolean isBetterLocation(Location location, Location currentBestLocation) {
	    if (currentBestLocation == null) {
	        // A new location is always better than no location
	        return true;
	    }
	    
	    if(location == null){
		    // new location is better
	    	return false;
	    }

	    // Check whether the new location fix is newer or older
	    long timeDelta = location.getTime() - currentBestLocation.getTime();
	    boolean isSignificantlyNewer = timeDelta > ONE_MINUTES;
	    boolean isSignificantlyOlder = timeDelta < -ONE_MINUTES;
	    boolean isNewer = timeDelta > 0;

	    // If it's been more than two minutes since the current location, use the new location
	    // because the user has likely moved
	    if (isSignificantlyNewer) {
	        return true;
	    // If the new location is more than two minutes older, it must be worse
	    } else if (isSignificantlyOlder) {
	        return false;
	    }

	    // Check whether the new location fix is more or less accurate
	    int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
	    boolean isLessAccurate = accuracyDelta > 0;
	    boolean isMoreAccurate = accuracyDelta < 0;
	    boolean isSignificantlyLessAccurate = accuracyDelta > 200;

	    // Check if the old and new location are from the same provider
	    boolean isFromSameProvider = isSameProvider(location.getProvider(),
	            currentBestLocation.getProvider());

	    // Determine location quality using a combination of timeliness and accuracy
	    if (isMoreAccurate) {
	        return true;
	    } else if (isNewer && !isLessAccurate) {
	        return true;
	    } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
	        return true;
	    }
	    return false;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
	    if (provider1 == null) {
	      return provider2 == null;
	    }
	    return provider1.equals(provider2);
	}
	
}