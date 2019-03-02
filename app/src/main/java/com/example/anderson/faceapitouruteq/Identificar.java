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
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.FaceRectangle;
import com.microsoft.projectoxford.face.contract.IdentifyResult;
import com.microsoft.projectoxford.face.contract.Person;
import com.microsoft.projectoxford.face.rest.ClientException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class Identificar extends AppCompatActivity {

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
        setContentView(R.layout.activity_identificar);

        Button btnEntrar = (Button) findViewById(R.id.btnEntrar);
        btnEntrar.setEnabled(false);
        btnEntrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView txtName = (TextView) findViewById(R.id.txtResultados);
                Intent intent = new Intent(Identificar.this, Detect.class);
                intent.putExtra("nameUsuario", txtName.getText().toString());
                startActivity(intent);

            }
        });

        validaPermisos();

        Button button1 = findViewById(R.id.btnCargarImg);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(
                        intent, "Select Picture"), PICK_IMAGE);*/
                tomarFotografia();
            }
        });


        detectionProgressDialog = new ProgressDialog(this);



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
                String logString = "";
                for (IdentifyResult identifyResult: result) {

                    /*logString += "Face " + identifyResult.faceId.toString() + " is identified as "
                            + (identifyResult.candidates.size() > 0
                            ? identifyResult.candidates.get(0).personId.toString()
                            : "Unknown Person")
                            + ". ";
                    */

                    if(identifyResult.candidates.size()>0){
                        logString=obtenerPersonas(identifyResult.candidates.get(0).personId);

                        /*try {

                            faceServiceClient.getPersonInLargePersonGroup("gruposistemas", identifyResult.candidates.get(0).personId);
                        } catch (ClientException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }*/
                    }else{
                        logString="persona desconocida";

                    }
                }


                Button btnEntrar = (Button) findViewById(R.id.btnEntrar);
                resultados.setText(logString);
                if(resultados.getText().toString()=="persona desconocida"){
                    btnEntrar.setEnabled(false);
                }else{
                    btnEntrar.setEnabled(true);
                }

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
                        int countFaces = faceResult.length;
                        if (countFaces==0){
                            txtResult.setText("Error no se ha encontrado ningun rostro!");

                        }else if (countFaces == 1){
                            txtResult.setText("Un rostro encontrado, seleccione guardar" + result[0].faceRectangle.toString());
                            Identificar(faceResult);

                        }else if (countFaces>1){
                            txtResult.setText("Error se ha encontrado mas de un rostro: " + countFaces);

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
        android.support.v7.app.AlertDialog.Builder dialogo=new android.support.v7.app.AlertDialog.Builder(Identificar.this);
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

}
