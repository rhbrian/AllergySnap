package com.example.brhee.allergysnap;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


public class ResultActivity extends AppCompatActivity {

    public String ingredients = "";
    public String product_name = "";
    public String barcode_number;

    private FirebaseAuth mAuth;
    private DatabaseReference myRef;
    private FirebaseDatabase mFirebaseDatabase;
    private FirebaseUser user;
    private User userObj;
    private String userID;

    static ResultActivity activityA;
    public static ResultActivity getInstance() {
        return activityA;
    }

    public class Sample {

        public String item_name;
        public String nf_ingredient_statement;

        public class Store {
            public String store_name;
            public String store_price;
            public String product_url;
            public String currency_code;
            public String currency_symbol;
        }

        public class Review {
            public String name;
            public String rating;
            public String title;
            public String review;
            public String datetime;
        }

        public class Product {
            public String barcode_number;
            public String barcode_type;
            public String barcode_formats;
            public String mpn;
            public String model;
            public String asin;
            public String product_name;
            public String title;
            public String category;
            public String manufacturer;
            public String brand;
            public String label;
            public String author;
            public String publisher;
            public String artist;
            public String actor;
            public String director;
            public String studio;
            public String genre;
            public String audience_rating;
            public String ingredients;
            public String nutrition_facts;
            public String color;
            public String format;
            public String package_quantity;
            public String size;
            public String length;
            public String width;
            public String height;
            public String weight;
            public String release_date;
            public String description;
            public Object[] features;
            public String[] images;
            public Store[] stores;
            public Review[] reviews;
        }

        public class RootObject {
            public Product[] products;
        }
    }

    public TextView barcodeResult;
    public TextView barcodeIngredients;
    public TextView barcodeName;
    public TextView qrResult;
    public TextView conflictView;
    public ListView ingredients_feed;
    public ListView conflicts_feed;


    public AsyncTask data2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        activityA = this;

        mAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        myRef = mFirebaseDatabase.getReference("Users");
        user = mAuth.getCurrentUser();

        barcodeResult = (TextView)findViewById(R.id.barcode_result);
        barcodeName = (TextView)findViewById(R.id.barcode_name);
        barcodeIngredients = (TextView)findViewById(R.id.barcode_ingredients);
        qrResult = (TextView)findViewById(R.id.qr_result);
        conflictView = (TextView)findViewById(R.id.conflict_result);

        //barcode_number = "";
        product_name = "";
        ingredients = "";

        ingredients_feed = (ListView)findViewById(R.id.ingredients_feed);
        conflicts_feed = (ListView)findViewById(R.id.conflicts_feed);

        // Checks MainActivity bundle
        Bundle bundle = getIntent().getExtras();
        barcodeIngredients.setMovementMethod(new ScrollingMovementMethod());
        conflictView.setMovementMethod(new ScrollingMovementMethod());
        // Bundle from MainActivity
        barcodeResult.setText(bundle.getString("barcode_number"));
        if (bundle.getString("ingredients") != null) {
            barcodeIngredients.setText(bundle.getString("ingredients"));
            String source = bundle.getString("ingredients");
            source = source.replaceAll("\\.", " ");
            source = source.replaceAll("\\/", " ");
            source = source.replaceAll("\\s", " ");
            List<String> ingList = new ArrayList<>();
            StringTokenizer st = new StringTokenizer(source, "[]():,");
            while (st.hasMoreTokens()) {
                String s = st.nextToken();
                ingList.add(s);
            }
            ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, (List) ingList);
            ingredients_feed.setAdapter(adapter);
            final String tokenizer = source;
            if (user != null) {
                userID = user.getUid();
                Query userData = myRef;
                userData.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            userObj = dataSnapshot.child(userID).getValue(User.class);
                            if (userObj != null) {
                                conflictCheck(tokenizer,userObj.allergies);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        }
        barcodeName.setText(bundle.getString("product_name"));
        if (bundle.getString("qr_result") != null) {
            qrResult.setText(bundle.getString("qr_result"));
            qrResult.setMovementMethod(LinkMovementMethod.getInstance());
        }

        //Bundle from Camera2
        String s = bundle.getString("picture_value");
        if (s != null) {
            StringTokenizer st = new StringTokenizer(s, ",");
            String dispText = "";
            String d;
            List<String> ingredientsList = new ArrayList<String>();
            int count = 0;
            while (st.hasMoreTokens()) {
                d = st.nextToken();
                ingredientsList.add(d);
                dispText += d;
                if (count > 0 && count % 5 == 0 ) dispText += System.getProperty("line.separator");
                else dispText += ", ";
                count++;
            }
            ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, (List) ingredientsList);
            ingredients_feed.setAdapter(adapter);
            final String tokenizer = s;
            if (user != null) {
                userID = user.getUid();
                Query userData = myRef;
                userData.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            userObj = dataSnapshot.child(userID).getValue(User.class);
                            if (userObj != null) {
                                conflictCheck(tokenizer,userObj.allergies);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
            barcodeIngredients.setText(dispText);
        }
    }

    public void conflictCheck(String s, List<Allergy> allergies ) {
        StringTokenizer tokenizer = new StringTokenizer(s, ", ");
        List<String> conflictList = new ArrayList<>();
        while (tokenizer.hasMoreTokens()) {
            String conflict = tokenizer.nextToken();
            for (int x = 0; x < allergies.size(); x++) {
                if (allergies.get(x).name.toLowerCase().equals("egg") || allergies.get(x).name.toLowerCase().equals("eggs")) {
                    if (conflict.toLowerCase().equals("egg") || conflict.toLowerCase().equals("eggs")) {
                        if (!conflictList.contains("Egg")) conflictList.add("Egg");
                    }
                }
                else if (conflict.toLowerCase().equals(allergies.get(x).name.toLowerCase())) {
                    if (!conflictList.contains(conflict)) conflictList.add(conflict);
                }
            }
        }
        if (!conflictList.isEmpty()) {

            ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, (List) conflictList);
            conflicts_feed.setAdapter(adapter);

            Bundle bundle = new Bundle();
            Intent i = new Intent(getApplicationContext(), Pop.class);
            bundle.putInt("size", conflictList.size());

            String conflictText = "YOU HAVE ALLERGY CONFLICTS!" + System.getProperty("line.separator");
            for (int x = 0; x < conflictList.size(); x++) {
                conflictText += conflictList.get(x);
                conflictText += " allergy!\n";
                bundle.putString("allergy" + x, conflictList.get(x));

            }
            i.putExtras(bundle);
            startActivity(i);

            conflictView.setText(conflictText);
        }
    }

