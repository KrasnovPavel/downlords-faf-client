package com.faforever.client.game;

import com.faforever.client.fxml.FxmlLoader;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.domain.GameAccess;
import com.faforever.client.map.MapService;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.Callback;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.util.regex.Pattern;

// TODO rename all Game* things to "Play" to be consistent with the menu
public class GamesController {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Predicate<GameInfoBean> OPEN_GAMES_PREDICATE = gameInfoBean -> gameInfoBean.getStatus() == GameState.OPEN;

  @FXML
  VBox teamListPane;

  @FXML
  Label mapLabel;

  @FXML
  Button createGameButton;

  @FXML
  Pane gameViewContainer;

  @FXML
  Node gamesRoot;

  @FXML
  ImageView mapImageView;

  @FXML
  Label gameTitleLabel;

  @FXML
  Label numberOfPlayersLabel;

  @FXML
  Label hosterLabel;

  @FXML
  Label gameModeLabel;

  @FXML
  VBox gamePreviewPanel;

  @FXML
  MenuButton switchViewButton;

  @Autowired
  ApplicationContext applicationContext;

  @Autowired
  I18n i18n;

  @Autowired
  GameService gameService;
  
  @Autowired
  MapService mapService;

  @Autowired
  CreateGameController createGameController;

  @Autowired
  EnterPasswordController enterPasswordController;

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  SceneFactory sceneFactory;

  @Autowired
  NotificationService notificationService;

  private Popup createGamePopup;
  private Popup passwordPopup;
  private FilteredList<GameInfoBean> filteredItems;
  Stage largePreview;

  //TODO Implement into options menu
  private boolean tilePaneSelected = false;
  private boolean firstGeneratedPane = true;
  private GameInfoBean currentGameInfoBean;

  @PostConstruct
  void postConstruct() {
    passwordPopup = new Popup();
    passwordPopup.setAutoHide(true);
    passwordPopup.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_TOP_LEFT);
    passwordPopup.getContent().setAll(enterPasswordController.getRoot());

