package servidor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import comun.Historial;
import comun.Protocolo;

// Celula servidor. Atiende UN solo servicio.
//
// Uso:
//   java servidor.ServidorMain srv-suma1 SUM 5001
//                              ^id       ^servicio ^puerto del nodo al que se conecta
//
// Problema: hay un servidor de suma en cada nodo, y por el broadcast los
// cuatro reciben la misma SUM_REQ. Si los cuatro contestan, la GUI recibe
// cuatro respuestas.
//
// Estrategia (supresion por espera aleatoria):
//   1. Al recibir una solicitud, cada servidor espera un tiempo al azar (0 a 500 ms).
//   2. Mientras espera, sigue escuchando el broadcast. Si ve pasar una
//      respuesta a esa misma solicitud, es que otro servidor gano: no responde.
//   3. Si termina de esperar y nadie ha respondido, responde el.
// Nadie coordina: cada servidor decide solo, con lo que ve en la red.
// Si un servidor se cae, los demas siguen atendiendo.
public class ServidorMain {

    public static String miId;
    public static String servicio;
    public static String archivo;
    public static PrintWriter salida;

    // Solicitudes que ya tienen respuesta (mia o de otro servidor)
    public static Set<String> respondidas = new HashSet<>();

    public static final int ESPERA_MAXIMA = 500;
    public static Random azar = new Random();

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Uso: java servidor.ServidorMain <id> <SUM|SUB|MUL|DIV> <puertoNodo>");
            return;
        }
        miId = args[0];
        servicio = args[1];
        int puertoNodo = Integer.parseInt(args[2]);
        archivo = "datos/resultados_" + miId + ".csv";

        Socket conexion = new Socket("localhost", puertoNodo);
        conexion.setTcpNoDelay(true); // cada mensaje sale de inmediato (importante para la espera aleatoria)
        BufferedReader entrada = new BufferedReader(new InputStreamReader(conexion.getInputStream()));
        salida = new PrintWriter(conexion.getOutputStream(), true);

        salida.println("HELLO|SERVER|" + miId + "|" + servicio);
        System.out.println(miId + " (" + servicio + ") conectado al nodo en el puerto " + puertoNodo);

        String mensaje;
        while ((mensaje = entrada.readLine()) != null) {
            String[] p = mensaje.split("\\|");
            String codigo = p[Protocolo.CODIGO];

            if (codigo.equals(servicio + "_REQ")) {
                // Es de mi servicio: la atiendo (o no) en otro hilo, para seguir escuchando
                new Thread(() -> atender(p)).start();

            } else if (codigo.equals(servicio + "_RES")) {
                // Alguien (yo u otro servidor) ya respondio esta solicitud
                marcarRespondida(p[Protocolo.ID_SOLICITUD]);
            }
            // Cualquier otro codigo no es para mi: lo ignoro
        }
        System.out.println("Se perdio la conexion con el nodo.");
    }

    public static void atender(String[] p) {
        String idSolicitud = p[Protocolo.ID_MENSAJE];
        String origen = p[Protocolo.ORIGEN];
        double a = Double.parseDouble(p[Protocolo.DATO1]);
        double b = Double.parseDouble(p[Protocolo.DATO2]);
        String operacion = a + " " + Protocolo.simbolo(servicio) + " " + b;

        int espera = azar.nextInt(ESPERA_MAXIMA);
        System.out.println("Solicitud " + idSolicitud + " de " + origen + " (" + operacion
                + "). Espero " + espera + " ms");

        try {
            Thread.sleep(espera);
        } catch (InterruptedException e) {
        }

        // Reviso y marco en un solo paso (synchronized) para no responder dos veces
        if (!marcarRespondida(idSolicitud)) {
            System.out.println("   Otro servidor ya respondio " + idSolicitud + ". No respondo.");
            return;
        }

        String idRespuesta = Protocolo.nuevoId(miId);
        String resultado;
        try {
            resultado = "" + Calculadora.calcular(servicio, a, b);
        } catch (Exception e) {
            resultado = "ERROR: " + e.getMessage();
        }

        // SUM_RES|srv-suma3-91bc-4|srv-suma3|gui1-3fa2-7|8.0
        String respuesta = servicio + "_RES|" + idRespuesta + "|" + miId + "|" + idSolicitud + "|" + resultado;
        salida.println(respuesta);
        System.out.println("   RESPONDI: " + respuesta);
        Historial.guardar(archivo, idSolicitud + "," + origen + "," + a + ","
                + Protocolo.simbolo(servicio) + "," + b + "," + resultado);
    }

    // Devuelve true si la solicitud NO estaba respondida (y la marca).
    // Devuelve false si ya estaba respondida.
    public static synchronized boolean marcarRespondida(String idSolicitud) {
        return respondidas.add(idSolicitud);
    }
}
