package com.example.edwin.login_aplication;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.READ_CONTACTS;

public class LoginActivity extends AppCompatActivity implements LoaderCallbacks<Cursor> {

    /**ID para el manejo de permisos de lectura de Contactos. */
    private static final int REQUEST_READ_CONTACTS = 0;
    private int op = 0;

    /**Array tipo String para el manejo de Cuentas con sus respectivos passwords. */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello", "bar@example.com:world"
    };
    /**Objeto para almacenar los usuarios con sus respectivos correos y contraseñas*/
    private UserLoginTask mAuthTask = null;

    /**Variables para el manejo de los componentes de la Interfaz*/
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private TextView registForm;
    private View mProgressView;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);

        /**Cargar la Función para el Autocompletado de Emails. */
        populateAutoComplete();

        mPasswordView = (EditText) findViewById(R.id.password);
        /**Adiciona la escucha del evento ENTER sobre la caja de Texto de la contraseña*/
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        /**Adicionar la escucha del evento Click sobre el boton Sign In, si se produce el evento
         * se lanza la función para validar el formulario*/
        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        /**Adicionar la escucha del evento Click sobre el textView Create Acount, si se produce el
         * evento se lanza el Activity para el Registro de un nuevo Usuario*/
        registForm = (TextView) findViewById(R.id.createAcount);
        registForm.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent  i = new Intent(LoginActivity.this,Registration.class);
                startActivity(i);
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }
    //==============================================================================================
    //                  MOSTRAR SUGERENCIAS CUANDO EL USUARIO ESCRIBE UN EMAIL
    //==============================================================================================
    private void populateAutoComplete() {
        /**Comprobamos si los permisos fueron concedidos. */
        if (!mayRequestContacts())return;

        /**Preparar el cargador o bien conectar con uno ya exixtente.
         * Carga una consulta asincrona para buscar el o los emails almacenados en el perfil de usuario */
        getLoaderManager().initLoader(0, null, this);
    }
    //==============================================================================================
    //                  SOLICITAR EL PERMISO PARA LEER LOS CONTACTOS DEL USUARIO
    //==============================================================================================
    //----------------------------------------------------------------------------------------------
    //Metodo para comprobar y solicitar los permisos
    private boolean mayRequestContacts() {

        /**Analiza si la versión de SDK del dispositivo sobre el cual esta corriendo
        la aplicación es menor a la versión Mashmallow */
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        /**Comprueba si el permiso para leer contactos se ha concedido con anterioridad */
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        /**Si un usuario previamente a denegado el permiso y aun asi sigue intentando entrar en la
        funcionalidad, por lo que es conveniete informarle las razones del permiso. */
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {

            /**Generar una notificación para informarle al usuario el porque de la solicitud del
            permiso, acompañado de un boton para conceder los permisos a la aplicación. */
            Snackbar.make(mEmailView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {

                            /**Solicita que se le otorguen permisos a esta aplicación solo hizo
                            click sobre el boton OK del Snackbar. */
                            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
                        }
                    });
        } else {
            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
        return false;
    }
    //----------------------------------------------------------------------------------------------
    //Metodo que se invoca cada vez que se solicitan permisos

     /**Recibe como parametros
      * El codigo de la solicitud aprobada.
      * Los permisos solicitados.
      * los resultados para los permisos correspondientes.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            /**comprueba si los resultados no estan vacios y si fueron concedidos por el usuario. */
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                /**Una vez concedidos los permisos, ir a la función de Autocompletado. */
                populateAutoComplete();
            }
        }
    }

    //==============================================================================================
    //                  VALIDAR LOS COMPONENTES DEL FORMULARIO DE INGRESO
    //==============================================================================================
    private void attemptLogin() {
        /**Comprobar si el objeto para el usuario esta vacio o no*/
        if (mAuthTask != null) {
            return;
        }

        /**Resetea los Errores*/
        mEmailView.setError(null);
        mPasswordView.setError(null);

        /**Obtiene y guarda los valores respectivos para el email y el password*/
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        /**
         * Bandera evidenciar algun error durante la validación de los datos
         * Variable para contener el campo a ser enfocado
         */
        boolean cancel = false;
        View focusView = null;

        /**Comprobar si el password ingresado no es nulo y es valido*/
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            /**Envia el error a la caja de Texto*/
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        /**Comprobar si el campo para el Email esta vacio. */
        if (TextUtils.isEmpty(email)) {
            /**Envia el error a la caja de Texto*/
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        }
        /**Comprobar si el Email Ingresado es valido. */
        else if (!isEmailValid(email)) {
            /**Envia el error a la caja de Texto*/
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        /**Comprobar si hubo un fallo durante el ingreso de datos*/
        if (cancel) {
            /**Enfocar el Campo del Error*/
            focusView.requestFocus();
        } else {
            /**Cargar Animación con una barra de progreso*/
            showProgress(true);

            /**Crea un nuevo Usuario a partir de la clase  mAuthTask*/
            mAuthTask = new UserLoginTask(email, password);
            /**Lanzar el Hilo para la Autenticación del Usuario*/
            mAuthTask.execute((Void) null);
        }
    }
    //----------------------------------------------------------------------------------------------
    //Comprobar si un email es valido o no
    private boolean isEmailValid(String email) {
        /**Si la cadena contiene el caracter @ es un email valido*/
        return email.contains("@");
    }
    //----------------------------------------------------------------------------------------------
    //Comprobar si la contraseña ingresada cumple con restricciones establecidas
    private boolean isPasswordValid(String password) {
        /**Si la cadena supera los 4 caracteres es una contraseña valida*/
        return password.length() > 4;
    }

    //==============================================================================================
    //                  CARGAR ANIMACION DE UNA BARRA DE PROGRESO CIRCULAR
    //==============================================================================================
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    //==============================================================================================
    //      CREAR LA LISTA DE EMAILS PROVENIENTES DEL PERFIL DE USUARIO PARA EL AUTOCOMPLETADO
    //==============================================================================================
    //----------------------------------------------------------------------------------------------
    //Consultar Asincronicamente el o los emails almacenados en el perfil del usuario
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        /**Obtener y Retornar un Cursor que apunta a una tabla con los datos especificados  en la
         * consulta*/
        return new CursorLoader(this,
                /**Creamos un filtro para que la consulta se centre solo en los emails*/
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                /**Especificamos que seleccione solo direcciones de Correo*/
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                /**Mostrar la dirección de correo Principal en la primera fila de la tabla*/
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }
    //----------------------------------------------------------------------------------------------
    //Mover los correos Obtenidos en la anterior consulta a través del Cursor retornado
    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();

        /**Mientras el Cursor no termine de recorrer las Filas de Tabla, adicione las direcciones
         * de correo a la lista*/
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        /**Enviar la lista de Emails para el Autocompletado*/
        addEmailsToAutoComplete(emails);
    }
    //----------------------------------------------------------------------------------------------
    //
    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }
    //----------------------------------------------------------------------------------------------
    //Adicionar la lista de terminos al TextView de tipo Autocomplete
    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        /**Crear un adaptador que permitira adaptar los datos de la la lista al AutoCompleteTextView
         * utilizando una lista dropdown*/
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }
    //----------------------------------------------------------------------------------------------
    //Definen los campos seleccionados para la consulta
    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }
    //==============================================================================================
    //  CLASE PARA ALMACENAR LOS USUARIOS Y METODOS ASICRONOS PARA VALIDAR EL USUARIO INGRESADO
    //==============================================================================================
    //----------------------------------------------------------------------------------------------
    //Clase para Almacenar los Usuarios
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;

        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }
        //------------------------------------------------------------------------------------------
        //Hilo para validar si el Correo y contraseña ingresados son correctos
        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                /**Ejecución del Hilo con un retraso de 2 segundos*/
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return false;
            }
            /**Ciclo en el cual se comparan los Emails y Contraseñas alamacenados en el Array tipo
             * string definido al inicio del activity y el email y clave ingresados por el usuario
             * en el formulario de Login*/
            for (String credential : DUMMY_CREDENTIALS) {
                String[] pieces = credential.split(":");
                if (pieces[0].equals(mEmail)) {
                    /**Retorna verdadero si  la Contraseña Coincide*/
                    return pieces[1].equals(mPassword);
                }
            }

            op = 1;
            return false;
        }
        //------------------------------------------------------------------------------------------
        //Muestra en el Activity actual el resultado de la tarea que se ejecuto en el Hilo
        /**En este caso Abre el Activity Bienvenido si los datos Fueron correctos de lo contrario
         * Lanza un mensaje Evidenciando el problema*/
        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                Intent  i = new Intent(LoginActivity.this,Welcome.class);
                startActivity(i);
            } else {
                if(op == 0){
                    mPasswordView.setError(getString(R.string.error_incorrect_password));
                    mPasswordView.requestFocus();
                }
                else{
                    Toast toast = Toast.makeText(LoginActivity.this,R.string.alert_not_found_email, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                    toast.show();
                }
            }
        }
        //------------------------------------------------------------------------------------------
        //En caso de que se cancele la Tarea inmersa en el Hilo
        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

