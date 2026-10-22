package servidor;

public class Calculadora {

    public static double sumar(double a, double b) {
        return a + b;
    }

    public static double restar(double a, double b) {
        return a - b;
    }

    public static double multiplicar(double a, double b) {
        return a * b;
    }

    public static double dividir(double a, double b) {
        if (b == 0) {
            throw new ArithmeticException("division entre cero no se puede");
        }
        return a / b;
    }

    // Ahora recibe el servicio (SUM, SUB, MUL, DIV) en vez del simbolo
    public static double calcular(String servicio, double a, double b) {
        switch (servicio) {
            case "SUM":
                return sumar(a, b);
            case "SUB":
                return restar(a, b);
            case "MUL":
                return multiplicar(a, b);
            case "DIV":
                return dividir(a, b);
            default:
                throw new IllegalArgumentException("servicio no valido: " + servicio);
        }
    }
}
