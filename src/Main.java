import org.json.simple.DeserializationException;
import org.json.simple.JsonArray;
import org.json.simple.JsonObject;
import org.json.simple.Jsoner;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;


public class Main {
    static {
        System.loadLibrary("clearScreen");
    }
    public native static void clearScreen();

    public static void main(String[] args) throws IOException, DeserializationException, InterruptedException {
        System.out.print("""
                Enter 1 to Enter as Commercial End User
                Enter 2 to Enter as Delivery Person
                Enter 3 to Enter as Restaurant User :
                """);
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
        do {
            out.println("Code-Login");
            out.println(subjectType);
            System.out.print("Enter your E-Name : ");
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
                case "Code-InvalidPassword" -> System.out.println("Invalid Password\nAttempts left "+ attempts);
                case "Code-UserDoesn'tExist" -> {
                    System.out.println("User Id Doesn't Exist, Retry");
                    continue;
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
            System.out.print("Enter the E-Name : ");
            do{
                userName = scanner.nextLine();
                if(!userName.matches("[a-z0-9]+@[a-z]+\\..*")){
                    System.err.print("Invalid E-Mail, Re-Type : ");                          // TODO if the acc mail is already exist.
                }else {
                    break;
                }
            }while (true);
            out.println(userName);
            System.out.print("NOTE : Minimum eight characters, at least one uppercase letter, one lowercase letter and one number\n" +
                    "Enter the password : ");
            do{
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
        Main.clearScreen();
        System.out.print("""
                Enter 1 to login to your Account
                Enter 2 to Create new Account :\s
                """);
        return switch (scanner.nextInt()){
            case 1 -> login(subjectType,out,in);
            case 2 -> createAccount(subjectType, out, in);
            default -> authenticate(subjectType, out, in);
        };
    }
}

sealed abstract class Subject implements Serializable permits CommercialEndClient, DeliveryPerson, Restaurant{
    abstract String getUserName();
    abstract String getPassword();
    abstract CoOrdinates getCoOrdinates();
}

final class CommercialEndClient extends Subject implements Authenticator {
    private String userName, password = null;
    private CoOrdinates coOrdinates;
    private PrintWriter out;
    private BufferedReader in;
    private final List<Food> cart = new ArrayList<>();
    CommercialEndClient() throws IOException, DeserializationException, InterruptedException {
        try{
            Socket socket = new Socket("127.0.0.1", 77_77);
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException exception){
            System.out.println("Err in establishing Sockets");
        }
        userName = Authenticator.super.authenticate("CommercialEndClient", out, in);
        mainMenu();
    }

    private void mainMenu() throws IOException, DeserializationException, InterruptedException {
        JsonObject temp;
        mainMenu:
        while (true) {
            System.out.print("""
                Enter 1 to View All the Foods
                Enter 2 to View your Cart
                Enter 3 or any number to Exit :
                """);
            int option = scanner.nextInt();
            scanner.nextLine();
            switch (option) {
                case 1 -> {
                    out.println("Code-ViewAllFoods");
                    JsonObject jsonObject = (JsonObject) Jsoner.deserialize(in.readLine()); // parse json
                    JsonArray jsonFoodArray = (JsonArray) jsonObject.get("Foods");
                    for (int i = 0 ;jsonFoodArray.size() > i; i++){
                        temp = (JsonObject) jsonFoodArray.get(i);
                        System.out.println(
                                "ID"+i+"\n" +
                                ""+temp.get("foodName") +"\n" +
                                "Prize : " + temp.get("foodPrize")+"\n"+
                                "restaurantName : " + temp.get("restaurantName") + "\n");
                    }
                    System.out.print("""
                            Enter Food ID to add to Cart
                            Enter CART to view your CART
                            Enter EXIT to get back to Main Menu :
                            """);
                    String option1 = scanner.nextLine();
                    if (option1.equals("CART")){
                        viewCart();
                    }else if(option1.equals("EXIT")){
                        mainMenu();
                        break mainMenu;
                    }else{
                        if (option1.matches("(ID)\\d+")) {
                            out.println("Code-AddFoodToCart");
                            JsonObject jsonFood = (JsonObject) jsonFoodArray.get(Integer.parseInt(option1.substring(2)));
                            out.println(jsonFood.toJson());
                        }else {
                            System.err.println("INVALID Entry");
                        }
                    }
                }
                case 2 -> viewCart();
                default -> {
                    break mainMenu;
                }
            }
        }
    }
    private void viewCart() throws IOException, DeserializationException {
        out.println("Code-ViewCart");
        JsonObject jsonCart = (JsonObject) Jsoner.deserialize(in.readLine());
        JsonArray jsonCartFoods = (JsonArray) jsonCart.get("Foods");
        JsonObject food;
        for (int i = 0 ;jsonCartFoods.size() > i; i++){
            food = (JsonObject) jsonCartFoods.get(i);
            System.out.println(
                    "ID"+i+"\n" +
                    food.get("foodName") + "\n" +
                    "Prize : " + food.get("foodPrize") + "\n"+
                    "RestaurantName : " + food.get("restaurantName") + "\n");
        }
        System.out.print("""
                    Enter ID number to REMOVE from the CART
                    Enter BOOK to Place the Order
                    Enter EXIT  to get back to MainMenu :
                    """);
        String option =  scanner.nextLine();
        if (option.equals("BOOK")) {
            out.println("Code-CheckOutTheCart");
            JsonObject jsonResponds = (JsonObject) Jsoner.deserialize(in.readLine());
            System.out.println(jsonResponds.get("Code-Type"));
            if (jsonResponds.get("Code-Type").equals("InValid")) {
                JsonArray jsonFoodArray = (JsonArray) jsonResponds.get("Foods");
                for (int i = 0; i < jsonFoodArray.size(); i++) {
                    JsonObject jsonFood = (JsonObject) jsonFoodArray.get(i);
                    System.out.println(
                        jsonFood.get("foodName") + "\n" +
                        "Prize : " + jsonFood.get("foodPrize") + "\n" +
                        "RestaurantName : " + jsonFood.get("restaurantName") + "\n");
                }
                System.out.println("The Above Food cannot be Delivered at this Moment, Try after removing from the Cart");
            }else if(jsonResponds.get("Code-Type").equals("Valid")){
                // Order placed
            }
        }else if(option.matches("(ID)\\d+")){
            out.println("Code-RemoveFoodFromCart");
            JsonObject jsonFood = (JsonObject) jsonCartFoods.get(Integer.parseInt(option.substring(2)));
            out.println(jsonFood.toJson());
        }else if(option.equals("EXIT")){
            return;                            // unnecessary
        }else{
            System.err.println("Invalid Entry");
            viewCart();
        }
    }
    public CommercialEndClient(String userName, String password, CoOrdinates coOrdinates) {
        this.userName = userName;
        this.password = password;
        this.coOrdinates = coOrdinates;
    }

    public void addToCart(JsonObject foodJson){
        cart.add(new Food((String) foodJson.get("foodName"),
                Float.parseFloat((String) foodJson.get("foodPrize")),
                (String)foodJson.get("restaurantName")));
    }
    public void removeFromCart(JsonObject foodJson){
        cart.remove(new Food((String) foodJson.get("foodName"),
                Float.parseFloat((String)foodJson.get("foodPrize")),
                (String) foodJson.get("restaurantName")));
    }

    @Override
    public String getUserName() {
        return userName;
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
    private final String userName;
    private String password;
    private PrintWriter out;
    private BufferedReader in;
    private CoOrdinates coOrdinates;
    private boolean isAvailable = true;
    DeliveryPerson() throws IOException{
        try{
            Socket socket = new Socket("127.0.0.1", 77_77);
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException exception){
            System.out.println("Err in establishing Sockets");
        }
        userName = Authenticator.super.authenticate("DeliveryPerson", out, in);
        mainMenu();
    }

    public DeliveryPerson(String userName, String password, CoOrdinates coOrdinates) {
        this.userName = userName;
        this.password = password;
        this.coOrdinates = coOrdinates;
    }
    private void mainMenu() throws IOException {
        while (true){
            if (in.readLine().equals("Code-TakeDelivery")){
                System.out.println("testing`");
            }
        }
    }

    @Override
    public String getUserName() {
        return userName;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public CoOrdinates getCoOrdinates() {
        return coOrdinates;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }
}
final class Restaurant extends Subject implements Authenticator{
    private final String userName;
    private String password;
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
        userName = Authenticator.super.authenticate("Restaurant", out, in);
        mainMenu();
        System.out.println("Thank You");
    }

    public Restaurant(String userName) {
        this.userName = userName;
    }

    public Restaurant(String name, String password, CoOrdinates coOrdinates) {
        this.userName = name;
        this.password = password;
        this.coOrdinates = coOrdinates;
    }
    void mainMenu() throws IOException {
        System.out.print("""
                Enter 1 to View All Foods in Your Restaurant
                Enter 2 to Add a Food Item in Your Restaurant
                Enter 3 or any number to EXIT :
                """);
        switch (scanner.nextInt()){
            case 1 -> {
                Main.clearScreen();
                out.println("Code-ViewFoodsInRestaurant");
                System.out.println(in.readLine());
                mainMenu();
            }
            case 2 -> {
                Main.clearScreen();
                addFoodJson();
            }
            default -> {
                Main.clearScreen();
                out.println("Code-EXIT");
                return;                // UnNecessary
            }
        }
    }

    private void addFoodJson() throws IOException {
        out.println("Code-AddAFoodInRestaurant");
        String foodName, foodPrize;
        scanner.nextLine();
        System.out.print("Enter Food Name : ");
        foodName = scanner.nextLine();
        System.out.print("Enter Food Prize : ");
        foodPrize = scanner.nextLine();
        StringBuffer foodJson = new StringBuffer();
        foodJson.append("{ \"foodName\" : \"")
                .append(foodName)
                .append("\", \"foodPrize\" : \"")
                .append(foodPrize)
                .append("\"}");
        foodJson.trimToSize();
        out.println(foodJson);
        mainMenu();
    }

    public void addFood(String foodName, float foodPrize){
        foods.add(new Food(foodName, foodPrize, userName));
    }

    @Override
    public String getUserName() {
        return userName;
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

    @Override
    public String toString() {
        return this.userName;
    }

    @Override
    public boolean equals(Object obj) {
        Restaurant restaurant = (Restaurant) obj;
        return this.userName.equals(restaurant.userName);
    }
}
