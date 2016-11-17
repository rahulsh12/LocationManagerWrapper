import android.location.LocationManager;

public class CustomLocationRequest{
	
	private long mPollInterval = 0;
	private long mMinDistinPoll = 0;
	private int mNumOfUpdates = 0;
	private String mLocationProvider = LocationManager.GPS_PROVIDER;
	
	public CustomLocationRequest(){
		
	}
	
	public void setPollInterval(long interval){
		mPollInterval = interval;
	}
	
	public void setMinDistanceForPoll(long dist){
		mMinDistinPoll = dist;
	}
	
	public void setLocationProvider(String provider){
		mLocationProvider = provider;
	}
	
	public long getPollInterval(){
		return mPollInterval;
	}
	
	public long getMinDistanceForPoll(){
		return mMinDistinPoll;
	}
	
	public String getLocationProvider(){
		return mLocationProvider;
	}
	
	public void setNumberOfUpdates(int num){
		mNumOfUpdates = num;
	}
	
	public int getNumberOfUpdates(){
		return mNumOfUpdates;
	}
}