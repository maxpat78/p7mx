import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.*;
import java.util.Base64;

import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSException;

//~ Finora sono stati osservati 3 tipi di fatture:
//~ 1) puri XML senza allegati, con i dati di fattura
//~ 2) P7M/DER con PDF allegato in un campo <Attachment> e, ovviamente, codificato Base64
//~ 3) P7M/BASE64/DER, per il resto come 2)

public class p7mx {

    static byte[] unB64(byte[] encoded) {
        try { // Prova a decodificare da Base64 e ritorna o l'oggetto decodificato o l'originale
            return Base64.getMimeDecoder().decode(encoded);
        }
        catch (Exception e) {
            return encoded;
        }
    }

    public static void main(String[] args) {
    
        if (args.length < 1) {
            System.out.println("Occorre specificare almeno una fattura firmata da decodificare!");
            System.exit(-1);
        }
        
        for (String arg : args) {
            byte[] raw = null;
            String s = null;
        
            try { // Estrae la fattura XML dal P7M e la trasforma in String
                // 1. Legge il p7M
                raw = Files.readAllBytes(Paths.get(arg));
                // 2. Se in Base64, lo decodifica
                raw = unB64(raw);
                // 3. Se si trattasse di un XML puro e semplice, prepara la stringa
                s = new String(raw, "ASCII");
                //~ Files.write(Paths.get("output.bin"), raw);
                // 4. Se si tratta di un DER, estrae la fattura XML e la pone definitivamente nella stringa;
                // se l'operazione fallisce, la stringa resta quella del punto 3
                CMSProcessableByteArray plain = (CMSProcessableByteArray) new CMSSignedData(raw).getSignedContent();
                s = new String(plain.getInputStream().readAllBytes(), "ASCII");
            }
            catch (CMSException e) {
            }
            catch (Exception e) { // Altre eccezioni fanno scartare il file
                System.out.println(e.toString() + " - Errore leggendo " + arg);
                //~ e.printStackTrace();
                continue;
            }
        
            // Cerca l'Attachment: a questo punto, dovremmo avere un XML valido
            Matcher m = Pattern.compile("<Attachment>(.+)</Attachment>",
            Pattern.MULTILINE|Pattern.DOTALL).matcher(s);
            if (m.find()) {
                s = m.group(1); // rimpiazza il P7M con il PDF
            } 
            else {
                System.out.println(arg + " non contiene una fattura PDF");
                //~ Files.write(Paths.get("output.bin"), s.getBytes());
                continue;
            }
        
            try { // Finalmente, prova a decodificare il PDF da Base64 e lo salva nella posizione di origine
                byte[] pdf = Base64.getMimeDecoder().decode(s.getBytes());
                if (pdf.length < 4 || (pdf[0] != '%' || pdf[1] != 'P' || pdf[2] != 'D' || pdf[3] != 'F')) {
                    System.out.println(arg + " non contiene una fattura PDF");
                    continue;
                }
                String new_name = arg.replaceAll("(?i)xml(.p7m)?", "pdf");
                System.out.println("Salvo la fattura PDF: " + new_name);
                Files.write(Paths.get(new_name), pdf);
            }
            catch (Exception e) {
                System.out.println("Errore decodificando la fattura PDF in " + arg);
                continue;
            }
        }
    }

}
