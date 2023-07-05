package uy.com.grupo15.CargaUyMobile;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import java.net.URL;


public class WebLoginActivity extends AppCompatActivity implements View.OnClickListener{
    static String callback_uri = "";
    static String code = "";
    static String state = "";
    static String token = "";
    static String estadoViaje = "";
    static String idViaje = "";
    static String ci = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_auth_login);

        String resultado = getIntent().getDataString();
        Uri uresultado = getIntent().getData();
        Log.e("onCreate", "Resultado:" + resultado);

        callback_uri = uresultado.getHost() + uresultado.getPath();
        Log.e("onCreate", "callback_uri:" + callback_uri);

        code = uresultado.getQueryParameter("code");
        state = uresultado.getQueryParameter("state");

        //escondo cardviews
        CardView cardView = (CardView) findViewById(R.id.cvEsConductor);
        cardView.setVisibility(View.GONE);
        CardView cardView2 = (CardView) findViewById(R.id.cvNoEsConductor);
        cardView2.setVisibility(View.GONE);
        //oculto los botones
        Button botonVerde = (Button) findViewById(R.id.button1);
        botonVerde.setVisibility(View.GONE);
        Button botonRojo = (Button) findViewById(R.id.button2);
        botonRojo.setVisibility(View.GONE);
        Button refresh = (Button) findViewById(R.id.refreshButton);
        refresh.setVisibility(View.GONE);

        //pasar el codigo al /token para obtener el accessToken
        if(code != null){
            token = getTokenFromCode(code,this);
        }
        Log.e("onCreate", "code:" + code);
    }

    //ACCIONES DE BOTONES
    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if(viewId == R.id.button1 || viewId == R.id.button2){
            updateEstadoViaje(idViaje, this);
        }else if(viewId == R.id.refreshButton){
            verifyUserAsDriver(ci,this);
        }

    }

    //BOTON PARA LOGOUT
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_logout) {
            logout(WebLoginActivity.this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //FUNCIONES CON LLAMADAS

    //Usar el codigo para obtener el token
    private static String  getTokenFromCode(String code, final WebLoginActivity activity) {
        final String[] accessToken = {null};
        AsyncTask<String, Void, String> tokenRequestTask = new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... params) {
                String code = params[0];

                String accessTokenUrl = Constants.ACCESSTOKEN_URL;
                String redirectUri = "http://localhost:8080/javaEE.lab2023-web/callback";
                String clientId = Constants.CLIENT_ID;
                String clientSecret = Constants.CLIENT_SECRET;
                String resultAccessToken = null;

                try {
                    URL url = new URL(accessTokenUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);

                    // Construct the request body
                    String requestBody = "grant_type=authorization_code" +
                            "&redirect_uri=" + Uri.encode(redirectUri) +
                            "&client_id=" + Uri.encode(clientId) +
                            "&client_secret=" + Uri.encode(clientSecret) +
                            "&code=" + Uri.encode(code);

                    // Send the request
                    OutputStream os = conn.getOutputStream();
                    os.write(requestBody.getBytes());
                    os.flush();
                    os.close();

                    // Get the response
                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        InputStream inputStream = conn.getInputStream();
                        String response = convertStreamToString(inputStream);

                        // Process the response
                        JSONObject jsonResponse = new JSONObject(response);

                        //parametro necesario para la info del usuario
                        resultAccessToken = jsonResponse.getString("access_token");
                    } else {
                        // Handle the error case
                        // You can return null or a specific error message here
                    }
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                    // Handle the exception
                    // You can return null or a specific error message here
                }
                return resultAccessToken;
            }

            @Override
            protected void onPostExecute(String resultAccessToken) {
                if (resultAccessToken != null) {
                    accessToken[0] = resultAccessToken;
                    token = accessToken[0];
                    //usar el accessToken para obtener la informacion del usuario de /userinfo
                    getCedulaFromToken(resultAccessToken,activity);
                } else {
                    // Handle the error case
                }
            }
        };

        tokenRequestTask.execute(code);
        return accessToken[0];
    }

    //Usar el token para obtener user info y crear el card con la informacion del usuario
    private static void  getCedulaFromToken(final String token, final WebLoginActivity activity) {
        AsyncTask<String, Void, JSONObject> userInfoRequestTask = new AsyncTask<String, Void, JSONObject>() {
            @Override
            protected JSONObject doInBackground(String... params) {
                String userinfoTokenUrl = Constants.USERINFO_URL;
                JSONObject jsonResponse = null;
                try {
                    URL url = new URL(userinfoTokenUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setRequestProperty("Authorization", "Bearer " + token);

                    int responseCode = conn.getResponseCode();

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder responseBuilder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            responseBuilder.append(line);
                        }

                        reader.close();

                        String responseBody = responseBuilder.toString();
                        jsonResponse = new JSONObject(responseBody);

                    } else {
                        // Handle the error case
                        // You can return null or a specific error message here
                    }
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                    // Handle the exception
                    // You can return null or a specific error message here
                }
                return jsonResponse;
            }

            @Override
            protected void onPostExecute(JSONObject jsonResponse) {
                if (jsonResponse != null) {

                    String numeroDocumento = jsonResponse.optString("numero_documento");
                    String primerNombre = jsonResponse.optString("primer_nombre");
                    String primerApellido = jsonResponse.optString("primer_apellido");
                    String email = jsonResponse.optString("email");

                    ci = numeroDocumento;

                    final TextView documentoTextView = (TextView) activity.findViewById(R.id.tvNumeroDocumento);
                    documentoTextView.setText("Documento: " + numeroDocumento);
                    final TextView nombreTextView = (TextView) activity.findViewById(R.id.tvPrimerNombre);
                    nombreTextView.setText("Nombre: " + primerNombre);
                    final TextView apellidoTextView = (TextView) activity.findViewById(R.id.tvPrimerApellido);
                    apellidoTextView.setText("Apellido: " + primerApellido);
                    final TextView emailTextView = (TextView) activity.findViewById(R.id.tvEmail);
                    emailTextView.setText("Email: " + email);
                    //llamar a funcion para verificar si el usuario es un conductor enviando la cedula
                    verifyUserAsDriver(numeroDocumento, activity);
                } else {
                    // Handle the error case
                }
            }
        };

        userInfoRequestTask.execute(token);
    }

    //Verificar si el usuario es un conductor y crear el card correspondiente
    private static void verifyUserAsDriver(String numeroDocumento, final WebLoginActivity activity) {
        String apiUrl = Constants.baseUrl + "/viaje/" + numeroDocumento;

        AsyncTask<String, Void, JSONObject> apiRequestTask = new AsyncTask<String, Void, JSONObject>() {
            @Override
            protected JSONObject doInBackground(String... params) {
                String apiUrl = params[0];
                JSONObject jsonResponse = null;

                try {
                    URL url = new URL(apiUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

                    int responseCode = conn.getResponseCode();

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder responseBuilder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            responseBuilder.append(line);
                        }

                        reader.close();

                        String responseBody = responseBuilder.toString();
                        jsonResponse = new JSONObject(responseBody);

                    } else {
                        jsonResponse = null;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    // Handle the exception
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                return jsonResponse;
            }

            @Override
            protected void onPostExecute(JSONObject jsonResponse) {
                if (jsonResponse != null) {
                    String ci = jsonResponse.optString("ci");
                    String estado = jsonResponse.optString("estado");
                    String id = jsonResponse.optString("id");
                    String desde = jsonResponse.optString("desde");
                    String hasta = jsonResponse.optString("hasta");
                    idViaje = id;
                    estadoViaje = estado;
                    //escondo cardView de ususario incorrecto
                    CardView cardView = (CardView) activity.findViewById(R.id.cvNoEsConductor);
                    cardView.setVisibility(View.GONE);

                    //seteo los parametros de la info del viaje
                    final TextView idTextView = (TextView) activity.findViewById(R.id.tvId);
                    idTextView.setText("ID: " + id);
                    final TextView estadoTextView = (TextView) activity.findViewById(R.id.tvEstado);
                    estadoTextView.setText("Estado: " + estado);
                    final TextView desdeTextView = (TextView) activity.findViewById(R.id.tvDesde);
                    desdeTextView.setText("Desde: " + desde);
                    final TextView hastaTextView = (TextView) activity.findViewById(R.id.tvHasta);
                    hastaTextView.setText("Hasta: " + hasta);
                    Button refresh = (Button) activity.findViewById(R.id.refreshButton);
                    refresh.setOnClickListener(activity);

                    createButtons(estado, activity);


                } else {
                    //El usuario iniciado no es un conductor
                    //escondo el cardView correspondiente a info del viaje
                    CardView cardView = (CardView) activity.findViewById(R.id.cvEsConductor);
                    cardView.setVisibility(View.GONE);
                    TextView idTextView = (TextView) activity.findViewById(R.id.tvError);
                    idTextView.setText("El usuario ingresado no es un Conductor");

                    cardView.setVisibility(View.VISIBLE);
                    //oculto los botones
                    Button botonVerde = (Button) activity.findViewById(R.id.button1);
                    botonVerde.setVisibility(View.GONE);
                    Button botonRojo = (Button) activity.findViewById(R.id.button2);
                    botonRojo.setVisibility(View.GONE);
                    Button refresh = (Button) activity.findViewById(R.id.refreshButton);
                    refresh.setVisibility(View.GONE);
                }
            }

            //Asignarle estado a los botones
            private void createButtons(String estado, final WebLoginActivity activity) {
                Button botonVerde = (Button) activity.findViewById(R.id.button1);
                botonVerde.setOnClickListener(activity);
                Button botonRojo = (Button) activity.findViewById(R.id.button2);
                botonRojo.setOnClickListener(activity);
                if (estado.equals("Pendiente")) {
                    botonVerde.setText("Iniciar viaje");
                    botonVerde.setEnabled(true);
                    botonRojo.setText("No hay ningun viaje en progreso");
                    botonRojo.setEnabled(false);
                } else if (estado.equals("EnProgreso")) {
                    botonVerde.setText("El viaje ya fue iniciado");
                    botonVerde.setEnabled(false);
                    botonRojo.setText("Finalizar viaje");
                    botonRojo.setEnabled(true);
                } else {
                    botonVerde.setText("No hay viajes pendientes");
                    botonVerde.setEnabled(false);
                    botonRojo.setText("El ultimo viaje ya fue finalizado");
                    botonRojo.setEnabled(false);
                }
            }
        };

        apiRequestTask.execute(apiUrl);
    }

    private static void updateEstadoViaje(String id ,final WebLoginActivity activity) {
        String apiUrl = Constants.baseUrl + "/updateEstadoViaje/" + id;

        AsyncTask<String, Void, Integer> apiRequestTask = new AsyncTask<String, Void, Integer>() {
            @Override
            protected Integer doInBackground(String... params) {
                String apiUrl = params[0];
                int responseCode = -1;

                try {
                    URL url = new URL(apiUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

                    responseCode = conn.getResponseCode();

                } catch (IOException e) {
                    e.printStackTrace();
                    // Handle the exception
                }

                return responseCode;
            }

            @Override
            protected void onPostExecute(Integer responseCode) {
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    verifyUserAsDriver(ci, activity);
                } else {
                    // API request failed, handle the logic accordingly
                }
            }
        };
        apiRequestTask.execute(apiUrl);
    }

    private static void logout(Context context) {
        new LogoutTask(context).execute();
    }
    private static class LogoutTask extends AsyncTask<Void, Void, Void> {
        private Context context;

        public LogoutTask(Context context) {
            this.context = context;
        }
        @Override
        protected Void doInBackground(Void... params) {
            try {
                String logoutEndpoint = Constants.LOGOUT_URL;
                String idTokenHint = token;
                String postLogoutRedirectUri = Constants.REDIRECT_URI;
                String estado = state;

                String url = logoutEndpoint + "?id_token_hint=" + idTokenHint +
                        "&post_logout_redirect_uri=" + postLogoutRedirectUri +
                        "&state=" + estado;

                URL logoutUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) logoutUrl.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    System.out.println("Logout success");
                } else {
                    System.out.println("Logout failed");
                }

                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            // Redirect to MainActivity
            callback_uri = "";
            code = "";
            state = "";
            token = "";
            idViaje = "";
            estadoViaje = "";
            ci = "";
            Intent intent = new Intent(context, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
    private static String convertStreamToString(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        reader.close();
        return stringBuilder.toString();
    }

}
