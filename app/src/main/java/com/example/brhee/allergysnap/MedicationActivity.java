package com.example.brhee.allergysnap;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.w3c.dom.Text;

public class MedicationActivity extends AppCompatActivity implements View.OnClickListener {


    private TextView jsonText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medication);

        jsonText = findViewById(R.id.jsontext);

        findViewById(R.id.test_button).setOnClickListener(this);

    }

    public void search() {


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.test_button:
                search();
                break;
        }
    }
}
