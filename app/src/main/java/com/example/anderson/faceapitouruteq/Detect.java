package com.example.anderson.faceapitouruteq;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.AddPersistedFaceResult;
import com.microsoft.projectoxford.face.contract.CreatePersonResult;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.FaceRectangle;
import com.microsoft.projectoxford.face.contract.IdentifyResult;
import com.microsoft.projectoxford.face.contract.Person;
import com.microsoft.projectoxford.face.rest.ClientException;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import Clases.Personas;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class Detect extends AppCompatActivity {


    String urlWebService ="http://102.165.48.76:8080/faceApiWeb/PersonaControlador";

    Personas persona = new Personas();

    private final String CARPETA_RAIZ="misImagenesPrueba/";
    private final String RUTA_IMAGEN=CARPETA_RAIZ+"misFotos";
    String idgrupo ="gruposistemas";
    String path;
    Bitmap imgBit = null;

    final int COD_FOTO=20;

    Button botonCargar;

    private final int PICK_IMAGE = 1;
    private ProgressDialog detectionProgressDialog;
    Face[] faceResult = null;

    private static String apiEndpoint = "https://centralus.api.cognitive.microsoft.com/face/v1.0";
    private static String subscriptionKey = "60f82b31ac904609a09bdb84ec97bcc8";

    static FaceServiceClient faceServiceClient =
            new FaceServiceRestClient(apiEndpoint, subscriptionKey);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect);

        validaPermisos();

        final String nameUsuario = getIntent().getExtras().getString("nameUsuario");
        String respuesta =comprobarUsuario(nameUsuario);

        if("no existe".equals(respuesta)){
            Toast.makeText(getApplicationContext(), "se creara un nuevo usuario", Toast.LENGTH_SHORT ).show();

            try {

                persona.setName(nameUsuario);
                persona.setPersonid(crearPersonInLargeGroup(idgrupo, nameUsuario));
                //txt.setText(persona.getPersonid());
                respuesta= insertarPersona(persona);
                //txt.setText(respuesta);
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            JSONObject obj = new JSONObject(respuesta);
            persona.setPersonid(obj.getString("personid"));
            persona.setName(obj.getString("name"));
        } catch (JSONException e) {
            e.printStackTrace();
        }


        //final String persona =  getIntent().getExtras().getString("personId");
        TextView txtPersonId = (TextView)findViewById(R.id.txtPersonId);
        txtPersonId.setText(persona.getPersonid());
        TextView txtPersonName = (TextView)findViewById(R.id.txtPersonName);
        txtPersonName.setText(persona.getName());

        Button button1 = findViewById(R.id.btnCargarImg);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(
                        intent, "Select Picture"), PICK_IMAGE);*/
                cargarImagen();
            }
        });


        detectionProgressDialog = new ProgressDialog(this);

        Button btnGuardar = (Button)findViewById(R.id.btnGuardar);
        btnGuardar.setEnabled(false);
        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(imgBit==null){
                    Toast.makeText(getApplicationContext(), "Img vacion", Toast.LENGTH_SHORT).show();
                }else{
                    guardarFacePerson(idgrupo, persona.getPersonid() );
                    entrenarGrupoPersona(idgrupo);
                }
            }
        });

    }

    Uri uriFoto=null;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            final TextView resultados  = (TextView) findViewById(R.id.txtResultados);
            resultados.setText(uri.toString());
            uriFoto = uri;
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                        getContentResolver(), uri);
                ImageView imageView = findViewById(R.id.imageView);
                imageView.setImageBitmap(bitmap);
                imgBit = bitmap.copy(bitmap.getConfig(), true);
                // Comment out for tutorial
                detectAndFrame(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else if(requestCode == COD_FOTO && resultCode == RESULT_OK){
            MediaScannerConnection.scanFile(this, new String[]{path}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        @Override
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i("Ruta de almacenamiento","Path: "+path);
                        }
                    });

            Bitmap bitmap= BitmapFactory.decodeFile(path);
            ImageView imagen = (ImageView)findViewById(R.id.imageView);
            imgBit = bitmap.copy(bitmap.getConfig(), true);
            Uri uri = Uri.parse(new File(path).toString());
            uriFoto = uri;
            final TextView resultados  = (TextView) findViewById(R.id.txtResultados);
            resultados.setText(uri.toString());

            imagen.setImageBitmap(bitmap);
            detectAndFrame(bitmap);
        }
    }

    private String obtenerPersonas(UUID personId){
        AsyncTask<UUID, String, Person> getPerson = new AsyncTask<UUID, String, Person>() {
            @Override
            protected Person doInBackground(UUID... uuids) {
                try {
                    return  faceServiceClient.getPersonInLargePersonGroup("gruposistemas", uuids[0]);
                } catch (ClientException e) {
                    e.printStackTrace();
                    return null;
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }

            }
        };

        try {
            return getPerson.execute(personId).get().name.toString();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "error";
    }

    private void Identificar(Face [] faces){
        final TextView resultados  = (TextView) findViewById(R.id.txtResultados);
        List<UUID> faceIds = new ArrayList<>();
        for (Face face : faceResult) {
            faceIds.add(face.faceId);
        }

        AsyncTask<UUID, String, IdentifyResult[]> identify = new AsyncTask<UUID, String, IdentifyResult[]>() {
            @Override
            protected IdentifyResult[] doInBackground(UUID... uuids) {
                try {
                    return faceServiceClient.identityInLargePersonGroup(
                            "gruposistemas",   /* personGroupId */
                            uuids,                  /* faceIds */
                            1);
                } catch (ClientException e) {
                    e.printStackTrace();
                    return null;
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
            @Override
            protected void onPostExecute(IdentifyResult[] result) {
                // Show the result on screen when detection is done.
                String logString = "Response: Success. ";
                for (IdentifyResult identifyResult: result) {

                    /*logString += "Face " + identifyResult.faceId.toString() + " is identified as "
                            + (identifyResult.candidates.size() > 0
                            ? identifyResult.candidates.get(0).personId.toString()
                            : "Unknown Person")
                            + ". ";
                    */

                    if(identifyResult.candidates.size()>0){
                        logString+=obtenerPersonas(identifyResult.candidates.get(0).personId)+".";

                        /*try {

                            faceServiceClient.getPersonInLargePersonGroup("gruposistemas", identifyResult.candidates.get(0).personId);
                        } catch (ClientException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }*/
                    }else{
                        logString+="persona desconocida" + ".";

                    }
                }



                resultados.setText(logString);

            }

        };

        identify.execute(faceIds.toArray(new UUID[faceIds.size()]));

    }


    ByteArrayInputStream imagen=null;

    private void entrenarGrupoPersona(String grupo){

        AsyncTask<String, String, String> guardarTask =
                new AsyncTask<String, String, String>() {
                    String exceptionMessage = "";

                    @Override
                    protected String doInBackground(String... strings) {
                        try {
                            publishProgress("Entrenando grupo...");
                            faceServiceClient.trainLargePersonGroup(strings[0]);
                            return strings[0];
                        } catch (ClientException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return "error";
                    }

                    @Override
                    protected void onPreExecute() {
                        //TODO: show progress dialog
                        detectionProgressDialog.show();
                    }
                    @Override
                    protected void onProgressUpdate(String... progress) {
                        //TODO: update progress
                        detectionProgressDialog.setMessage(progress[0]);
                    }
                    @Override
                    protected void onPostExecute(String  result) {
                        //TODO: update face frames
                        detectionProgressDialog.dismiss();

                        TextView txtResult = (TextView) findViewById(R.id.txtResultados);
                        if(result =="error"){
                            txtResult.setText("error");

                        }else{
                            txtResult.setText(result);
                        }



                    }
                };

        guardarTask.execute(grupo);



    }

    private void guardarFacePerson(String grupo, String persona){
        ImageView imageView = findViewById(R.id.imageView);
        imageView.setImageBitmap(imgBit);

        final UUID personId = UUID.fromString(persona);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        imgBit.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        final InputStream imageInputStream = new ByteArrayInputStream(stream.toByteArray());
        final FaceRectangle faceRect = faceResult[0].faceRectangle;
        final String idGrupo = grupo;


        AsyncTask<InputStream, String, AddPersistedFaceResult> guardarTask =
                new AsyncTask<InputStream, String, AddPersistedFaceResult>() {
                    String exceptionMessage = "";

                    @Override
                    protected AddPersistedFaceResult doInBackground(InputStream... inputStreams) {
                        try {
                            publishProgress("Guardando faces...");
                            return faceServiceClient.addPersonFaceInLargePersonGroup(
                                    idGrupo,
                                    personId,
                                    inputStreams[0],
                                    "User data",
                                    faceRect);

                        } catch (ClientException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    protected void onPreExecute() {
                        //TODO: show progress dialog
                        detectionProgressDialog.show();
                    }
                    @Override
                    protected void onProgressUpdate(String... progress) {
                        //TODO: update progress
                        detectionProgressDialog.setMessage(progress[0]);
                    }
                    @Override
                    protected void onPostExecute(AddPersistedFaceResult result) {
                        //TODO: update face frames
                        detectionProgressDialog.dismiss();

                        TextView txtResult = (TextView) findViewById(R.id.txtResultados);
                        if(result ==null){
                            txtResult.setText("error");

                        }else{
                            txtResult.setText(result.persistedFaceId.toString());
                        }



                    }
                };

        guardarTask.execute(imageInputStream);
        Button btnGuardar = (Button)findViewById(R.id.btnGuardar);
        btnGuardar.setEnabled(false);


    }

    private void detectAndFrame(final Bitmap imageBitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(outputStream.toByteArray());

        imagen = inputStream;

        AsyncTask<InputStream, String, Face[]> detectTask =
                new AsyncTask<InputStream, String, Face[]>() {
                    String exceptionMessage = "";

                    @Override
                    protected Face[] doInBackground(InputStream... params) {
                        try {
                            publishProgress("Detecting...");
                            Face[] result = faceServiceClient.detect(
                                    params[0],
                                    true,         // returnFaceId
                                    false,        // returnFaceLandmarks
                                    null          // returnFaceAttributes:
                                /* new FaceServiceClient.FaceAttributeType[] {
                                    FaceServiceClient.FaceAttributeType.Age,
                                    FaceServiceClient.FaceAttributeType.Gender }
                                */
                            );
                            if (result == null){
                                publishProgress(
                                        "Detection Finished. Nothing detected");
                                return null;
                            }
                            publishProgress(String.format(
                                    "Detection Finished. %d face(s) detected",
                                    result.length));
                            return result;
                        } catch (Exception e) {
                            exceptionMessage = String.format(
                                    "Detection failed: %s", e.getMessage());
                            return null;
                        }
                    }

                    @Override
                    protected void onPreExecute() {
                        //TODO: show progress dialog
                        detectionProgressDialog.show();
                    }
                    @Override
                    protected void onProgressUpdate(String... progress) {
                        //TODO: update progress
                        detectionProgressDialog.setMessage(progress[0]);
                    }
                    @Override
                    protected void onPostExecute(Face[] result) {
                        //TODO: update face frames
                        detectionProgressDialog.dismiss();

                        if(!exceptionMessage.equals("")){
                            showError(exceptionMessage);
                        }
                        if (result == null) return;

                        ImageView imageView = findViewById(R.id.imageView);
                        imageView.setImageBitmap(
                                drawFaceRectanglesOnBitmap(imageBitmap, result));
                        imageBitmap.recycle();
                        faceResult = result;

                        TextView txtResult = (TextView) findViewById(R.id.txtResultados);
                        Button btnGuardar = (Button) findViewById(R.id.btnGuardar);
                        int countFaces = faceResult.length;
                        if (countFaces==0){
                            txtResult.setText("Error no se ha encontrado ningun rostro!");
                            btnGuardar.setEnabled(false);
                        }else if (countFaces == 1){
                            txtResult.setText("Un rostro encontrado, seleccione guardar") ;
                            btnGuardar.setEnabled(true);
                        }else if (countFaces>1){
                            txtResult.setText("Error se ha encontrado mas de un rostro: " + countFaces);
                            btnGuardar.setEnabled(false);
                        }


                    }
                };

        detectTask.execute(inputStream);
        /*try {
            faceResult =  detectTask.execute(inputStream).get();

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
    }

    private void showError(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }})
                .create().show();
    }

    private static Bitmap drawFaceRectanglesOnBitmap(
            Bitmap originalBitmap, Face[] faces) {

        Bitmap bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
        paint.setStrokeWidth(10);

        if (faces != null) {
            for (Face face : faces) {

                FaceRectangle faceRectangle = face.faceRectangle;
                canvas.drawRect(
                        faceRectangle.left,
                        faceRectangle.top,
                        faceRectangle.left + faceRectangle.width,
                        faceRectangle.top + faceRectangle.height,
                        paint);

            }
        }
        return bitmap;
    }

    private void cargarImagen() {

        final CharSequence[] opciones={"Tomar Foto","Cargar Imagen","Cancelar"};
        final android.support.v7.app.AlertDialog.Builder alertOpciones=new android.support.v7.app.AlertDialog.Builder(Detect.this);
        alertOpciones.setTitle("Seleccione una Opción");
        alertOpciones.setItems(opciones, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (opciones[i].equals("Tomar Foto")){
                    tomarFotografia();
                }else{
                    if (opciones[i].equals("Cargar Imagen")){
                        Intent intent=new Intent(Intent.ACTION_GET_CONTENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        intent.setType("image/");
                        startActivityForResult(intent.createChooser(intent,"Seleccione la Aplicación"),PICK_IMAGE);
                    }else{
                        dialogInterface.dismiss();
                    }
                }
            }
        });
        alertOpciones.show();

    }

    private boolean validaPermisos() {

        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.M){
            return true;
        }

        if((checkSelfPermission(CAMERA)==PackageManager.PERMISSION_GRANTED)&&
                (checkSelfPermission(WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED)){
            return true;
        }

        if((shouldShowRequestPermissionRationale(CAMERA)) ||
                (shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE))){
            cargarDialogoRecomendacion();
        }else{
            requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE,CAMERA},100);
        }

        return false;
    }

    private void cargarDialogoRecomendacion() {
        android.support.v7.app.AlertDialog.Builder dialogo=new android.support.v7.app.AlertDialog.Builder(Detect.this);
        dialogo.setTitle("Permisos Desactivados");
        dialogo.setMessage("Debe aceptar los permisos para el correcto funcionamiento de la App");

        dialogo.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE,CAMERA},100);
            }
        });
        dialogo.show();
    }

    private void tomarFotografia() {
        File fileImagen=new File(Environment.getExternalStorageDirectory(),RUTA_IMAGEN);
        boolean isCreada=fileImagen.exists();
        String nombreImagen="";
        if(isCreada==false){
            isCreada=fileImagen.mkdirs();
        }

        if(isCreada==true){
            nombreImagen=(System.currentTimeMillis()/1000)+".jpg";
        }


        path=Environment.getExternalStorageDirectory()+
                File.separator+RUTA_IMAGEN+File.separator+nombreImagen;

        File imagen=new File(path);

        Intent intent=null;
        intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        ////
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
        {
            String authorities=getApplicationContext().getPackageName()+".provider";
            Uri imageUri=FileProvider.getUriForFile(this,authorities,imagen);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        }else
        {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imagen));
        }
        startActivityForResult(intent,COD_FOTO);

        ////
    }

    public String crearPersonInLargeGroup(String idGrupo, String name) throws ExecutionException, InterruptedException {

        AsyncTask<String, String, String> insertar = new AsyncTask<String, String, String>() {
            @Override
            protected String doInBackground(String... strings) {
                String respuesta = "";
                CreatePersonResult createPersonResult=null;
                try {
                    createPersonResult  = faceServiceClient.createPersonInLargePersonGroup(strings[0], strings[1], "userdata");
                } catch (ClientException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(createPersonResult!=null){
                    respuesta = createPersonResult.personId.toString();
                }
                return respuesta;
            }
        };

        return insertar.execute(idGrupo, name).get();
    }

    public String insertarPersona(Personas persona){


        String respuesta ="";
        AsyncTask<String, String, String> insertarPersonaBd = new AsyncTask<String, String, String>() {
            @Override
            protected String doInBackground(String... strings) {
                try {
                    return peticionPost(strings[0], strings[1]);
                } catch (IOException e) {
                    return e.toString();
                }
            }

            @Override
            protected void onPostExecute(String result) {
                //txt.setText(result);
                //return result;
            }

        };
        try {
            respuesta =  (insertarPersonaBd.execute(urlWebService,"accion=insertar&namePersona="+persona.getName()+"&personaId="+persona.getPersonid()).get());
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return respuesta;

    }


    public String comprobarUsuario(String nameUsuario){

        String respuesta ="";
        AsyncTask<String, String, String> createPerson = new AsyncTask<String, String, String>() {
            @Override
            protected String doInBackground(String... strings) {
                try {
                    return peticionPost(strings[0], strings[1]);
                } catch (IOException e) {
                    return e.toString();
                }
            }

            @Override
            protected void onPostExecute(String result) {
                //txt.setText(result);
                //return result;
            }

        };
        try {
            respuesta =  (createPerson.execute(urlWebService,"accion=buscar&namePersona="+nameUsuario).get());
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return respuesta;

    }


    public String peticionPost(String urlRequest, String parametros) throws IOException {

        String server_response="";

        byte[] postData       = parametros.getBytes( StandardCharsets.UTF_8 );
        int    postDataLength = postData.length;
        //String request        = "http://192.168.1.4:8080/faceApiWeb/PersonaControlador";
        URL url            = new URL( urlRequest);
        HttpURLConnection conn= (HttpURLConnection) url.openConnection();
        conn.setDoOutput( true );
        conn.setInstanceFollowRedirects( false );
        conn.setRequestMethod( "POST" );
        /*conn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty( "charset", "utf-8");
        conn.setRequestProperty( "Content-Length", Integer.toString( postDataLength ));*/
        conn.setUseCaches( false );
        try( DataOutputStream wr = new DataOutputStream( conn.getOutputStream())) {
            wr.write(postData);
        }


        int responseCode = conn.getResponseCode();
        if(responseCode == HttpURLConnection.HTTP_OK){
            server_response = readStream(conn.getInputStream());
            //Log.v("CatalogClient", server_response);
        }
        return server_response;

    }
    private String readStream(InputStream in) {
        BufferedReader reader = null;
        StringBuffer response = new StringBuffer();
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return response.toString();
    }



}
