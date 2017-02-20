//
// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license.
//
// Microsoft Cognitive Services (formerly Project Oxford): https://www.microsoft.com/cognitive-services
//
// Microsoft Cognitive Services (formerly Project Oxford) GitHub:
// https://github.com/Microsoft/Cognitive-Face-Android
//
// Copyright (c) Microsoft Corporation
// All rights reserved.
//
// MIT License:
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
package com.microsoft.projectoxford.face.samples.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.samples.R;
import com.microsoft.projectoxford.face.samples.helper.ImageHelper;
import com.microsoft.projectoxford.face.samples.helper.LogHelper;
import com.microsoft.projectoxford.face.samples.helper.SampleApp;
import com.microsoft.projectoxford.face.samples.log.DetectionLogActivity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.microsoft.speech.tts.*;

public class DetectionActivity extends AppCompatActivity {
    // Note: Sign up at http://www.projectoxford.ai for the client credentials.
    private Synthesizer m_syn;

    // Background task of face detection.
    private class DetectionTask extends AsyncTask<InputStream, String, Face[]> {
        private boolean mSucceed = true;

        @Override
        protected Face[] doInBackground(InputStream... params) {
            // Get an instance of face service client to detect faces in image.
            FaceServiceClient faceServiceClient = SampleApp.getFaceServiceClient();
            try {
                publishProgress("Detecting...");

                // Start detection.
                return faceServiceClient.detect(
                        params[0],  /* Input stream of image to detect */
                        true,       /* Whether to return face ID */
                        true,       /* Whether to return face landmarks */
                        /* Which face attributes to analyze, currently we support:
                           age,gender,headPose,smile,facialHair */
                        new FaceServiceClient.FaceAttributeType[] {
                                FaceServiceClient.FaceAttributeType.Age,
                                FaceServiceClient.FaceAttributeType.Gender,
                                FaceServiceClient.FaceAttributeType.Glasses,
                                FaceServiceClient.FaceAttributeType.Smile,
                                FaceServiceClient.FaceAttributeType.HeadPose
                        });
            } catch (Exception e) {
                mSucceed = false;
                publishProgress(e.getMessage());
                addLog(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            mProgressDialog.show();
            addLog("Request: Detecting in image " + mImageUri);
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            mProgressDialog.setMessage(progress[0]);
            setInfo(progress[0]);
        }

        @Override
        protected void onPostExecute(Face[] result) {
            if (mSucceed) {
                addLog("Response: Success. Detected " + (result == null ? 0 : result.length)
                        + " face(s) in " + mImageUri);
            }

            // Show the result on screen when detection is done.
            setUiAfterDetection(result, mSucceed);
        }
    }

    // Flag to indicate which task is to be performed.
    private static final int REQUEST_SELECT_IMAGE = 0;

    // The URI of the image selected to detect.
    private Uri mImageUri;

    // The image selected to detect.
    private Bitmap mBitmap;

    // Progress dialog popped up when communicating with server.
    ProgressDialog mProgressDialog;

    // When the activity is created, set all the member variables to initial state.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(getString(R.string.progress_dialog_title));

        // Disable button "detect" as the image to detect is not selected.
        setDetectButtonEnabledStatus(false);

        LogHelper.clearDetectionLog();

    }

