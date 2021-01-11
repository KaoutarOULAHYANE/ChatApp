import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.io.*;

public class JMSChat extends Application {

    private MessageProducer producer;
    private Session session;

    public static void main(String[] args) {
        Application.launch(JMSChat.class);
    }

    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("JMS Chat App");
        /*----------------------------------------------*/
        final HBox hBox = new HBox(10);
        hBox.setPadding(new Insets(10));
        hBox.setBackground(
                new Background(
                        new BackgroundFill(Color.LIGHTBLUE, CornerRadii.EMPTY, Insets.EMPTY)
                )
        );

        Label codeLabel = new Label("Code : ");
        Label hostLabel = new Label("Host : ");
        Label portLabel = new Label("Port : ");
        final TextField codeTF = new TextField("C1");
        /*Hint*/
        /*codeTF.setPromptText("Code");*/
        final TextField hostTF = new TextField("localhost");
        final TextField portTF = new TextField("61616");
        Button connectButton = new Button("Connect");
        hBox.getChildren().addAll(codeLabel, codeTF, hostLabel, hostTF, portLabel, portTF, connectButton);
        /*----------------------------------------------*/
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(10));
        gridPane.setAlignment(Pos.CENTER);

        Label toLabel = new Label("To");
        Label messageLabel = new Label("Message");
        Label imageLabel = new Label("Image");
        final TextField toTF = new TextField();
        final TextArea messageTA = new TextArea();
        messageTA.setPrefRowCount(2);

        File imageFile = new File("images");
        ObservableList<String> imagesObservable
                = FXCollections.observableArrayList(imageFile.list());
        final ComboBox<String> imageCB = new ComboBox<String>(imagesObservable);
        imageCB.getSelectionModel().select(0);

        Button envoyerMessageBtn = new Button("Envoyer");
        Button envoyerImageBtn = new Button("Envoyer Image");

        gridPane.add(toLabel, 0, 0);
        gridPane.add(messageLabel, 0, 1);
        gridPane.add(imageLabel, 0, 2);

        gridPane.add(toTF, 1, 0);
        gridPane.add(messageTA, 1, 1);
        gridPane.add(imageCB, 1, 2);

        gridPane.add(envoyerMessageBtn, 2, 1);
        gridPane.add(envoyerImageBtn, 2, 2);
        /*----------------------------------------------*/
        HBox hBox2 = new HBox(10);
        hBox2.setPadding(new Insets(10));

        final ObservableList<String> messagesObservable
                = FXCollections.observableArrayList();
        ListView messagesLV = new ListView(messagesObservable);
        messagesLV.setPrefWidth(420);

        final File selectedImageFile =
                new File("images/" + imageCB.getSelectionModel().getSelectedItem());
        Image selectedImage = new Image(selectedImageFile.toURI().toString());
        final ImageView imageView = new ImageView(selectedImage);
        imageView.setFitWidth(450);
        imageView.setFitHeight(300);
        hBox2.getChildren().addAll(messagesLV, imageView);
        /*----------------------------------------------*/
        VBox vBox = new VBox(10);
        vBox.getChildren().addAll(gridPane, hBox2);
        /*----------------------------------------------*/
        BorderPane borderPane = new BorderPane();
        borderPane.setTop(hBox);
        borderPane.setCenter(vBox);
        /*----------------------------------------------*/
        Scene scene = new Scene(borderPane, 900, 600);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
        /*----------------------------------------------*/
        connectButton.setOnAction(
                new EventHandler<ActionEvent>() {
                    public void handle(ActionEvent event) {
                        try {
                            String code = codeTF.getText();
                            String host = hostTF.getText();
                            int port = Integer.parseInt(portTF.getText());
                            String URL = "tcp://" + host + ":" + port;
                            ConnectionFactory connectionFactory =
                                    new ActiveMQConnectionFactory(URL);
                            Connection connection =
                                    connectionFactory.createConnection();
                            connection.start();
                            hBox.setDisable(true);
                            session =
                                    connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                            Destination destination =
                                    session.createTopic("chat.topic");
                            producer =
                                    session.createProducer(destination);
                            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

                            MessageConsumer consumer =
                                    session.createConsumer(destination, "code='" + code + "'");
                            consumer.setMessageListener(new MessageListener() {
                                public void onMessage(Message message) {
                                    if (message instanceof TextMessage) {
                                        try {
                                            TextMessage textMessage = (TextMessage) message;
                                            messagesObservable.add(
                                                    message.getStringProperty("producer") + " : " + textMessage.getText()
                                            );
                                        } catch (JMSException jmsException) {
                                            jmsException.printStackTrace();
                                        }
                                    } else if (message instanceof StreamMessage) {
                                        try {
                                            StreamMessage streamMessage = (StreamMessage) message;

                                            String imageName = streamMessage.readString();
                                            int imageSize = streamMessage.readInt();
                                            byte[] data = new byte[imageSize];
                                            streamMessage.readBytes(data);

                                            ByteArrayInputStream arrayInputStream =
                                                    new ByteArrayInputStream(data);

                                            Image receivedImage = new Image(arrayInputStream);

                                            messagesObservable.add("Réception d'un image : " + imageName);
                                            imageView.setImage(receivedImage);

                                        } catch (JMSException jmsException) {
                                            jmsException.printStackTrace();
                                        }
                                    }
                                }
                            });
                        } catch (JMSException jmsException) {
                            jmsException.printStackTrace();
                        }
                    }
                }
        );
        /*----------------------------------------------*/
        imageCB.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        new ChangeListener<String>() {
                            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                                File selectedImageFile =
                                        new File("images/" + newValue);
                                Image selectedImage = new Image(selectedImageFile.toURI().toString());
                                imageView.setImage(selectedImage);
                            }
                        }
                );
        /*----------------------------------------------*/
        envoyerMessageBtn.setOnAction(
                new EventHandler<ActionEvent>() {
                    public void handle(ActionEvent event) {
                        try {
                            String message = messageTA.getText();
                            TextMessage textMessage = session.createTextMessage();
                            textMessage.setStringProperty("code", toTF.getText());
                            textMessage.setStringProperty("producer", codeTF.getText());
                            textMessage.setText(message);

                            producer.send(textMessage);

                            messageTA.clear();
                            toTF.clear();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
        );
        /*----------------------------------------------*/
        envoyerImageBtn.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                try {

                    String imageName = imageCB.getSelectionModel().getSelectedItem();

                    StreamMessage streamMessage = session.createStreamMessage();
                    streamMessage.setStringProperty("code", toTF.getText());
                    streamMessage.setStringProperty("producer", codeTF.getText());

                    File receivedImage =
                            new File("images/" + imageName);
                    FileInputStream inputStream
                            = new FileInputStream(receivedImage);
                    byte[] data = new byte[(int) receivedImage.length()];
                    inputStream.read(data);

                    streamMessage.writeString(imageName);
                    streamMessage.writeInt(data.length);
                    streamMessage.writeBytes(data);

                    producer.send(streamMessage);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}