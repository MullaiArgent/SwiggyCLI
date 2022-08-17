import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class Main {
    static {
        System.loadLibrary("clearScreen");
    }
    public native static void clearScreen();

    public static void main(String[] args) throws IOException {
        System.out.print("""
                Enter 1 to Enter as Commercial End User
                Enter 2 to Enter as Delivery Person
                Enter 3 to Enter as Restaurant User :\s""");
        switch (new Scanner(System.in).nextInt()){
            case 1 -> {
                clearScreen();
                new CommercialEndClient();
            }
            case 2 -> {
                clearScreen();
                new DeliveryPerson();
            }
            case 3 -> {
                clearScreen();
                new Restaurant();
            }
        }
    }
}
interface Authenticator {
    Scanner scanner = new Scanner(System.in);
    default String login(String subjectType, PrintWriter out, BufferedReader in) throws IOException {
        scanner.nextLine();
        int attempts = 2;
        String serverResponds;
        String userName;
        out.println("Code-Login");
        out.println(subjectType);
        do {
            System.out.print("Enter your User-Name : ");
            userName = scanner.nextLine();
            out.println(userName);
            System.out.print("Enter the Password : ");
            out.println(scanner.nextLine());
            serverResponds = in.readLine();
            switch (serverResponds){
                case "Code-Verified" -> {
                    Main.clearScreen();
                    return userName;
                }
                case "Code-InvalidPassword" -> {
                    System.out.println("Invalid Password\nAttempts left "+ attempts);
                    out.println("Code-Login");
                }
                case "Code-UserDoesn'tExist" -> {
                    System.out.println("User Id Doesn't Exist");
                    authenticate(subjectType, out, in);
                }
                case "Code-AttemptExpired" -> {
                    System.out.println("Attempts Expired");
                    return null;
                }
            }
            attempts--;
        }while (attempts > 0);
        return null;
    }
    default String createAccount(String subjectType, PrintWriter out, BufferedReader in) throws IOException{
        String userName, password;
        do {
            scanner.nextLine();
            out.println("Code-CreateAccount");
            out.println(subjectType);
            System.out.print("Enter the UserName : ");
            do{
                userName = scanner.nextLine();
                if(!userName.matches("[a-z0-9]+@[a-z]+\\..*")){
                    System.out.print("Invalid E-Mail, Re-Type : ");
                }else {
                    break;
                }
            }while (true);
            out.println(userName);
            do{
                System.out.print("NOTE : Minimum eight characters, at least one uppercase letter, one lowercase letter and one number\n" +
                        "Enter the password : ");
                password = scanner.nextLine();
                if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{8,}$")){
                    System.err.print("Invalid Password Format, Re-type : ");
                }else {
                    break;
                }
            }while (true);
            out.println(password);
            System.out.print("Enter the x Axis : ");
            out.println(scanner.nextInt());
            System.out.print("Enter the y Axis : ");
            out.println(scanner.nextInt());
        }while (!in.readLine().equals("Code-Verified"));
        Main.clearScreen();
        return userName;
    }
    default String authenticate(String subjectType, PrintWriter out, BufferedReader in) throws IOException{
        System.out.print("""
                Enter 1 to login to your Account
                Enter 2 to Create new Account :\s
                """);
        return switch (scanner.nextInt()){
            case 1 -> login(subjectType,out,in);
            case 2 -> createAccount(subjectType, out, in);
            default -> null;
        };
    }
}
sealed abstract class Subject permits CommercialEndClient, DeliveryPerson, Restaurant {
    abstract String getName();
    abstract String getPassword();
    abstract CoOrdinates getCoOrdinates();
}

final class CommercialEndClient extends Subject implements Authenticator {
    private String name, password = null;
    private CoOrdinates coOrdinates;
    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;
    private List<Food> cart;
    CommercialEndClient() throws IOException {
        try{
            this.socket = new Socket("127.0.0.1", 77_77);
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException exception){
            System.out.println("Err in establishing Sockets");
        }
        name = Authenticator.super.authenticate("CommercialEndClient", out, in);
        assert name != null;
        mainMenu();
    }

    private void mainMenu() throws IOException {
        System.out.print("""
                Enter 1 to View All the Foods
                Enter 2 to View your Cart :"\s
                """);
        switch (scanner.nextInt()){
            case 1 -> {
                out.println("Code-ViewAllFoods");
                System.out.println(in.readLine()); // parse json
            }
            case 2 -> {
                out.println("Code-ViewCart");
                System.out.println(in.readLine()); // JSON obj of the result
                // TODO Check out
            }
            default -> {
                System.err.println("Invalid Entry");
                mainMenu();
            }
        }
    }

    public CommercialEndClient(String name, String password, CoOrdinates coOrdinates) {
        this.name = name;
        this.password = password;
        this.coOrdinates = coOrdinates;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public CoOrdinates getCoOrdinates() {
        return coOrdinates;
    }

    public List<Food> getCart(){
        return cart;
    }
}
final class DeliveryPerson extends Subject implements Authenticator{
    private String name, password;
    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;
    private CoOrdinates coOrdinates;
    DeliveryPerson() throws IOException{
        try{
            this.socket = new Socket("127.0.0.1", 77_77);
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException exception){
            System.out.println("Err in establishing Sockets");
        }
        name = Authenticator.super.authenticate("DeliveryPerson", out, in);
    }

    public DeliveryPerson(String name, String password, CoOrdinates coOrdinates) {
        this.name = name;
        this.password = password;
        this.coOrdinates = coOrdinates;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public CoOrdinates getCoOrdinates() {
        return coOrdinates;
    }
}
final class Restaurant extends Subject implements Authenticator{
    private String name, password;
    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;
    private CoOrdinates coOrdinates;
    private List<Food> foods = new LinkedList<>();
    Restaurant() throws IOException{
        try{
            this.socket = new Socket("127.0.0.1", 77_77);
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException exception){
            System.out.println("Err in establishing Sockets");
        }
        name = Authenticator.super.authenticate("Restaurant", out, in);
        assert name != null;
        mainMenu();

    }
    public Restaurant(String name, String password, CoOrdinates coOrdinates) {
        this.name = name;
        this.password = password;
        this.coOrdinates = coOrdinates;
    }
    void mainMenu(){
        System.out.print("""
                Enter 1 to View All Foods in Your Restaurant
                Enter 2 to Add a Food Item in Your Restaurant :\s
                """);
        switch (scanner.nextInt()){
            case 1 -> out.println("Code-ViewFoodsInRestaurant");
        }
    }
    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public CoOrdinates getCoOrdinates() {
        return coOrdinates;
    }

    public List<Food> getFoods() {
        return foods;
    }
}
