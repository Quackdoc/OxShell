package com.OxGames.OxShell;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.OxGames.OxShell.Data.IntentLaunchData;

public class AccessService extends AccessibilityService {
    private static AccessService instance;
    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        AccessibilityServiceInfo info = getServiceInfo();

        // Set the type of events that this service wants to listen to. Others
        // won't be passed to this service.
        info.eventTypes = ~0;

        // If you only want this service to work with specific applications, set their
        // package names here. Otherwise, when the service is activated, it will listen
        // to events from all applications.
        //info.packageNames = new String[] {"com.example.android.myFirstApp", "com.example.android.mySecondApp"};

        // Set the type of feedback your service will provide.
        info.feedbackType = ~0;

        // Comma separated package names from which this service would like to receive events (leave out for all packages).
        //info.packageNames = new String[] { "com.OxGames.OxShell" };

        // Default services are invoked only if no package-specific ones are present
        // for the type of AccessibilityEvent generated. This service *is*
        // application-specific, so the flag isn't necessary. If this was a
        // general-purpose service, it would be worth considering setting the
        // DEFAULT flag.

        info.notificationTimeout = 100;

        this.setServiceInfo(info);
    }
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        //Log.d("AccessService", event.toString());
        // get the source node of the event
        //AccessibilityNodeInfo nodeInfo = event.getSource();

        // Use the event and node information to determine
        // what action to take

        // take action on behalf of the user
        //nodeInfo.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);

        // recycle the nodeInfo object
        //nodeInfo.recycle();
    }
    @Override
    public void onInterrupt() {

    }

    public static void showRecentApps() {
        if (instance != null)
            instance.performGlobalAction(GLOBAL_ACTION_RECENTS);
        else {
            //TODO: show popup telling user why they're about to go to the accessibility settings and option to cancel
            Log.e("AccessService", "Service not turned on");
            IntentLaunchData.createFromAction(Settings.ACTION_ACCESSIBILITY_SETTINGS, Intent.FLAG_ACTIVITY_NEW_TASK).launch();
            //context.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            //IntentLaunchData recents = new IntentLaunchData("com.android.settings.accessibility.AccessibilitySettings");
            //recents.launch();
        }
    }
}
