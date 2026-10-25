import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDate;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Generator licencji dla Pack Mixer. TYLKO DLA AUTORA - nie dawaj tego folderu ani klucza prywatnego nikomu.
 *
 * Uruchamiaj z głównego folderu projektu (Java 21+):
 *   java tools/LicenseGenerator.java keygen
 *   java tools/LicenseGenerator.java issue XXXX-XXXX-XXXX-XXXX --owner Jan --days 365
 *   java tools/LicenseGenerator.java verify <klucz> XXXX-XXXX-XXXX-XXXX
 */
public class LicenseGenerator {
    static final String PREFIX = "PMX1-";
    static final Path PRIVATE = Path.of("tools/keys/private.key");
    static final Path PUBLIC_MOD = Path.of("src/main/resources/assets/packmixer/license_public.key");
    static final Path ISSUED = Path.of("tools/issued.csv");

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            return;
        }
        switch (args[0]) {
            case "keygen" -> keygen(args.length > 1 && args[1].equals("--force"));
            case "issue" -> issue(args);
            case "verify" -> verify(args);
            default -> usage();
        }
    }

    static void usage() {
        System.out.println("""
                Pack Mixer - generator licencji

                  keygen [--force]                       tworzy parę kluczy (klucz publiczny trafia do moda)
                  issue <ID_URZADZENIA> [--owner NAZWA] [--days N]
                                                         wystawia licencję dla jednego urządzenia
                  verify <KLUCZ> <ID_URZADZENIA>         sprawdza klucz (test)

                Uruchamiaj z głównego folderu projektu.""");
    }

    static void keygen(boolean force) throws Exception {
        if (Files.exists(PRIVATE) && !force) {
            System.out.println("Klucz prywatny już istnieje: " + PRIVATE);
            System.out.println("Użyj 'keygen --force', żeby go nadpisać (wszystkie dotychczasowe licencje przestaną działać).");
            return;
        }
        KeyPair kp = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        Files.createDirectories(PRIVATE.getParent());
        Files.writeString(PRIVATE, Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded()));
        Files.createDirectories(PUBLIC_MOD.getParent());
        Files.writeString(PUBLIC_MOD, Base64.getEncoder().encodeToString(kp.getPublic().getEncoded()));
        System.out.println("Klucz prywatny:  " + PRIVATE + "   (TRZYMAJ W TAJEMNICY)");
        System.out.println("Klucz publiczny: " + PUBLIC_MOD + "   (wchodzi do moda - przebuduj mod!)");
    }

    static void issue(String[] a) throws Exception {
        if (a.length < 2) {
            usage();
            return;
        }
        String machine = a[1].replaceAll("[^0-9A-Fa-f]", "").toUpperCase(Locale.ROOT);
        if (machine.length() != 16) {
            System.out.println("ID urządzenia musi mieć 16 znaków hex (format XXXX-XXXX-XXXX-XXXX).");
            return;
        }
        String owner = "";
        long days = 0;
        for (int i = 2; i < a.length - 1; i++) {
            if (a[i].equals("--owner")) owner = a[++i];
            else if (a[i].equals("--days")) days = Long.parseLong(a[++i]);
        }
        owner = owner.replaceAll("[;=\\r\\n]", " ").trim();
        if (owner.length() > 40) owner = owner.substring(0, 40);

        byte[] serialBytes = new byte[6];
        new SecureRandom().nextBytes(serialBytes);
        String serial = HexFormat.of().withUpperCase().formatHex(serialBytes);
        long exp = days > 0 ? LocalDate.now().plusDays(days).toEpochDay() : 0;

        String payload = "serial=" + serial + ";machine=" + machine + ";exp=" + exp + ";owner=" + owner;
        PrivateKey pk = loadPrivate();
        Signature s = Signature.getInstance("Ed25519");
        s.initSign(pk);
        s.update(payload.getBytes(StandardCharsets.UTF_8));
        byte[] sig = s.sign();

        Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
        String key = PREFIX + enc.encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." + enc.encodeToString(sig);

        boolean header = !Files.exists(ISSUED);
        String line = (header ? "serial,machine,owner,expires,issued\n" : "")
                + serial + "," + machine + "," + owner + "," + (exp == 0 ? "never" : LocalDate.ofEpochDay(exp)) + "," + LocalDate.now() + "\n";
        Files.writeString(ISSUED, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        System.out.println("Licencja wystawiona (serial " + serial + ", urządzenie " + machine + ", ważna " + (exp == 0 ? "bezterminowo" : "do " + LocalDate.ofEpochDay(exp)) + "):\n");
        System.out.println(key);
        System.out.println("\nZapisano w " + ISSUED);
    }

    static void verify(String[] a) throws Exception {
        if (a.length < 3) {
            usage();
            return;
        }
        PublicKey pub = KeyFactory.getInstance("Ed25519")
                .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(Files.readString(PUBLIC_MOD).trim())));
        String key = a[1].replaceAll("\\s+", "");
        String body = key.substring(PREFIX.length());
        int dot = body.indexOf('.');
        byte[] payload = Base64.getUrlDecoder().decode(body.substring(0, dot));
        byte[] sig = Base64.getUrlDecoder().decode(body.substring(dot + 1));
        Signature s = Signature.getInstance("Ed25519");
        s.initVerify(pub);
        s.update(payload);
        boolean ok = s.verify(sig);
        String text = new String(payload, StandardCharsets.UTF_8);
        String machine = a[2].replaceAll("[^0-9A-Fa-f]", "").toUpperCase(Locale.ROOT);
        System.out.println("Podpis: " + (ok ? "OK" : "NIEPRAWIDŁOWY"));
        System.out.println("Dane:   " + text);
        System.out.println("Urządzenie pasuje: " + text.contains("machine=" + machine));
    }

    static PrivateKey loadPrivate() throws Exception {
        if (!Files.exists(PRIVATE)) throw new IllegalStateException("Brak klucza prywatnego. Najpierw: java tools/LicenseGenerator.java keygen");
        return KeyFactory.getInstance("Ed25519")
                .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(Files.readString(PRIVATE).trim())));
    }
}
