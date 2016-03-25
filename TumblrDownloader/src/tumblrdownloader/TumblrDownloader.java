
package tumblrdownloader;

import java.util.Scanner;

/**
 *
 * @author Victor_Reiner
 */
public class TumblrDownloader {

    public static void main(String[] args) {
        //Ejemplo enlace sencillo: http://pruebasphp.victorreiner.com/pruebas/pruebaarticulos.html
        //ejemplo tumblr: http://fantasticpapersong.tumblr.com/
        System.out.println("Introduzca la URL del tumblr: ");
        System.out.println("*Ejemplo: http://fantasticpapersong.tumblr.com/*");
        Scanner in = new Scanner(System.in);
        String enlace = in.next();
        String directorio = enlace.substring(7,enlace.indexOf("."));

        TumblrProcessor tumblr = new TumblrProcessor(("./" + directorio), 10, 5);

        System.out.println("Procediendo a decargar imagenes:");
        tumblr.process(enlace);
    }

}
