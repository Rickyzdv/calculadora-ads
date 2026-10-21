package servidor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import comun.Historial;
import middleware.MiddlewareMain;

public class ServidorMain {

    public static void main(String[] args) throws Exception {

        // El identificador se pasa al ejecutar: java ServidorMain servidor1
        String miId = "servidor1";
        if (args.length > 0) {
            miId = args[0];
        }
        String archivo = "datos/resultados_" + miId + ".csv";

        Socket conexion = new Socket("localhost", MiddlewareMain.PUERTO);
        BufferedReader entrada = new BufferedReader(new InputStreamReader(conexion.getInputStream()));
        PrintWriter salida = new PrintWriter(conexion.getOutputStream(), true);

        // Saludo: le decimos al middleware que somos un servidor
        salida.println("HELLO|SERVER|" + miId);
        System.out.println(miId + " conectado al middleware.");

        String mensaje;
        while ((mensaje = entrada.readLine()) != null) {
            System.out.println("Solicitud recibida: " + mensaje);

            // REQ|cliente1|7|5|+|3
            String[] p = mensaje.split("\\|");
            String idCliente = p[1];
            String numPeticion = p[2];
            double a = Double.parseDouble(p[3]);
            String operador = p[4];
            double b = Double.parseDouble(p[5]);

            String respuesta;
            String paraElArchivo;

            try {
                double resultado = Calculadora.calcular(a, operador, b);
                respuesta = "RES|" + miId + "|" + idCliente + "|" + numPeticion + "|" + resultado;
                paraElArchivo = numPeticion + "," + idCliente + "," + a + "," + operador + "," + b + "," + resultado;
            } catch (Exception e) {
                respuesta = "RES|" + miId + "|" + idCliente + "|" + numPeticion + "|ERROR: " + e.getMessage();
                paraElArchivo = numPeticion + "," + idCliente + "," + a + "," + operador + "," + b + ",ERROR";
            }

            Historial.guardar(archivo, paraElArchivo);
            salida.println(respuesta);
            System.out.println("Respondi: " + respuesta);
        }
    }
}