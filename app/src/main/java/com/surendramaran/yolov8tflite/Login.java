package com.surendramaran.yolov8tflite;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.surendramaran.yolov8tflite.Entidades.Usuario;
import com.surendramaran.yolov8tflite.Util.MiVariableGlobal;
import com.surendramaran.yolov8tflite.Util.Utils;
public class Login extends AppCompatActivity implements Response.Listener<JSONObject>, Response.ErrorListener {

    EditText editUsuario , editTextPassword;
    Button buttonLogin;
    RequestQueue request; //Aqui seleccionar la ultima opcion "ADD dependencies..."
    JsonObjectRequest jsonObjectRequest;
    ProgressDialog progreso;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editUsuario = findViewById(R.id.editUsuario);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);

        request = Volley.newRequestQueue(this);

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*Intent intent = new
                        Intent(Login.this, MainActivity.class);
                startActivity(intent);*/
                cargarWebService();
            }
        });


        }

    private void cargarWebService() {

        progreso.show();
        String url = getString(R.string.db_url) + "?documento=" + editUsuario.getText().toString();
        url = url.replace(" ", "%20");
        jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, this, this); //HACER CICK EN EL PENULTIMO (/PRIMERO), CLICK EN EL ULTIMO (/PRIMERO)
        request.add(jsonObjectRequest);
        Toast.makeText(this, "Resultado", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResponse(JSONObject response) {

        Usuario usuario = new Usuario();
        JSONArray jsonArray = response.optJSONArray("usuario");
        JSONObject jsonObject = null;

        try {
            assert jsonArray != null;
            jsonObject = jsonArray.getJSONObject(0);
            usuario.setUser(jsonObject.optString("usuario"));
            usuario.setPassword(jsonObject.optString("contrasena"));
            usuario.setIdUser(jsonObject.optString("cod_usu"));


            if (usuario.getPassword().equals(editTextPassword.getText().toString())) {
                Intent intent = new
                        Intent(Login.this, MainActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Error: Usuario o Contraseña incorrectos"  , Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }



    }

    @Override
    public void onErrorResponse(VolleyError error) {
        progreso.hide();
        Log.i("Error", error.toString());
        Toast.makeText(this, "Error: " + error.toString(), Toast.LENGTH_SHORT).show();
    }
}
