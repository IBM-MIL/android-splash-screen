# android-splash-screen

While Android does not require an app to have a splash screen, having one for either the visual appeal or to perform any necessary background work before startup can be useful. While implementing a splash screen is fairly straightforward, there are some subtle details that are often overlooked.

Create an activity for the splash screen and declare it as the launcher activity in your app's manifest file. The layout for a splash screen is typically simple; in our case it is an ImageView centered in the middle of the screen.

We will use a Handler object in order to transition to our main activity after a pre-determined amount of time. The Handler class provides a method postDelayed(Runnable r, long delayMillis) that will fire off the Runnable object after the specified duration. It does this from the thread that it was invoked from (the main UI thread in our case) without blocking.

Our Runnable object will simply start the MainActivity when it gets invoked by the Handler. Calling finish() on the current activity (SplashActivity) will prevent it from being added to the backstack. This prevents the user from seeing the splash screen again if they back out of our app.

A naive implementation of our splash screen activity will invoke postDelayed(Runnable r, long delayMillis) from onCreate(Bundle savedInstanceState). If the user leaves the app (e.g. pressing the home button on the phone) while the splash screen is still visible, our app will re-appear on the user's screen when the MainActivity gets launched. To prevent this from happening, we move the invocation of postDelayed(Runnable r, long delayMillis) to the activity's onStart() and ensure we prevent our Runnable object from being called by the Handler in onStop(). The latter is done by passing a reference to our Runnable object to the Handler's removeCallbacks(Runnable r) method.
