import org.json.simple.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

class Server {
    public static void main(String[] args) throws IOException {
        new MyServer(77_77);
    }
}
class MyServer{
    private static final Map<Subject, IncomingClientModel> commercialEndClients = new LinkedHashMap<>();
    private static final Map<Subject, IncomingClientModel> deliveryPeople = new LinkedHashMap<>();
    private static final Map<Subject, IncomingClientModel> restaurants = new LinkedHashMap<>();

    private static final List<String> files = List.of(new String[]{"_CommercialEndUsers.txt", "_DeliveryPeople.txt", "_Restaurants.txt"});

    MyServer(int port) throws IOException{
        AtomicBoolean flag = new AtomicBoolean(true);
        deserializeAll();
        ServerSocket serverSocket = new ServerSocket(port);
        new Thread(() ->  {
            String code;
            do{
                code = new Scanner(System.in).nextLine();
            }while (!code.equals("Code-Red"));
            serializeAll();
            flag.set(false);
        }).start();

        while (flag.get()){
            try{
                System.out.println("flag state inside " + flag.get());
                new Thread(new IncomingClientModel(serverSocket.accept())).start();
            }catch (InterruptedIOException e){
                break;
            }
        }
    }
    private void populate(ObjectInputStream objectInputStream, FileInputStream fileInputStream,  Map<Subject, IncomingClientModel> subjects) throws IOException {
        while (fileInputStream.available() != 0){
            try{
                subjects.put((Subject) objectInputStream.readObject(), null);
                //fileInputStream.skip(4L);
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
                fileInputStream = new FileInputStream("C:\\Users\\mulla\\OneDrive\\Documents\\GitHub\\SwiggyCLI\\src\\Classified\\" + file);
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
                case 1 -> populate(objectInputStream, fileInputStream, deliveryPeople);
                case 2 -> populate(objectInputStream, fileInputStream, restaurants);
            }
        }
    }
    @Deprecated(forRemoval = true)
    private synchronized static void serializeAll(){
        for(String file : files) {
            System.out.println("Serializing " + file);
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream("C:\\Users\\mulla\\OneDrive\\Documents\\GitHub\\SwiggyCLI\\src\\Classified\\" + file);
            } catch (FileNotFoundException fileNotFoundException) {
                System.out.println("FileNotFound");
            }

            ObjectOutputStream objectOutputStream = null;
            try {
                objectOutputStream = new ObjectOutputStream(fileOutputStream);
            } catch (IOException ioException) {
                System.out.println("Err in ObjStream");
            }
            switch (files.indexOf(file)){
                case 0 -> populate(objectOutputStream, commercialEndClients);
                case 1 -> populate(objectOutputStream, deliveryPeople);
                case 2 -> populate(objectOutputStream, restaurants);
            }
        }
    }
    @Deprecated(forRemoval = true)
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
        private Subject subject;
        private CommercialEndClient commercialEndClient;
        private DeliveryPerson deliveryPerson;
        private Restaurant restaurant;

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
                    System.out.println("Out of Auth");
                    if (subject instanceof CommercialEndClient){
                        commercialEndClient = (CommercialEndClient) subject;
                    }else if(subject instanceof DeliveryPerson){
                        deliveryPerson = (DeliveryPerson) subject;
                    }else if (subject instanceof Restaurant){
                        restaurant = (Restaurant) subject;
                    }
                    break;
                }
                codeListener:
                while (true){
                    System.out.println("awaiting code from the client");
                    String a = in.readLine();
                    switch (a){
                        case "Code-ViewAllFoods" -> out.println(getAllFoodsJson());
                        case "Code-ViewCart" -> out.println(getAllFoodsInCartJson());
                        case "Code-ViewFoodsInRestaurant" -> out.println(getAllFoodsInRestaurantJson());
                        case "Code-AddAFoodInRestaurant" -> updateNewFood(in.readLine());
                        case "Code-AddFoodToCart" -> commercialEndClient.addToCart((JsonObject) Jsoner.deserialize(in.readLine()));
                        case "Code-RemoveFoodFromCart" -> commercialEndClient.removeFromCart((JsonObject) Jsoner.deserialize(in.readLine()));
                        case "Code-CheckOutTheCart" -> validateOrder();
                        case "Code-ServerPing" -> System.err.println("Client End ping");
                        case "Code-EXIT" -> {
                            break codeListener;
                        }
                        default -> System.out.println(a); // for testing purpose
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
        private String authenticateLogin() throws IOException{
            Map<Subject, IncomingClientModel> subjects = switch (in.readLine()){                                         // Dynamic Polymorphism(Late Binding)
                case "CommercialEndClient" -> commercialEndClients;
                case "DeliveryPerson" -> deliveryPeople;
                case "Restaurant" -> restaurants;
                default -> null;
            };
            String userName = in.readLine();
            String password = in.readLine();
            for (Map.Entry<Subject, IncomingClientModel> subject : subjects.entrySet()){
                System.out.println("testing  : " + subject.getKey().getUserName());
                System.out.println("testing  : " + subject.getKey().getPassword());
                if (subject.getKey().getUserName().equals(userName)){
                    if (subject.getKey().getPassword().equals(password)){
                        this.userName = subject.getKey().getUserName();
                        this.subject = subject.getKey();                                // To reduce multiple iterations
                        subjects.put(subject.getKey(), this);                           // to update the 'null' data populated while deserializing.
                        return "Code-Verified";
                    }else {
                        return "Code-InvalidPassword";
                    }
                }
            }
            in.readLine();
            return "Code-UserDoesn'tExist";
        }
        private String authenticateNewAccount()throws IOException{
            switch (in.readLine()){
                case "CommercialEndClient" -> {
                    userName = in.readLine();
                    if (isUserNotExist(userName)) {
                        CommercialEndClient commercialEndClient = new CommercialEndClient(userName, in.readLine(),
                                new CoOrdinates(Integer.parseInt(in.readLine()),
                                                Integer.parseInt(in.readLine())));
                        commercialEndClients.put(commercialEndClient, this);
                        subject = commercialEndClient;
                        //MyServer.serialize(commercialEndClient, "CommercialEndUsers");
                    }else{
                        // TODO already do exist
                    }
                }
                case "DeliveryPerson" -> {
                    userName = in.readLine();
                    if (isUserNotExist(userName)) {
                        DeliveryPerson deliveryPerson = new DeliveryPerson(userName, in.readLine(),
                                new CoOrdinates(Integer.parseInt(in.readLine()),
                                                Integer.parseInt(in.readLine())));
                        deliveryPeople.put(deliveryPerson, this);
                        subject = deliveryPerson;
                        //MyServer.serialize(deliveryPerson, "DeliveryPeople");
                    }else{
                        // TODO already do exist
                    }
                }
                case "Restaurant" -> {
                    userName = in.readLine();
                    if (isUserNotExist(userName)) {
                        Restaurant restaurant = new Restaurant(userName, in.readLine(),
                                new CoOrdinates(Integer.parseInt(in.readLine()),
                                                Integer.parseInt(in.readLine())));
                        restaurants.put(restaurant, this);
                        subject = restaurant;
                        //MyServer.serialize(restaurant,"Restaurants");
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
        private String getAllFoodsJson() {
            JsonObject allFoods = new JsonObject();
            allFoods.put("Type", "AllFoods");
            JsonArray foods = new JsonArray();
            for (Map.Entry<Subject, IncomingClientModel> subject : restaurants.entrySet()){
                Restaurant restaurant1 = (Restaurant) subject.getKey();
                foods.addAll(restaurant1.getFoods());
            }
            allFoods.put("Foods", foods);
            return allFoods.toJson();
        }
        private String getAllFoodsInCartJson() {
            JsonObject allFoodsInCart = new JsonObject();
            allFoodsInCart.put("Type", "AllCartFoods");
            JsonArray jsonFoods = new JsonArray();
            jsonFoods.addAll(commercialEndClient.getCart());
            allFoodsInCart.put("Foods", jsonFoods);
            System.out.println(allFoodsInCart + "testing");
            return allFoodsInCart.toJson();
        }
        private StringBuffer getAllFoodsInRestaurantJson(){
            StringBuffer allFoodsInRestaurantJson = new StringBuffer();
            allFoodsInRestaurantJson.append("{ \"Type\" : \"AllFoodsInRestaurant\",\"Foods\" : [");
            for (Food food : restaurant.getFoods()){
                allFoodsInRestaurantJson.append(food).append(",");
            }
            if (allFoodsInRestaurantJson.length() > 0) allFoodsInRestaurantJson.deleteCharAt(allFoodsInRestaurantJson.length()-1);
            allFoodsInRestaurantJson.append("]}");
            return allFoodsInRestaurantJson;
        }
        private void validateOrder() throws IOException {
            JsonObject jsonResponds = new JsonObject();
            JsonArray jsonArray = new JsonArray();
            for (Food food : commercialEndClient.getCart()) {
                for (Map.Entry<Subject, IncomingClientModel> modelEntry : restaurants.entrySet()) {
                    if (modelEntry.getKey().getUserName().equals(food.restaurantName())) {
                        try {
                            modelEntry.getValue().out.print(""); // pinging the server to get the online status data
                        } catch (NullPointerException e) {
                            jsonResponds.putIfAbsent("Code-Type", "InValid");
                            jsonArray.add(food.toJson());
                        }
                    }
                }
            }
            if (jsonResponds.containsKey("Code-Type")){
                jsonResponds.put("Foods", jsonArray);
                out.println(jsonResponds.toJson());
            }else{
                jsonResponds.putIfAbsent("Code-Type", "Valid");
                out.println(jsonResponds.toJson());
                placeOrder();
            }
        }
        private void placeOrder() throws IOException {
            List<Map.Entry<Subject, IncomingClientModel>> availableDeliveryPeople = deliveryPeople
                    .entrySet()
                    .stream()
                    .filter(e -> {
                DeliveryPerson deliveryPerson = (DeliveryPerson) e.getKey();
                return e.getValue() != null && deliveryPerson.isAvailable();
            }).toList();

            double shortestDistance = 0, currentDistance = 0;
            DeliveryPerson nearestDeliveryPerson = null;
            Food firstPickUp = null;
            for (Map.Entry<Subject, IncomingClientModel> entry : availableDeliveryPeople){
                DeliveryPerson deliveryPerson1 = (DeliveryPerson) entry.getKey();
                for (Food food : commercialEndClient.getCart()){
                    for (Map.Entry<Subject, IncomingClientModel> res : restaurants.entrySet()){
                        if (food.restaurantName().equals(res.getKey().getUserName())){
                            Restaurant restaurant1 = (Restaurant) res.getKey();
                            currentDistance = restaurant1.getCoOrdinates().distance(deliveryPerson1.getCoOrdinates());
                            if (currentDistance < shortestDistance){
                                shortestDistance = currentDistance;
                                nearestDeliveryPerson = deliveryPerson1;
                                firstPickUp = food;
                            }
                        }
                    }
                }
            }
            commercialEndClient.getCart().remove(firstPickUp);
            commercialEndClient.getCart().add(0, firstPickUp);
            Food temp;
            CoOrdinates currentCoOrdinate;
            for (int i = 0; i < commercialEndClient.getCart().size() - 1; i++) {
                currentCoOrdinate = getCoOrdinate(commercialEndClient.getCart().get(i));
                for (int j = i + 1; j < commercialEndClient.getCart().size(); j++) {
                    if (currentCoOrdinate.distance(getCoOrdinate(commercialEndClient.getCart().get(i+1))) > currentCoOrdinate.distance(getCoOrdinate(commercialEndClient.getCart().get(j)))){
                        temp = commercialEndClient.getCart().get(j);
                        commercialEndClient.getCart().set(j, commercialEndClient.getCart().get(i+1));
                        commercialEndClient.getCart().set(i+1, temp);
                    }
                }
            }
            System.out.println(commercialEndClient.getCart());
            deliveryPeople.get(nearestDeliveryPerson).out.println("Code-TakeDelivery");
            // TODO calc time
            in.readLine();
        }
        private void updateNewFood(String foodJsonString) throws DeserializationException {
            JsonObject foodJson = (JsonObject) Jsoner.deserialize(foodJsonString);
            restaurant.addFood(foodJson.get("foodName").toString(),Float.parseFloat(foodJson.get("foodPrize").toString()));
            System.out.println(foodJson);
        }

        private CoOrdinates getCoOrdinate(Food food){
            // TODO Food is null
            for (Map.Entry<Subject, IncomingClientModel> res : restaurants.entrySet()) {
                if (food.restaurantName().equals(res.getKey().getUserName())) {
                    return res.getKey().getCoOrdinates();
                }
            }
            System.out.println("NUll Data");
            return null;
        }
    }
}

record Food(String foodName, float foodPrize, String restaurantName) implements Serializable, Jsonable {

    @Override
    public String toString(){
        JsonObject jsonFood = new JsonObject();
        jsonFood.put("foodName", foodName);
        jsonFood.put("foodPrize", String.valueOf(foodPrize));
        jsonFood.put("restaurantName", restaurantName);
        return jsonFood.toJson();
    }

    @Override
    public String toJson() {
        return toString();
    }

    @Override
    public void toJson(Writer writer) throws IOException {

    }
    @Override
    public boolean equals(Object obj) {
        Food food  = (Food) obj;
        return this.foodName.equals(food.foodName) &&
                this.foodPrize == food.foodPrize &&
                this.restaurantName.equals(food.restaurantName);
    }
}
record CoOrdinates(int x, int y) implements Serializable{
    double distance(CoOrdinates coOrdinates){
        return Math.sqrt((Math.pow((coOrdinates.x - this.x),2) + Math.pow((coOrdinates.y - this.y),2)));
    }
}
