import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

class Server {
    public static void main(String[] args) throws IOException {
        new MyServer(77_77);
    }
}
class MyServer{
    private static final List<Subject> commercialEndClients = new LinkedList<>();
    private static final List<Subject> deliveryPeople = new LinkedList<>();
    private static final List<Subject> restaurants = new LinkedList<>();
    private static final List<String> files = List.of(new String[]{"CommercialEndUsers.txt", "DeliveryPeople.txt", "Restaurants.txt"});

    MyServer(int port) throws IOException {
        deserialize();
        ServerSocket serverSocket = new ServerSocket(port);
        while (true){
            try{
                new Incoming(serverSocket.accept()).run();
            }catch (InterruptedIOException e){
                break;
            }
        }
    }
    private void populate(ObjectInputStream objectInputStream, List<Subject> subjects){
        while (true){
            try{
                subjects.add((Subject) objectInputStream.readObject());
            } catch (IOException | ClassNotFoundException e){
                break;
            }
        }
    }
    private static void populate(ObjectOutputStream objectOutputStream, List<Subject> subjects){
        for (Subject subject : subjects){
            try{
                objectOutputStream.writeObject(subject);
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

    private static class Incoming implements Runnable{
        private PrintWriter out;
        private BufferedReader in;
        private String userName;
        Incoming(Socket clientSocket){
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
            List<Subject> subjects = switch (in.readLine()){
                case "CommercialEndClient" -> commercialEndClients;
                case "DeliveryPerson" -> deliveryPeople;
                case "Restaurant" -> restaurants;
                default -> null;
            };

            assert subjects != null;
            for (Subject subject : subjects){
                if (subject.getName().equals(in.readLine())){
                    if (subject.getPassword().equals(in.readLine())){
                        this.userName = subject.getName();
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
                    commercialEndClients.add(
                            new CommercialEndClient(in.readLine(), in.readLine(),
                                    new CoOrdinates(Integer.parseInt(in.readLine()), Integer.parseInt(in.readLine())))
                    );
                    this.userName = commercialEndClients.get(commercialEndClients.size() - 1).getName();
                }
                case "DeliveryPerson" -> {
                    deliveryPeople.add(
                            new DeliveryPerson(in.readLine(), in.readLine(),
                                    new CoOrdinates(Integer.parseInt(in.readLine()), Integer.parseInt(in.readLine())))
                    );
                    this.userName = deliveryPeople.get(deliveryPeople.size()-1).getName();
                }
                case "Restaurant" -> {
                    restaurants.add(
                            new Restaurant(in.readLine(), in.readLine(),
                                    new CoOrdinates(Integer.parseInt(in.readLine()), Integer.parseInt(in.readLine())))
                    );
                    this.userName = restaurants.get(restaurants.size()-1).getName();
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
            for (Subject restaurant :  restaurants){
                Restaurant restaurant1 = (Restaurant) restaurant;
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
            for(Subject subject : commercialEndClients){
                if (subject.getName().equals(userName)){
                    CommercialEndClient commercialEndClient = (CommercialEndClient) subject;
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
            for (Subject subject : restaurants){
                if (subject.getName().equals(userName)){
                    Restaurant restaurant = (Restaurant) subject;
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