    createGamePopup = new Popup();
    createGamePopup.setAutoFix(false);
    createGamePopup.setAutoHide(true);
    createGamePopup.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_TOP_LEFT);
    createGamePopup.getContent().setAll(createGameController.getRoot());

    enterPasswordController.setOnPasswordEnteredListener(this::doJoinGame);

    if (preferencesService.getPreferences().getForgedAlliance().getPath() == null) {
      createGameButton.setDisable(true);
      createGameButton.setTooltip(new Tooltip(i18n.get("missingGamePath.notification")));

      preferencesService.addUpdateListener(preferences -> {
        if (preferencesService.getPreferences().getForgedAlliance().getPath() != null) {
          createGameButton.setDisable(false);
          createGameButton.setTooltip(null);
        }
      });
    }

    filteredItems = new FilteredList<>(gameService.getGameInfoBeans());
    filteredItems.setPredicate(OPEN_GAMES_PREDICATE);

    // FIXME remove after switching between tiles/tables is implemented
    onDetailsButtonPressed();
    switchViewButton.setVisible(false);
    switchViewButton.setManaged(false);
  }

  public void displayGameDetail(GameInfoBean gameInfoBean) {
    currentGameInfoBean = gameInfoBean;
    mapImageView.setImage(mapService.loadLargePreview(gameInfoBean.getMapName()));
    gameTitleLabel.setText(gameInfoBean.getTitle());
    numberOfPlayersLabel.setText(i18n.get("game.detail.players.format", gameInfoBean.getNumPlayers(), gameInfoBean.getMaxPlayers()));
    hosterLabel.setText(gameInfoBean.getHost());
    gameModeLabel.setText(gameInfoBean.getFeaturedMod());
    mapLabel.setText(gameInfoBean.getMapName());
    createTeam(gameInfoBean.getTeams());
  }

  private void createTeam(ObservableMap<? extends String, ? extends List<String>> teamsList) {
    teamListPane.getChildren().clear();
    for (Map.Entry<? extends String, ? extends List<String>> entry : teamsList.entrySet()) {
      TeamCardController teamCardController = applicationContext.getBean(TeamCardController.class);
      boolean teamCardSuccess = teamCardController.setTeam(entry.getValue(), Integer.parseInt(entry.getKey()));
      if (teamCardSuccess) {
        teamListPane.getChildren().add(teamCardController.getRoot());
      }
    }
  }

  @FXML
  void onShowPrivateGames(ActionEvent actionEvent) {
    CheckBox checkBox = (CheckBox) actionEvent.getSource();
    boolean selected = checkBox.isSelected();
    if (selected) {
      filteredItems.setPredicate(OPEN_GAMES_PREDICATE);
    } else {
      filteredItems.setPredicate(
          OPEN_GAMES_PREDICATE.and(
              gameInfoBean -> gameInfoBean.getAccess() != GameAccess.PASSWORD)
      );
    }
  }

  public void onJoinGame(GameInfoBean gameInfoBean, String password, double screenX, double screenY) {
    PlayerInfoBean currentPlayer = playerService.getCurrentPlayer();
    int playerRating = RatingUtil.getRating(currentPlayer);

    if ((playerRating < gameInfoBean.getMinRating() || playerRating > gameInfoBean.getMaxRating())) {
      showRatingOutOfBoundsConfirmation(playerRating, gameInfoBean, screenX, screenY);
    } else {
      doJoinGame(gameInfoBean, password, screenX, screenY);
    }
  }

  private void showRatingOutOfBoundsConfirmation(int playerRating, GameInfoBean gameInfoBean, double screenX, double screenY) {
    notificationService.addNotification(new ImmediateNotification(
        i18n.get("game.joinGameRatingConfirmation.title"),
        i18n.get("game.joinGameRatingConfirmation.text", gameInfoBean.getMinRating(), gameInfoBean.getMaxRating(), playerRating),
        Severity.INFO,
        Arrays.asList(
            new Action(i18n.get("game.join"), event -> doJoinGame(gameInfoBean, null, screenX, screenY)),
            new Action(i18n.get("game.cancel"))
        )
    ));
  }

  private void doJoinGame(GameInfoBean gameInfoBean, String password, double screenX, double screenY) {
    // FIXME check if game path is set

    if (gameInfoBean.getAccess() == GameAccess.PASSWORD && password == null) {
      enterPasswordController.setGameInfoBean(gameInfoBean);
      passwordPopup.show(gamesRoot.getScene().getWindow(), screenX, screenY);
    } else {
      gameService.joinGame(gameInfoBean, password, new Callback<Void>() {
        @Override
        public void success(Void result) {
          // Cool.
        }

        @Override
        public void error(Throwable e) {
          // FIXME implement
          logger.warn("Game could not be joined", e);
        }
      });
    }
  }

  public boolean isFirstGeneratedPane() {
    return firstGeneratedPane;
  }

  @FXML
  void onTilesButtonPressed() {
    if (!tilePaneSelected || isFirstGeneratedPane()) {
      GamesTiledController gamesTiledController = applicationContext.getBean(GamesTiledController.class);
      gamesTiledController.createTiledFlowPane(filteredItems);

      Node root = gamesTiledController.getRoot();
      populateContainer(root);
      firstGeneratedPane = false;
      tilePaneSelected = true;
    }
  }

  @FXML
  void onDetailsButtonPressed() {
    if (tilePaneSelected || isFirstGeneratedPane()) {
      GameTableController gameTableController = applicationContext.getBean(GameTableController.class);
      gameTableController.initializeGameTable(filteredItems);

      Node root = gameTableController.getRoot();
      populateContainer(root);
      firstGeneratedPane = false;
      tilePaneSelected = false;
    }
  }

  private void populateContainer(Node root) {
    gameViewContainer.getChildren().setAll(root);
    AnchorPane.setBottomAnchor(root, 0d);
    AnchorPane.setLeftAnchor(root, 0d);
    AnchorPane.setRightAnchor(root, 0d);
    AnchorPane.setTopAnchor(root, 0d);
  }

  public void setUpIfNecessary() {
  }

  public Node getRoot() {
    return gamesRoot;
  }

  public void onCreateGameButtonClicked(ActionEvent actionEvent) {
    Button button = (Button) actionEvent.getSource();

    Bounds screenBounds = createGameButton.localToScreen(createGameButton.getBoundsInLocal());
    createGamePopup.show(button.getScene().getWindow(), screenBounds.getMinX(), screenBounds.getMaxY());
  }

  //TODO do we want to create new pane or repopulate the same pane
  public void onMapLargePreview(Event event) {
    if (currentGameInfoBean == null) {
      return;
    }
    //FIXME official maps need to be checked TEMPORARY
    if (mapService.isOfficialMap(currentGameInfoBean.getMapName())) {
      return;
    }
    largePreview = new Stage(StageStyle.TRANSPARENT);
    largePreview.initModality(Modality.NONE);
    largePreview.initOwner(getRoot().getScene().getWindow());

    MapPreviewLargeController mapPreviewLargeController = applicationContext.getBean(MapPreviewLargeController.class);
    MapInfoBean mapInfoBean = mapService.getMapInfoBeanFromVaultFromName(currentGameInfoBean.getMapName());
    mapPreviewLargeController.createPreview(mapInfoBean);

    sceneFactory.createScene(largePreview, mapPreviewLargeController.getRoot(), false, WindowDecorator.WindowButtonType.CLOSE);
    largePreview.show();
    largePreview.toFront();
  }
}