    private void onSpeekerTTS(String tts_text){
        if (getString(R.string.api_key).startsWith("Please")) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.add_subscription_key_tip_title))
                    .setMessage(getString(R.string.add_subscription_key_tip))
                    .setCancelable(false)
                    .show();
        } else {

            if (m_syn == null) {
                // Create Text To Speech Synthesizer.
                m_syn = new Synthesizer(getString(R.string.api_key));
            }

            Toast.makeText(this, "If the wave is not played, please see the log for more information.", Toast.LENGTH_LONG).show();

            m_syn.SetServiceStrategy(Synthesizer.ServiceStrategy.AlwaysService);

           // Voice v = new Voice("en-US", "Microsoft Server Speech Text to Speech Voice (en-US, ZiraRUS)", Voice.Gender.Female, true);
            Voice v = new Voice("zh-CN", "Microsoft Server Speech Text to Speech Voice (zh-CN, HuihuiRUS)", Voice.Gender.Female, true);
            m_syn.SetVoice(v, null);

            // Use a string for speech.
            //m_syn.SpeakToAudio(getString(R.string.tts_text));
            m_syn.SpeakToAudio(tts_text);

            // Use SSML for speech.
            String text = "<speak version=\"1.0\" xmlns=\"http://www.w3.org/2001/10/synthesis\" xmlns:mstts=\"http://www.w3.org/2001/mstts\" xml:lang=\"en-US\"><voice name=\"Microsoft Server Speech Text to Speech Voice (en-US, ZiraRUS)\"></voice></speak>";
            m_syn.SpeakSSMLToAudio(text);
        }
    }

    // Save the activity state when it's going to stop.
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable("ImageUri", mImageUri);
    }

    // Recover the saved state when the activity is recreated.
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mImageUri = savedInstanceState.getParcelable("ImageUri");
        if (mImageUri != null) {
            mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                    mImageUri, getContentResolver());
        }
    }

    // Called when image selection is done.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_SELECT_IMAGE:
                if (resultCode == RESULT_OK) {
                    // If image is selected successfully, set the image URI and bitmap.
                    mImageUri = data.getData();
                    mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                            mImageUri, getContentResolver());
                    if (mBitmap != null) {
                        // Show the image on screen.
                        ImageView imageView = (ImageView) findViewById(R.id.image);
                        imageView.setImageBitmap(mBitmap);

                        // Add detection log.
                        addLog("Image: " + mImageUri + " resized to " + mBitmap.getWidth()
                                + "x" + mBitmap.getHeight());
                    }

                    // Clear the detection result.
                    FaceListAdapter faceListAdapter = new FaceListAdapter(null);
                    ListView listView = (ListView) findViewById(R.id.list_detected_faces);
                    listView.setAdapter(faceListAdapter);

                    // Clear the information panel.
                    setInfo("");

                    // Enable button "detect" as the image is selected and not detected.
                    setDetectButtonEnabledStatus(true);
                }
                break;
            default:
                break;
        }
    }

    // Called when the "Select Image" button is clicked.
    public void selectImage(View view) {
        Intent intent = new Intent(this, SelectImageActivity.class);
        startActivityForResult(intent, REQUEST_SELECT_IMAGE);
    }

    // Called when the "Detect" button is clicked.
    public void detect(View view) {
        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        // Start a background task to detect faces in the image.
        new DetectionTask().execute(inputStream);

        // Prevent button click during detecting.
        setAllButtonsEnabledStatus(false);
    }

    // View the log of service calls.
    public void viewLog(View view) {
        Intent intent = new Intent(this, DetectionLogActivity.class);
        startActivity(intent);
    }

    // Show the result on screen when detection is done.
    private void setUiAfterDetection(Face[] result, boolean succeed) {
        // Detection is done, hide the progress dialog.
        mProgressDialog.dismiss();

        // Enable all the buttons.
        setAllButtonsEnabledStatus(true);

        // Disable button "detect" as the image has already been detected.
        setDetectButtonEnabledStatus(false);

        if (succeed) {
            // The information about the detection result.
            String detectionResult;
            if (result != null) {
                detectionResult = result.length + " face"
                        + (result.length != 1 ? "s" : "") + " detected";

                // Show the detected faces on original image.
                ImageView imageView = (ImageView) findViewById(R.id.image);
                imageView.setImageBitmap(ImageHelper.drawFaceRectanglesOnBitmap(
                        mBitmap, result, true));

                // Set the adapter of the ListView which contains the details of the detected faces.
                FaceListAdapter faceListAdapter = new FaceListAdapter(result);

                // Show the detailed list of detected faces.
                ListView listView = (ListView) findViewById(R.id.list_detected_faces);
                listView.setAdapter(faceListAdapter);
            } else {
                detectionResult = "0 face detected";
            }
            setInfo(detectionResult);
        }

        mImageUri = null;
        mBitmap = null;
    }

    // Set whether the buttons are enabled.
    private void setDetectButtonEnabledStatus(boolean isEnabled) {
        Button detectButton = (Button) findViewById(R.id.detect);
        detectButton.setEnabled(isEnabled);
    }

    // Set whether the buttons are enabled.
    private void setAllButtonsEnabledStatus(boolean isEnabled) {
        Button selectImageButton = (Button) findViewById(R.id.select_image);
        selectImageButton.setEnabled(isEnabled);

        Button detectButton = (Button) findViewById(R.id.detect);
        detectButton.setEnabled(isEnabled);

        Button ViewLogButton = (Button) findViewById(R.id.view_log);
        ViewLogButton.setEnabled(isEnabled);
    }

    // Set the information panel on screen.
    private void setInfo(String info) {
        TextView textView = (TextView) findViewById(R.id.info);
        textView.setText(info);
    }

    // Add a log item.
    private void addLog(String log) {
        LogHelper.addDetectionLog(log);
    }

    // The adapter of the GridView which contains the details of the detected faces.
    private class FaceListAdapter extends BaseAdapter {
        // The detected faces.
        List<Face> faces;

        // The thumbnails of detected faces.
        List<Bitmap> faceThumbnails;

        // Initialize with detection result.
        FaceListAdapter(Face[] detectionResult) {
            faces = new ArrayList<>();
            faceThumbnails = new ArrayList<>();

            if (detectionResult != null) {
                faces = Arrays.asList(detectionResult);
                for (Face face : faces) {
                    try {
                        // Crop face thumbnail with five main landmarks drawn from original image.
                        faceThumbnails.add(ImageHelper.generateFaceThumbnail(
                                mBitmap, face.faceRectangle));
                    } catch (IOException e) {
                        // Show the exception when generating face thumbnail fails.
                        setInfo(e.getMessage());
                    }
                }
            }
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        @Override
        public int getCount() {
            return faces.size();
        }

        @Override
        public Object getItem(int position) {
            return faces.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater layoutInflater =
                        (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(R.layout.item_face_with_description, parent, false);
            }
            convertView.setId(position);

            // Show the face thumbnail.
            ((ImageView) convertView.findViewById(R.id.face_thumbnail)).setImageBitmap(
                    faceThumbnails.get(position));

            // Show the face details.
            DecimalFormat formatter = new DecimalFormat("#0.0");
            String face_description = "Age: " + formatter.format(faces.get(position).faceAttributes.age) + "\n"
                    + "Gender: " + faces.get(position).faceAttributes.gender + "\n"
                    + "Head pose(in degree): roll(" + formatter.format(faces.get(position).faceAttributes.headPose.roll) + "), "
                    + "yaw(" + formatter.format(faces.get(position).faceAttributes.headPose.yaw) + ")\n"
                    + "Glasses: " + faces.get(position).faceAttributes.glasses + "\n"
                    + "Smile: " + formatter.format(faces.get(position).faceAttributes.smile);
            ((TextView) convertView.findViewById(R.id.text_detected_face)).setText(face_description);

            face_description = "";

            if (faces.get(position).faceAttributes.gender.equals("male")){
                face_description += "您好，先生,";
            }
            else if (faces.get(position).faceAttributes.gender.equals("female")){
                face_description += "您好，女士,";
            }

            if (faces.get(position).faceAttributes.age<40){
                face_description += "您看上去真年轻啊。";
            }
            else{
                face_description += "您看上去一点也不嫌老。";
            }

            if (faces.get(position).faceAttributes.glasses.toString().toLowerCase().equals("readingglasses")){
                face_description += "您带着眼镜确实很帅气！";
            }
            else if (faces.get(position).faceAttributes.glasses.toString().toLowerCase().equals("sunglasses")){
                face_description += "带上这副墨镜显得太酷了！";
            }

            if (faces.get(position).faceAttributes.smile>0.1){
                face_description += "笑起来也真迷人！";
            }
            else{
                face_description += "要是再微笑一点就更棒了。";
            }

            onSpeekerTTS(face_description);

            return convertView;
        }
    }
}
