package nodo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

// Atiende UNA conexion del nodo. Puede ser:
//   - entrante: alguien se conecto a mi puerta (una celula o un nodo nuevo)
//   - saliente: yo me conecte a otro nodo para formar la malla
public class ManejadorConexion implements Runnable {

    private Socket socket;
    private boolean saliente;
    private BufferedReader entrada;
    private PrintWriter salida;

    private String identificador = "desconocido";
    private String tipo = "?";        // NODE, GUI o SERVER
    private String direccion = "";    // solo para nodos: ip:puerto donde escuchan
    private boolean registrado = false;

    public ManejadorConexion(Socket socket, boolean saliente) {
        this.socket = socket;
        this.saliente = saliente;
    }

    public String getIdentificador() {
        return identificador;
    }

    public String getTipo() {
        return tipo;
    }

    public String getDireccion() {
        return direccion;
    }

    public void enviar(String mensaje) {
        salida.println(mensaje);
    }

    @Override
    public void run() {
        try {
            // Manda cada mensaje en cuanto se escribe, sin esperar a juntar mas (ver README)
            socket.setTcpNoDelay(true);
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            salida = new PrintWriter(socket.getOutputStream(), true);

            if (saliente) {
                saludoSaliente();
            } else {
                saludoEntrante();
            }
            if (!registrado) {
                return; // conexion duplicada o saludo invalido
            }

            // A partir de aqui, solo llegan mensajes de datos
            String mensaje;
            while ((mensaje = entrada.readLine()) != null) {
                NodoMain.recibir(mensaje, this);
            }

        } catch (Exception e) {
            // se cayo la conexion
        } finally {
            if (registrado) {
                NodoMain.eliminar(this);
            } else if (saliente) {
                NodoMain.liberar(identificador);
            }
            try {
                socket.close();
            } catch (Exception e) {
            }
        }
    }

    // Yo me conecto a otro nodo:
    //   yo   -> HELLO|NODE|nodo4|5004
    //   otro -> WELCOME|nodo3|nodo1@127.0.0.1:5001,nodo2@127.0.0.1:5002
    private void saludoSaliente() throws Exception {
        salida.println("HELLO|NODE|" + NodoMain.miId + "|" + NodoMain.miPuerto);

        String respuesta = entrada.readLine();
        String[] p = respuesta.split("\\|", -1); // -1 para no perder el campo vacio si no conoce a nadie
        tipo = "NODE";
        identificador = p[1];
        direccion = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();

        registrado = NodoMain.registrarNodo(this);
        if (!registrado) {
            return;
        }

        // DESCUBRIMIENTO: me conecto a los nodos que el otro conoce y yo todavia no
        if (!p[2].equals("")) {
            for (String otro : p[2].split(",")) {
                String idOtro = otro.split("@")[0];
                String dirOtro = otro.split("@")[1];
                if (NodoMain.reservar(idOtro)) {
                    System.out.println("Descubri a " + idOtro + " en " + dirOtro + ", me conecto");
                    NodoMain.conectarANodo(dirOtro);
                }
            }
        }
    }

    // Alguien se conecto a mi. Lo primero que manda es su saludo:
    //   HELLO|GUI|gui1
    //   HELLO|SERVER|srv-suma1|SUM
    //   HELLO|NODE|nodo4|5004
    private void saludoEntrante() throws Exception {
        String saludo = entrada.readLine();
        String[] p = saludo.split("\\|");
        if (p.length < 3 || !p[0].equals("HELLO")) {
            System.out.println("Saludo invalido: " + saludo);
            return;
        }
        tipo = p[1];
        identificador = p[2];

        if (tipo.equals("NODE")) {
            // Guardo donde escucha (su IP + el puerto que me dijo), para pasarselo a futuros nodos
            direccion = socket.getInetAddress().getHostAddress() + ":" + p[3];
            // Le contesto quien soy y a quien conozco ANTES de registrarlo,
            // asi la lista no lo incluye a el mismo
            salida.println("WELCOME|" + NodoMain.miId + "|" + NodoMain.listaDeNodos());
            registrado = NodoMain.registrarNodo(this);
        } else {
            NodoMain.registrarCelula(this);
            registrado = true;
        }
    }
}
