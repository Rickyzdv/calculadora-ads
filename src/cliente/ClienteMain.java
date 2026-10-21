package cliente;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import comun.Historial;
import middleware.MiddlewareMain;

public class ClienteMain {

    public static String miId = "cliente1";
    public static String archivo;
    public static PrintWriter salida;
    public static int contadorPeticiones = 0;
    public static VentanaGUI ventana;

    public static void main(String[] args) throws Exception {

        if (args.length > 0) {
            miId = args[0];
        }
        archivo = "datos/historial_" + miId + ".csv";

        Socket conexion = new Socket("localhost", MiddlewareMain.PUERTO);
        BufferedReader entrada = new BufferedReader(new InputStreamReader(conexion.getInputStream()));
        salida = new PrintWriter(conexion.getOutputStream(), true);

        salida.println("HELLO|CLIENT|" + miId);
        System.out.println(miId + " conectado al middleware.");

        // Abrimos la ventana
        ventana = new VentanaGUI(miId);
        ventana.setVisible(true);

        // HILO 2: se queda escuchando lo que llegue, para siempre
        new Thread(() -> {
            try {
                String mensaje;
                while ((mensaje = entrada.readLine()) != null) {
                    // RES|servidor1|cliente1|7|8.0
                    String[] p = mensaje.split("\\|");
                    String idServidor = p[1];
                    String idCliente = p[2];
                    String numPeticion = p[3];
                    String resultado = p[4];

                    String texto;
                    if (idCliente.equals(miId)) {
                        texto = "[#" + numPeticion + "] " + idServidor + " respondio: " + resultado;
                    } else {
                        texto = "[#" + numPeticion + "] (de " + idCliente + ") "
                                + idServidor + " respondio: " + resultado;
                    }

                    ventana.mostrarLinea(texto);
                    Historial.guardar(archivo, "RECIBIDO," + numPeticion + "," + idCliente
                            + "," + idServidor + "," + resultado);

                    if (idCliente.equals(miId)) {
                        ventana.mostrarResultado(resultado);
                    }
                }
            } catch (Exception e) {
                System.out.println("Se perdio la conexion con el middleware.");
            }
        }).start();
    }

    // La ventana llama a esto cuando se aprieta el boton
    public static void enviarOperacion(double a, String operador, double b) {
        contadorPeticiones++;
        String peticion = "REQ|" + miId + "|" + contadorPeticiones + "|" + a + "|" + operador + "|" + b;
        ventana.mostrarLinea("Envie: " + a + " " + operador + " " + b + "  (peticion #" + contadorPeticiones + ")");
        salida.println(peticion);
        Historial.guardar(archivo, "ENVIADO," + contadorPeticiones + "," + a + "," + operador + "," + b);
    }
}