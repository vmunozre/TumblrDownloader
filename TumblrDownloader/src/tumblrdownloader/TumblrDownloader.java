
package tumblrdownloader;

import java.util.Scanner;

/**
 *
 * @author Victor_Reiner
 */
public class TumblrDownloader {

    public static void main(String[] args) {
        //Ejemplo enlace sencillo: http://pruebasphp.victorreiner.com/pruebas/pruebaarticulos.html
        //ejemplo tumblr : http://fantasticpapersong.tumblr.com/
        System.out.println("Introduzca la id del tumblr: ");
        System.out.println("*Ejemplo: 'fantasticpapersong' http:// | fantasticpapersong | .tumblr.com/*");
        Scanner in = new Scanner(System.in);
        String id = in.next();
        
        String enlace = ("http://" + id + ".tumblr.com/");
        //Ahora sabemos, si o si, que el enlace empieza por http:// y acaba por .tumblr.com/
        System.out.println(enlace);
        //String directorio = enlace.substring(7,enlace.indexOf("."));
        
        TumblrProcessor tumblr = new TumblrProcessor(("./downloads/" + id), 10, 5);

        System.out.println("Procediendo a decargar imagenes:");
        tumblr.process(enlace);
    }

}
