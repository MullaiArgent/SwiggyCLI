import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.IllegalFormatCodePointException;
import java.util.List;
import java.util.Map;

class Server {
    public static void main(String[] args) throws IOException {
        new MyServer(77_77);
    }
}
class MyServer{
    private static final Map<Subject, IncomingClientModel> commercialEndClients = new HashMap<>();
    private static final Map<Subject, IncomingClientModel> deliveryPeople = new HashMap<>();
    private static final Map<Subject, IncomingClientModel> restaurants = new HashMap<>();

    private static final List<String> files = List.of(new String[]{"CommercialEndUsers.txt", "DeliveryPeople.txt", "Restaurants.txt"});

    MyServer(int port) throws IOException {
        deserialize();
        ServerSocket serverSocket = new ServerSocket(port);
        while (true){
            try{
                new IncomingClientModel(serverSocket.accept()).run();
            }catch (InterruptedIOException e){
                break;
            }
        }
    }
    private void populate(ObjectInputStream objectInputStream, Map<Subject, IncomingClientModel> subjects){
        while (true){
            try{
                subjects.put((Subject) objectInputStream.readObject(), null);
            } catch (IOException | ClassNotFoundException e){
                break;
            }
        }
    }
    private static void populate(ObjectOutputStream objectOutputStream, Map<Subject, IncomingClientModel> subjects){
        for (Map.Entry<Subject, IncomingClientModel> subject : subjects.entrySet()){
            try{
                objectOutputStream.writeObject(subject.getKey());
            }catch (IOException ioException){
                System.out.println("Err in deserializing");
            }
        }
    }
    private void deserialize(){
        ObjectInputStream objectInputStream = null;
        FileInputStream fileInputStream = null;
        for(String file : files) {
            System.out.println("Deserializing " + file);
            try {
                fileInputStream = new FileInputStream("Classified\\" + file);
            } catch (FileNotFoundException fileNotFoundException) {
                System.out.println("FileNotFound");
            }

            try {
                objectInputStream = new ObjectInputStream(fileInputStream);
            } catch (EOFException ioException) {
                continue;                                 // EOF is thrown when bottom of the file is reached
            } catch (IOException ioException){
                ioException.printStackTrace();
            }
            assert objectInputStream != null;
            switch (files.indexOf(file)){
                case 0 -> populate(objectInputStream, commercialEndClients);
                case 1 -> populate(objectInputStream, deliveryPeople);
                case 2 -> populate(objectInputStream, restaurants);
            }
        }
    }
    private static void serialize(){
        for(String file : files) {
            System.out.println("Serializing " + file);
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream("Classified\\" + file);
            } catch (FileNotFoundException fileNotFoundException) {
                System.out.println("FileNotFound");
            }
            assert fileOutputStream != null;

            ObjectOutputStream objectOutputStream = null;
            try {
                objectOutputStream = new ObjectOutputStream(fileOutputStream);
            } catch (IOException ioException) {
                System.out.println("Err in ObjStream");
            }
            assert objectOutputStream != null;
            switch (files.indexOf(file)){
                case 0 -> populate(objectOutputStream, commercialEndClients);
                case 1 -> populate(objectOutputStream, deliveryPeople);
                case 2 -> populate(objectOutputStream, restaurants);
            }
        }
    }

    private static class IncomingClientModel implements Runnable{
        private PrintWriter out;
        private BufferedReader in;
        private String userName;
        IncomingClientModel(Socket clientSocket){
            try{
                this.out = new PrintWriter(clientSocket.getOutputStream(), true);
                this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            } catch (IOException exception){
                System.out.println("Err in establishing Sockets");
            }
        }

        @Override
        public void run() {
            try {
                out.println(switch(in.readLine()){
                    case "Code-Login" -> authenticateLogin();
                    case "Code-CreateAccount" -> authenticateNewAccount();
                    default -> "InValid-Entry"; // Will never Occur
                });

                switch (in.readLine()){
                    case "Code-ViewAllFoods" -> out.println(getAllFoodsJson());
                    case "Code-ViewCart" -> out.println(getAllFoodsInCartJson());
                    case "Code-ViewFoodsInRestaurant" -> out.println(getAllFoodInRestaurantJson());
                }
            } catch (IOException e) {
                System.out.println("Err in incoming msgs");
            }
            try {
                this.out.close();
                this.in.close();
                MyServer.serialize();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String authenticateLogin() throws IOException{
            Map<Subject, IncomingClientModel> subjects = switch (in.readLine()){                                         // Dynamic Polymorphism(Lat Binding)
                case "CommercialEndClient" -> commercialEndClients;
                case "DeliveryPerson" -> deliveryPeople;
                case "Restaurant" -> restaurants;
                default -> null;
            };

            assert subjects != null;
            for (Map.Entry<Subject, IncomingClientModel> subject : subjects.entrySet()){
                if (subject.getKey().getUserName().equals(in.readLine())){
                    if (subject.getKey().getPassword().equals(in.readLine())){
                        this.userName = subject.getKey().getUserName();
                        // TODO update the value with .this
                        return "Code-Verified";
                    }else {
                        return "Code-InvalidPassword";
                    }
                }
            }
            return "Code-UserDoesn't Exist";
        }
        String authenticateNewAccount()throws IOException{
            switch (in.readLine()){
                case "CommercialEndClient" -> {
                    userName = in.readLine();
                    if (isUserNotExist(userName)) {
                        commercialEndClients.put(
                                new CommercialEndClient(userName, in.readLine(),
                                        new CoOrdinates(Integer.parseInt(in.readLine()), Integer.parseInt(in.readLine())))
                                , this);
                    }else{
                        // TODO already do exist
                    }
                }
                case "DeliveryPerson" -> {
                    userName = in.readLine();
                    if (isUserNotExist(userName)) {
                        deliveryPeople.put(
                                new DeliveryPerson(userName, in.readLine(),
                                        new CoOrdinates(Integer.parseInt(in.readLine()), Integer.parseInt(in.readLine())))
                                , this);
                    }else{
                        // TODO already do exist
                    }
                }
                case "Restaurant" -> {
                    userName = in.readLine();
                    if (isUserNotExist(userName)) {
                        restaurants.put(
                                new Restaurant(userName, in.readLine(),
                                        new CoOrdinates(Integer.parseInt(in.readLine()), Integer.parseInt(in.readLine())))
                                , this);
                    }else{
                        // TODO already do exist
                    }
                }
            }
            System.out.println("New Account Created");
            return "Code-Verified";
        }
        private boolean isUserNotExist(String userName){
            // TODO look everywhere
            return true;
        }
        private StringBuffer getAllFoodsJson() {
            StringBuffer allFoods = new StringBuffer();
            allFoods.append("{ \"Type\" : \"AllFoods\",{ \"Foods\" : \"[\"");
            for (Map.Entry<Subject, IncomingClientModel> subject : restaurants.entrySet()){
                Restaurant restaurant1 = (Restaurant) subject.getKey();
                for (Food food : restaurant1.getFoods()){
                    allFoods.append("\"").append(food).append("\",");
                }
            }
            if (allFoods.length() > 0) allFoods.deleteCharAt(allFoods.length()-1);        // to remove the redundant comma ',' in the end
            allFoods.append("]}");
            allFoods.trimToSize();
            return allFoods;
        }
        private StringBuffer getAllFoodsInCartJson() {
            StringBuffer allFoodsInCart = new StringBuffer();
            allFoodsInCart.append("{ \"Type\" : \"AllCartFoods\",{ \"Foods\" : \"[\"");
            for(Map.Entry<Subject, IncomingClientModel> subject : commercialEndClients.entrySet()){
                if (subject.getKey().getUserName().equals(userName)){
                    CommercialEndClient commercialEndClient = (CommercialEndClient) subject.getKey();
                    for (Food food : commercialEndClient.getCart()){
                        allFoodsInCart.append("\"").append(food).append("\",");
                    }
                    break;
                }
            }
            if (allFoodsInCart.length() > 0) allFoodsInCart.deleteCharAt(allFoodsInCart.length()-1);
            allFoodsInCart.append("]}");
            allFoodsInCart.trimToSize();
            return allFoodsInCart;
        }
        private StringBuffer getAllFoodInRestaurantJson(){
            StringBuffer allFoodInRestaurantJson = new StringBuffer();
            allFoodInRestaurantJson.append("{ \"Type\" : \"AllCartFoods\",{ \"Foods\" : \"[\"");
            for (Map.Entry<Subject, IncomingClientModel> subject : restaurants.entrySet()){
                if (subject.getKey().getUserName().equals(userName)){
                    Restaurant restaurant = (Restaurant) subject.getKey();
                    for (Food food : restaurant.getFoods()){
                        allFoodInRestaurantJson.append("\"").append(food).append("\"");
                    }
                    break;
                }
            }
            if (allFoodInRestaurantJson.length() > 0) allFoodInRestaurantJson.deleteCharAt(allFoodInRestaurantJson.length()-1);
            allFoodInRestaurantJson.append("]}");
            return allFoodInRestaurantJson;
        }
    }
}

record Food(String name, float prize, String restaurantName) {}
record CoOrdinates(int x, int y){}
