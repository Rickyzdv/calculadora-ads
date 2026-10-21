package middleware;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ManejadorConexion implements Runnable {

    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;
    private String identificador = "desconocido";

    public ManejadorConexion(Socket socket) {
        this.socket = socket;
    }

    public String getIdentificador() {
        return identificador;
    }

    // Manda un mensaje por ESTA conexion
    public void enviar(String mensaje) {
        salida.println(mensaje);
    }

    @Override
    public void run() {
        try {
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            salida = new PrintWriter(socket.getOutputStream(), true);

            // 1. Lo primero que debe llegar es el saludo: HELLO|CLIENT|cliente1
            String saludo = entrada.readLine();
            String[] partes = saludo.split("\\|");
            String tipo = partes[1];
            identificador = partes[2];

            if (tipo.equals("CLIENT")) {
                MiddlewareMain.registrarCliente(this);
            } else {
                MiddlewareMain.registrarServidor(this);
            }

            // 2. A partir de aqui, escucha mensajes para siempre
            String mensaje;
            while ((mensaje = entrada.readLine()) != null) {
                System.out.println("RECIBIDO de " + identificador + ": " + mensaje);

                if (mensaje.startsWith("REQ")) {
                    MiddlewareMain.enviarATodosLosServidores(mensaje);
                } else if (mensaje.startsWith("RES")) {
                    MiddlewareMain.enviarATodosLosClientes(mensaje);
                }
            }

        } catch (Exception e) {
            System.out.println("Conexion terminada con " + identificador);
        } finally {
            MiddlewareMain.eliminar(this);
            try {
                socket.close();
            } catch (Exception e) {
            }
        }
    }
}