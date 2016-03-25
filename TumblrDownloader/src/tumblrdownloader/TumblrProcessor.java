
package tumblrdownloader;

/**
 *
 * @author Victor_Reiner
 */
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class TumblrProcessor {
    //CONSTANTES

    private int NUMHILOSCARGAR = 10;

    private String folderPath; // Ruta donde se guardaran las imagenes
    private int nDown; // Numero de hilos que se crean
    private int maxDown; // Numero máximo de hilos permitidos que funcionen a la vez.
    //Variables generales
    private ArrayList<String> arrayImagenes;
    private int contadorDescargas;
    private boolean esUltima;
    private int numPagina;

    //semaforos
    private Semaphore sArray;
    private Semaphore sSincroFin;
    private Semaphore sPaginas;
    //private Semaphore sAddArray;
    private Semaphore sSincroCargar;

    public TumblrProcessor(String path, int nDown, int maxDown) {
        this.folderPath = path;
        this.nDown = nDown;
        this.maxDown = maxDown;
        this.arrayImagenes = new ArrayList<String>();
        this.contadorDescargas = 0;
        this.numPagina = 1;
        this.esUltima = false;
    } // FIN CONSTRUCTOR

    // INICIO METODOS
    private void cargarImagenes(String enlace) {
        // Busca las imagenes articulo a articulo y añade su enlace a un arraylist
        try {

            //Inicializo los semaforos
            this.sPaginas = new Semaphore(1);
            //this.sAddArray = new Semaphore(1);
            this.sSincroCargar = new Semaphore(0);

            //Inicializo los threads
            List<Thread> ths = new ArrayList<Thread>();
            for (int i = 0; i < NUMHILOSCARGAR; i++) {
                Thread th = new Thread(new Runnable() {
                    public void run() {
                        try {
                            hiloCargarImagenes(enlace);
                        } catch (Exception e) {
                            System.out.println("Error en el Hilo: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }, "HiloDescargas" + i);
                th.start();
                ths.add(th);
            }

            //Espero a que se terminen todos los hijos para acabar
            this.sSincroCargar.acquire(NUMHILOSCARGAR);
        } catch (Exception ex) {
            System.err.println("Error: " + ex.getMessage());

        }

    } // FIN CARGARIMAGENES

    public void process(String enlace) {
        // carga las imagenes y despues lanza los hilos para que las descargue.
        // Primero miramos si existe y si no creamos el directorio

        try {
            File folder = new File(folderPath);
            if (!folder.isDirectory()) {
                folder.mkdirs();
            }

            //Inicio los semaforos
            this.sArray = new Semaphore(maxDown);
            this.sSincroFin = new Semaphore(0);

            //Cargo las imagenes
            this.cargarImagenes(enlace);
            System.out.println("Ya se han cargado las imagenes");
            //Creo los threads
            List<Thread> ths = new ArrayList<Thread>();
            for (int i = 0; i < nDown; i++) {
                Thread th = new Thread(new Runnable() {
                    public void run() {
                        try {
                            hiloDescargas();
                        } catch (Exception e) {
                            System.out.println("Error en el Hilo: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }, "HiloDescargas" + i);
                th.start();
                ths.add(th);
            }

            Thread thCheckeo = new Thread(new Runnable() {
                public void run() {
                    try {
                        hiloCheckeo();
                    } catch (Exception e) {
                        System.out.println("Error en el Hilo: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }, "HiloCheckeo");
            thCheckeo.start();

            sSincroFin.acquire(this.arrayImagenes.size());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

    } // FIN PROCESS

    private void getImages(String src) throws IOException {

        String folder = null;

        // Exctract the name of the image from the src attribute
        int indexname = src.lastIndexOf("/");

        if (indexname == src.length()) {
            src = src.substring(1, indexname);
        }

        indexname = src.lastIndexOf("/");
        String name = src.substring(indexname, src.length());

        //System.out.println(name);
        // Open a URL Stream
        URL url = new URL(src);
        InputStream in = url.openStream();

        OutputStream out = new BufferedOutputStream(new FileOutputStream(folderPath + name));

        for (int b; (b = in.read()) != -1;) {
            out.write(b);
        }
        out.close();
        in.close();

    }
    // FIN METODOS

    // INICIO THREADS
    private void hiloCargarImagenes(String enlace) throws Exception {
        String url;
        if (enlace.charAt(enlace.length() - 1) == '/') {
            url = (enlace + "page/");
        } else {
            url = (enlace + "/page/");
        }
        int auxNumPagina = 0;
        //PRIMER ACCESO SECCION CRITICA
        this.sPaginas.acquire();
        while (!this.esUltima) {
            //SECCION CRITICA PAGINAS (al final del bucle)
            auxNumPagina = this.numPagina;
            this.numPagina++;
            this.sPaginas.release();
            //FIN SECCION CRITICA PAGINAS
            System.out.println("Conectando al enlance, pagina: " + auxNumPagina);
            Document doc = Jsoup.connect(url + auxNumPagina).get();
            System.out.println("Conectado al enlace");
            //Get all elements with img tag ,

            Elements articles = doc.getElementsByTag("article");
            if (articles.isEmpty()) {
                esUltima = true;
            } else {
                System.out.println("Lista de articulos encontrada");
                for (Element el : articles) {

                    Elements img = el.getElementsByTag("img");
                    for (Element e : img) {
                        String src = e.absUrl("src");
                        //System.out.println("Image Found!");
                        //System.out.println("src attribute is : "+src);

                        //SECCION CRITICA ADD ARRAY
                        //this.sAddArray.acquire();
                        //if(!arrayImagenes.contains(src)){
                        this.arrayImagenes.add(src);
                        //}
                        //this.sAddArray.release();
                        //FIN SECCION CRITICA ADD ARRAY

                    }

                }
            }

            this.sPaginas.acquire();
        }
        this.sPaginas.release();
        this.sSincroCargar.release();
    } // FIN HILOCARGARIMAGENES

    private void hiloDescargas() throws Exception {
        while (this.contadorDescargas <= (this.arrayImagenes.size() - 1)) {
            //SECCION CRITICA ARRAY
            this.sArray.acquire();
            String src = this.arrayImagenes.get(this.contadorDescargas);
            this.contadorDescargas++;
            this.sArray.release();
            //FIN SECCION CRITICA ARRAY
            this.getImages(src);

            //Libero un semaforo para informar que ya se ha realizado la descarga.
            sSincroFin.release();
        }
    } // FIN HILODESCARGAS

    private void hiloCheckeo() throws Exception {
        while (this.contadorDescargas <= (this.arrayImagenes.size() - 1)) {
            //SECCION CRITICA ARRAY
            this.sArray.acquire();
            System.out.println("Proceso: " + this.contadorDescargas + "/" + this.arrayImagenes.size());
            this.sArray.release();
            //FIN SECCION CRITICA ARRAY

            Thread.sleep(3000);
        }
    } // FIN HILOCHECKEO

    // FIN THREADS
}