    public void scanBarcode(View v) {
        if (conflictView != null) {
            conflictView.setText("");
        }
        if (barcodeIngredients != null) {
            barcodeIngredients.setText("");
        }
        if (barcodeName != null) {
            barcodeName.setText("");
        }
        if (barcodeResult != null) {
            barcodeResult.setText("");
        }
        if (qrResult != null) {
            qrResult.setText("");
        }
        //finish();
        Intent intent = new Intent(this, Camera2.class);
        startActivityForResult(intent, 0);
    }

    public void home(View v) {
        startActivity(new Intent(ResultActivity.this, MainActivity.class));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra("barcode");

                    //conflictCheck()

                    // If the Barcode is a number
                    if (barcode.valueFormat == 5) {
                        // Increment barcode scanning - from scan again
                        if (userObj != null) {
                            userObj.scans.set(1, userObj.scans.get(1) + 1);
                        }
                        if (barcodeIngredients != null) {
                            barcodeIngredients.setText("");
                        }
                        if (barcodeName != null) {
                            barcodeName.setText("");
                        }
                        if (barcodeResult != null) {
                            barcodeResult.setText("");
                        }
                        if (qrResult != null) {
                            qrResult.setText("");
                        }

                        barcode_number = barcode.displayValue;

                        //new JsonTask().execute("https://api.barcodelookup.com/v2/products?barcode=" + barcode.displayValue + "&formatted=y&key=jjgszqhu4fhqqa6369sd9elzn13omy");
                        new JsonTask().execute("https://api.nutritionix.com/v1_1/item?upc=" + barcode.displayValue + "&appId=b50adcc6&appKey=5c4bb39799462d82436788bc1311f47e\n\n\n");

                        FirebaseDatabase.getInstance().getReference("Users")
                                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                .setValue(userObj);
                    }
                    // If the scan results in a URL
                    else if (barcode.valueFormat == 8) {
                        // Increment qr scanning - from scan again
                        userObj.scans.set(2, userObj.scans.get(2) + 1);
                        qrResult.setText(barcode.displayValue);
                        qrResult.setMovementMethod(LinkMovementMethod.getInstance());
                        FirebaseDatabase.getInstance().getReference("Users")
                                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                .setValue(userObj);
                    }
                    else {
                        barcodeResult.setText("No barcode found!");
                    }
                }
                else {
                    barcodeResult.setText("No barcode found!");
                }
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private class JsonTask extends AsyncTask<String, String, String> {

        public ProgressDialog pd = new ProgressDialog(ResultActivity.this);;

        protected void onPreExecute() {
            super.onPreExecute();
            pd.setMessage("Please wait");
            pd.setCancelable(false);
            pd.show();
        }

        protected String doInBackground(String... params) {

            HttpURLConnection connection = null;
            BufferedReader reader = null;
            //barcode_number = "";
            product_name = "";
            ingredients = "";

            try {
                URL url = new URL(params[0]);
                BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
                String str = "";
                String data2 = "";

                while (null != (str= br.readLine())) {
                    data2 +=str;
                }

                Gson g = new Gson();

                Sample value = g.fromJson(data2, Sample.class);

                //barcode_number = "";
                //barcode_number = value.i;

                product_name = "";
                product_name = value.item_name;

                ingredients = "";
                ingredients = value.nf_ingredient_statement;

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (pd.isShowing()) {
                pd.dismiss();
            }
            if (product_name != null) {
                barcodeName.setText(product_name.toString());
            }
            if (ingredients != null) {
                barcodeIngredients.setText(ingredients.toString());
            }
            if (barcode_number != null) {
                barcodeResult.setText(barcode_number);
            }

            if (ingredients != null) {
                String source = ingredients;
                source = source.replaceAll("\\.", " ");
                source = source.replaceAll("\\/", " ");
                source = source.replaceAll("\\s", " ");
                final String tokenizer = source;
                List<String> ingList2 = new ArrayList<>();
                StringTokenizer st = new StringTokenizer(source, "[]():,");
                while (st.hasMoreTokens()) {
                    String s = st.nextToken();
                    ingList2.add(s);
                }
                ArrayAdapter adapter = new ArrayAdapter(ResultActivity.this, android.R.layout.simple_list_item_1, (List) ingList2);
                ingredients_feed.setAdapter(adapter);
                if (user != null) {
                    userID = user.getUid();
                    Query userData = myRef;
                    userData.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                userObj = dataSnapshot.child(userID).getValue(User.class);
                                if (userObj != null) {
                                    conflictCheck(tokenizer,userObj.allergies);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }
            }
        }
    }
    // Changes button back
    @Override
    protected void onResume() {
        super.onResume();
        //finish();
    }
}
