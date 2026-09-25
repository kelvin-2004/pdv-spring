import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class PonteImpressao {

    public static void main(String[] args) throws Exception {
        String acao = args.length > 0 ? args[0] : "fila";
        switch (acao) {
            case "listar" -> listarImpressoras();
            case "teste" -> imprimirTeste(env("IMPRESSORA", "termica"));
            case "fila" -> loopFila();
            default -> {
                System.out.println("Ação desconhecida: " + acao);
                System.out.println("Use: listar | teste | fila");
            }
        }
    }

    private static void loopFila() throws Exception {
        String url = env("PONTE_URL", null);
        String usuario = env("PONTE_USUARIO", null);
        String senha = env("PONTE_SENHA", null);
        String impressora = env("IMPRESSORA", "termica");
        int intervalo = Integer.parseInt(env("INTERVALO_SEGUNDOS", "3"));

        if (url == null || url.isBlank() || usuario == null || usuario.isBlank() || senha == null) {
            System.err.println("Defina PONTE_URL, PONTE_USUARIO e PONTE_SENHA (variáveis de ambiente).");
            System.exit(1);
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        String auth = Base64.getEncoder().encodeToString(
                (usuario + ":" + senha).getBytes(StandardCharsets.UTF_8));
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        System.out.println("Ponte iniciada. Buscando comandas em " + url + " ...");

        while (true) {
            try {
                List<Comanda> pendentes = buscarPendentes(http, url, auth);
                for (Comanda c : pendentes) {
                    System.out.println("Imprimindo pedido #" + c.id + " ...");
                    String texto = new String(Base64.getDecoder().decode(c.textoBase64), StandardCharsets.UTF_8);
                    imprimirTexto(texto, impressora);
                    concluir(http, url, auth, c.id);
                    System.out.println("Pedido #" + c.id + " impresso e marcado como concluído.");
                }
            } catch (Exception e) {
                System.err.println("Erro na ponte: " + e.getMessage());
            }
            Thread.sleep(intervalo * 1000L);
        }
    }

    private static List<Comanda> buscarPendentes(HttpClient http, String url, String auth) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url + "/api/impressao/pendentes"))
                .header("Authorization", "Basic " + auth)
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("GET /api/impressao/pendentes retornou HTTP " + resp.statusCode());
        }
        return parsePendentes(resp.body());
    }

    private static void concluir(HttpClient http, String url, String auth, long id) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url + "/api/impressao/" + id + "/concluido"))
                .header("Authorization", "Basic " + auth)
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 300) {
            throw new RuntimeException("POST concluído retornou HTTP " + resp.statusCode());
        }
    }

    private static void listarImpressoras() {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        if (services.length == 0) {
            System.out.println("Nenhuma impressora detectada nesta máquina.");
            return;
        }
        System.out.println("Impressoras disponíveis:");
        for (PrintService s : services) {
            System.out.println("  - " + s.getName());
        }
    }

    private static void imprimirTeste(String impressora) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("==================================================\n");
        sb.append("TESTE DE IMPRESSÃO (PONTE)\n");
        sb.append("Marmitas Sousa\n");
        sb.append("==================================================\n");
        sb.append("A ponte está imprimindo corretamente.\n");
        imprimirTexto(sb.toString(), impressora);
        System.out.println("Teste enviado para \"" + impressora + "\".");
    }

    private static void imprimirTexto(String texto, String nomeAlvo) throws Exception {
        PrintService impressora = null;
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        for (PrintService s : services) {
            if (s.getName().equalsIgnoreCase(nomeAlvo)) {
                impressora = s;
                break;
            }
        }
        if (impressora == null) {
            StringBuilder nomes = new StringBuilder();
            for (PrintService s : services) {
                if (nomes.length() > 0) {
                    nomes.append(", ");
                }
                nomes.append(s.getName());
            }
            throw new RuntimeException("Impressora \"" + nomeAlvo + "\" não encontrada. Disponíveis: "
                    + (nomes.length() > 0 ? nomes : "nenhuma"));
        }

        String textoFormatado = texto + "\n\n\n\n\n";
        InputStream stream = new ByteArrayInputStream(textoFormatado.getBytes("CP850"));
        DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
        Doc documento = new SimpleDoc(stream, flavor, null);
        DocPrintJob job = impressora.createPrintJob();
        PrintRequestAttributeSet atributos = new HashPrintRequestAttributeSet();
        job.print(documento, atributos);
        stream.close();
    }

    private static List<Comanda> parsePendentes(String json) {
        List<Comanda> lista = new ArrayList<>();
        int i = 0;
        while ((i = json.indexOf('{', i)) >= 0) {
            int fim = json.indexOf('}', i);
            if (fim < 0) {
                break;
            }
            String obj = json.substring(i, fim + 1);
            Long id = extrairLong(obj, "\"id\":");
            String b64 = extrairString(obj, "\"textoBase64\":\"");
            if (id != null && b64 != null) {
                lista.add(new Comanda(id, b64));
            }
            i = fim + 1;
        }
        return lista;
    }

    private static Long extrairLong(String obj, String chave) {
        int k = obj.indexOf(chave);
        if (k < 0) {
            return null;
        }
        int j = k + chave.length();
        while (j < obj.length() && (obj.charAt(j) == ' ' || obj.charAt(j) == '\t')) {
            j++;
        }
        int inicio = j;
        while (j < obj.length() && Character.isDigit(obj.charAt(j))) {
            j++;
        }
        if (inicio == j) {
            return null;
        }
        return Long.parseLong(obj.substring(inicio, j));
    }

    private static String extrairString(String obj, String chave) {
        int k = obj.indexOf(chave);
        if (k < 0) {
            return null;
        }
        int inicio = k + chave.length();
        int fim = obj.indexOf('"', inicio);
        if (fim < 0) {
            return null;
        }
        return obj.substring(inicio, fim);
    }

    private static String env(String nome, String padrao) {
        String v = System.getenv(nome);
        return (v == null || v.isBlank()) ? padrao : v;
    }

    private static class Comanda {
        final long id;
        final String textoBase64;

        Comanda(long id, String textoBase64) {
            this.id = id;
            this.textoBase64 = textoBase64;
        }
    }
}
