package com.gemx.gemx;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "CameraPreview";
    private SurfaceHolder mHolder;
    private Camera mCamera;

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);
                setCameraDisplayOrientation((Activity) getContext(), Camera.CameraInfo.CAMERA_FACING_BACK, mCamera);
                mCamera.startPreview();
            } else {
                Log.e(TAG, "Camera is null in surfaceCreated.");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (mHolder.getSurface() == null) {
            return;
        }

        try {
            if (mCamera != null) {
                mCamera.stopPreview();
            }
        } catch (Exception e) {
        }

        if (mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            Camera.Size optimalSize = getOptimal16x9PreviewSize(parameters.getSupportedPreviewSizes());
            if (optimalSize != null) {
                parameters.setPreviewSize(optimalSize.width, optimalSize.height);
                mCamera.setParameters(parameters);
                adjustSurfaceViewSize(optimalSize, w, h);
            }

            try {
                mCamera.setPreviewDisplay(mHolder);
                setCameraDisplayOrientation((Activity) getContext(), Camera.CameraInfo.CAMERA_FACING_BACK, mCamera);
                mCamera.startPreview();
            } catch (Exception e) {
                Log.e(TAG, "Error starting camera preview: " + e.getMessage());
            }
        }
    }

    public static void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    private Camera.Size getOptimal16x9PreviewSize(List<Camera.Size> sizes) {
        Camera.Size optimalSize = null;
        for (Camera.Size size : sizes) {
            if ((float) size.width / size.height == 16.0 / 9.0) {
                optimalSize = size;
                break;
            }
        }
        return optimalSize;
    }

    private void adjustSurfaceViewSize(Camera.Size previewSize, int width, int height) {
        float ratio = (float) previewSize.width / previewSize.height;
        int newWidth, newHeight;

        if (width > height) {
            newWidth = width;
            newHeight = (int) (width / ratio);
        } else {
            newWidth = (int) (height * ratio);
            newHeight = height;
        }

        // Crop to fit the FrameLayout
        if (newHeight > height) {
            newHeight = height;
            newWidth = (int) (height * ratio);
        } else if (newWidth > width) {
            newWidth = width;
            newHeight = (int) (width / ratio);
        }

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(newWidth, newHeight);
        setLayoutParams(params);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            focusOnTouch(event);
        }
        return true;
    }

    private void focusOnTouch(MotionEvent event) {
        if (mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            if (parameters.getMaxNumMeteringAreas() > 0) {
                Log.d(TAG, "Focusing on touch area");

                int x = (int) (event.getX() / getWidth() * 2000 - 1000);
                int y = (int) (event.getY() / getHeight() * 2000 - 1000);

                List<Camera.Area> focusAreas = new ArrayList<>();
                focusAreas.add(new Camera.Area(new android.graphics.Rect(
                        x - 100, y - 100, x + 100, y + 100), 1000));

                parameters.setFocusAreas(focusAreas);
                try {
                    mCamera.setParameters(parameters);
                }catch (Exception ignore){}
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                    }
                });
            } else {
                mCamera.autoFocus(null);
            }
        }
    }
}
