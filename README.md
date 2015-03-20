# Splash Screens on Android

### Table of Contents

1. [Introduction](#introduction)
2. [The Basics](#the-basics)
3. [Gotchas](#gotchas)
4. [Performing Background Work](#performing-background-work)
5. [Conclusion](#conclusion)

### Introduction

You may find yourself needing to implement a splash screen for your Android app. Reasons for doing so might include having to match an existing iOS design of your app, to perform any necessary background work at start up, or simply for the visual appeal that it provides. It should be noted that splash screens are certainly not required in your app. In fact, [some feel that they should be avoided entirely](http://cyrilmottier.com/2012/05/03/splash-screens-are-evil-dont-use-them/). Still, it is not uncommon to come across Android apps that utilize a splash screen.

This blog post provides a detailed outline for implementing a splash screen on Android. While the implementation is relatively straight forward, there are a few caveats that developers often overlook. We've also provided a working sample project that can be run on your device or emulator.

### The Basics

First, create an `Activity` named `SplashActivity`.

**SplashActivity.java**
``` java
public class SplashActivity extends Activity {
    ...
}
```

It's important that we extend from `Activity` and not `ActionBarActivity`. This will exclude the `ActionBar` from being visible on our splash screen. Next, declare `SplashActivity` as the **launcher activity** in the app's manifest file.

**AndroidManifest.xml**
``` xml
<activity
  android:name=".SplashActivity"
  android:label="@string/app_name">
    <intent-filter>
      <action android:name="android.intent.action.MAIN" />
      <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

Our splash screen will now be the initial screen shown when the app launches.

The layout for a splash screen is typically very simple. For our purposes, we will simply show an `ImageView` in the center of the screen.

**activity_splash.xml**
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

Inside `onCreate(Bundle)` we will initialize a `Handler` and its corresponding `Runnable` that will be responsible for starting the app's main activity after a specified duration.

**SplashActivity.java**
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

`Handler` is part of the `android.os` package and it easily allows developers to utilize a message queue for a thread without having to deal with synchronization and the lower-level threading APIs in Java. In our case, we will use the `Handler` to invoke our `Runnable` object at a later point in time on the main UI thread without blocking other operations from occurring in the meantime.

The implementation of `run()` is straightforward: the app's main activity (simply called `MainActivity` in the example) is started via an `Intent`. The call to `finish()` marks `SplashActivity` as done and effectively removes it from the app's back stack. This prevents the user from seeing the splash screen again if they back out of the app via the system's back button.

From `onResume()`, the method `postDelayed(Runnable, long)` is invoked on `mHandler` and is passed both our `Runnable` instance and a timed delay which is measured in milliseconds. This will enqueue `mRunnable` onto the thread's message queue and then dequeue it for execution after our specified delay.

**SplashActivity.java**
``` java
private static final long SPLASH_DURATION = 2500L;
...
@Override
protected void onResume() {
    super.onResume();
    mHandler.postDelayed(mRunnable, SPLASH_DURATION);
}
```

We will also remove `mRunnable` from the `Handler` in `onPause()` to ensure it doesn't execute when `SplashActivity` is no longer in a resumed state.

**SplashActivity.java**
``` java
@Override
protected void onPause() {
    super.onPause();
    mHandler.removeCallbacks(mRunnable);
}
```

And that encompasses all of the necessary components for properly implementing a basic splash screen. The next section describes some of the pitfalls that developers sometimes encounter when implemetning a splash screen. For more advanced uses of a splash screen, skip to the section [Performing Background Work](#performing-background-work).

### Gotchas

There are often a few oversights made when developing a splash screen. For example, we made sure to remove our `Runnable` from the `Handler` in `onPause()` and then effectively restart the splash screen duration each time `onResume()` was called.

**SplashActiviy.java**
``` java
@Override
protected void onResume() {
    super.onResume();
    mHandler.postDelayed(mRunnable, SPLASH_DURATION);
}

@Override
protected void onPause() {
    super.onPause();
    mHandler.removeCallbacks(mRunnable);
}
```

If instead we moved the invocation of `postDelayed(Runnable, long)` to `onCreate()` and then didn't bother removing the `Runnable` callback in `onPause()`, the `Runnable` would end up being executed even if the app was in the background. As a result, `MainActivity` would suddenly appear on the user's screen even though the app was no longer in an active state.

``` java
// DON'T DO THIS!
@Override
protected void onCreate(Bundle savedInstanceState) {
    ...
    new Handler().postDelayed(new Runnable() {
        @Override
        public void run() {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }
    }, SPLASH_DURATION);
}
```

Likewise, we could have forgot to cancel our `AsyncTask` in `onDestroy()` or in our `Runnable` implementation. Forgetting to do so could easily cause a **null pointer exception** at runtime. Imagine if we had the following line of code in `onPostExecute(Bitmap)`:

**ImageLoader.java**
``` java
@Override
protected void onPostExecute(Bitmap result) {
    Log.i(SplashActivity.class.getName(), "Image successfully downloaded.");

    if (result != null) {
        // do something with the bitmap
    }
}
```

### Performing Background Work

A common pattern is to perform some necessary background work at start up when the splash screen is present. This can be helpful if there is data shown on the home screen that needs to be retrieved from a network or external memory device. A valid alternative is to display a progress bar or dialog to indicate to the user that a long standing operation is being performed. With a splash screen, while inhibiting the user's progress through the app, we get the benefit of hiding expensive operations from the user.

As a trivial example, let's assume we wanted to download an image from the network (e.g. a user's profile image that is shown on the home screen). We can create an `AsyncTask` that will do exactly this:

**ImageLoader.java**
``` java
public class ImageLoader extends AsyncTask<String, Void, Bitmap> {
    @Override
    protected Bitmap doInBackground(String... urls) {
        // make network call to fetch image
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(new URL(urls[0]).openStream());
        } catch (Exception e) {
            e.printStackTrace();
            cancel(true);
        }
        return bitmap;
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        Log.i(SplashActivity.class.getName(), "Image successfully downloaded!");
        if (result != null) {
            // do something with the bitmap
            ...
        }
    }

    @Override
    protected void onCancelled() {
        Log.i(SplashActivity.class.getName(), "Image download not successful.");
    }
}
```

`doInBackground(String...)` runs on its own thread and performs the actual network call for fetching the image. Both `onPostExecute(Bitmap)` and `onCancelled()` will run on the thread that the `ImageLoader` was invoked from, which is the main UI thread in our case.

For demonstration purposes we've written our own `AsyncTask` for grabbing the image. [Excellent libraries](http://square.github.io/picasso/) already exist that perform this operation and more. Note that making a network call requires the `INTERNET` permission to be added to the manifest file.

How the `AsyncTask` interacts with our splash screen is up to us. A good approach is to have the splash screen remain visible for a specified duration, like how we did in the [previous section](#the-basics), and cancel the background task if it takes too long. It is not a good idea to have the duration of our splash screen dependent on the background work being completed. Many types of tasks, such as those involving network calls, can take an undetermined amount of time to complete and having a timeout mechanism is important. We can simply augment our `Runnable` object to cancel the `AsyncTask` if it hasn't been completed after the specified delay in order to achieve this.

**SplashActivity.java**
``` java
private ImageLoader mImageLoader;
...
@Override
protected void onCreate(Bundle savedInstanceState) {
    ...
    mImageLoader = new ImageLoader();
    mRunnable = new Runnable() {
        @Override
        public void run() {
            // cancel AsyncTask if it hasn't finished
            if (mImageLoader.getStatus() != AsyncTask.Status.FINISHED) {
                mImageLoader.cancel(true);
            }

            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }
    };

    mImageLoader.execute(IMAGE_URL);
}
```

Executing `ImageLoader` inside of `onCreate(Bundle)` allows the background work to start as soon as the `SplashActivity` is created. Consequently, we can cancel the task in `onDestroy()` to allow the operation to continue in the background even if the activity is no longer visible.

**SplashActivity.java**
``` java
@Override
protected void onDestroy() {
    super.onDestroy();
    mImageLoader.cancel(true);
}
```

The rest of `SplashActivity` remains intact from the [previous section](#the-basics).

### Conclusion
