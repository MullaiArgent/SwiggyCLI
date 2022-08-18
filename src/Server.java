import org.json.simple.DeserializationException;
import org.json.simple.JsonObject;
import org.json.simple.Jsoner;

import javax.imageio.event.IIOWriteProgressListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class Server {
    public static void main(String[] args) throws IOException {
        new MyServer(77_77);
    }
}
class MyServer{
    private static final Map<Subject, IncomingClientModel> commercialEndClients = new LinkedHashMap<>();
    private static final Map<Subject, IncomingClientModel> deliveryPeople = new LinkedHashMap<>();
    private static final Map<Subject, IncomingClientModel> restaurants = new LinkedHashMap<>();

    private static final List<String> files = List.of(new String[]{"CommercialEndUsers.txt", "DeliveryPeople.txt", "Restaurants.txt"});

    MyServer(int port) throws IOException {
        deserializeAll();
        ServerSocket serverSocket = new ServerSocket(port);
        while (true){
            try{
                new IncomingClientModel(serverSocket.accept()).run();
            }catch (InterruptedIOException e){
                break;
            }
        }
        serializeAll();

    }
    private void populate(ObjectInputStream objectInputStream, FileInputStream fileInputStream,  Map<Subject, IncomingClientModel> subjects) throws IOException {
        while (fileInputStream.available() != 0){
            try{
                subjects.put((Subject) objectInputStream.readObject(), null);
                fileInputStream.skip(4L);
            } catch (IOException | ClassNotFoundException e){
                break;
            }
        }
        for (Map.Entry<Subject, IncomingClientModel> s : subjects.entrySet()){
            System.out.println(s.getKey().getUserName());
        }

    }
    @Deprecated(forRemoval = true)
    private static void populate(ObjectOutputStream objectOutputStream, Map<Subject, IncomingClientModel> subjects){
        for (Map.Entry<Subject, IncomingClientModel> subject : subjects.entrySet()){
            try{
                objectOutputStream.writeObject(subject.getKey());
            }catch (IOException ioException){
                System.out.println("Err in deserializing");
            }
        }
    }
    private void deserializeAll() throws IOException {
        ObjectInputStream objectInputStream = null;
        FileInputStream fileInputStream = null;
        for(String file : files) {
            System.out.println("Deserializing " + file);
            try {
                fileInputStream = new FileInputStream("C:\\Users\\mulla\\OneDrive\\Documents\\GitHub\\SwiggyCLI\\src\\Classified\\_" + file);
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
            switch (files.indexOf(file)){
                case 0 -> populate(objectInputStream, fileInputStream, commercialEndClients);
                case 1 -> populate(objectInputStream, fileInputStream,deliveryPeople);
                case 2 -> populate(objectInputStream, fileInputStream,restaurants);
            }
        }
    }
    @Deprecated(forRemoval = true)
    private static void serializeAll(){
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

    private static void serialize(Subject subject, String subjectType) {
        FileOutputStream fileOutputStream = null;
        ObjectOutputStream objectOutputStream = null;
        try{
            fileOutputStream = new FileOutputStream("C:\\Users\\mulla\\OneDrive\\Documents\\GitHub\\SwiggyCLI\\src\\Classified\\_ "+ subjectType +".txt");
        }catch (IOException e){
            System.out.println("Err in opening file");
            e.printStackTrace();
        }
        try{
            objectOutputStream = new ObjectOutputStream(fileOutputStream);
        }catch (IOException e){
            System.out.println("Err in opening obj stream");
            e.printStackTrace();
        }

        try{
            objectOutputStream.writeObject(subject);
            System.out.println(subject.getUserName() + "'s data Serialized");
        }catch (IOException e){
            System.out.println("Err in writing the obj");
            e.printStackTrace();
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
            String authenticationResponds;
            try {
                while (true) {
                    switch (in.readLine()) {
                        case "Code-Login" -> {
                            authenticationResponds = authenticateLogin();
                            out.println(authenticationResponds);
                            if (!authenticationResponds.equals("Code-Verified")){
                                continue;
                            }
                        }
                        case "Code-CreateAccount" -> {
                            authenticationResponds = authenticateNewAccount();
                            out.println(authenticationResponds);
                            if (!authenticationResponds.equals("Code-Verified")){
                                continue;
                            }
                        }
                    }
                    break;
                }

                switch (in.readLine()){
                    case "Code-ViewAllFoods" -> out.println(getAllFoodsJson());
                    case "Code-ViewCart" -> out.println(getAllFoodsInCartJson());
                    case "Code-ViewFoodsInRestaurant" -> out.println(getAllFoodInRestaurantJson());
                    case "Code-AddFood" -> {
                        updateNewFood(in.readLine());
                        out.print("Updated");
                    }
                }
            } catch (IOException | DeserializationException e) {
                System.out.println("Err in incoming msgs");
            }
            try {
                System.out.println("Elapsed");
                this.out.close();
                this.in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String authenticateLogin() throws IOException{
            Map<Subject, IncomingClientModel> subjects = switch (in.readLine()){                                         // Dynamic Polymorphism(Late Binding)
                case "CommercialEndClient" -> commercialEndClients;
                case "DeliveryPerson" -> deliveryPeople;
                case "Restaurant" -> restaurants;
                default -> null;
            };

            for (Map.Entry<Subject, IncomingClientModel> subject : subjects.entrySet()){
                if (subject.getKey().getUserName().equals(in.readLine())){
                    if (subject.getKey().getPassword().equals(in.readLine())){
                        this.userName = subject.getKey().getUserName();
                        subjects.put(subject.getKey(), this);                           // to update the 'null' data populated while deserializing.
                        // TODO Cross check for the vulnerability
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
                        CommercialEndClient commercialEndClient = new CommercialEndClient(userName, in.readLine(),
                                new CoOrdinates(Integer.parseInt(in.readLine()), Integer.parseInt(in.readLine())));
                        commercialEndClients.put(commercialEndClient, this);
                        MyServer.serialize(commercialEndClient, "CommercialEndUsers");
                    }else{
                        // TODO already do exist
                    }
                }
                case "DeliveryPerson" -> {
                    userName = in.readLine();
                    if (isUserNotExist(userName)) {
                        DeliveryPerson deliveryPerson = new DeliveryPerson(userName, in.readLine(),
                                new CoOrdinates(Integer.parseInt(in.readLine()), Integer.parseInt(in.readLine())));
                        deliveryPeople.put( deliveryPerson, this);
                        MyServer.serialize(deliveryPerson, "DeliveryPeople");
                    }else{
                        // TODO already do exist
                    }
                }
                case "Restaurant" -> {
                    userName = in.readLine();
                    if (isUserNotExist(userName)) {
                        Restaurant restaurant = new Restaurant(userName, in.readLine(),
                                new CoOrdinates(Integer.parseInt(in.readLine()), Integer.parseInt(in.readLine())));
                        restaurants.put(restaurant, this);
                        MyServer.serialize(restaurant,"Restaurants");
                    }else{
                        // TODO already do exist
                    }
                }
            }
            System.out.println("New Account Created : " + userName);
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
        private void updateNewFood(String foodJson) throws DeserializationException {
            JsonObject json = (JsonObject) Jsoner.deserialize(foodJson);
            System.out.println(json.get("name"));
            System.out.println(json.get("prize"));
            System.out.println(json);
            
        }
    }
}

record Food(String name, float prize) implements Serializable{}
record CoOrdinates(int x, int y) implements Serializable{}
