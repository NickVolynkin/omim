package com.mapswithme.maps.traffic;

import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.mapswithme.util.Utils;
import com.mapswithme.util.log.DebugLogger;
import com.mapswithme.util.log.Logger;

import java.util.ArrayList;
import java.util.List;

@MainThread
public enum TrafficManager
{
  INSTANCE;
  private final String mTag = TrafficManager.class.getSimpleName();
  @NonNull
  private final Logger mLogger = new DebugLogger(mTag);
  @NonNull
  private final TrafficState.StateChangeListener mStateChangeListener = new TrafficStateListener();
  @TrafficState.Value
  private int mState = TrafficState.DISABLED;
  @NonNull
  private final List<TrafficCallback> mCallbacks = new ArrayList<>();
  private boolean mInitialized = false;

  public void initialize()
  {
    mLogger.d("Initialization of traffic manager and setting the listener for traffic state changes");
    TrafficState.nativeSetListener(mStateChangeListener);
    mInitialized = true;
  }

  public void toggle()
  {
    checkInitialization();

    if (mState == TrafficState.DISABLED)
      enable();
    else
      disable();
  }

  private void enable()
  {
    mLogger.d("Enable traffic");
    TrafficState.nativeEnable();
  }

  public void disable()
  {
    checkInitialization();

    mLogger.d("Disable traffic");
    TrafficState.nativeDisable();
  }
  
  public boolean isEnabled()
  {
    checkInitialization();

    return mState != TrafficState.DISABLED;
  }

  public void attach(@NonNull TrafficCallback callback)
  {
    checkInitialization();

    if (mCallbacks.contains(callback))
    {
      throw new IllegalStateException("A callback '" + callback
                                      + "' is already attached. Check that the 'detachAll' method was called.");
    }

    mLogger.d("Attach callback '" + callback + "'");
    mCallbacks.add(callback);
    postPendingState();
  }

  private void postPendingState()
  {
    mStateChangeListener.onTrafficStateChanged(mState);
  }

  public void detachAll()
  {
    checkInitialization();

    if (mCallbacks.isEmpty())
    {
      Log.w(mTag, "There are no attached callbacks. Invoke the 'detachAll' method " +
                                      "only when it's really needed!", new Throwable());
      return;
    }

    for (TrafficCallback callback : mCallbacks)
      mLogger.d("Detach callback '" + callback + "'");
    mCallbacks.clear();
  }

  private void checkInitialization()
  {
    if (!mInitialized)
      throw new AssertionError("Traffic manager is not initialized!");
  }

  private class TrafficStateListener implements TrafficState.StateChangeListener
  {
    @Override
    @MainThread
    public void onTrafficStateChanged(@TrafficState.Value int state)
    {
      mLogger.d("onTrafficStateChanged current state = " + TrafficState.nameOf(mState)
                + " new value = " + TrafficState.nameOf(state));
      mState = state;

      iterateOverCallbacks(new Utils.Proc<TrafficCallback>()
      {
        @Override
        public void invoke(@NonNull TrafficCallback callback)
        {
          switch (mState)
          {
            case TrafficState.DISABLED:
              callback.onDisabled();
              break;

            case TrafficState.ENABLED:
              callback.onEnabled();
              break;

            case TrafficState.WAITING_DATA:
              callback.onWaitingData();
              break;

            case TrafficState.NO_DATA:
              callback.onNoData();
              break;

            case TrafficState.OUTDATED:
              callback.onOutdated();
              break;

            case TrafficState.NETWORK_ERROR:
              callback.onNetworkError();
              break;

            case TrafficState.EXPIRED_DATA:
              callback.onExpiredData();
              break;

            case TrafficState.EXPIRED_APP:
              callback.onExpiredApp();
              break;

            default:
              throw new IllegalArgumentException("Unsupported traffic state: " + mState);
          }
        }
      });
    }

    private void iterateOverCallbacks(@NonNull Utils.Proc<TrafficCallback> proc)
    {
      for (TrafficCallback callback : mCallbacks)
        proc.invoke(callback);
    }
  }

  public interface TrafficCallback
  {
    void onEnabled();
    void onDisabled();
    void onWaitingData();
    void onOutdated();
    void onNoData();
    void onNetworkError();
    void onExpiredData();
    void onExpiredApp();
  }
}
