package sockets;

import java.io.*;
import java.net.*;

public class Server {
    //CONFIGURAÇÃO DO SERVIDOR
    private static final int DOOR = 2026; //PORTA
    private static final String DIRETORIO_SERVER = "./arquivos_servidor/";

    public static void main(String[] args) {
        //CRIAR PASTA, CASO NÃO EXISTA
        File diretorio = new File(DIRETORIO_SERVER);
        if (!diretorio.exists()){
            diretorio.mkdirs();
        }
        System.out.println("SERVIDOR INICIADO NA PORTA "+DOOR+". . .");

        //CRIA SERVERSOCKET PARA ESCUTAR CONEXÕES DE REDE
        try (ServerSocket serverSocket = new ServerSocket(DOOR)){
            //LOOP PARA MANTER O SERVIDOR RODANDO E ACEITANDO CONEXÕES
            while (true){
                Socket socket = serverSocket.accept();
                System.out.println("CLIENTE CONECTADO! [ "+socket.getInetAddress()+" ]");

                tratarClient(socket); //PROCESSA A REQUISIÇÃO DO CLIENTE
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    //LÊ AS SOLICITAÇÕES DO CLIENTE E EXECUTA AÇÕES CORRESPONDENTES
    private static void tratarClient(Socket socket){
        try(DataInputStream in = new DataInputStream(socket.getInputStream()); //CANAL DE ENTRADA DE DADOS
            DataOutputStream out = new DataOutputStream(socket.getOutputStream())) // CANAL DE SAÍDA DE DADOS
        {
            String request = in.readUTF().trim().toUpperCase(); //LÊ O COMANDO ENVIADO PELO CLIETE

            //EXECUTA O METODO REFENTE AO COMANDO RECEBIDO
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
                socket.close(); // GARANTE QUE SOCKET SEJA FECHADO APÓS A CONCLUSÃO DO REQUEST
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    //METODO QUE LISTA OS ARQUIVOS PARA O CLIENTE
    private static void listarArquivos(DataOutputStream out) throws IOException{
        //MAPEIA OS ARQUIVOS DENTRO DA PASTA DO SERVIDOR
        File file = new File(DIRETORIO_SERVER);
        File[] docs = file.listFiles();

        if (docs != null && docs.length > 0){
            out.writeInt(docs.length); //QUANTIDADE DE ARQUIVOS
            for (File f : docs){
                if (f.isFile()){
                    out.writeUTF(f.getName()); // NOME DOS ARQUIVOS
                }
            }
        } else{
            out.writeInt(0);
        }
    }
    //METODO QUE RECEBE OS ARQUIVOS ENVIADOS PELO CLIENTE
    private static void receberArquivos (DataInputStream in) throws IOException{
        String nomeDoc = in.readUTF(); //NOME DO ARQUIVO
        long docSize = in.readLong(); //TAMANHO DO ARQUIVO EM BYTES

        File doc = new File(DIRETORIO_SERVER + nomeDoc);
        try (FileOutputStream fout = new FileOutputStream(doc)){ //GRAVA O ARQUIVO EM BLOCOS DE BYTES
            byte[] buffer = new byte[4096];
            int bytesLidos;
            long totalLido = 0; //CONTADOR DE BYTES

            //CONTINUA LENDO O ARQUIVO ATÉ ATINGIR O TAMANHO ESPERADO
            while (totalLido < docSize && (bytesLidos = in.read(buffer, 0, (int)
                    Math.min(buffer.length, docSize - totalLido))) != -1){
                fout.write(buffer, 0, bytesLidos);
                totalLido += bytesLidos;
            }
        }
        System.out.println("ARQUIVO RECEBIDO COM SUCESSO"+ nomeDoc);
    }

    //METODO QUE ENVIA ARQUIVOS PARA O CLIENTE
    private static void enviarArquivos(DataInputStream in, DataOutputStream out) throws IOException{
        String nomeDoc = in.readUTF(); //RECEBE O NOME DO ARQUIVO
        File doc = new File(DIRETORIO_SERVER + nomeDoc);

        //VERIFICA SE O ARQUIVO EXISTE NO SERVIDOR
        if (doc.exists() && doc.isFile()){
            out.writeBoolean(true); // INFORMA QUE O ARQUIVO EXISTE
            out.writeLong(doc.length());

            //LÊ E ENVIA O ARQUIVO
            try (FileInputStream fin = new FileInputStream(doc)){
               byte[] buffer = new byte[4096];
               int bytesLidos;
               while ((bytesLidos = fin.read(buffer)) != -1){
                   out.write(buffer, 0, bytesLidos);
               }
            }
            System.out.println("ARQUIVO ENVIADO!" + nomeDoc);
        } else {
            out.writeBoolean(false); //AVISA O CLIENTE QUE O ARQUIVO NÃO EXISTE
        }
    }
}
