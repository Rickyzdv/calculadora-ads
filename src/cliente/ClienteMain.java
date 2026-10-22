package cliente;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

import comun.Historial;
import comun.Protocolo;
import javafx.application.Application;

// Celula cliente (GUI). La parte de red; la ventana vive en VentanaGUI.
//
// Uso:
//   java --module-path <javafx>/lib --add-modules javafx.controls -cp out cliente.ClienteMain gui1 5001
//                                                                                   ^id  ^puerto del nodo
public class ClienteMain {

    public static String miId = "gui1";
    public static int puertoNodo = 5001;
    public static String archivo;
    public static PrintWriter salida;
    public static VentanaGUI ventana;

    // Solicitudes mias que todavia no tienen respuesta
    public static Set<String> pendientes = new HashSet<>();

    public static final int TIEMPO_LIMITE = 5000; // ms sin respuesta -> aviso

    public static void main(String[] args) {
        if (args.length > 0) {
            miId = args[0];
        }
        if (args.length > 1) {
            puertoNodo = Integer.parseInt(args[1]);
        }
        archivo = "datos/historial_" + miId + ".csv";

        // En JavaFX no creamos la ventana nosotros: le pedimos a JavaFX que arranque
        // y el llama a VentanaGUI.start(), que a su vez llama a conectar()
        Application.launch(VentanaGUI.class);
    }

    // La llama la ventana cuando ya esta lista
    public static void conectar(VentanaGUI v) throws Exception {
        ventana = v;
        Socket conexion = new Socket("localhost", puertoNodo);
        conexion.setTcpNoDelay(true);
        BufferedReader entrada = new BufferedReader(new InputStreamReader(conexion.getInputStream()));
        salida = new PrintWriter(conexion.getOutputStream(), true);

        salida.println("HELLO|GUI|" + miId);
        ventana.mostrarLinea(miId + " conectado al nodo en el puerto " + puertoNodo);

        // Hilo que escucha todo lo que llega por broadcast
        new Thread(() -> {
            try {
                String mensaje;
                while ((mensaje = entrada.readLine()) != null) {
                    procesar(mensaje);
                }
            } catch (Exception e) {
            }
            ventana.mostrarLinea("Se perdio la conexion con el nodo.");
        }).start();
    }

    public static void procesar(String mensaje) {
        String[] p = mensaje.split("\\|");
        String codigo = p[Protocolo.CODIGO];
        String origen = p[Protocolo.ORIGEN];

        if (Protocolo.esRespuesta(codigo)) {
            // SUM_RES|srv-suma3-91bc-4|srv-suma3|gui1-3fa2-7|8.0
            String idSolicitud = p[Protocolo.ID_SOLICITUD];
            String resultado = p[Protocolo.DATO1];

            if (quitarPendiente(idSolicitud)) {
                // Es la respuesta a una solicitud MIA, y es la primera que llega
                ventana.mostrarLinea("<- " + codigo + " para " + idSolicitud + " = " + resultado
                        + "  (atendio " + origen + ")");
                ventana.mostrarResultado(resultado, origen);
                Historial.guardar(archivo, "RECIBIDO," + idSolicitud + "," + codigo + ","
                        + origen + "," + resultado);

            } else if (idSolicitud.startsWith(miId + "-")) {
                // Es mia, pero ya habia recibido una respuesta antes
                ventana.mostrarLinea("   Respuesta duplicada de " + origen + " para "
                        + idSolicitud + ", ignorada");

            } else {
                ventana.mostrarLinea("   (broadcast) " + codigo + " de " + origen
                        + " para " + idSolicitud + " = " + resultado);
            }

        } else if (Protocolo.esSolicitud(codigo) && !origen.equals(miId)) {
            ventana.mostrarLinea("   (broadcast) " + codigo + " de " + origen + ": "
                    + p[Protocolo.DATO1] + ", " + p[Protocolo.DATO2]);
        }
    }

    // La ventana llama a esto cuando se aprieta "="
    public static void enviarOperacion(double a, String servicio, double b) {
        String id = Protocolo.nuevoId(miId);
        agregarPendiente(id);

        // SUM_REQ|gui1-3fa2-7|gui1|-|5.0|3.0
        String solicitud = servicio + "_REQ|" + id + "|" + miId + "|-|" + a + "|" + b;
        salida.println(solicitud);
        ventana.mostrarLinea("-> " + solicitud);
        Historial.guardar(archivo, "ENVIADO," + id + "," + servicio + "_REQ," + a + "," + b);

        // Si en 5 segundos nadie contesto, aviso (por ejemplo, si no hay servidores de ese tipo)
        new Thread(() -> {
            try {
                Thread.sleep(TIEMPO_LIMITE);
            } catch (InterruptedException e) {
            }
            if (quitarPendiente(id)) {
                ventana.mostrarLinea("!! Sin respuesta para " + id + " despues de "
                        + (TIEMPO_LIMITE / 1000) + " s");
                ventana.mostrarResultado("sin respuesta", "nadie");
            }
        }).start();
    }

    public static synchronized void agregarPendiente(String id) {
        pendientes.add(id);
    }

    // true si estaba pendiente (y la quita), false si no
    public static synchronized boolean quitarPendiente(String id) {
        return pendientes.remove(id);
    }
}
