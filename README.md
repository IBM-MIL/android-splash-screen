# Implementing a Splash Screen on Android

### Introduction

You may find yourself wanting to implement a splash screen for your Android app. Reasons for doing so include the visual appeal, to match an existing iOS version of your app, or to perform any necessary background work at start up. It should be noted that splash screens are certainly not required in your app. In fact, [some feel that they should be avoided entirely](http://cyrilmottier.com/2012/05/03/splash-screens-are-evil-dont-use-them/). Still, it is not uncommon to come across Android apps that utilize the notion of a splash screen.

This blog post provides a detailed outline for implementing a splash screen on Android. While the implementation is relatively straight forward, there are a few caveats that developers often overlook. We've also provided a working sample project that can be run on your device or emulator.

### The Basics

Create an `Activity` named `SplashActivity` that extends `Activity` as opposed to `ActionBarActivity`.

###### SplashActivity.java
``` java
public class SplashActivity extends Activity {
  ...
}
```

This will exclude the `ActionBar` from showing in `SplashActivity`. Next, declare `SplashActivity` as the **launcher activity** in the app's manifest file.

###### AndroidManifest.xml
``` xml
...
<activity
  android:name=".SplashActivity"
  android:label="@string/app_name">
    <intent-filter>
      <action android:name="android.intent.action.MAIN" />
      <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
...
```

`SplashActivity` will now be the initial screen shown when the app launches.

The layout for a splash screen is typically very simple. For our purposes, we will simply show an `ImageView` in the center of the screen. The XML layout for `SplashActivity` follows:

###### activity_splash.xml
``` xml
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@android:color/white"
    tools:context="com.ibm.mil.splashscreendemo.SplashActivity">

    <ImageView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_centerInParent="true"
        android:src="@drawable/splash_logo"
        android:contentDescription="@string/splash_logo_desc" />

</RelativeLayout>
```

Inside `onCreate(Bundle)` of `SplashActivity` we will initialize a `Handler` and its corresponding `Runnable` that will start the app's main activity after a specified duration.

###### SplashActivity.java
``` java
private Handler mHandler;
private Runnable mRunnable;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_splash);

    mHandler = new Handler();
    mRunnable = new Runnable() {
        @Override
        public void run() {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }
    };
}
```

`Handler` is part of the `android.os` package and it allows developers to easily utilize a message queue for a thread without having to deal with synchronization and lower-level threading APIs in Java. In our case, we will use the `Handler` to invoke our `Runnable` object at a later point in time on the main UI thread without blocking other operations from occurring on the same thread in the meantime.

The implementation of `run()` is straightforward: the app's main activity (simply called `MainActivity` in the example) is started via an `Intent`. The call to `finish()` on `SplashActivity` marks it as done and effectively removes it from the app's back stack. This prevents the user from seeing the splash screen again if they back out of the app via the system's back button.

From `onResume()` in `SplashActivity`, the method `postDelayed(Runnable, long)` is invoked on `mHandler` and is passed both our `Runnable` instance and a delay measured in milliseconds. This will enqueue `mRunnable` onto the thread's message queue and then dequeue it for execution after the specified delay.

###### SplashActivity.java
``` java
private static final long SPLASH_DURATION = 2500L;
...
@Override
public void onResume() {
    super.onResume();
    mHandler.postDelayed(mRunnable, SPLASH_DURATION);
}
```

We will also remove `mRunnable` from the `Handler` in `onPause()` to ensure it doesn't execute when `SplashActivity` is no longer in a resumed state.

###### SplashActivity.java
``` java
@Override
public void onPause() {
    super.onPause();
    mHandler.removeCallbacks(mRunnable);
}
```

And that encompasses all of the necessary components for properly implementing a basic splash screen. The next section describes some of the pitfalls that developers sometimes fall into when implemetning a splash screen. For more advanced uses of a splash screen, skip to the section [Performing Background Work]().

### Gotchas

### Performing Background Work

### Conclusion
