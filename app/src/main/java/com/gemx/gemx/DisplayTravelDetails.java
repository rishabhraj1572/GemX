package com.gemx.gemx;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class DisplayTravelDetails extends AppCompatActivity {
    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 1;
    String city = "City";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_travel);

        Intent i = getIntent();
        String heading = i.getStringExtra("heading");
        String description = i.getStringExtra("description");
        city = i.getStringExtra("city");
        Log.d("Description",description);

        TextView head = findViewById(R.id.heading);
        TextView desc = findViewById(R.id.description);

        head.setText(heading);
        desc.setText(description);

        ImageView back = findViewById(R.id.back);
        back.setOnClickListener(v -> onBackPressed());

        Button getPdfBtn = findViewById(R.id.button);
        getPdfBtn.setOnClickListener(v -> {
            // Make PDF
            if (Build.VERSION.SDK_INT <= 32) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
                } else {
                    createPdfUsingMediaStore(heading, description);
                }
            } else {
                createPdfUsingMediaStore(heading, description);
            }
        });
    }

    private void createPdfUsingMediaStore(String heading, String description) {
        PdfDocument pdfDocument = new PdfDocument();

        // Create paint objects for heading and description
        Paint headingPaint = new Paint();
        headingPaint.setColor(Color.BLACK);
        headingPaint.setTextSize(20 * getResources().getDisplayMetrics().scaledDensity);
        headingPaint.setTextAlign(Paint.Align.LEFT);

        Paint descriptionPaint = new Paint();
        descriptionPaint.setColor(Color.BLACK);
        descriptionPaint.setTextSize(12 * getResources().getDisplayMetrics().scaledDensity);
        descriptionPaint.setTextAlign(Paint.Align.LEFT);

        // Define margins and spacing
        int marginStart = (int) (15 * getResources().getDisplayMetrics().density);
        int marginTop = (int) (25 * getResources().getDisplayMetrics().density);
        int lineSpacing = (int) (10 * getResources().getDisplayMetrics().density);

        // Split text into lines
        List<String> headingLines = splitTextIntoLines(headingPaint, heading, 595 - 2 * marginStart);
        List<String> descriptionLines = splitTextIntoLines(descriptionPaint, description, 595 - 2 * marginStart);

        // Create a page
        int pageHeight = 842;
        int yPosition = marginTop;
        PdfDocument.Page page = createNewPage(pdfDocument, 595, pageHeight);
        Canvas canvas = page.getCanvas();

        // Write heading
        for (String line : headingLines) {
            if (yPosition + headingPaint.getTextSize() > pageHeight) {
                pdfDocument.finishPage(page);
                page = createNewPage(pdfDocument, 595, pageHeight);
                canvas = page.getCanvas();
                yPosition = marginTop;
            }
            canvas.drawText(line, marginStart, yPosition, headingPaint);
            yPosition += headingPaint.getTextSize() + lineSpacing;
        }

        // Write description
        for (String line : descriptionLines) {
            if (yPosition + descriptionPaint.getTextSize() > pageHeight) {
                pdfDocument.finishPage(page);
                page = createNewPage(pdfDocument, 595, pageHeight);
                canvas = page.getCanvas();
                yPosition = marginTop;
            }
            canvas.drawText(line, marginStart, yPosition, descriptionPaint);
            yPosition += descriptionPaint.getTextSize() + lineSpacing;
        }

        // Finish the last page
        pdfDocument.finishPage(page);

        // Save the document
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "Itinerary For "+city+".pdf");
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        ContentResolver resolver = getContentResolver();
        Uri uri = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
        }

        if (uri != null) {
            try (OutputStream outputStream = resolver.openOutputStream(uri)) {
                if (outputStream != null) {
                    pdfDocument.writeTo(outputStream);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        pdfDocument.close();
        Toast.makeText(this, "PDF saved successfully!", Toast.LENGTH_SHORT).show();
    }

    private PdfDocument.Page createNewPage(PdfDocument pdfDocument, int width, int height) {
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(width, height, pdfDocument.getPages().size() + 1).create();
        return pdfDocument.startPage(pageInfo);
    }


    private List<String> splitTextIntoLines(Paint paint, String text, float maxWidth) {
        String[] lines = text.split("\n");
        List<String> wrappedLines = new ArrayList<>();

        for (String line : lines) {
            String[] words = line.split(" ");
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
                float textWidth = paint.measureText(testLine);
                if (textWidth > maxWidth) {
                    wrappedLines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    currentLine.append(currentLine.length() == 0 ? word : " " + word);
                }
            }

            if (currentLine.length() > 0) {
                wrappedLines.add(currentLine.toString());
            }
        }

        return wrappedLines;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                createPdfUsingMediaStore(getIntent().getStringExtra("heading"), getIntent().getStringExtra("description"));
            } else {
                Toast.makeText(this, "Error: Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
