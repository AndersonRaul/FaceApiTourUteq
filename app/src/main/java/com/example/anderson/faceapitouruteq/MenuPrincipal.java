package com.example.anderson.faceapitouruteq;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


public class MenuPrincipal extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_principal);

        Button btnAddFaces = (Button)findViewById(R.id.btnAddFaces);

        btnAddFaces.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText nombreUsuario = (EditText) findViewById(R.id.textUsuario);
                Intent intent = new Intent(MenuPrincipal.this, Detect.class);
                intent.putExtra("nameUsuario", nombreUsuario.getText().toString());
                startActivity(intent);
            }
        });


        Button btnIdentificar = (Button)findViewById(R.id.btnIdentificar);
        btnIdentificar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MenuPrincipal.this, Identificar.class);
                startActivity(intent);
            }
        });
    }


}
