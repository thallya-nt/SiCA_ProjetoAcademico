package sockets;

import java.io.*;
import java.net.*;

public class Server {
    private static final int DOOR = 2026;
    private static final String DIRETORIO_SERVER = "./arquivos_servidor/";

    public static void main(String[] args) {
        //CRIAR DIRETÓRIO CASO NÃO EXISTA
        File diretorio = new File(DIRETORIO_SERVER);
        if (!diretorio.exists()){
            diretorio.mkdirs();
        }
        System.out.println("SERVIDOR INICIADO NA PORTA "+DOOR+". . .");

        try (ServerSocket serverSocket = new ServerSocket(DOOR)){
            while (true){
                Socket socket = serverSocket.accept();
                System.out.println("CLIENTE CONECTADO! [ "+socket.getInetAddress()+" ]");

                tratarClient(socket);
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private static void tratarClient(Socket socket){
        try(DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream()))
        {
            String request = in.readUTF().trim().toUpperCase();

            switch (request){
                case "LIST":
                    listarArquivos(out);
                    break;
                case "UPLOAD":
                    receberArquivos(in);
                    break;
                case "DOWNLOAD":
                    enviarArquivos(in, out);
                    break;
                default:
                    System.out.println("PEDIDO NÃO RECONHECIDO "+ request);
            }
        } catch (IOException e){
            System.out.println("ERRO ⚠️"+ e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    private static void listarArquivos(DataOutputStream out) throws IOException{
        File file = new File(DIRETORIO_SERVER);
        File[] docs = file.listFiles();

        if (docs != null && docs.length > 0){
            out.writeInt(docs.length);
            for (File f : docs){
                if (f.isFile()){
                    out.writeUTF(f.getName());
                }
            }
        } else{
            out.writeInt(0);
        }
    }
    private static void receberArquivos (DataInputStream in) throws IOException{
        String nomeDoc = in.readUTF();
        long docSize = in.readLong();

        File doc = new File(DIRETORIO_SERVER + nomeDoc);
        try (FileOutputStream fout = new FileOutputStream(doc)){
            byte[] buffer = new byte[4096];
            int bytesLidos;
            long totalLido = 0;

            while (totalLido < docSize && (bytesLidos = in.read(buffer, 0, (int)
                    Math.min(buffer.length, docSize - totalLido))) != -1){
                fout.write(buffer, 0, bytesLidos);
                totalLido += bytesLidos;
            }
        }
        System.out.println("ARQUIVO RECEBIDO COM SUCESSO"+ nomeDoc);
    }
    private static void enviarArquivos(DataInputStream in, DataOutputStream out) throws IOException{
        String nomeDoc = in.readUTF();
        File doc = new File(DIRETORIO_SERVER + nomeDoc);

        if (doc.exists() && doc.isFile()){
            out.writeBoolean(true);
            out.writeLong(doc.length());

            try (FileInputStream fin = new FileInputStream(doc)){
               byte[] buffer = new byte[4096];
               int bytesLidos;
               while ((bytesLidos = fin.read(buffer)) != -1){
                   out.write(buffer, 0, bytesLidos);
               }
            }
            System.out.println("ARQUIVO ENVIADO!" + nomeDoc);
        } else {
            out.writeBoolean(false);
        }
    }
}
