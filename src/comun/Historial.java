package comun;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Historial {

    private static DateTimeFormatter formato = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static synchronized void guardar(String archivo, String linea) {
        try {
            String fecha = LocalDateTime.now().format(formato);
            PrintWriter escritor = new PrintWriter(new FileWriter(archivo, true));
            escritor.println(fecha + "," + linea);
            escritor.close();
        } catch (Exception e) {
            System.out.println("No se pudo escribir en " + archivo + ": " + e.getMessage());
        }
    }
}