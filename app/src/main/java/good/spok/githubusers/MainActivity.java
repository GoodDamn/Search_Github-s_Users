package good.spok.githubusers;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {

    private ArrayList<String> arrayList;
    private AsyncHttpClient client;

    private static final String HISTORY_FILE = "history";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AutoCompleteTextView autoCompleteTextView = findViewById(R.id.main_editText_search);
        RecyclerView recyclerView = findViewById(R.id.main_listView_users);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ArrayList<String> strings = null;
        try {
            strings = getWords(readText(openFileInput(HISTORY_FILE)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (strings != null)
            autoCompleteTextView.setAdapter(new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, strings));

        client = new AsyncHttpClient();
        autoCompleteTextView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);

        autoCompleteTextView.setOnFocusChangeListener((view, b) -> {
            if (autoCompleteTextView.getAdapter() != null) autoCompleteTextView.showDropDown();
        });
        autoCompleteTextView.setOnClickListener((view -> {
            if (autoCompleteTextView.getAdapter() != null) autoCompleteTextView.showDropDown();
        }));

        autoCompleteTextView.setOnEditorActionListener((textView, i, keyEvent) -> {
            arrayList = new ArrayList<>();
            recyclerView.setAdapter(new RecyclerHolder(arrayList));

            RequestParams params = new RequestParams();
            params.put("q", textView.getText().toString());
            client.addHeader("User-Agent","request");
            client.get("https://api.github.com/search/users", params,
                    new AsyncHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                            try {
                                String jsonString = new String(responseBody, StandardCharsets.UTF_8);
                                JSONArray jsonArray = new JSONObject(jsonString).getJSONArray("items");
                                for (int i = 0; i < jsonArray.length(); i++)
                                {
                                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                                    arrayList.add(jsonObject.getString("login") + "|" +
                                            jsonObject.getString("avatar_url") + "|" +
                                            jsonObject.getString("id") + "|" +
                                            jsonObject.getString("repos_url"));
                                }
                                recyclerView.setAdapter(new RecyclerHolder(arrayList));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                            Log.d("123456 FAIL", error.getMessage());
                        }
                    });

            try {
                if (arrayList == null)
                    saveText(textView.getText().toString(), openFileOutput(HISTORY_FILE, Context.MODE_PRIVATE));
                else saveText("|" + textView.getText().toString(), openFileOutput(HISTORY_FILE, Context.MODE_APPEND));
            } catch (FileNotFoundException e) { e.printStackTrace(); }

            // Hide soft keyboard
            View view = this.getCurrentFocus();
            if (view != null) {
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            return true;
        });

    }

    private String readText(FileInputStream fileInputStream)
    {
        try
        {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String text;
            while ((text = bufferedReader.readLine()) != null)
                stringBuilder.append(text + "\n");
            return stringBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally
        {
            try {
                if (fileInputStream != null)
                    fileInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    private void saveText(String text,FileOutputStream fileOutputStream)
    {
        try {
            fileOutputStream.write(text.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (fileOutputStream != null) fileOutputStream.close();
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private ArrayList<String> getWords(String string)
    {
        ArrayList<String> strings = new ArrayList<>();
        String word = "";
        for (char ch : string.toCharArray())
        {
            if (ch != '|') word += ch;
            else{
                Log.d("123456", word);
                strings.add(word);
                word = "";
            }
        }
        Log.d("123456", word);
        strings.add(word);
        return strings;
    }

    protected class RecyclerHolder extends RecyclerView.Adapter<RecyclerHolder.ViewHolder>
    {
        private ArrayList<String> arrayList;

        public RecyclerHolder(ArrayList<String> arrayList)
        {
            this.arrayList = arrayList;
        }

        @NonNull
        @Override
        public RecyclerHolder.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view_user, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerHolder.ViewHolder holder, int position) {
            ArrayList<String> infoList = getWords(arrayList.get(position));
            holder.user.setText(infoList.get(0));
            holder.userName_dialog.setText(infoList.get(0));
            new LoadImage(holder.avatar, holder.avatar_dialog).execute(infoList.get(1));
            holder.id.setText(infoList.get(2));
            holder.repos_url = infoList.get(3);
        }

        @Override
        public int getItemCount() {
            return arrayList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            public TextView user, id, userName_dialog;
            public ShapeableImageView avatar, avatar_dialog;
            public LinearLayout layout;
            public String repos_url = "";
            public ListView listView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);

                user = itemView.findViewById(R.id.card_view_users_name);
                avatar = itemView.findViewById(R.id.card_view_users_avatar);
                layout = itemView.findViewById(R.id.card_view_users_Llayout);

                Dialog dialog = new Dialog(itemView.getContext());
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setCancelable(true);
                dialog.setContentView(R.layout.dialog_info);

                userName_dialog = dialog.findViewById(R.id.dialog_username);
                id = dialog.findViewById(R.id.dialog_id);
                listView = dialog.findViewById(R.id.dialog_listView_repos);
                avatar_dialog = dialog.findViewById(R.id.dialog_avatar);

                layout.setOnClickListener(view -> {
                    dialog.show();

                    client.get(repos_url, new AsyncHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                            try {
                                String jsonString = new String(responseBody, StandardCharsets.UTF_8);
                                JSONArray jsonRepos = new JSONArray(jsonString);
                                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(itemView.getContext(), android.R.layout.simple_list_item_1);
                                listView.setAdapter(arrayAdapter);
                                for (int i = 0; i < jsonRepos.length(); i++)
                                    arrayAdapter.add(jsonRepos.getJSONObject(i).getString("name"));
                                listView.setAdapter(arrayAdapter);
                            } catch (JSONException e) { e.printStackTrace(); }
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                            Log.d("123456", error.getMessage());
                        }
                    });
                });
            }
        }
    }

    protected static class LoadImage extends AsyncTask<String, Void, Bitmap>
    {
        @SuppressLint("StaticFieldLeak")
        private ShapeableImageView avatar;
        @SuppressLint("StaticFieldLeak")
        private final ShapeableImageView avatar_dialog;

        public LoadImage(ShapeableImageView avatar, ShapeableImageView avatar_dialog)
        {
            this.avatar = avatar;
            this.avatar_dialog = avatar_dialog;
        }

        @Override
        protected Bitmap doInBackground(String... strings) {

            try {
                return BitmapFactory.decodeStream(new java.net.URL(strings[0]).openStream());
            } catch (IOException e) { e.printStackTrace(); avatar = null;}

            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null)
            {
                avatar.setImageBitmap(bitmap);
                avatar_dialog.setImageBitmap(bitmap);
            }
        }
    }
}
