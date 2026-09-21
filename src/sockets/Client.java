package sockets;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    //CONFIGURAÇÕES DO SERVIDOR
    private static final String SERVER_IP = "127.0.0.1";       //LOCALHOST
    private static final int DOOR = 2026;
    private static final String DIRETORIO_CLIENT = "./arquivos_cliente/";

    public static void main(String[] args) {
        //CRIA PASTA ONDE OS ARQUIVOS FICARÃO, CASO NÃO EXISTA
        File diretorio = new File(DIRETORIO_CLIENT);
        if (!diretorio.exists()) {
            diretorio.mkdirs();
        }
        //RECEBE A/AS OPÇÃO/OPÇÕES ESCOLHIDA PELO CLIENTE
        Scanner scanner = new Scanner(System.in);

        // MENU EM LOOP NO TERMINAL
        while (true) {
            System.out.println("\n ### ESCOLHA UMA OPÇÃO ###");
            System.out.println("1. LISTAR ARQUIVOS;");
            System.out.println("2. ENVIAR ARQUIVOS;");
            System.out.println("3. BAIXAR ARQUIVO;");
            System.out.println("4. SAIR.");
            System.out.println("opção escolhida:");

            int opcao = scanner.nextInt();
            scanner.nextLine();

            if (opcao == 4) {
                System.out.println("ENCERRANDO . . .");
                break;
            }

            //ABRE CONEXÃO SOCKET COM O SERVIDOR A CADA OPERAÇÃO
            try (Socket socket = new Socket(SERVER_IP, DOOR);
            DataInputStream in = new DataInputStream(socket.getInputStream()); //RECEBER DADOS
            DataOutputStream out = new DataOutputStream(socket.getOutputStream())){ // ENVIAR DADOS

                //ACIONA O METODO CORRESPONDENTE A OPÇÃO ESCOLHIDA PELO CLIENTE
                switch (opcao) {
                    case 1:
                        listarArquivos(out, in);
                        break;
                    case 2:
                        System.out.println("digite o nome do arquivo para enviar:");
                        String nSended = scanner.nextLine();
                        enviarArquivos(out, nSended);
                        break;
                    case 3:
                        System.out.println("digite o nome do arquivo que será baixado:");
                        String nDownload = scanner.nextLine();
                        baixarArquivos(out, in, nDownload);
                        break;
                    default:
                        System.out.println("OPÇÃO INVÁLIDA.");
                }
            } catch(IOException e){
                System.out.println("ERRO NA COMUNICAÇÃO" + e.getMessage());
            }
        }
        scanner.close(); //FECHA O SCANNER
    }
    //METODO RESPONSÁVEL POR LISTAR OS ARQUIVOS PRESENTES NO SERVIDOR
    private static void listarArquivos(DataOutputStream out, DataInputStream in) throws IOException{
        out.writeUTF("LIST"); // INDICA QUAL COMANDO ESTÁ SENDO PEDIDO PELO CLIENTE

        int quantidade = in.readInt();
        System.out.println("\n- - ARQUIVOS NO SERVIDOR ["+ quantidade + "] - -");
        for (int i = 0; i < quantidade; i++) {
            System.out.println("- "+ in.readUTF());
        }
    }
    //METODO RESPONSÁVEL POR ENVIAR ARQUIVOS PARA O SERVIDOR
    private static void enviarArquivos(DataOutputStream out, String nDoc) throws IOException {
        File doc = new File(DIRETORIO_CLIENT + nDoc);

        if (!doc.exists() || !doc.isFile()){ //VERIFICA SE O ARQUIVO EXISTE NA PASTA DO CLIENTE
            System.out.println("ARQUIVO NÃO ENCONTRADO.");
            return;
        }
        out.writeUTF("UPLOAD".trim()); // INDICA O COMANDO PEDIDO PELO CLIENTE
        out.writeUTF(doc.getName());
        out.writeLong(doc.length());

        //LÊ O ARQUIVO EM BLOCOS DE BYTE
        try (FileInputStream fin = new FileInputStream(doc)){
            byte [] buffer = new byte[4096];
            int bytesLidos;
            while ((bytesLidos = fin.read(buffer)) != -1){
                out.write(buffer, 0, bytesLidos);
            }
        }
        System.out.println("UPLOAD CONCLUÍDO");
    }
    //METODO RESPONSÁVEL POR RECEBER ARQUIVOS DO SERVIDOR
    private static void baixarArquivos(DataOutputStream out, DataInputStream in, String nDoc)
            throws IOException {
        out.writeUTF("DOWNLOAD"); // INDICA QUAL COMANDO O CLIENTE SOLICITOU
        out.writeUTF(nDoc); // NOME DO ARQUIVO QUE SERÁ BAIXADO

        //VERIFICA SE O ARQUIVO EXISTE
        boolean exist = in.readBoolean();
        if (!exist) {
            System.out.println("O ARQUIVO SOLICITADO NÃO EXISTE.");
            return;
        }
        long dSize = in.readLong();
        File doc = new File(DIRETORIO_CLIENT + nDoc); //SALVA O ARQUIVO NO DISCO LOCAL

        try (FileOutputStream fout = new FileOutputStream(doc)){
            byte[] buffer = new byte[4096];
            int bytesLidos;
            long allRead = 0;  //CONTROLADOR DE BYTES RECEBIDOS

            //CONTINUA RECEBENDO BYTES ATÉ ATINGIR A QUANTIDADE ESPERADA
            while (allRead < dSize && (bytesLidos = in.read(buffer, 0, (int)
                    Math.min(buffer.length, dSize - allRead))) != -1){
                fout.write(buffer, 0, bytesLidos);
                allRead += bytesLidos;
            }
        }
        System.out.println("DOWNLOAD CONCLUÍDO.");
    }
}
