package nodo;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import comun.Protocolo;

// Nodo del Campo de Datos.
//
// Uso:
//   java nodo.NodoMain nodo1 5001              -> primer nodo, no conoce a nadie
//   java nodo.NodoMain nodo2 5002 5001         -> conoce a un nodo; al resto lo descubre
//   java nodo.NodoMain nodo3 5003 otraPC:5001  -> tambien sirve con otra maquina
//
// Diferencia con el middleware del primer parcial: el middleware solo
// escuchaba. El nodo escucha (celulas y nodos nuevos) Y ADEMAS se conecta
// hacia afuera a otros nodos para formar la malla. Es servidor y cliente.
public class NodoMain {

    public static String miId;
    public static int miPuerto;

    // Conexiones activas, separadas por tipo de entidad
    public static List<ManejadorConexion> nodos = new ArrayList<>();
    public static List<ManejadorConexion> celulas = new ArrayList<>();

    // Nodos a los que ya me estoy conectando pero todavia no terminan el saludo.
    // Evita abrir dos conexiones al mismo nodo durante el descubrimiento.
    public static List<String> enCamino = new ArrayList<>();

    // Mensajes ya procesados: ID_MENSAJE -> momento en que llego (milisegundos)
    public static Map<String, Long> vistos = new HashMap<>();
    public static final long TIEMPO_RECUERDO = 60000; // se olvidan despues de 60 s

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Uso: java nodo.NodoMain <id> <puerto> [contacto ...]");
            return;
        }
        miId = args[0];
        miPuerto = Integer.parseInt(args[1]);

        ServerSocket puerta = new ServerSocket(miPuerto);
        System.out.println(miId + " escuchando en el puerto " + miPuerto);

        // Hilo que borra de "vistos" los IDs viejos, para que la lista no crezca para siempre
        new Thread(() -> limpiarVistos()).start();

        // Si me pasaron contactos, me conecto a ellos. El resto de la malla
        // la descubro con la respuesta WELCOME de cada uno.
        for (int i = 2; i < args.length; i++) {
            conectarANodo(args[i]);
        }

        // Igual que el middleware: una sola puerta, un hilo por conexion
        while (true) {
            Socket conexion = puerta.accept();
            new Thread(new ManejadorConexion(conexion, false)).start();
        }
    }

    // Abre una conexion SALIENTE hacia otro nodo. "direccion" puede ser
    // solo el puerto (5001) o host:puerto (192.168.1.20:5001).
    public static void conectarANodo(String direccion) {
        String host = "localhost";
        String puerto = direccion;
        if (direccion.contains(":")) {
            host = direccion.split(":")[0];
            puerto = direccion.split(":")[1];
        }
        try {
            Socket conexion = new Socket(host, Integer.parseInt(puerto));
            new Thread(new ManejadorConexion(conexion, true)).start();
        } catch (Exception e) {
            System.out.println("No se pudo conectar con el nodo en " + direccion);
        }
    }

    // ---------------- Control de conexiones ----------------

    // Devuelve false si ese nodo ya estaba conectado (conexion duplicada)
    public static synchronized boolean registrarNodo(ManejadorConexion m) {
        enCamino.remove(m.getIdentificador());
        if (m.getIdentificador().equals(miId) || conozcoNodo(m.getIdentificador())) {
            return false;
        }
        nodos.add(m);
        System.out.println("Nodo conectado: " + m.getIdentificador() + "  " + resumen());
        return true;
    }

    public static synchronized void registrarCelula(ManejadorConexion m) {
        celulas.add(m);
        System.out.println("Celula conectada: " + m.getIdentificador()
                + " (" + m.getTipo() + ")  " + resumen());
    }

    public static synchronized void eliminar(ManejadorConexion m) {
        nodos.remove(m);
        celulas.remove(m);
        System.out.println("Se desconecto: " + m.getIdentificador() + "  " + resumen());
    }

    public static synchronized boolean conozcoNodo(String id) {
        for (ManejadorConexion n : nodos) {
            if (n.getIdentificador().equals(id)) {
                return true;
            }
        }
        return false;
    }

    // Aparta un nodo para conectarme a el. false si soy yo, ya lo tengo o ya voy en camino.
    public static synchronized boolean reservar(String id) {
        if (id.equals(miId) || conozcoNodo(id) || enCamino.contains(id)) {
            return false;
        }
        enCamino.add(id);
        return true;
    }

    public static synchronized void liberar(String id) {
        enCamino.remove(id);
    }

    // Lista de nodos que conozco, para mandarsela a un nodo nuevo.
    // Ejemplo: nodo2@127.0.0.1:5002,nodo3@127.0.0.1:5003
    public static synchronized String listaDeNodos() {
        String lista = "";
        for (ManejadorConexion n : nodos) {
            if (!lista.equals("")) {
                lista = lista + ",";
            }
            lista = lista + n.getIdentificador() + "@" + n.getDireccion();
        }
        return lista;
    }

    public static String resumen() {
        return "[nodos: " + nodos.size() + ", celulas: " + celulas.size() + "]";
    }

    // ---------------- Broadcast y prevencion de ciclos ----------------

    // Se llama cada vez que llega un mensaje de datos, venga de una celula o de un nodo
    public static synchronized void recibir(String mensaje, ManejadorConexion deQuien) {
        String[] p = mensaje.split("\\|");
        if (p.length < 5) {
            System.out.println("Mensaje mal formado de " + deQuien.getIdentificador() + ": " + mensaje);
            return;
        }
        String codigo = p[Protocolo.CODIGO];
        String idMensaje = p[Protocolo.ID_MENSAJE];

        // 1. Si ya lo vi, no lo vuelvo a difundir. Esto es lo que corta los ciclos.
        if (vistos.containsKey(idMensaje)) {
            System.out.println("   DESCARTADO (ya visto) " + codigo + " " + idMensaje
                    + " que llego de " + deQuien.getIdentificador());
            return;
        }
        vistos.put(idMensaje, System.currentTimeMillis());

        // 2. Si es nuevo, lo mando a TODOS menos al que me lo mando
        int aNodos = 0;
        int aCelulas = 0;
        for (ManejadorConexion n : nodos) {
            if (n != deQuien) {
                n.enviar(mensaje);
                aNodos++;
            }
        }
        for (ManejadorConexion c : celulas) {
            if (c != deQuien) {
                c.enviar(mensaje);
                aCelulas++;
            }
        }
        System.out.println("RECIBIDO de " + deQuien.getIdentificador() + ": " + mensaje);
        System.out.println("   BROADCAST a " + aNodos + " nodo(s) y " + aCelulas + " celula(s)");
    }

    public static void limpiarVistos() {
        while (true) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
            }
            synchronized (NodoMain.class) {
                long ahora = System.currentTimeMillis();
                // Borra todos los IDs que llevan mas de TIEMPO_RECUERDO guardados
                vistos.entrySet().removeIf(e -> ahora - e.getValue() > TIEMPO_RECUERDO);
            }
        }
    }
}
