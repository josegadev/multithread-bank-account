package Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;

public class Receiver implements Runnable {
    private ServerSocket serverSocket;
    private BankAccount sharedAccount;
    private ExecutorService executorService;
    private String name;
    private Logger logger;
    private Sting event;

    public Receiver(String name, ServerSocket serverSocket, BankAccount sharedAccount, ExecutorService executorService) {
        this.name = name;
        this.serverSocket = serverSocket;
        this.sharedAccount = sharedAccount;
        this.executorService = executorService;
    }

    public synchronized String getName() {
        return this.name;
    }

    public synchronized String getTime() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        return dtf.format(now);
    }

    @Override
    public void run() {
        System.out.println(getTime() + ": " + getName() + " listening!");
        Socket firstSocket = null;
        try {
            firstSocket = serverSocket.accept();
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (true) {
            try {
                DataInputStream dis = new DataInputStream(firstSocket.getInputStream());
                DataOutputStream dos = new DataOutputStream(firstSocket.getOutputStream());
                String atm = dis.readUTF();
                String operation = dis.readUTF();
                double ammount = dis.readDouble();


                ClientRequest request = new ClientRequest(
                        sharedAccount,
                        atm,
                        operation,
                        ammount,
                        getTime()
                );

                // Go to the queue's executor
                Future<String> taskResult = executorService.submit(request);

                // Timeout default message case
                String result =  "---- TIME OUT ----\n" + "Client: " + atm + "\nInitial balance: $" + String.format("%.2f", sharedAccount.getBalance()) + "\n" +
                        "Arrive time: " + getTime() + "\nOperation: $" + String.format("%.2f", ammount) + " NOT deposited. \n" +
                        "Final balance: $" + String.format("%.2f", sharedAccount.getBalance()) + "\n";

                try {
                    result = taskResult.get(10, TimeUnit.SECONDS);
                    System.out.println(result);
                } catch (TimeoutException | InterruptedException | ExecutionException e) {
                    System.out.println(getTime() + ": " + "Time out reached for " + atm + " request!");
                } finally {
                    dos.writeUTF(result);
                }

            } catch (IOException ex) {
                System.out.println(getTime() + ": " + "ATM connection rejected on port: " + serverSocket.getLocalPort());
            }
        }
    }
}
