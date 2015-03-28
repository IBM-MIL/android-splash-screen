/*
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2015. All Rights Reserved.
 */

package com.ibm.mil.splashscreendemo;

import android.os.AsyncTask;
import android.os.Bundle;

public class WorkerSplashActivity extends SplashActivity {
    private static final String IMAGE_URL = "";

    private ImageLoader mImageLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mImageLoader = new ImageLoader();
        mImageLoader.execute(IMAGE_URL);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mImageLoader.getStatus() != AsyncTask.Status.FINISHED) {
            mImageLoader.cancel(true);
        }
    }

}
